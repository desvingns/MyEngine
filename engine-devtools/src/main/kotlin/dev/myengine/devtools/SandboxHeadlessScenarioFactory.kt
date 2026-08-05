package dev.myengine.devtools

import dev.myengine.core.CommandId
import dev.myengine.core.Tick
import dev.myengine.core.command.BuildTowerCommand
import dev.myengine.core.command.TileCoordinate
import dev.myengine.games.sandbox.SandboxGame
import dev.myengine.games.sandbox.SandboxRuntime
import dev.myengine.render.AsciiRenderer
import java.nio.file.Path

/** Default adapter for the checked-in sandbox pack; other games register their own factory. */
class SandboxHeadlessScenarioFactory : HeadlessScenarioFactory {
    override val id: String = "sandbox"

    override fun create(packRoot: Path, scenarioId: String, seed: Long): HeadlessScenario {
        return create(packRoot, scenarioId, seed, emptySet())
    }

    override fun create(
        packRoot: Path,
        scenarioId: String,
        seed: Long,
        metaUnlockIds: Set<String>,
    ): HeadlessScenario {
        require(scenarioId.isNotBlank()) { "Headless scenario id cannot be blank." }
        val runtime = if (scenarioId in setOf("canonical", "kill", "resist")) {
            SandboxGame.createDevtoolsReplayRuntime(
                scenarioId,
                packRoot = packRoot,
                seed = seed,
                metaUnlockIds = metaUnlockIds,
            )
        } else {
            val registry = SandboxGame.loadRegistry(packRoot)
            SandboxGame.createRuntime(registry, seed = seed, metaUnlockIds = metaUnlockIds)
        }
        return SandboxHeadlessScenario(scenarioId, runtime)
    }
}

private class SandboxHeadlessScenario(
    override val scenarioId: String,
    private val runtime: SandboxRuntime,
) : HeadlessScenario {
    override val packId: String = runtime.state.registry.manifest.id

    private var nextCommandId = 1L

    override fun submitScriptCommand(command: String) {
        val fields = command.split(':')
        require(fields.size == 5 && fields[1] == "build_tower") {
            "Sandbox headless commands must use tick:build_tower:tower_id:x:y."
        }
        val scheduledTick = fields[0].toLongOrNull()
            ?: error("Invalid scheduled tick '${fields[0]}'.")
        val x = fields[3].toIntOrNull() ?: error("Invalid x coordinate '${fields[3]}'.")
        val y = fields[4].toIntOrNull() ?: error("Invalid y coordinate '${fields[4]}'.")
        check(
            runtime.submit(
                BuildTowerCommand(
                    id = CommandId(nextCommandId++),
                    scheduledTick = Tick(scheduledTick),
                    towerId = fields[2],
                    position = TileCoordinate(x, y),
                ),
            ),
        ) { "Sandbox scenario rejected command '$command'." }
    }

    override fun step(ticks: Int) {
        runtime.step(ticks)
    }

    override fun asciiFrame(): String = AsciiRenderer().render(runtime.snapshot())

    override fun stateDump(): HeadlessStateDump {
        val state = runtime.state
        val entities = state.entities.all()
            .sortedBy { it.id.value }
            .map { entity ->
                HeadlessEntityDump(
                    id = entity.id.value,
                    type = entity.type,
                    x = entity.position?.tile?.x,
                    y = entity.position?.tile?.y,
                    health = entity.health?.current,
                    maxHealth = entity.health?.max,
                    inventory = entity.inventory?.resources.orEmpty(),
                )
            }
        val inventories = buildMap {
            put("global", state.inventory.resources)
            entities.filter { it.inventory.isNotEmpty() }.forEach { entity ->
                put("entity:${entity.id}", entity.inventory)
            }
        }
        val metrics = state.defense.metrics
        return HeadlessStateDump(
            tick = state.tick.value,
            entities = entities,
            inventories = inventories,
            defenseMetrics = HeadlessDefenseDump(
                coreHealth = state.defense.coreHealth,
                enemiesSpawned = metrics.enemiesSpawned,
                enemiesKilled = metrics.enemiesKilled,
                enemiesLeaked = metrics.enemiesLeaked,
                coreDamage = metrics.coreDamage,
                towerShots = metrics.towerShots,
                towerMetrics = state.defense.towerMetrics.mapValues { (_, metric) ->
                    HeadlessTowerMetricDump(metric.actualDamage, metric.kills)
                },
            ),
            hash = state.stableHash(),
        )
    }
}
