package dev.myengine.devtools

import dev.myengine.content.ContentPackLoader
import dev.myengine.content.ContentRegistry
import dev.myengine.content.ContentValidationError
import dev.myengine.content.WaveContent
import dev.myengine.content.WaveModifier
import dev.myengine.content.effectiveStats
import dev.myengine.core.CommandId
import dev.myengine.core.RunStatus
import dev.myengine.core.Tick
import dev.myengine.core.command.BuildTowerCommand
import dev.myengine.core.command.TileCoordinate
import dev.myengine.games.sandbox.SandboxGame
import java.nio.file.Path

private const val PLAYTEST_SCHEMA = "proc-008-playtest-v1"
private const val DEFAULT_SEED_START = 7L
private const val DEFAULT_SEED_COUNT = 3
private const val DEFAULT_MAX_TICKS = 240
private const val MAX_SEED_COUNT = 1_000
private const val MAX_TICKS = 100_000

private enum class PlaytestStrategy(
    val id: String,
    val buildTick: Long?,
) {
    NO_BUILD("no-build", null),
    SPAWN_TOWER("spawn-tower", 1L),
    LATE_TOWER("late-tower", 20L),
}

data class PlaytestRunReport(
    val seed: Long,
    val status: String,
    val terminalReason: String?,
    val ticks: Long,
    val waves: Int,
    val kills: Int,
    val leaks: Int,
    val coreDamage: Int,
    val coreHealth: Int,
) {
    fun toJson(): String = buildJson(
        "seed" to seed,
        "status" to status,
        "terminal_reason" to terminalReason,
        "ticks" to ticks,
        "waves" to waves,
        "kills" to kills,
        "leaks" to leaks,
        "core_damage" to coreDamage,
        "core_health" to coreHealth,
    )
}

data class PlaytestStrategyReport(
    val strategyId: String,
    val runs: List<PlaytestRunReport>,
) {
    val wins: Int get() = runs.count { it.status == "won" }
    val losses: Int get() = runs.count { it.status == "lost" }
    val timeouts: Int get() = runs.count { it.status == "timeout" }
    val totalLeaks: Int get() = runs.sumOf(PlaytestRunReport::leaks)
    val totalCoreDamage: Int get() = runs.sumOf(PlaytestRunReport::coreDamage)

    fun toJson(): String {
        val winRate = if (runs.isEmpty()) 0.0 else wins.toDouble() / runs.size.toDouble()
        val averageLeaks = if (runs.isEmpty()) 0.0 else totalLeaks.toDouble() / runs.size.toDouble()
        return buildJson(
            "strategy_id" to strategyId,
            "run_count" to runs.size,
            "wins" to wins,
            "losses" to losses,
            "timeouts" to timeouts,
            "win_rate" to winRate,
            "total_leaks" to totalLeaks,
            "average_leaks" to averageLeaks,
            "total_core_damage" to totalCoreDamage,
            "runs" to RawJson(runs.joinToString(prefix = "[", postfix = "]") { it.toJson() }),
        )
    }
}

data class PlaytestWaveCurveRow(
    val waveId: String,
    val waveNumber: Int,
    val startTick: Long,
    val enemyCount: Long,
    val totalHealth: Long,
    val totalReward: Long,
) {
    fun toJson(): String = buildJson(
        "wave_id" to waveId,
        "wave_number" to waveNumber,
        "start_tick" to startTick,
        "enemy_count" to enemyCount,
        "total_health" to totalHealth,
        "total_reward" to totalReward,
    )
}

data class PlaytestReport(
    val packId: String?,
    val seedStart: Long,
    val seedCount: Int,
    val maxTicks: Int,
    val strategies: List<PlaytestStrategyReport>,
    val difficultyCurve: List<PlaytestWaveCurveRow>,
    val errors: List<String>,
) {
    fun toJson(): String = buildJson(
        "schema" to PLAYTEST_SCHEMA,
        "pack_id" to packId,
        "seed_start" to seedStart,
        "seed_count" to seedCount,
        "max_ticks" to maxTicks,
        "strategies" to RawJson(strategies.joinToString(prefix = "[", postfix = "]") { it.toJson() }),
        "difficulty_curve" to RawJson(
            difficultyCurve.joinToString(prefix = "[", postfix = "]") { it.toJson() },
        ),
        "errors" to RawJson(errors.joinToString(prefix = "[", postfix = "]") { "\"${escape(it)}\"" }),
    )
}

/** Deterministic, report-only headless bot for seeded balance curves. */
object PlaytestBot {
    fun run(
        packRoot: Path = SandboxGame.contentRoot(),
        seedStart: Long = DEFAULT_SEED_START,
        seedCount: Int = DEFAULT_SEED_COUNT,
        maxTicks: Int = DEFAULT_MAX_TICKS,
    ): PlaytestReport {
        require(seedCount in 1..MAX_SEED_COUNT) {
            "Playtest seed count must be between 1 and $MAX_SEED_COUNT."
        }
        require(maxTicks in 1..MAX_TICKS) {
            "Playtest max ticks must be between 1 and $MAX_TICKS."
        }

        val load = ContentPackLoader.load(packRoot)
        val registry = load.registry
        if (!load.isValid || registry == null) {
            return PlaytestReport(
                packId = registry?.manifest?.id,
                seedStart = seedStart,
                seedCount = seedCount,
                maxTicks = maxTicks,
                strategies = emptyList(),
                difficultyCurve = emptyList(),
                errors = load.errors.map(ContentValidationError::toString),
            )
        }

        val towerId = registry.towers.keys.sorted().firstOrNull()
        val buildPosition = registry.requireMap().primarySpawn.position.let {
            TileCoordinate(it.x + 1, it.y + 1)
        }
        val strategyReports = PlaytestStrategy.entries.map { strategy ->
            PlaytestStrategyReport(
                strategyId = strategy.id,
                runs = (0 until seedCount).map { index ->
                    val seed = seedStart + index.toLong()
                    runOne(
                        registry = registry,
                        strategy = strategy,
                        seed = seed,
                        maxTicks = maxTicks,
                        towerId = towerId,
                        buildPosition = buildPosition,
                    )
                },
            )
        }
        return PlaytestReport(
            packId = registry.manifest.id,
            seedStart = seedStart,
            seedCount = seedCount,
            maxTicks = maxTicks,
            strategies = strategyReports,
            difficultyCurve = difficultyCurve(registry),
            errors = if (towerId == null) listOf("No tower definitions are available for build strategies.") else emptyList(),
        )
    }

    private fun runOne(
        registry: ContentRegistry,
        strategy: PlaytestStrategy,
        seed: Long,
        maxTicks: Int,
        towerId: String?,
        buildPosition: TileCoordinate,
    ): PlaytestRunReport {
        val runtime = SandboxGame.createRuntime(registry, seed = seed)
        if (strategy.buildTick != null && towerId != null) {
            runtime.submit(
                BuildTowerCommand(
                    id = CommandId(1),
                    scheduledTick = Tick(strategy.buildTick),
                    towerId = towerId,
                    position = buildPosition,
                ),
            )
        }
        var ticks = 0
        while (!runtime.state.run.isTerminal && ticks < maxTicks) {
            runtime.step()
            ticks += 1
        }
        val metrics = runtime.state.defense.metrics
        val status = when {
            runtime.state.run.status == RunStatus.WON -> "won"
            runtime.state.run.status == RunStatus.LOST -> "lost"
            else -> "timeout"
        }
        return PlaytestRunReport(
            seed = seed,
            status = status,
            terminalReason = runtime.state.run.terminalReason?.name?.lowercase(),
            ticks = runtime.state.tick.value,
            waves = runtime.state.defense.spawnedWaveIds.size,
            kills = metrics.enemiesKilled,
            leaks = metrics.enemiesLeaked,
            coreDamage = metrics.coreDamage,
            coreHealth = runtime.state.defense.coreHealth,
        )
    }

    private fun difficultyCurve(registry: ContentRegistry): List<PlaytestWaveCurveRow> {
        val map = registry.requireMap()
        return registry.waves.values
            .sortedWith(compareBy<WaveContent> { it.startTick }.thenBy { it.id })
            .mapIndexed { index, wave ->
                val routes = (wave.spawnSelection ?: map.spawns.keys.toList()).size.coerceAtLeast(1)
                var enemyCount = 0L
                var totalHealth = 0L
                var totalReward = 0L
                repeat(routes) {
                    var ordinal = 0
                    wave.spawns.forEach { spawn ->
                        val enemy = registry.requireEnemy(spawn.enemyId)
                        repeat(spawn.count) {
                            val stats = enemy.effectiveStats(
                                waveModifier = modifierAt(wave.modifiers, ordinal),
                                waveHealthPercent = wave.healthScalePercent,
                                waveRewardPercent = wave.rewardScalePercent,
                            )
                            enemyCount = saturatedAdd(enemyCount, 1L)
                            totalHealth = saturatedAdd(totalHealth, stats.health.toLong())
                            totalReward = saturatedAdd(totalReward, stats.rewardAmount.toLong())
                            ordinal += 1
                        }
                    }
                }
                PlaytestWaveCurveRow(
                    waveId = wave.id,
                    waveNumber = index + 1,
                    startTick = wave.startTick,
                    enemyCount = enemyCount,
                    totalHealth = totalHealth,
                    totalReward = totalReward,
                )
            }
    }

    private fun modifierAt(modifiers: List<WaveModifier>, enemyOrdinal: Int): WaveModifier? {
        var covered = 0
        modifiers.forEach { modifier ->
            if (enemyOrdinal >= covered && enemyOrdinal < covered + modifier.count) return modifier
            covered += modifier.count
        }
        return null
    }

    private fun saturatedAdd(left: Long, right: Long): Long =
        if (right > 0L && left > Long.MAX_VALUE - right) Long.MAX_VALUE else left + right
}
