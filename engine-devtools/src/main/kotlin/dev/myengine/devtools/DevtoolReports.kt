package dev.myengine.devtools

import dev.myengine.ai.GoalField
import dev.myengine.content.ContentPackLoader
import dev.myengine.content.ContentRegistry
import dev.myengine.content.ContentValidationError
import dev.myengine.content.EndlessWaveGenerator
import dev.myengine.content.EffectiveEnemyStats
import dev.myengine.content.EnemyContent
import dev.myengine.content.TowerContent
import dev.myengine.content.TowerUpgradeTier
import dev.myengine.content.WaveModifier
import dev.myengine.content.WaveContent
import dev.myengine.content.effectiveStats
import dev.myengine.core.Tick
import dev.myengine.core.SeededRandom
import dev.myengine.defense.DamageFormula
import dev.myengine.defense.DefenseRuntime
import dev.myengine.entities.Entity
import dev.myengine.entities.EntityStore
import dev.myengine.entities.HealthComponent
import dev.myengine.entities.MovementComponent
import dev.myengine.entities.PositionComponent
import dev.myengine.entities.TowerComponent
import dev.myengine.games.sandbox.SandboxGame
import dev.myengine.world.TilePosition
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

private const val BALANCE_TICKS_PER_SECOND = 20
private const val RESIST_REPLAY_GOLDEN_HASH = "3f02607020d48668"
private val EFFECTIVE_DPS_ASSUMPTIONS = RawJson(
    "{\"targeting\":\"single-target\",\"range\":\"in-range\",\"splash\":false,\"ticks_per_second\":$BALANCE_TICKS_PER_SECOND}",
)

data class EffectiveDpsRow(
    val towerId: String,
    val profileId: String,
    val upgradeBranch: String?,
    val upgradeTier: Int,
    val enemyId: String,
    val damageTypeId: String?,
    val resistPercent: Int,
    val effectiveDamagePerShot: Int,
    val cooldownTicks: Int,
    val ticksPerSecond: Int = BALANCE_TICKS_PER_SECOND,
    val effectiveDps: BigDecimal,
) {
    fun toJson(): String = buildJson(
        "tower_id" to towerId,
        "profile_id" to profileId,
        "upgrade_branch" to upgradeBranch,
        "upgrade_tier" to upgradeTier,
        "enemy_id" to enemyId,
        "damage_type_id" to damageTypeId,
        "resist_percent" to resistPercent,
        "effective_damage_per_shot" to effectiveDamagePerShot,
        "cooldown_ticks" to cooldownTicks,
        "ticks_per_second" to ticksPerSecond,
        "effective_dps" to effectiveDps,
    )
}

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

/** Advisory rebuild measurement for the canonical 64x64 sandbox map. */
data class GoalFieldRebuildReport(
    val width: Int,
    val height: Int,
    val reachableTiles: Int,
    val rebuildNanos: Long,
) {
    fun toJson(): String = buildJson(
        "width" to width,
        "height" to height,
        "reachable_tiles" to reachableTiles,
        "rebuild_ns" to rebuildNanos,
    )
}

data class SpatialIndexBenchmarkReport(
    val scenario: String,
    val enemyCount: Int,
    val concurrentEnemies: Int,
    val towerCount: Int,
    val ticks: Int,
    val queryCount: Int,
    val towerShots: Int,
    val aliveEnemiesAfter: Int,
    val elapsedNanos: Long,
) {
    fun toJson(): String = buildJson(
        "scenario" to scenario,
        "enemy_count" to enemyCount,
        "concurrent_enemies" to concurrentEnemies,
        "tower_count" to towerCount,
        "ticks" to ticks,
        "query_count" to queryCount,
        "tower_shots" to towerShots,
        "alive_enemies_after" to aliveEnemiesAfter,
        "elapsed_ns" to elapsedNanos,
        "sim_ms" to elapsedNanos / 1_000_000.0,
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

data class EndlessWaveScalingRow(
    val waveNumber: Int,
    val waveId: String,
    val startTick: Long,
    val composition: Map<String, Int>,
    val enemyCount: Long,
    val totalHealth: Long,
    val totalReward: Long,
)

data class EndlessWaveScalingReport(
    val packId: String?,
    val valid: Boolean,
    val waveCount: Int,
    val seed: Long,
    val rows: List<EndlessWaveScalingRow>,
    val errors: List<String>,
) {
    fun toJson(): String {
        val rowJson = rows.joinToString(",") { row ->
            val composition = row.composition.toSortedMap().entries.joinToString(",") { (enemyId, count) ->
                "\"${escape(enemyId)}\":$count"
            }
            "{\"wave_number\":${row.waveNumber}," +
                "\"wave_id\":\"${escape(row.waveId)}\"," +
                "\"start_tick\":${row.startTick}," +
                "\"composition\":{$composition}," +
                "\"enemy_count\":${row.enemyCount}," +
                "\"total_health\":${row.totalHealth}," +
                "\"total_reward\":${row.totalReward}}"
        }
        val errorJson = errors.joinToString(",") { "\"${escape(it)}\"" }
        return "{\"pack_id\":${packId.jsonValue()}," +
            "\"valid\":$valid," +
            "\"wave_count\":$waveCount," +
            "\"seed\":$seed," +
            "\"rows\":[$rowJson]," +
            "\"errors\":[$errorJson]}"
    }
}

data class BalancePackSummary(
    val packId: String,
    val enemyTypes: Int,
    val eliteEnemyTypes: Int,
    val bossEnemyTypes: Int,
    val waveEnemies: Int,
    val eliteWaveEnemies: Int,
    val bossWaveEnemies: Int,
    val enemyHealthTotal: Int,
    val coreDamagePotential: Int,
    val rewardTotal: Int,
    val resourceTypes: Int,
    val recipeOutputPerTick: Double,
    /** Number of tower definitions declaring content-defined splash behavior. */
    val splashTowerTypes: Int,
    /** Sum of declared splash radii across splash tower definitions. */
    val splashRadiusTotal: Int,
    /** Sum of declared per-ring splash falloff percentages across splash tower definitions. */
    val splashFalloffPercentTotal: Int,
    /** Total non-zero-damage Manhattan tiles across splash tower definitions. */
    val splashEffectiveAoeTiles: Int,
    /** Single-target, in-range, no-splash DPS matrix at the fixed simulation tick rate. */
    val effectiveDpsRows: List<EffectiveDpsRow> = emptyList(),
) {
    fun toJson(): String = buildJson(
        "pack_id" to packId,
        "enemy_types" to enemyTypes,
        "elite_enemy_types" to eliteEnemyTypes,
        "boss_enemy_types" to bossEnemyTypes,
        "wave_enemies" to waveEnemies,
        "elite_wave_enemies" to eliteWaveEnemies,
        "boss_wave_enemies" to bossWaveEnemies,
        "enemy_health_total" to enemyHealthTotal,
        "core_damage_potential" to coreDamagePotential,
        "reward_total" to rewardTotal,
        "resource_types" to resourceTypes,
        "recipe_output_per_tick" to recipeOutputPerTick,
        "splash_tower_types" to splashTowerTypes,
        "splash_radius_total" to splashRadiusTotal,
        "splash_falloff_percent_total" to splashFalloffPercentTotal,
        "splash_effective_aoe_tiles" to splashEffectiveAoeTiles,
        "effective_dps_assumptions" to EFFECTIVE_DPS_ASSUMPTIONS,
        "assumptions" to EFFECTIVE_DPS_ASSUMPTIONS,
        "ticks_per_second" to BALANCE_TICKS_PER_SECOND,
        "effective_dps_rows" to RawJson(effectiveDpsRows.joinToString(prefix = "[", postfix = "]") { it.toJson() }),
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

    /** Emits a deterministic, machine-readable curve for an endless pack without mutating a runtime RNG. */
    fun endlessWaveScalingReport(
        root: Path = SandboxGame.contentRoot(),
        waveCount: Int = 10,
        seed: Long = 7L,
    ): EndlessWaveScalingReport {
        require(waveCount > 0) { "Endless scaling report requires a positive wave count." }
        val load = ContentPackLoader.load(root)
        val registry = load.registry
        val config = registry?.endlessWave
        if (!load.isValid || registry == null || config == null) {
            val errors = if (load.errors.isNotEmpty()) {
                load.errors.map(ContentValidationError::toString)
            } else {
                listOf("Content pack does not declare endless.properties.")
            }
            return EndlessWaveScalingReport(
                packId = registry?.manifest?.id,
                valid = false,
                waveCount = waveCount,
                seed = seed,
                rows = emptyList(),
                errors = errors,
            )
        }
        val random = SeededRandom(seed)
        val rows = (1..waveCount).map { waveNumber ->
            val wave = EndlessWaveGenerator.generate(config, waveNumber, random)
            val composition = linkedMapOf<String, Int>()
            var enemyCount = 0L
            var totalHealth = 0L
            var totalReward = 0L
            wave.spawns.forEach { spawn ->
                composition[spawn.enemyId] = saturatedAdd(composition[spawn.enemyId]?.toLong() ?: 0L, spawn.count.toLong())
                    .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                val enemy = registry.requireEnemy(spawn.enemyId)
                val stats = enemy.effectiveStats(
                    waveHealthPercent = wave.healthScalePercent,
                    waveRewardPercent = wave.rewardScalePercent,
                )
                enemyCount = saturatedAdd(enemyCount, spawn.count.toLong())
                totalHealth = saturatedAdd(totalHealth, saturatedMultiply(stats.health.toLong(), spawn.count.toLong()))
                totalReward = saturatedAdd(totalReward, saturatedMultiply(stats.rewardAmount.toLong(), spawn.count.toLong()))
            }
            EndlessWaveScalingRow(
                waveNumber = waveNumber,
                waveId = wave.id,
                startTick = wave.startTick,
                composition = composition.toSortedMap(),
                enemyCount = enemyCount,
                totalHealth = totalHealth,
                totalReward = totalReward,
            )
        }
        return EndlessWaveScalingReport(
            packId = registry.manifest.id,
            valid = true,
            waveCount = waveCount,
            seed = seed,
            rows = rows,
            errors = emptyList(),
        )
    }

    /** Canonical (no-kill) scenario report; its hash is the long-standing baseline. */
    fun runSandboxScenario(): HeadlessScenarioReport =
        scenarioReport("canonical") { SandboxGame.runScriptedScenario() }

    /** Kill-bearing scenario report; exercises the tower-kill + reward-deposit path. */
    fun runSandboxKillScenario(): HeadlessScenarioReport =
        scenarioReport("kill") { SandboxGame.runScriptedKillScenario() }

    /** Both canonical scenarios reported together so the default gate covers kills+rewards. */
    fun runScenarioSuite(): String {
        val reports = listOf(runSandboxScenario(), runSandboxKillScenario())
        return "{\"scenarios\":[${reports.joinToString(",") { it.toJson() }}]," +
            "\"goal_field_rebuild\":${goalFieldRebuildBenchmark().toJson()}," +
            "\"spatial_index\":${spatialIndexBenchmark().toJson()}}"
    }

    /** Deterministic one-tick workload with at least 1,000 concurrent enemies. */
    fun spatialIndexBenchmark(
        enemyCount: Int = 1_024,
        towerCount: Int = 16,
    ): SpatialIndexBenchmarkReport {
        require(enemyCount >= 1_000) { "Spatial benchmark requires at least 1,000 enemies." }
        require(towerCount > 0) { "Spatial benchmark requires at least one tower." }

        val state = SandboxGame.createInitialState()
        val registry = state.registry
        val map = registry.requireMap(state.mapId)
        val core = TilePosition(map.core.x, map.core.y)
        val goalField = GoalField.build(state.world, core)
        val positions = state.world.positions().filter(goalField::canReach)
        check(positions.isNotEmpty()) { "Spatial benchmark requires reachable map positions." }
        val enemy = registry.enemies.values.sortedBy { it.id }.first()
        val tower = registry.towers.values.sortedBy { it.id }.first()
        val entities = EntityStore()

        repeat(enemyCount) { index ->
            val position = positions[index % positions.size]
            entities.create("enemy:${enemy.id}", setOf("enemy")) { id ->
                Entity(
                    id = id,
                    type = "enemy:${enemy.id}",
                    tags = setOf("enemy"),
                    position = PositionComponent(position),
                    health = HealthComponent(enemy.health, enemy.health),
                    movement = MovementComponent(),
                )
            }
        }
        repeat(towerCount) { index ->
            val position = positions[(index * 7) % positions.size]
            entities.create("tower:${tower.id}", setOf("tower")) { id ->
                Entity(
                    id = id,
                    type = "tower:${tower.id}",
                    tags = setOf("tower"),
                    position = PositionComponent(position),
                    tower = TowerComponent(tower.id, targetingMode = tower.targetingMode),
                    attack = dev.myengine.entities.AttackComponent(
                        range = tower.range,
                        damage = tower.damage,
                        cooldownTicks = tower.cooldownTicks,
                    ),
                )
            }
        }

        val started = System.nanoTime()
        val result = DefenseRuntime().updateTowers(registry, entities, goalField, Tick(1))
        val elapsedNanos = System.nanoTime() - started
        return SpatialIndexBenchmarkReport(
            scenario = "spatial-index-1k",
            enemyCount = enemyCount,
            concurrentEnemies = enemyCount,
            towerCount = towerCount,
            ticks = 1,
            queryCount = towerCount,
            towerShots = result.metrics.towerShots,
            aliveEnemiesAfter = entities.byTag("enemy").count { it.health?.isAlive() == true },
            elapsedNanos = elapsedNanos,
        )
    }

    /**
     * Rebuilds the sandbox's canonical 64x64 field once and emits its wall-clock cost.  This is a
     * metric, not a pass/fail budget: it feeds the future performance-baseline work.
     */
    fun goalFieldRebuildBenchmark(): GoalFieldRebuildReport {
        val state = SandboxGame.createInitialState()
        val map = state.registry.requireMap(state.mapId)
        val core = TilePosition(map.core.x, map.core.y)
        check(state.world.size.width == 64 && state.world.size.height == 64) {
            "Goal-field benchmark requires the canonical 64x64 sandbox map."
        }
        val started = System.nanoTime()
        val field = GoalField.build(state.world, core)
        val elapsedNanos = System.nanoTime() - started
        return GoalFieldRebuildReport(
            width = state.world.size.width,
            height = state.world.size.height,
            reachableTiles = field.reachableTileCount,
            rebuildNanos = elapsedNanos,
        )
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
        val waveEntries = registry.waves.values.flatMap { wave -> effectiveWaveEntries(registry, wave) }
        val waveEnemies = waveEntries.size
        val eliteWaveEnemies = waveEntries.count { it.first.isElite }
        val bossWaveEnemies = waveEntries.count { it.first.isBoss }
        val enemyHealthTotal = waveEntries.sumOf { it.second.health.toLong() }
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val coreDamagePotential = waveEntries.sumOf { it.first.coreDamage.toLong() }
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val rewardTotal = waveEntries.sumOf { it.second.rewardAmount.toLong() }
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val recipeOutputPerTick = registry.recipes.values.sumOf { recipe ->
            recipe.outputAmount.toDouble() / recipe.durationTicks.toDouble()
        }
        val splashTowers = registry.towers.values.filter { it.splashRadius != null }
        return BalancePackSummary(
            packId = registry.manifest.id,
            enemyTypes = registry.enemies.size,
            eliteEnemyTypes = registry.enemies.values.count { it.isElite },
            bossEnemyTypes = registry.enemies.values.count { it.isBoss },
            waveEnemies = waveEnemies,
            eliteWaveEnemies = eliteWaveEnemies,
            bossWaveEnemies = bossWaveEnemies,
            enemyHealthTotal = enemyHealthTotal,
            coreDamagePotential = coreDamagePotential,
            rewardTotal = rewardTotal,
            resourceTypes = registry.resources.size,
            recipeOutputPerTick = recipeOutputPerTick,
            splashTowerTypes = splashTowers.size,
            splashRadiusTotal = splashTowers.sumOf { it.splashRadius!! },
            splashFalloffPercentTotal = splashTowers.sumOf { it.falloffPercent },
            splashEffectiveAoeTiles = splashTowers.sumOf(::effectiveSplashAoeTiles),
            effectiveDpsRows = effectiveDpsRows(registry),
        )
    }

    private fun effectiveDpsRows(registry: ContentRegistry): List<EffectiveDpsRow> {
        val profiles = registry.towers.values
            .sortedBy(TowerContent::id)
            .flatMap { tower ->
                val base = TowerBalanceProfile(
                    towerId = tower.id,
                    profileId = tower.id,
                    upgradeBranch = null,
                    upgradeTier = 0,
                    damage = tower.damage,
                    cooldownTicks = tower.cooldownTicks,
                    damageTypeId = tower.damageTypeId,
                )
                val upgrades = tower.upgradeTiers.values
                    .sortedWith(compareBy<TowerUpgradeTier> { it.branch }.thenBy { it.tier })
                    .map { tier ->
                        TowerBalanceProfile(
                            towerId = tower.id,
                            profileId = "${tower.id}.upgrade.${tier.branch}.${tier.tier}",
                            upgradeBranch = tier.branch,
                            upgradeTier = tier.tier,
                            damage = tier.damage,
                            cooldownTicks = tier.cooldownTicks,
                            damageTypeId = tower.damageTypeId,
                        )
                    }
                listOf(base) + upgrades
            }
        return profiles
            .sortedBy { it.profileId }
            .flatMap { profile ->
                registry.enemies.values.sortedBy { it.id }.map { enemy ->
                    val resistPercent = profile.damageTypeId?.let(enemy.resists::get) ?: 0
                    val effectiveDamage = DamageFormula.effectiveDamage(
                        baseDamage = profile.damage,
                        distance = 0,
                        falloffPercent = 0,
                        resistPercent = resistPercent,
                    )
                    EffectiveDpsRow(
                        towerId = profile.towerId,
                        profileId = profile.profileId,
                        upgradeBranch = profile.upgradeBranch,
                        upgradeTier = profile.upgradeTier,
                        enemyId = enemy.id,
                        damageTypeId = profile.damageTypeId,
                        resistPercent = resistPercent,
                        effectiveDamagePerShot = effectiveDamage,
                        cooldownTicks = profile.cooldownTicks,
                        effectiveDps = BigDecimal.valueOf(effectiveDamage.toLong())
                            .multiply(BigDecimal.valueOf(BALANCE_TICKS_PER_SECOND.toLong()))
                            .divide(BigDecimal.valueOf(profile.cooldownTicks.toLong()), 6, RoundingMode.HALF_UP)
                            .stripTrailingZeros(),
                    )
                }
            }
    }

    private fun effectiveWaveEntries(
        registry: ContentRegistry,
        wave: WaveContent,
    ): List<Pair<EnemyContent, EffectiveEnemyStats>> {
        val entries = ArrayList<Pair<EnemyContent, EffectiveEnemyStats>>()
        var ordinal = 0
        wave.spawns.forEach { spawn ->
            val enemy = registry.enemies.getValue(spawn.enemyId)
            repeat(spawn.count) {
                val modifier = waveModifierAt(wave.modifiers, ordinal)
                entries += enemy to enemy.effectiveStats(modifier)
                ordinal += 1
            }
        }
        return entries
    }

    private fun waveModifierAt(modifiers: List<WaveModifier>, enemyOrdinal: Int): WaveModifier? {
        var covered = 0
        modifiers.forEach { modifier ->
            if (enemyOrdinal >= covered && enemyOrdinal < covered + modifier.count) return modifier
            covered += modifier.count
        }
        return null
    }

    /**
     * Counts Manhattan-grid tiles that can receive non-zero damage from one declared splash
     * tower: center contributes one tile and every positive-damage ring contributes `4 * ring`.
     * This mirrors DefenseRuntime's integer truncation, so a high falloff can shrink the
     * effective area below its declared geometric radius.
     */
    private fun effectiveSplashAoeTiles(tower: dev.myengine.content.TowerContent): Int {
        val radius = tower.splashRadius ?: return 0
        return (0..radius).sumOf { distance ->
            val damage = DamageFormula.effectiveDamage(
                baseDamage = tower.damage,
                distance = distance,
                falloffPercent = tower.falloffPercent,
                resistPercent = 0,
            )
            if (damage <= 0) 0 else if (distance == 0) 1 else 4 * distance
        }
    }

    private fun balanceDeltas(baseline: BalancePackSummary, changed: BalancePackSummary): List<BalanceMetricDelta> =
        listOf(
            delta("enemy", "enemy_types", baseline.enemyTypes.toDouble(), changed.enemyTypes.toDouble()),
            delta("enemy", "elite_enemy_types", baseline.eliteEnemyTypes.toDouble(), changed.eliteEnemyTypes.toDouble()),
            delta("enemy", "boss_enemy_types", baseline.bossEnemyTypes.toDouble(), changed.bossEnemyTypes.toDouble()),
            delta("enemy", "wave_enemies", baseline.waveEnemies.toDouble(), changed.waveEnemies.toDouble()),
            delta("enemy", "elite_wave_enemies", baseline.eliteWaveEnemies.toDouble(), changed.eliteWaveEnemies.toDouble()),
            delta("enemy", "boss_wave_enemies", baseline.bossWaveEnemies.toDouble(), changed.bossWaveEnemies.toDouble()),
            delta("enemy", "enemy_health_total", baseline.enemyHealthTotal.toDouble(), changed.enemyHealthTotal.toDouble()),
            delta("core", "core_damage_potential", baseline.coreDamagePotential.toDouble(), changed.coreDamagePotential.toDouble()),
            delta("resource", "reward_total", baseline.rewardTotal.toDouble(), changed.rewardTotal.toDouble()),
            delta("resource", "resource_types", baseline.resourceTypes.toDouble(), changed.resourceTypes.toDouble()),
            delta("resource", "recipe_output_per_tick", baseline.recipeOutputPerTick, changed.recipeOutputPerTick),
            delta("tower", "splash_tower_types", baseline.splashTowerTypes.toDouble(), changed.splashTowerTypes.toDouble()),
            delta("tower", "splash_radius_total", baseline.splashRadiusTotal.toDouble(), changed.splashRadiusTotal.toDouble()),
            delta("tower", "splash_falloff_percent_total", baseline.splashFalloffPercentTotal.toDouble(), changed.splashFalloffPercentTotal.toDouble()),
            delta("tower", "splash_effective_aoe_tiles", baseline.splashEffectiveAoeTiles.toDouble(), changed.splashEffectiveAoeTiles.toDouble()),
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
                    "maps" to registry.maps.keys.toList(),
                )
            },
        )
    }

    fun replayInspect(): String {
        val canonical = replayScenarioJson("canonical", "1:build_tower:pulse:30:32", SandboxGame.runScriptedScenario())
        val kill = replayScenarioJson("kill", "1:build_tower:pulse:2:2", SandboxGame.runScriptedKillScenario())
        val resist = SandboxGame.runScriptedResistScenario()
        val repeat = SandboxGame.runScriptedResistScenario()
        val unresisted = SandboxGame.runScriptedUnresistedScenario()
        val resistJson = buildJson(
            "scenario" to "resist",
            "commands" to "1:build_tower:pulse:2:2",
            "final_hash" to resist.hash,
            "golden_hash" to RESIST_REPLAY_GOLDEN_HASH,
            "repeat_hash" to repeat.hash,
            "zero_resist_hash" to unresisted.hash,
            "golden_match" to (resist.hash == RESIST_REPLAY_GOLDEN_HASH),
            "repeat_stable" to (resist.hash == repeat.hash),
            "differs_from_zero_resist" to (resist.hash != unresisted.hash),
            "tick" to resist.snapshot.debug.tick.value,
            "save_bytes" to resist.saveText.length,
            "enemies_killed" to resist.metrics.enemiesKilled,
        )
        return "{\"scenarios\":[$canonical,$kill,$resistJson]}"
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

private data class TowerBalanceProfile(
    val towerId: String,
    val profileId: String,
    val upgradeBranch: String?,
    val upgradeTier: Int,
    val damage: Int,
    val cooldownTicks: Int,
    val damageTypeId: String?,
)

private data class RawJson(val value: String)

private fun saturatedAdd(left: Long, right: Long): Long =
    if (right > 0L && left > Long.MAX_VALUE - right) Long.MAX_VALUE else left + right

private fun saturatedMultiply(left: Long, right: Long): Long = when {
    left <= 0L || right <= 0L -> 0L
    left > Long.MAX_VALUE / right -> Long.MAX_VALUE
    else -> left * right
}

fun buildJson(vararg values: Pair<String, Any?>): String =
    values.joinToString(prefix = "{", postfix = "}") { (key, value) ->
        "\"${escape(key)}\":${value.jsonValue()}"
    }

fun Any?.jsonValue(): String = when (this) {
    is RawJson -> value
    null -> "null"
    is Number, is Boolean -> toString()
    else -> "\"${escape(toString())}\""
}

fun escape(text: String): String =
    text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
