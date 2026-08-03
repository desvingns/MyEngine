package dev.myengine.games.sandbox

import dev.myengine.ai.HaulJobSpec
import dev.myengine.ai.Job
import dev.myengine.ai.JobBoard
import dev.myengine.entities.Entity
import dev.myengine.entities.EntityId
import dev.myengine.entities.JobActorComponent
import dev.myengine.entities.PositionComponent
import dev.myengine.entities.WorkerComponent
import dev.myengine.logistics.Inventory
import dev.myengine.logistics.StockpileZone
import dev.myengine.logistics.ZoneStore
import dev.myengine.core.CommandId
import dev.myengine.core.Tick
import dev.myengine.core.command.PlaceBuildingCommand
import dev.myengine.core.command.RemoveBuildingCommand
import dev.myengine.core.command.TileCoordinate
import dev.myengine.world.TilePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SandboxExtractorTest {
    @Test
    fun finiteExtractorUsesUnderlyingNodeAndEmitsPartialFinalBatch() {
        val state = extractorState()
        state.world.setTile(
            TilePosition(5, 5),
            state.world.tileAt(TilePosition(5, 5)).tile.copy(
                resourceNode = state.world.tileAt(TilePosition(5, 5)).tile.resourceNode!!.copy(amount = 3),
            ),
        )
        val runtime = SandboxRuntime(state)

        runtime.submit(place(1, 5, 5))
        runtime.step(3)

        assertEquals("placed:1", state.lastCommandOrError)
        assertEquals(1, state.producers.size)
        assertEquals(TilePosition(5, 5), state.producers.single().resourceNodePosition)
        assertEquals(3, state.haulSources.get("producer:extractor:1")?.resources?.get("bolt"))
        assertEquals(0, state.world.tileAt(TilePosition(5, 5)).tile.resourceNode?.amount)
    }

    @Test
    fun extractorCanUseAdjacentNodeButRejectsUnrelatedPlacement() {
        val state = extractorState()
        state.inventory = Inventory(mapOf("bolt" to 4))
        val runtime = SandboxRuntime(state)

        runtime.submit(place(1, 6, 5))
        runtime.submit(place(2, 20, 20))
        runtime.step(2)

        assertEquals("tile_not_buildable", state.lastCommandOrError)
        assertEquals(TilePosition(5, 5), state.producers.single().resourceNodePosition)
        assertEquals(TilePosition(6, 5), state.entities.byTag("building").single().position?.tile)
    }

    @Test
    fun infiniteExtractorLeavesNodeAmountUnchanged() {
        val loaded = SandboxGame.loadRegistry()
        val baseMap = loaded.requireMap("sandbox-canonical")
        val infiniteMap = baseMap.copy(
            terrainMapping = baseMap.terrainMapping.mapValues { (_, symbol) ->
                symbol.copy(resourceNode = symbol.resourceNode?.copy(infinite = true))
            },
        )
        val registry = loaded.copy(maps = loaded.maps + (infiniteMap.id to infiniteMap))
        val state = SandboxGame.createInitialState(registry, mapId = infiniteMap.id).also {
            it.inventory = Inventory(mapOf("bolt" to 2))
            it.producers = emptyList()
        }
        val runtime = SandboxRuntime(state)

        runtime.submit(place(1, 5, 5))
        runtime.step(3)

        assertEquals(100, state.world.tileAt(TilePosition(5, 5)).tile.resourceNode?.amount)
        assertTrue(state.world.tileAt(TilePosition(5, 5)).tile.resourceNode?.infinite == true)
        assertEquals(7, state.haulSources.get("producer:extractor:1")?.resources?.get("bolt"))
    }

    @Test
    fun extractorOutputIsConsumableByHaulingAndSourceStaysStableAcrossSave() {
        val state = extractorState()
        val runtime = SandboxRuntime(state)
        runtime.submit(place(1, 6, 5))
        runtime.step(3)

        val source = assertNotNull(state.haulSources.get("producer:extractor:1"))
        assertTrue(source.position != TilePosition(6, 5))
        val save = SandboxSaveCodec.encode(state, seed = 7L)
        val restored = SandboxSaveCodec.decode(save, SandboxGame.loadRegistry())
        assertEquals(state.stableHash(), restored.stableHash())
        assertEquals(source.position, restored.haulSources.get(source.id)?.position)

        state.producers = emptyList()
        state.entities.upsert(worker(2, source.position))
        state.jobBoard = JobBoard(
            listOf(
                Job(
                    id = "haul-extractor",
                    type = "haul",
                    target = TilePosition(8, 4),
                    priority = 10,
                    haul = HaulJobSpec(source.id, "bolt", 2, "stockpile"),
                ),
            ),
        )
        state.zones = ZoneStore(listOf(StockpileZone("stockpile", listOf(TilePosition(8, 4)), setOf("bolt"))))
        SandboxRuntime(state).step(4)

        assertEquals(2, state.zones.stockpile("stockpile")?.storedResources?.get("bolt"))
        assertEquals(5, state.haulSources.get(source.id)?.resources?.get("bolt"))
    }

    @Test
    fun extractorSavePreservesNodeBindingAndProgress() {
        val state = extractorState()
        val runtime = SandboxRuntime(state)
        runtime.submit(place(1, 5, 5))
        runtime.step()
        val save = SandboxSaveCodec.encode(state, seed = 7L)
        val restored = SandboxSaveCodec.decode(save, SandboxGame.loadRegistry())

        assertEquals(state.stableHash(), restored.stableHash())
        assertEquals(state.producers, restored.producers)
        assertEquals(1, restored.producers.single().progressTicks)
        assertEquals(100, restored.world.tileAt(TilePosition(5, 5)).tile.resourceNode?.amount)

        SandboxRuntime(state).step()
        SandboxRuntime(restored).step()
        assertEquals(state.stableHash(), restored.stableHash())
        assertEquals(2, restored.producers.single().progressTicks)
    }

    @Test
    fun removingExtractorRemovesProducerAndEmptyOutputSource() {
        val state = extractorState()
        val runtime = SandboxRuntime(state)
        runtime.submit(place(1, 5, 5))
        runtime.step()
        runtime.submit(RemoveBuildingCommand(CommandId(2), Tick(2), 1L))
        runtime.step()

        assertNull(state.entities.get(EntityId(1)))
        assertTrue(state.producers.isEmpty())
        assertNull(state.haulSources.get("producer:extractor:1"))
        assertEquals("removed:1", state.lastCommandOrError)
    }

    private fun extractorState(): SandboxState = SandboxGame.createInitialState(SandboxGame.loadRegistry()).also {
        it.inventory = Inventory(mapOf("bolt" to 2))
        it.producers = emptyList()
    }

    private fun place(id: Long, x: Int, y: Int) =
        PlaceBuildingCommand(CommandId(id), Tick(id), "bolt-extractor", TileCoordinate(x, y))

    private fun worker(id: Long, position: TilePosition): Entity = Entity(
        id = EntityId(id),
        type = "worker:hauler",
        tags = setOf("worker"),
        position = PositionComponent(position),
        jobActor = JobActorComponent(),
        worker = WorkerComponent("hauler"),
    )
}
