package dev.myengine.devtools

import dev.myengine.content.ContentPackLoader
import dev.myengine.games.sandbox.SandboxGame
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

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

object DevtoolReports {
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
