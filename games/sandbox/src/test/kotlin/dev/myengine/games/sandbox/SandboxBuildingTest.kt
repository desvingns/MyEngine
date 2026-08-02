package dev.myengine.games.sandbox

import dev.myengine.content.MapContent
import dev.myengine.content.MapCoordinate
import dev.myengine.content.MapSpawn
import dev.myengine.content.MapTerrainSymbol
import dev.myengine.core.CommandId
import dev.myengine.core.EngineCommand
import dev.myengine.core.Tick
import dev.myengine.core.command.PlaceBuildingCommand
import dev.myengine.core.command.RemoveBuildingCommand
import dev.myengine.core.command.TileCoordinate
import dev.myengine.entities.EntityId
import dev.myengine.logistics.Inventory
import dev.myengine.world.TilePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SandboxBuildingTest {
    @Test
    fun placeAndRemoveWallChargeOccupyRefundAndClearAtomically() {
        val runtime = runtimeWithInventory(6)
        val position = TilePosition(4, 1)

        runtime.submit(place(1, 1, position))
        runtime.step()

        assertEquals("placed:1", runtime.state.lastCommandOrError)
        assertEquals(4, runtime.state.inventory.amount("bolt"))
        assertEquals(1L, runtime.state.world.tileAt(position).tile.occupiedBy)
        assertEquals(20, runtime.state.entities.get(EntityId(1))!!.health!!.current)

        runtime.submit(RemoveBuildingCommand(CommandId(2), Tick(2), 1L))
        runtime.step()

        assertEquals("removed:1", runtime.state.lastCommandOrError)
        assertEquals(5, runtime.state.inventory.amount("bolt"), "floor(2 * 0.5) refund")
        assertTrue(runtime.state.world.tileAt(position).tile.occupiedBy == null)
        assertTrue(runtime.state.entities.get(EntityId(1)) == null)
    }

    @Test
    fun rejectedWallPlacementDoesNotSpendOccupyOrConsumeEntityId() {
        val runtime = runtimeWithInventory(1)
        val position = TilePosition(4, 1)
        val beforeNextId = runtime.state.entities.nextIdSnapshot()
        val beforeInventory = runtime.state.inventory

        runtime.submit(place(1, 1, position))
        runtime.step()

        assertEquals("missing_resource:bolt", runtime.state.lastCommandOrError)
        assertEquals(beforeInventory, runtime.state.inventory)
        assertEquals(beforeNextId, runtime.state.entities.nextIdSnapshot())
        assertTrue(runtime.state.world.canBuild(position))
    }

    @Test
    fun sealedForcedCorridorRejectsWithoutMutationAndAcceptedBlockerReroutesSameTick() {
        val registry = forcedCorridorRegistry()
        val sealed = SandboxRuntime(
            SandboxGame.createInitialState(registry, mapId = "forced-corridor").also {
                it.producers = emptyList()
            },
        )
        val sealedPosition = TilePosition(2, 1)
        val sealedInventory = sealed.state.inventory
        val sealedNextId = sealed.state.entities.nextIdSnapshot()
        sealed.submit(place(1, 1, sealedPosition))
        sealed.step()

        assertEquals("blocks_spawn_path", sealed.state.lastCommandOrError)
        assertEquals(sealedInventory, sealed.state.inventory)
        assertEquals(sealedNextId, sealed.state.entities.nextIdSnapshot())
        assertTrue(sealed.state.world.canBuild(sealedPosition))
        assertEquals(0, sealed.state.entities.count())

        val reroute = SandboxGame.createRuntime().also { it.state.producers = emptyList() }
        reroute.step(11)
        reroute.submit(place(1, 12, TilePosition(4, 1)))
        reroute.step()

        assertEquals("placed:4", reroute.state.lastCommandOrError)
        assertEquals(listOf(TilePosition(3, 2)), reroute.state.entities.byTag("enemy").map { it.position!!.tile }.distinct())
    }

    @Test
    fun wallSnapshotContainsHealthAndProjectionDoesNotChangeStableHash() {
        val runtime = runtimeWithInventory(6)
        runtime.submit(place(1, 1, TilePosition(4, 1)))
        runtime.step()
        val before = runtime.state.stableHash()

        val snapshot = runtime.snapshot()
        val wall = snapshot.entities.single { it.type == "building:wall" }

        assertEquals(20, wall.health)
        assertEquals(before, runtime.state.stableHash())
        assertEquals(1, snapshot.entities.count { it.type == "building:wall" })
    }

    @Test
    fun v14SaveRoundtripPreservesWallAndPendingBuildingCommand() {
        val registry = SandboxGame.loadRegistry()
        val session = SandboxSession.start(registry).also {
            it.runtime.state.producers = emptyList()
            it.runtime.state.inventory = Inventory(mapOf("bolt" to 6))
        }
        val pending = place(12, 20, TilePosition(4, 1))
        session.submit(pending)
        session.step(5)
        val save = session.save()

        assertTrue(save.contains("saveVersion=14"))
        val restored = SandboxSession.restore(save, registry)
        assertEquals(listOf(pending), restored.runtime.pendingCommands())
        assertEquals(session.stableHash(), restored.stableHash())

        session.step(15)
        restored.step(15)
        assertEquals(session.stableHash(), restored.stableHash())
        assertEquals(1, restored.runtime.state.entities.byTag("building").size)
        assertEquals(4, restored.runtime.state.inventory.amount("bolt"))
    }

    @Test
    fun forcedCorridorReplayHashIsDeterministicAcrossRuns() {
        fun run(): String {
            val registry = forcedCorridorRegistry()
            val runtime = SandboxRuntime(
                SandboxGame.createInitialState(registry, mapId = "forced-corridor").also {
                    it.producers = emptyList()
                },
                seed = 19L,
            )
            runtime.submit(place(1, 1, TilePosition(3, 2)))
            runtime.step(12)
            assertEquals("placed:1", runtime.state.lastCommandOrError)
            return runtime.state.stableHash()
        }

        val first = run()
        assertEquals(first, run())
        assertNotEquals("", first)
    }

    private fun runtimeWithInventory(amount: Int): SandboxRuntime =
        SandboxGame.createInitialState().also {
            it.producers = emptyList()
            it.inventory = Inventory(mapOf("bolt" to amount))
        }.let(::SandboxRuntime)

    private fun place(id: Long, tick: Long, position: TilePosition) =
        PlaceBuildingCommand(
            id = CommandId(id),
            scheduledTick = Tick(tick),
            buildingId = "wall",
            position = TileCoordinate(position.x, position.y),
        )

    private fun forcedCorridorRegistry() = SandboxGame.loadRegistry().copy(
        maps = mapOf(
            "forced-corridor" to MapContent(
                id = "forced-corridor",
                width = 5,
                height = 3,
                terrainRows = listOf("....C", "##.##", "....#"),
                terrainMapping = mapOf(
                    '.' to MapTerrainSymbol("floor"),
                    '#' to MapTerrainSymbol("wall"),
                    'C' to MapTerrainSymbol("core"),
                ),
                spawns = mapOf(
                    "primary" to MapSpawn("primary", MapCoordinate(0, 0)),
                    "secondary" to MapSpawn("secondary", MapCoordinate(0, 2)),
                ),
                core = MapCoordinate(4, 0),
            ),
        ),
        waves = emptyMap(),
    )
}
