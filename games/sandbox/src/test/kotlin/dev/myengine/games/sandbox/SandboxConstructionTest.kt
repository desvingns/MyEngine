package dev.myengine.games.sandbox

import dev.myengine.content.ContentRegistry
import dev.myengine.core.CommandId
import dev.myengine.core.Tick
import dev.myengine.core.command.CancelBlueprintCommand
import dev.myengine.core.command.PlaceBlueprintCommand
import dev.myengine.core.command.TileCoordinate
import dev.myengine.entities.Entity
import dev.myengine.entities.EntityId
import dev.myengine.entities.EntityStore
import dev.myengine.entities.JobActorComponent
import dev.myengine.entities.PositionComponent
import dev.myengine.entities.WorkerComponent
import dev.myengine.logistics.HaulSource
import dev.myengine.logistics.HaulSourceStore
import dev.myengine.logistics.Inventory
import dev.myengine.world.TilePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SandboxConstructionTest {
    @Test
    fun blueprintIsNonBlockingThenHaulsFromLowestAvailableSourceAndSpawnsBuilding() {
        val state = constructionState(SandboxGame.loadRegistry())
        val runtime = SandboxRuntime(state)
        val position = TilePosition(4, 1)

        runtime.submit(PlaceBlueprintCommand(CommandId(1), Tick(1), "wall", TileCoordinate(position.x, position.y)))
        runtime.step()

        assertEquals("construction:1", state.constructionSites.all().single().id)
        assertNull(state.world.tileAt(position).tile.occupiedBy)
        assertTrue(state.world.canOccupy(position))
        assertEquals("source-a", state.jobBoard.get("construction-haul:construction:1")?.haul?.sourceId)
        assertEquals(mapOf("bolt" to 2), state.entities.require(EntityId(1)).inventory?.resources)

        runtime.step(2)

        assertTrue(state.constructionSites.all().isEmpty())
        assertEquals(1, state.entities.byTag("building").size)
        assertTrue(state.world.tileAt(position).tile.occupiedBy != null)
        assertEquals("built:2", state.lastCommandOrError)
    }

    @Test
    fun missingSourceIsRetriedWhenASortedSourceBecomesAvailable() {
        val registry = SandboxGame.loadRegistry()
        val state = constructionState(
            registry,
            sourceAAmount = 1,
            sourceZAmount = 1,
        )
        val runtime = SandboxRuntime(state)
        val position = TilePosition(4, 1)
        runtime.submit(PlaceBlueprintCommand(CommandId(1), Tick(1), "wall", TileCoordinate(position.x, position.y)))
        runtime.step()
        assertNull(state.jobBoard.get("construction-haul:construction:1"))

        state.haulSources.addOutput("source-a", TilePosition(1, 1), "bolt", 1)
        runtime.step()

        assertEquals("source-a", state.jobBoard.get("construction-haul:construction:1")?.haul?.sourceId)
    }

    @Test
    fun cancelRefundsCarriedMaterialsAndRemovesConstructionJobs() {
        val state = constructionState(SandboxGame.loadRegistry())
        val runtime = SandboxRuntime(state)
        val position = TilePosition(4, 1)
        runtime.submit(PlaceBlueprintCommand(CommandId(1), Tick(1), "wall", TileCoordinate(position.x, position.y)))
        runtime.step()
        runtime.submit(CancelBlueprintCommand(CommandId(2), Tick(2), "construction:1"))
        runtime.step()

        assertNull(state.constructionSites.get("construction:1"))
        assertEquals(2, state.haulSources.get("source-a")?.resources?.get("bolt"))
        assertNull(state.entities.require(EntityId(1)).inventory)
        assertTrue(state.jobBoard.all().none { it.id.contains("construction") })
    }

    @Test
    fun cancelRefundsCarriedConstructionMaterialWithoutDroppingOtherWorkerCargo() {
        val state = constructionState(SandboxGame.loadRegistry())
        val runtime = SandboxRuntime(state)
        runtime.submit(PlaceBlueprintCommand(CommandId(1), Tick(1), "wall", TileCoordinate(4, 1)))
        runtime.step()
        state.entities.update(EntityId(1)) { entity ->
            entity.copy(inventory = entity.inventory!!.copy(resources = mapOf("bolt" to 2, "other" to 1)))
        }

        runtime.submit(CancelBlueprintCommand(CommandId(2), Tick(2), "construction:1"))
        runtime.step()

        assertEquals(2, state.haulSources.get("source-a")?.resources?.get("bolt"))
        assertEquals(mapOf("other" to 1), state.entities.require(EntityId(1)).inventory?.resources)
    }

    @Test
    fun cancelRefundsDeliveredMaterialsBeforeBuildCompletes() {
        val registry = registryWithBuildTicks(3)
        val state = constructionState(registry)
        val runtime = SandboxRuntime(state)
        val position = TilePosition(4, 1)
        runtime.submit(PlaceBlueprintCommand(CommandId(1), Tick(1), "wall", TileCoordinate(position.x, position.y)))
        runtime.step(3)
        assertEquals(2, state.constructionSites.get("construction:1")?.deliveredAmount)
        assertTrue(state.jobBoard.get("construction-build:construction:1")?.status != dev.myengine.ai.JobStatus.DONE)

        runtime.submit(CancelBlueprintCommand(CommandId(2), Tick(4), "construction:1"))
        runtime.step()

        assertNull(state.constructionSites.get("construction:1"))
        assertEquals(2, state.haulSources.get("source-a")?.resources?.get("bolt"))
        assertTrue(state.jobBoard.all().none { it.id.contains("construction") })
        assertTrue(state.entities.byTag("building").isEmpty())
    }

    @Test
    fun midConstructionSaveRestorePreservesLedgerJobAndContinuation() {
        val registry = registryWithBuildTicks(3)
        val original = SandboxRuntime(constructionState(registry))
        original.submit(PlaceBlueprintCommand(CommandId(1), Tick(1), "wall", TileCoordinate(4, 1)))
        original.step(3)
        val save = SandboxSaveCodec.encode(original.state, seed = 7L)
        val restoredState = SandboxSaveCodec.decode(save, registry)
        val restored = SandboxRuntime(restoredState, seed = 7L)

        assertEquals(original.state.stableHash(), restoredState.stableHash())
        assertEquals(original.state.constructionSites.all(), restoredState.constructionSites.all())
        assertEquals(original.state.jobBoard.all(), restoredState.jobBoard.all())

        original.step(3)
        restored.step(3)

        assertEquals(original.state.stableHash(), restored.state.stableHash())
        assertEquals(1, restored.state.entities.byTag("building").size)
    }

    @Test
    fun blueprintReplayHashIsDeterministic() {
        fun run(): String {
            val runtime = SandboxRuntime(constructionState(SandboxGame.loadRegistry()))
            runtime.submit(PlaceBlueprintCommand(CommandId(1), Tick(1), "wall", TileCoordinate(4, 1)))
            runtime.step(3)
            return runtime.state.stableHash()
        }

        assertEquals(run(), run())
    }

    @Test
    fun pendingBlueprintCommandsRoundTripThroughSaveCodec() {
        val state = constructionState(SandboxGame.loadRegistry())
        val place = PlaceBlueprintCommand(CommandId(11), Tick(8), "wall", TileCoordinate(4, 1), actorId = 3L)
        val cancel = CancelBlueprintCommand(CommandId(12), Tick(9), "construction:11", actorId = 3L)

        val save = SandboxSaveCodec.encode(state, seed = 7L, pendingCommands = listOf(place, cancel))

        assertEquals(listOf(place, cancel), SandboxSaveCodec.decodePendingCommands(save))
    }

    private fun constructionState(registry: ContentRegistry, sourceAAmount: Int = 2, sourceZAmount: Int = 2): SandboxState =
        SandboxGame.createInitialState(registry).copy(
            entities = EntityStore(
                nextEntityId = 2,
                initialEntities = listOf(
                    Entity(
                        id = EntityId(1),
                        type = "worker:hauler",
                        tags = setOf("worker"),
                        position = PositionComponent(TilePosition(1, 1)),
                        jobActor = JobActorComponent(),
                        worker = WorkerComponent("hauler"),
                    ),
                ),
            ),
            producers = emptyList(),
            inventory = Inventory(),
            haulSources = HaulSourceStore(
                listOf(
                    HaulSource("source-z", TilePosition(1, 2), mapOf("bolt" to sourceZAmount)),
                    HaulSource("source-a", TilePosition(1, 1), mapOf("bolt" to sourceAAmount)),
                ),
            ),
        )

    private fun registryWithBuildTicks(ticks: Int): ContentRegistry {
        val registry = SandboxGame.loadRegistry()
        return registry.copy(buildings = registry.buildings + ("wall" to registry.requireBuilding("wall").copy(buildWorkTicks = ticks)))
    }
}
