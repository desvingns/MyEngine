package dev.myengine.games.sandbox

import dev.myengine.ai.HaulJobSpec
import dev.myengine.ai.HaulPhase
import dev.myengine.ai.Job
import dev.myengine.ai.JobBoard
import dev.myengine.ai.JobStatus
import dev.myengine.entities.Entity
import dev.myengine.entities.EntityId
import dev.myengine.entities.EntityStore
import dev.myengine.entities.JobActorComponent
import dev.myengine.entities.PositionComponent
import dev.myengine.entities.WorkerComponent
import dev.myengine.logistics.HaulSource
import dev.myengine.logistics.HaulSourceStore
import dev.myengine.logistics.Inventory
import dev.myengine.logistics.Producer
import dev.myengine.logistics.StockpileZone
import dev.myengine.logistics.ZoneStore
import dev.myengine.world.TilePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SandboxHaulingTest {
    @Test
    fun workerMovesSourceItemToStockpileWithDeterministicReservation() {
        val first = runHaul()
        val second = runHaul()

        assertEquals(JobStatus.DONE, first.jobBoard.get("haul-1")?.status)
        assertEquals(mapOf("bolt" to 2), first.zones.stockpile("stockpile")?.storedResources)
        assertEquals(0, first.haulSources.get("source")?.resources?.get("bolt") ?: 0)
        assertNull(first.entities.require(EntityId(1)).inventory)
        assertEquals(first.stableHash(), second.stableHash())
    }

    @Test
    fun twoHaulJobsCannotReserveTheSameSourceAmount() {
        val registry = SandboxGame.loadRegistry()
        val state = SandboxGame.createInitialState(registry).copy(
            entities = EntityStore(
                nextEntityId = 3,
                initialEntities = listOf(worker(1, TilePosition(1, 1)), worker(2, TilePosition(1, 1))),
            ),
            producers = emptyList(),
            inventory = Inventory(),
            jobBoard = JobBoard(listOf(haul("haul-a", TilePosition(4, 1)), haul("haul-b", TilePosition(4, 1))),),
            zones = ZoneStore(listOf(StockpileZone("stockpile", listOf(TilePosition(4, 1)), setOf("bolt")))),
            haulSources = HaulSourceStore(listOf(HaulSource("source", TilePosition(1, 1), mapOf("bolt" to 2)))),
        )

        SandboxRuntime(state).step()

        assertEquals(EntityId(1), state.jobBoard.get("haul-a")?.reservedBy)
        assertEquals(JobStatus.OPEN, state.jobBoard.get("haul-b")?.status)
        assertEquals(0, state.haulSources.get("source")?.resources?.get("bolt") ?: 0)
        assertTrue(state.entities.require(EntityId(1)).inventory?.resources == mapOf("bolt" to 2))
    }

    @Test
    fun midCarrySaveRoundtripPreservesCarryPhaseAndReservation() {
        val registry = SandboxGame.loadRegistry()
        val state = baseState()
        val runtime = SandboxRuntime(state)
        runtime.step()
        assertEquals(HaulPhase.TO_STOCKPILE, state.jobBoard.get("haul-1")?.haul?.phase)
        assertEquals(mapOf("bolt" to 2), state.entities.require(EntityId(1)).inventory?.resources)

        val save = SandboxSaveCodec.encode(state, seed = 7L)
        val restored = SandboxSaveCodec.decode(save, registry)

        assertEquals(17, SandboxSaveCodec.SAVE_VERSION)
        assertEquals(state.stableHash(), restored.stableHash())
        assertEquals(state.entities.require(EntityId(1)), restored.entities.require(EntityId(1)))
        assertEquals(state.haulSources.all(), restored.haulSources.all())
        assertEquals(HaulPhase.TO_STOCKPILE, restored.jobBoard.get("haul-1")?.haul?.phase)
    }

    @Test
    fun positionedProducerOutputBecomesHaulSourceInsteadOfGlobalInventory() {
        val registry = SandboxGame.loadRegistry()
        val state = SandboxGame.createInitialState(registry).copy(
            producers = listOf(Producer("producer-1", "bolt-generator", progressTicks = 4, position = TilePosition(2, 2))),
            inventory = Inventory(),
        )

        SandboxRuntime(state).step()

        assertEquals(0, state.inventory.amount("bolt"))
        assertEquals(1, state.haulSources.get("producer:producer-1")?.resources?.get("bolt"))
    }

    @Test
    fun workerCapacityRejectsHaulBeforeSourceConsumption() {
        val state = baseState().also {
            it.jobBoard = JobBoard(listOf(
                Job(
                    id = "too-large",
                    type = "haul",
                    target = TilePosition(4, 1),
                    priority = 10,
                    haul = HaulJobSpec("source", "bolt", 5, "stockpile"),
                ),
            ))
            it.haulSources = HaulSourceStore(listOf(HaulSource("source", TilePosition(1, 1), mapOf("bolt" to 5))))
        }

        SandboxRuntime(state).step()

        assertEquals(JobStatus.OPEN, state.jobBoard.get("too-large")?.status)
        assertEquals(5, state.haulSources.get("source")?.resources?.get("bolt"))
        assertNull(state.entities.require(EntityId(1)).inventory)
    }

    private fun runHaul(): SandboxState {
        val state = baseState()
        SandboxRuntime(state).step(3)
        return state
    }

    private fun baseState(): SandboxState {
        val registry = SandboxGame.loadRegistry()
        return SandboxGame.createInitialState(registry).copy(
            entities = EntityStore(nextEntityId = 2, initialEntities = listOf(worker(1, TilePosition(1, 2)))),
            producers = emptyList(),
            inventory = Inventory(),
            jobBoard = JobBoard(listOf(haul("haul-1", TilePosition(4, 1)))),
            zones = ZoneStore(listOf(StockpileZone("stockpile", listOf(TilePosition(4, 1)), setOf("bolt")))),
            haulSources = HaulSourceStore(listOf(HaulSource("source", TilePosition(1, 1), mapOf("bolt" to 2)))),
        )
    }

    private fun haul(id: String, target: TilePosition): Job = Job(
        id = id,
        type = "haul",
        target = target,
        priority = 10,
        haul = HaulJobSpec("source", "bolt", 2, "stockpile"),
    )

    private fun worker(id: Long, position: TilePosition): Entity = Entity(
        id = EntityId(id),
        type = "worker:hauler",
        tags = setOf("worker"),
        position = PositionComponent(position),
        jobActor = JobActorComponent(),
        worker = WorkerComponent("hauler"),
    )
}
