package dev.myengine.games.sandbox

import dev.myengine.core.CommandId
import dev.myengine.core.Tick
import dev.myengine.core.command.BuildTowerCommand
import dev.myengine.core.command.SetTowerTargetingModeCommand
import dev.myengine.core.command.TargetingMode
import dev.myengine.core.command.TileCoordinate
import dev.myengine.world.TilePosition
import java.io.StringReader
import java.io.StringWriter
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * End-to-end acceptance coverage for ENG-008's command, persistence, HUD, and replay boundaries.
 * Selector semantics themselves are pinned independently in TargetSelectorTest.
 */
class SandboxTargetingModeTest {
    @Test
    fun queuedTargetingModeSwitchAppliesAtItsTickAndInvalidTowerIsRejectedWithoutMutation() {
        val runtime = SandboxGame.createRuntime()
        val tower = placePulseTower(runtime)

        runtime.submit(
            SetTowerTargetingModeCommand(
                id = CommandId(2),
                scheduledTick = Tick(3),
                towerEntityId = tower.id.value,
                targetingMode = TargetingMode.STRONGEST,
            ),
        )
        runtime.submit(
            SetTowerTargetingModeCommand(
                id = CommandId(3),
                scheduledTick = Tick(4),
                towerEntityId = 999L,
                targetingMode = TargetingMode.WEAKEST,
            ),
        )

        runtime.step() // tick 2: both commands remain queued.
        assertEquals(TargetingMode.NEAREST, runtime.state.entities.require(tower.id).tower?.targetingMode)
        assertEquals(2, runtime.pendingCommands().size)

        runtime.step() // tick 3: valid command drains.
        assertEquals(TargetingMode.STRONGEST, runtime.state.entities.require(tower.id).tower?.targetingMode)
        assertEquals("targeting_mode:${tower.id.value}:strongest", runtime.state.lastCommandOrError)

        runtime.step() // tick 4: invalid command drains without changing the real tower.
        assertEquals(TargetingMode.STRONGEST, runtime.state.entities.require(tower.id).tower?.targetingMode)
        assertEquals("unknown_tower_entity:999", runtime.state.lastCommandOrError)
    }

    @Test
    fun v6TowerMigrationUsesTheContentDefaultAndPendingModeSwitchRoundtripsExactly() {
        val baseRegistry = SandboxGame.loadRegistry()
        val registry = baseRegistry.copy(
            towers = baseRegistry.towers + (
                "pulse" to baseRegistry.requireTower("pulse").copy(targetingMode = TargetingMode.STRONGEST)
                ),
        )
        val runtime = SandboxGame.createRuntime(registry)
        val tower = placePulseTower(runtime)
        runtime.submit(
            SetTowerTargetingModeCommand(CommandId(2), Tick(2), tower.id.value, TargetingMode.WEAKEST),
        )
        runtime.step()

        val v8 = SandboxSaveCodec.encode(runtime.state, seed = 7)
        assertEquals(TargetingMode.WEAKEST, SandboxSaveCodec.decode(v8, registry).entities.require(tower.id).tower?.targetingMode)

        val v6 = asV6Save(v8)
        val migrated = SandboxSaveCodec.decode(v6, registry)
        assertEquals(TargetingMode.STRONGEST, migrated.entities.require(tower.id).tower?.targetingMode)

        val modeSwitch = SetTowerTargetingModeCommand(
            id = CommandId(47),
            scheduledTick = Tick(25),
            towerEntityId = tower.id.value,
            targetingMode = TargetingMode.WEAKEST,
            actorId = 909L,
        )
        val session = SandboxSession.start(registry)
        session.submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", TileCoordinate(2, 2)))
        session.step()
        val sessionTowerId = session.runtime.state.entities.byTag("tower").single().id.value
        val queued = modeSwitch.copy(towerEntityId = sessionTowerId)
        session.submit(queued)
        session.step(10)

        val restored = SandboxSession.restore(session.save(), registry)
        assertEquals(queued, assertIs<SetTowerTargetingModeCommand>(restored.runtime.pendingCommands().single()))
    }

    @Test
    fun hudExposesContentDefaultAndQueuedOverrideForPlacedTower() {
        val runtime = SandboxGame.createRuntime()
        val tower = placePulseTower(runtime)

        assertEquals(TargetingMode.NEAREST, runtime.snapshot().hud.towers.single().targetingMode)

        runtime.submit(
            SetTowerTargetingModeCommand(CommandId(2), Tick(2), tower.id.value, TargetingMode.LAST),
        )
        runtime.step()

        val hudTower = runtime.snapshot().hud.towers.single { it.entityId == tower.id.value }
        assertEquals(TargetingMode.LAST, hudTower.targetingMode)
    }

    @Test
    fun midRunTargetingModeSwitchProducesAStableReplayHash() {
        fun run(): String {
            val runtime = SandboxGame.createRuntime()
            runtime.submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", TileCoordinate(2, 2)))
            runtime.step()
            val towerId = runtime.state.entities.byTag("tower").single().id.value
            runtime.submit(
                SetTowerTargetingModeCommand(CommandId(2), Tick(18), towerId, TargetingMode.STRONGEST),
            )
            runtime.step(34)
            assertEquals(TargetingMode.STRONGEST, runtime.state.entities.byTag("tower").single().tower?.targetingMode)
            return runtime.state.stableHash()
        }

        assertEquals(run(), run())
    }

    private fun placePulseTower(runtime: SandboxRuntime) = runtime.apply {
        submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", TileCoordinate(2, 2)))
        step()
    }.state.entities.byTag("tower").single()

    private fun asV6Save(text: String): String {
        val props = Properties().also { it.load(StringReader(text)) }
        props["saveVersion"] = "6"
        props["entities"] = props.getProperty("entities").split(';').joinToString(";") { row ->
            row.split('|').take(15).joinToString("|")
        }
        return StringWriter().also { props.store(it, "MyEngine sandbox v6 fixture") }.toString()
    }
}
