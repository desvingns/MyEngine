package dev.myengine.ai

import dev.myengine.entities.Entity
import dev.myengine.entities.EntityId
import dev.myengine.entities.EntityStore
import dev.myengine.entities.JobActorComponent
import dev.myengine.entities.MovementComponent
import dev.myengine.entities.PositionComponent
import dev.myengine.world.TerrainRule
import dev.myengine.world.TilePosition
import dev.myengine.world.TileWorld
import dev.myengine.world.WorldSize
import dev.myengine.world.WorldTile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JobExecutionSystemTest {
    private val terrain = mapOf(
        "floor" to TerrainRule("floor", buildable = true, blocksMovement = false),
        "wall" to TerrainRule("wall", buildable = false, blocksMovement = true),
    )

    @Test
    fun assignmentUsesEntityIdOrderThenPriorityAndJobIdOrder() {
        val jobs = JobBoard(
            listOf(
                Job("job-b", "haul", TilePosition(4, 0), priority = 10),
                Job("job-a", "build", TilePosition(4, 0), priority = 10),
                Job("job-c", "mine", TilePosition(4, 0), priority = 1),
            ),
        )
        val entities = entities(
            worker(2, TilePosition(0, 0)),
            worker(1, TilePosition(0, 1)),
        )

        JobExecutionSystem().tick(openWorld(), entities, jobs)

        assertEquals(EntityId(1), jobs.get("job-a")?.assignedTo)
        assertEquals(JobStatus.CLAIMED, jobs.get("job-a")?.status)
        assertEquals(EntityId(2), jobs.get("job-b")?.assignedTo)
        assertEquals(JobStatus.CLAIMED, jobs.get("job-b")?.status)
        assertEquals(JobStatus.OPEN, jobs.get("job-c")?.status)
        assertEquals("job-a", entities.require(EntityId(1)).jobActor?.assignedJobId)
        assertEquals("job-b", entities.require(EntityId(2)).jobActor?.assignedJobId)
    }

    @Test
    fun onlyEntitiesWithJobActorAndPositionAreEligibleWorkers() {
        val jobs = JobBoard(listOf(Job("only", "haul", TilePosition(0, 0), priority = 1)))
        val entities = entities(
            Entity(
                id = EntityId(1),
                type = "worker-without-position",
                jobActor = JobActorComponent(),
            ),
            Entity(
                id = EntityId(2),
                type = "position-only",
                position = PositionComponent(TilePosition(0, 0)),
            ),
        )

        JobExecutionSystem().tick(openWorld(), entities, jobs)

        assertEquals(JobStatus.OPEN, jobs.get("only")?.status)
        assertNull(entities.require(EntityId(1)).jobActor?.assignedJobId)
    }

    @Test
    fun lifecycleMovesOneStepPerTickThenCountsWorkTicksAndEmitsSortedResourceEffects() {
        val effects = mutableListOf<JobCompletionEffect>()
        val jobs = JobBoard(
            listOf(
                Job(
                    id = "haul-1",
                    type = "haul",
                    target = TilePosition(3, 0),
                    priority = 1,
                    workTicks = 2,
                    completionEffects = listOf(
                        JobCompletionEffect.ResourceDelta("zinc", 2),
                        JobCompletionEffect.ResourceDelta("amber", 1),
                    ),
                ),
            ),
        )
        val entities = entities(worker(1, TilePosition(0, 0)))
        val system = JobExecutionSystem(
            completionEffectSink = JobCompletionEffectSink { _, _, effect -> effects += effect },
        )

        system.tick(openWorld(), entities, jobs)
        assertEquals(TilePosition(1, 0), entities.require(EntityId(1)).position?.tile)
        assertEquals(JobStatus.CLAIMED, jobs.get("haul-1")?.status)
        assertEquals(0, entities.require(EntityId(1)).jobActor?.workTicks)

        system.tick(openWorld(), entities, jobs)
        assertEquals(TilePosition(2, 0), entities.require(EntityId(1)).position?.tile)
        assertEquals(JobStatus.CLAIMED, jobs.get("haul-1")?.status)

        system.tick(openWorld(), entities, jobs)
        assertEquals(TilePosition(3, 0), entities.require(EntityId(1)).position?.tile)
        assertEquals(JobStatus.CLAIMED, jobs.get("haul-1")?.status)

        system.tick(openWorld(), entities, jobs)
        assertEquals(JobStatus.IN_PROGRESS, jobs.get("haul-1")?.status)
        assertEquals(1, entities.require(EntityId(1)).jobActor?.workTicks)
        assertNull(entities.require(EntityId(1)).movement)

        system.tick(openWorld(), entities, jobs)
        assertEquals(JobStatus.DONE, jobs.get("haul-1")?.status)
        assertNull(jobs.get("haul-1")?.assignedTo)
        assertNull(jobs.get("haul-1")?.reservedBy)
        assertNull(entities.require(EntityId(1)).jobActor?.assignedJobId)
        assertEquals(0, entities.require(EntityId(1)).jobActor?.workTicks)
        assertEquals(
            listOf<JobCompletionEffect>(
                JobCompletionEffect.ResourceDelta("amber", 1),
                JobCompletionEffect.ResourceDelta("zinc", 2),
            ),
            effects,
        )
    }

    @Test
    fun completionSinkEmitsSpawnBuildingEffectWithJobTarget() {
        val effects = mutableListOf<JobCompletionEffect>()
        val jobs = JobBoard(
            listOf(
                Job(
                    id = "construction-build:1",
                    type = "construction_build",
                    target = TilePosition(0, 0),
                    priority = 1,
                    completionEffects = listOf(JobCompletionEffect.SpawnBuilding("wall", "construction:1")),
                ),
            ),
        )
        val entities = entities(worker(1, TilePosition(0, 0)))

        JobExecutionSystem(
            completionEffectSink = JobCompletionEffectSink { _, _, effect -> effects += effect },
        ).tick(openWorld(), entities, jobs)

        assertEquals(JobStatus.DONE, jobs.get("construction-build:1")?.status)
        assertEquals(
            listOf<JobCompletionEffect>(JobCompletionEffect.SpawnBuilding("wall", "construction:1")),
            effects,
        )
    }

    @Test
    fun invalidTargetReleasesAndCannotBeReclaimedByAnotherWorkerInTheSameTick() {
        val jobs = JobBoard(
            listOf(
                Job("invalid", "haul", TilePosition(-1, 0), priority = 20),
                Job("valid", "build", TilePosition(0, 1), priority = 1, workTicks = 2),
            ),
        )
        val entities = entities(
            worker(
                1,
                TilePosition(0, 0),
                movement = MovementComponent(listOf(TilePosition(0, 0), TilePosition(1, 0))),
            ),
            worker(2, TilePosition(0, 1)),
        )

        JobExecutionSystem().tick(openWorld(), entities, jobs)

        assertEquals(JobStatus.OPEN, jobs.get("invalid")?.status)
        assertEquals("target_out_of_bounds", jobs.get("invalid")?.failureReason)
        assertNull(jobs.get("invalid")?.assignedTo)
        assertNull(jobs.get("invalid")?.reservedBy)
        assertEquals(JobStatus.IN_PROGRESS, jobs.get("valid")?.status)
        assertEquals(EntityId(2), jobs.get("valid")?.assignedTo)
        assertNull(entities.require(EntityId(1)).movement)
        assertNull(entities.require(EntityId(1)).jobActor?.assignedJobId)
    }

    @Test
    fun noPathReleasesJobAndClearsExistingMovement() {
        val world = openWorld(width = 3, height = 1).also {
            it.setTile(TilePosition(1, 0), WorldTile("wall"))
        }
        val jobs = JobBoard(
            listOf(Job("sealed", "mine", TilePosition(2, 0), priority = 1)),
        )
        val entities = entities(
            worker(
                1,
                TilePosition(0, 0),
                movement = MovementComponent(
                    path = listOf(TilePosition(0, 0), TilePosition(1, 0), TilePosition(2, 0)),
                ),
            ),
        )

        JobExecutionSystem().tick(world, entities, jobs)

        assertEquals(JobStatus.OPEN, jobs.get("sealed")?.status)
        assertEquals("unreachable", jobs.get("sealed")?.failureReason)
        assertNull(jobs.get("sealed")?.assignedTo)
        assertNull(jobs.get("sealed")?.reservedBy)
        assertNull(entities.require(EntityId(1)).movement)
        assertNull(entities.require(EntityId(1)).jobActor?.assignedJobId)
    }

    @Test
    fun staleAssignedActorIsClearedWithoutSameTickReclaimOrReprocess() {
        val world = openWorld(width = 3, height = 1).also {
            it.setTile(TilePosition(1, 0), WorldTile("wall"))
        }
        val jobs = JobBoard(
            listOf(
                Job(
                    id = "shared",
                    type = "haul",
                    target = TilePosition(2, 0),
                    priority = 10,
                    reservedBy = EntityId(1),
                    assignedTo = EntityId(1),
                    status = JobStatus.CLAIMED,
                    workTicks = 2,
                ),
            ),
        )
        val entities = entities(
            worker(2, TilePosition(2, 0), assignedJobId = "shared"),
            worker(1, TilePosition(0, 0), assignedJobId = "shared"),
        )

        val report = JobExecutionSystem().tick(world, entities, jobs)

        assertEquals(listOf("shared"), report.releasedJobIds)
        assertEquals(JobStatus.OPEN, jobs.get("shared")?.status)
        assertEquals("unreachable", jobs.get("shared")?.failureReason)
        assertNull(jobs.get("shared")?.assignedTo)
        assertNull(jobs.get("shared")?.reservedBy)
        assertEquals(JobActorComponent(), entities.require(EntityId(1)).jobActor)
        assertEquals(JobActorComponent(), entities.require(EntityId(2)).jobActor)
        assertNull(entities.require(EntityId(1)).movement)
        assertNull(entities.require(EntityId(2)).movement)
        assertEquals(TilePosition(2, 0), entities.require(EntityId(2)).position?.tile)
    }

    private fun openWorld(width: Int = 5, height: Int = 2): TileWorld =
        TileWorld.filled(WorldSize(width, height), terrain, "floor")

    private fun entities(vararg workers: Entity): EntityStore = EntityStore(
        nextEntityId = workers.maxOfOrNull { it.id.value }?.plus(1) ?: 1,
        initialEntities = workers.toList(),
    )

    private fun worker(
        id: Long,
        position: TilePosition,
        movement: MovementComponent? = null,
        assignedJobId: String? = null,
        workTicks: Int = 0,
    ): Entity = Entity(
        id = EntityId(id),
        type = "worker",
        position = PositionComponent(position),
        movement = movement,
        jobActor = JobActorComponent(assignedJobId = assignedJobId, workTicks = workTicks),
    )
}
