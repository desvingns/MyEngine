package dev.myengine.logistics

import dev.myengine.ai.HaulDestinationKind
import dev.myengine.ai.HaulJobSpec
import dev.myengine.ai.Job
import dev.myengine.ai.JobBoard
import dev.myengine.entities.Entity
import dev.myengine.entities.EntityId
import dev.myengine.entities.EntityStore
import dev.myengine.entities.JobActorComponent
import dev.myengine.entities.PositionComponent
import dev.myengine.entities.WorkerComponent
import dev.myengine.content.WorkerContent
import dev.myengine.world.TerrainRule
import dev.myengine.world.TilePosition
import dev.myengine.world.TileWorld
import dev.myengine.world.WorldSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConstructionSiteTest {
    @Test
    fun sourceAvailabilityOnlySubtractsReservationsForTheRequestedResource() {
        val sources = HaulSourceStore(
            listOf(HaulSource("multi", TilePosition(0, 0), mapOf("bolt" to 2, "wood" to 5))),
        )

        assertTrue(sources.reserve("multi", "bolt-job", "bolt", 2))

        assertEquals(0, sources.get("multi")?.available("bolt"))
        assertEquals(5, sources.get("multi")?.available("wood"))
    }

    @Test
    fun siteTracksDeliveredMaterialByOriginalSourceAndRefundableHashState() {
        val first = ConstructionSiteStore(
            listOf(ConstructionSite("construction:1", "wall", TilePosition(2, 0), "bolt", 3)),
        )
        assertTrue(first.deposit("construction:1", "source-a", "bolt", 2))
        assertEquals(2, first.get("construction:1")?.deliveredAmount)
        assertEquals(mapOf("source-a" to 2), first.get("construction:1")?.deliveredBySource)
        assertEquals(first.all(), ConstructionSiteStore(first.all()).all())
    }

    @Test
    fun injectableDestinationSinkReceivesConstructionHaulWithoutUsingStockpiles() {
        val world = TileWorld.filled(
            WorldSize(4, 1),
            mapOf("floor" to TerrainRule("floor", buildable = true, blocksMovement = false)),
            "floor",
        )
        val worker = Entity(
            id = EntityId(1),
            type = "worker:hauler",
            position = PositionComponent(TilePosition(0, 0)),
            jobActor = JobActorComponent(),
            worker = WorkerComponent("hauler"),
        )
        val entities = EntityStore(nextEntityId = 2, initialEntities = listOf(worker))
        val jobs = JobBoard(
            listOf(
                Job(
                    id = "construction-haul:1",
                    type = "construction_haul",
                    target = TilePosition(3, 0),
                    priority = 1,
                    haul = HaulJobSpec(
                        sourceId = "source-a",
                        resourceId = "bolt",
                        amount = 2,
                        destinationZoneId = "construction:1",
                        destinationKind = HaulDestinationKind.CONSTRUCTION,
                    ),
                ),
            ),
        )
        val sources = HaulSourceStore(listOf(HaulSource("source-a", TilePosition(0, 0), mapOf("bolt" to 2))))
        val deposits = mutableListOf<String>()

        val system = HaulingSystem()
        system.tick(
            world,
            entities,
            jobs,
            sources,
            ZoneStore(),
            mapOf("hauler" to WorkerContent("hauler", speedTilesPerTick = 3, capacity = 2)),
            destinationSink = dev.myengine.logistics.HaulDestinationSink { job, _, resourceId, amount ->
                deposits += "${job.haul!!.destinationZoneId}:$resourceId:$amount"
                true
            },
        )
        system.tick(
            world,
            entities,
            jobs,
            sources,
            ZoneStore(),
            mapOf("hauler" to WorkerContent("hauler", speedTilesPerTick = 3, capacity = 2)),
            destinationSink = dev.myengine.logistics.HaulDestinationSink { job, _, resourceId, amount ->
                deposits += "${job.haul!!.destinationZoneId}:$resourceId:$amount"
                true
            },
        )

        assertEquals(listOf("construction:1:bolt:2"), deposits)
        assertEquals(0, sources.get("source-a")?.resources?.get("bolt") ?: 0)
        assertEquals(dev.myengine.ai.JobStatus.DONE, jobs.get("construction-haul:1")?.status)
    }
}
