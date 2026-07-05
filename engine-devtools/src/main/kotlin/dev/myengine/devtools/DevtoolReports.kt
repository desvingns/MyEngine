package dev.myengine.devtools

import dev.myengine.content.ContentPackLoader
import dev.myengine.content.ContentRegistry
import dev.myengine.games.sandbox.SandboxGame
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.math.abs

data class HeadlessScenarioReport(
    val scenario: String,
    val packId: String,
    val ticks: Long,
    val finalHash: String,
    val enemiesSpawned: Int,
    val enemiesKilled: Int,
    val enemiesLeaked: Int,
    val coreDamage: Int,
    val towerShots: Int,
    val simMs: Long,
) {
    fun toJson(): String = buildJson(
        "scenario" to scenario,
        "pack_id" to packId,
        "ticks" to ticks,
        "final_hash" to finalHash,
        "enemies_spawned" to enemiesSpawned,
        "enemies_killed" to enemiesKilled,
        "enemies_leaked" to enemiesLeaked,
        "core_damage" to coreDamage,
        "tower_shots" to towerShots,
        "sim_ms" to simMs,
    )
}

data class ContentReport(
    val packId: String?,
    val valid: Boolean,
    val errors: List<String>,
    val ids: Map<String, List<String>>,
) {
    fun toJson(): String {
        val idJson = ids.toSortedMap().entries.joinToString(",") { (key, values) ->
            "\"${escape(key)}\":[${values.sorted().joinToString(",") { "\"${escape(it)}\"" }}]"
        }
        return "{\"pack_id\":${packId.jsonValue()},\"valid\":$valid,\"errors\":[${errors.joinToString(",") { "\"${escape(it)}\"" }}],\"ids\":{$idJson}}"
    }
}

data class PackValidation(val root: String, val report: ContentReport)

data class AggregateContentReport(val results: List<PackValidation>) {
    val valid: Boolean get() = results.isNotEmpty() && results.all { it.report.valid }

    fun toJson(): String {
        val items = results.joinToString(",") { pv ->
            "{\"root\":\"${escape(pv.root)}\"," +
                "\"pack_id\":${pv.report.packId.jsonValue()}," +
                "\"valid\":${pv.report.valid}," +
                "\"errors\":[${pv.report.errors.joinToString(",") { "\"${escape(it)}\"" }}]}"
        }
        return "{\"valid\":$valid,\"pack_count\":${results.size},\"packs\":[$items]}"
    }
}

data class BalancePackSummary(
    val packId: String,
    val enemyTypes: Int,
    val waveEnemies: Int,
    val enemyHealthTotal: Int,
    val coreDamagePotential: Int,
    val rewardTotal: Int,
    val resourceTypes: Int,
    val recipeOutputPerTick: Double,
) {
    fun toJson(): String = buildJson(
        "pack_id" to packId,
        "enemy_types" to enemyTypes,
        "wave_enemies" to waveEnemies,
        "enemy_health_total" to enemyHealthTotal,
        "core_damage_potential" to coreDamagePotential,
        "reward_total" to rewardTotal,
        "resource_types" to resourceTypes,
        "recipe_output_per_tick" to recipeOutputPerTick,
    )
}

data class BalanceMetricDelta(
    val category: String,
    val metric: String,
    val baseline: Double,
    val changed: Double,
    val delta: Double,
    val percentDelta: Double?,
    val flagged: Boolean,
) {
    fun toJson(): String = buildJson(
        "category" to category,
        "metric" to metric,
        "baseline" to baseline,
        "changed" to changed,
        "delta" to delta,
        "percent_delta" to percentDelta,
        "flagged" to flagged,
    )
}

data class BalanceWarning(
    val category: String,
    val metric: String,
    val message: String,
) {
    fun toJson(): String = buildJson(
        "category" to category,
        "metric" to metric,
        "message" to message,
    )
}

data class BalanceDeltaReport(
    val baselineRoot: String,
    val changedRoot: String,
    val valid: Boolean,
    val largePercentDelta: Double,
    val largeAbsoluteDelta: Double,
    val baseline: BalancePackSummary?,
    val changed: BalancePackSummary?,
    val deltas: List<BalanceMetricDelta>,
    val warnings: List<BalanceWarning>,
    val errors: List<String>,
) {
    fun toJson(): String {
        val deltaJson = deltas.joinToString(",") { it.toJson() }
        val warningJson = warnings.joinToString(",") { it.toJson() }
        val errorJson = errors.joinToString(",") { "\"${escape(it)}\"" }
        return "{\"baseline_root\":\"${escape(baselineRoot)}\"," +
            "\"changed_root\":\"${escape(changedRoot)}\"," +
            "\"valid\":$valid," +
            "\"thresholds\":${buildJson("large_percent_delta" to largePercentDelta, "large_absolute_delta" to largeAbsoluteDelta)}," +
            "\"baseline\":${baseline?.toJson() ?: "null"}," +
            "\"changed\":${changed?.toJson() ?: "null"}," +
            "\"deltas\":[$deltaJson]," +
            "\"warnings\":[$warningJson]," +
            "\"errors\":[$errorJson]}"
    }
}

object DevtoolReports {
    private const val LARGE_PERCENT_DELTA = 0.25
    private const val LARGE_ABSOLUTE_DELTA = 5.0

    /** Walk up from [start] to find the repo root (first ancestor holding a `games/` directory). */
    fun repoRoot(start: Path = Paths.get("").toAbsolutePath()): Path =
        generateSequence(start) { it.parent }.take(8)
            .firstOrNull { Files.isDirectory(it.resolve("games")) } ?: start

    /** Every content pack root under games/<game>/content/<pack>. */
    fun discoverPackRoots(repoRoot: Path = repoRoot()): List<Path> {
        val gamesDir = repoRoot.resolve("games")
        if (!Files.isDirectory(gamesDir)) return emptyList()
        return Files.newDirectoryStream(gamesDir).use { gameDirs ->
            gameDirs.filter { Files.isDirectory(it) }.flatMap { game ->
                val contentDir = game.resolve("content")
                if (Files.isDirectory(contentDir)) {
                    Files.newDirectoryStream(contentDir).use { packs ->
                        packs.filter { Files.isDirectory(it) }.toList()
                    }
                } else {
                    emptyList()
                }
            }
        }
    }

    /** Validate every discovered pack and aggregate into one report, ordered by repo-relative root. */
    fun contentReportAll(repoRoot: Path = repoRoot()): AggregateContentReport =
        AggregateContentReport(
            discoverPackRoots(repoRoot)
                .map { root ->
                    val relative = repoRoot.relativize(root).toString().replace('\\', '/')
                    PackValidation(relative, contentReport(root))
                }
                // Sort on the emitted forward-slash key so ordering is stable across OSes.
                .sortedBy { it.root },
        )

    /** Canonical (no-kill) scenario report; its hash is the long-standing baseline. */
    fun runSandboxScenario(): HeadlessScenarioReport =
        scenarioReport("canonical") { SandboxGame.runScriptedScenario() }

    /** Kill-bearing scenario report; exercises the tower-kill + reward-deposit path. */
    fun runSandboxKillScenario(): HeadlessScenarioReport =
        scenarioReport("kill") { SandboxGame.runScriptedKillScenario() }

    /** Both canonical scenarios reported together so the default gate covers kills+rewards. */
    fun runScenarioSuite(): String {
        val reports = listOf(runSandboxScenario(), runSandboxKillScenario())
        return "{\"scenarios\":[${reports.joinToString(",") { it.toJson() }}]}"
    }

    fun balanceDeltaReport(
        baselineRoot: Path = SandboxGame.contentRoot(),
        changedRoot: Path = repoRoot().resolve("games/signal-garden/content/signal-garden"),
    ): BalanceDeltaReport {
        val baselineLoad = ContentPackLoader.load(baselineRoot)
        val changedLoad = ContentPackLoader.load(changedRoot)
        val errors = buildList {
            addAll(baselineLoad.errors.map { "baseline:$it" })
            addAll(changedLoad.errors.map { "changed:$it" })
        }
        val baseline = baselineLoad.registry?.let(::summarizeBalance)
        val changed = changedLoad.registry?.let(::summarizeBalance)
        val deltas = if (baseline != null && changed != null) balanceDeltas(baseline, changed) else emptyList()
        val warnings = deltas.filter { it.flagged }.map { delta ->
            BalanceWarning(
                category = delta.category,
                metric = delta.metric,
                message = "${delta.metric} changed from ${delta.baseline} to ${delta.changed}",
            )
        }
        return BalanceDeltaReport(
            baselineRoot = baselineRoot.toString().replace('\\', '/'),
            changedRoot = changedRoot.toString().replace('\\', '/'),
            valid = errors.isEmpty() && baseline != null && changed != null,
            largePercentDelta = LARGE_PERCENT_DELTA,
            largeAbsoluteDelta = LARGE_ABSOLUTE_DELTA,
            baseline = baseline,
            changed = changed,
            deltas = deltas,
            warnings = warnings,
            errors = errors,
        )
    }

    private fun scenarioReport(name: String, run: () -> dev.myengine.games.sandbox.SandboxScenarioResult): HeadlessScenarioReport {
        val started = System.nanoTime()
        val result = run()
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        val metrics = result.metrics
        return HeadlessScenarioReport(
            scenario = name,
            packId = SandboxGame.loadRegistry().manifest.id,
            ticks = result.snapshot.debug.tick.value,
            finalHash = result.hash,
            enemiesSpawned = metrics.enemiesSpawned,
            enemiesKilled = metrics.enemiesKilled,
            enemiesLeaked = metrics.enemiesLeaked,
            coreDamage = metrics.coreDamage,
            towerShots = metrics.towerShots,
            simMs = elapsedMs,
        )
    }

    private fun summarizeBalance(registry: ContentRegistry): BalancePackSummary {
        val waveEnemyCounts = registry.waves.values.flatMap { it.spawns }
            .groupingBy { it.enemyId }
            .fold(0) { count, spawn -> count + spawn.count }
        val waveEnemies = waveEnemyCounts.values.sum()
        val enemyHealthTotal = waveEnemyCounts.entries.sumOf { (enemyId, count) ->
            registry.enemies.getValue(enemyId).health * count
        }
        val coreDamagePotential = waveEnemyCounts.entries.sumOf { (enemyId, count) ->
            registry.enemies.getValue(enemyId).coreDamage * count
        }
        val rewardTotal = waveEnemyCounts.entries.sumOf { (enemyId, count) ->
            registry.enemies.getValue(enemyId).rewardAmount * count
        }
        val recipeOutputPerTick = registry.recipes.values.sumOf { recipe ->
            recipe.outputAmount.toDouble() / recipe.durationTicks.toDouble()
        }
        return BalancePackSummary(
            packId = registry.manifest.id,
            enemyTypes = registry.enemies.size,
            waveEnemies = waveEnemies,
            enemyHealthTotal = enemyHealthTotal,
            coreDamagePotential = coreDamagePotential,
            rewardTotal = rewardTotal,
            resourceTypes = registry.resources.size,
            recipeOutputPerTick = recipeOutputPerTick,
        )
    }

    private fun balanceDeltas(baseline: BalancePackSummary, changed: BalancePackSummary): List<BalanceMetricDelta> =
        listOf(
            delta("enemy", "enemy_types", baseline.enemyTypes.toDouble(), changed.enemyTypes.toDouble()),
            delta("enemy", "wave_enemies", baseline.waveEnemies.toDouble(), changed.waveEnemies.toDouble()),
            delta("enemy", "enemy_health_total", baseline.enemyHealthTotal.toDouble(), changed.enemyHealthTotal.toDouble()),
            delta("core", "core_damage_potential", baseline.coreDamagePotential.toDouble(), changed.coreDamagePotential.toDouble()),
            delta("resource", "reward_total", baseline.rewardTotal.toDouble(), changed.rewardTotal.toDouble()),
            delta("resource", "resource_types", baseline.resourceTypes.toDouble(), changed.resourceTypes.toDouble()),
            delta("resource", "recipe_output_per_tick", baseline.recipeOutputPerTick, changed.recipeOutputPerTick),
        )

    private fun delta(category: String, metric: String, baseline: Double, changed: Double): BalanceMetricDelta {
        val absolute = changed - baseline
        val percent = if (baseline == 0.0) null else absolute / baseline
        val flagged = abs(absolute) >= LARGE_ABSOLUTE_DELTA ||
            (percent != null && abs(percent) >= LARGE_PERCENT_DELTA)
        return BalanceMetricDelta(
            category = category,
            metric = metric,
            baseline = baseline,
            changed = changed,
            delta = absolute,
            percentDelta = percent,
            flagged = flagged,
        )
    }

    fun contentReport(root: Path = SandboxGame.contentRoot()): ContentReport {
        val result = ContentPackLoader.load(root)
        val registry = result.registry
        return ContentReport(
            packId = registry?.manifest?.id,
            valid = result.isValid,
            errors = result.errors.map { it.toString() },
            ids = if (registry == null) {
                emptyMap()
            } else {
                mapOf(
                    "tiles" to registry.tiles.keys.toList(),
                    "resources" to registry.resources.keys.toList(),
                    "towers" to registry.towers.keys.toList(),
                    "enemies" to registry.enemies.keys.toList(),
                    "recipes" to registry.recipes.keys.toList(),
                    "waves" to registry.waves.keys.toList(),
                    "incidents" to registry.incidents.keys.toList(),
                )
            },
        )
    }

    fun replayInspect(): String {
        val canonical = replayScenarioJson("canonical", "1:build_tower:pulse:30:32", SandboxGame.runScriptedScenario())
        val kill = replayScenarioJson("kill", "1:build_tower:pulse:2:2", SandboxGame.runScriptedKillScenario())
        return "{\"scenarios\":[$canonical,$kill]}"
    }

    private fun replayScenarioJson(
        name: String,
        commands: String,
        result: dev.myengine.games.sandbox.SandboxScenarioResult,
    ): String = buildJson(
        "scenario" to name,
        "commands" to commands,
        "final_hash" to result.hash,
        "tick" to result.snapshot.debug.tick.value,
        "save_bytes" to result.saveText.length,
        "enemies_killed" to result.metrics.enemiesKilled,
    )
}

fun buildJson(vararg values: Pair<String, Any?>): String =
    values.joinToString(prefix = "{", postfix = "}") { (key, value) ->
        "\"${escape(key)}\":${value.jsonValue()}"
    }

fun Any?.jsonValue(): String = when (this) {
    null -> "null"
    is Number, is Boolean -> toString()
    else -> "\"${escape(toString())}\""
}

fun escape(text: String): String =
    text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
