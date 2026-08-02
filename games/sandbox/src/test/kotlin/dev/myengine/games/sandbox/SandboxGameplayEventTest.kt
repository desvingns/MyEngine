package dev.myengine.games.sandbox

import dev.myengine.core.CommandId
import dev.myengine.core.GameplayEvent
import dev.myengine.core.GameplayEventType
import dev.myengine.core.Tick
import dev.myengine.core.command.BuildTowerCommand
import dev.myengine.core.command.SellTowerCommand
import dev.myengine.core.command.TileCoordinate
import java.io.StringReader
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SandboxGameplayEventTest {
    @Test
    fun sameSeedAndCommandsProduceTheSameOrderedGameplayEventLog() {
        fun run(): List<GameplayEvent> {
            val runtime = SandboxGame.createRuntime(seed = 123L)
            runtime.submit(
                BuildTowerCommand(
                    id = CommandId(1),
                    scheduledTick = Tick(1),
                    towerId = "pulse",
                    position = TileCoordinate(2, 2),
                    actorId = 5L,
                ),
            )
            runtime.submit(SellTowerCommand(CommandId(2), Tick(21), towerEntityId = 1L, actorId = 6L))
            return collectEvents(runtime, ticks = 21)
        }

        val first = run()
        val second = run()

        assertEquals(first, second)
        assertEquals(
            setOf(
                GameplayEventType.BUILD,
                GameplayEventType.SELL,
                GameplayEventType.WAVE_START,
                GameplayEventType.SHOT,
                GameplayEventType.HIT,
                GameplayEventType.DEATH,
            ),
            first.map { it.type }.toSet(),
        )
        assertEquals(1, first.first { it.type == GameplayEventType.BUILD }.tick.value)
        assertEquals(21, first.first { it.type == GameplayEventType.SELL }.tick.value)
        assertTrue(first.any { it.type == GameplayEventType.DEATH })
        first.groupBy { it.tick }.values.forEach { eventsAtTick ->
            assertEquals((0 until eventsAtTick.size).toList(), eventsAtTick.map { it.ordinal })
            assertEquals(eventsAtTick.sortedBy { it.ordinal }, eventsAtTick)
        }
    }

    @Test
    fun gameplayFeedIsReplacedByTheLatestTick() {
        val runtime = SandboxGame.createRuntime()
        runtime.submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", TileCoordinate(2, 2)))

        runtime.step()
        val buildSnapshot = runtime.snapshot()
        assertEquals(Tick(1), buildSnapshot.combatEvents.gameplayEvents.single().tick)
        assertEquals(GameplayEventType.BUILD, buildSnapshot.combatEvents.gameplayEvents.single().type)

        runtime.step()

        assertEquals(Tick(2), runtime.snapshot().debug.tick)
        assertTrue(runtime.snapshot().combatEvents.gameplayEvents.isEmpty())
    }

    @Test
    fun rejectedCommandsDoNotEmitGameplayEvents() {
        val runtime = SandboxGame.createRuntime()
        runtime.submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", TileCoordinate(2, 2)))
        runtime.submit(BuildTowerCommand(CommandId(2), Tick(2), "pulse", TileCoordinate(2, 2)))

        val snapshots = buildList {
            repeat(2) {
                runtime.step()
                add(runtime.snapshot())
            }
        }

        assertEquals(GameplayEventType.BUILD, snapshots[0].combatEvents.gameplayEvents.single().type)
        assertTrue(snapshots[1].combatEvents.gameplayEvents.isEmpty())
        assertEquals("tile_not_buildable", runtime.state.lastCommandOrError)
    }

    @Test
    fun gameplayEventsDoNotChangeStableHashOrSaveVersion() {
        fun run(readSnapshot: Boolean): Pair<String, String> {
            val runtime = SandboxGame.createRuntime(seed = 77L)
            runtime.submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", TileCoordinate(2, 2)))
            runtime.step()
            if (readSnapshot) {
                assertTrue(runtime.snapshot().combatEvents.gameplayEvents.isNotEmpty())
            }
            val save = SandboxSaveCodec.encode(runtime.state, seed = 77L)
            return runtime.state.stableHash() to save
        }

        val withoutReading = run(readSnapshot = false)
        val withEvents = run(readSnapshot = true)
        assertEquals(withoutReading.first, withEvents.first)

        val properties = Properties().apply { load(StringReader(withEvents.second)) }
        assertEquals(SandboxSaveCodec.SAVE_VERSION.toString(), properties.getProperty("saveVersion"))
        assertFalse(withEvents.second.contains("gameplayEvent"))
    }

    private fun collectEvents(runtime: SandboxRuntime, ticks: Int): List<GameplayEvent> = buildList {
        repeat(ticks) {
            runtime.step()
            addAll(runtime.snapshot().combatEvents.gameplayEvents)
        }
    }
}
