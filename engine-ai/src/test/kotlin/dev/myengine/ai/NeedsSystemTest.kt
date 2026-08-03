package dev.myengine.ai

import dev.myengine.content.NeedContent
import dev.myengine.entities.Entity
import dev.myengine.entities.EntityId
import dev.myengine.entities.EntityStore
import dev.myengine.entities.JobActorComponent
import dev.myengine.entities.NeedsComponent
import dev.myengine.entities.PositionComponent
import dev.myengine.world.TilePosition
import kotlin.test.Test
import kotlin.test.assertEquals

class NeedsSystemTest {
    private val definitions = mapOf(
        "hunger" to NeedContent("hunger", decayPerTick = 2, threshold = 25, recoveryAmount = 50, jobType = "eat", priority = 50),
        "rest" to NeedContent("rest", decayPerTick = 1, threshold = 25, recoveryAmount = 40, jobType = "sleep", priority = 50),
    )

    @Test
    fun thresholdCrossingCreatesOneDeterministicRecoveryJob() {
        val entities = EntityStore(
            nextEntityId = 2,
            initialEntities = listOf(
                Entity(
                    id = EntityId(1),
                    type = "colonist",
                    position = PositionComponent(TilePosition(2, 2)),
                    needs = NeedsComponent(mapOf("hunger" to 26, "rest" to 100)),
                ),
            ),
        )
        val jobs = JobBoard()

        val first = NeedsSystem(definitions).tick(entities, jobs)
        val second = NeedsSystem(definitions).tick(entities, jobs)

        assertEquals(listOf("need:1:hunger:0"), first.enqueuedJobIds)
        assertEquals(emptyList(), second.enqueuedJobIds)
        assertEquals(JobStatus.OPEN, jobs.get("need:1:hunger:0")?.status)
        assertEquals("eat", jobs.get("need:1:hunger:0")?.type)
        assertEquals(50, jobs.get("need:1:hunger:0")?.priority)
        assertEquals(22, entities.require(EntityId(1)).needs?.level("hunger"))
        assertEquals(1, entities.require(EntityId(1)).needs?.triggerCounts?.get("hunger"))
    }

    @Test
    fun needPriorityUsesStableJobBoardArbitrationAgainstWork() {
        val entities = EntityStore(
            nextEntityId = 2,
            initialEntities = listOf(
                Entity(
                    id = EntityId(1),
                    type = "colonist",
                    position = PositionComponent(TilePosition(2, 2)),
                    jobActor = JobActorComponent(),
                    needs = NeedsComponent(mapOf("hunger" to 26)),
                ),
            ),
        )
        val jobs = JobBoard(listOf(Job("work:1", "build", TilePosition(2, 2), priority = 50)))

        NeedsSystem(definitions).tick(entities, jobs)

        assertEquals("need:1:hunger:0", jobs.assignNext(EntityId(1))?.job?.id)
    }

    @Test
    fun tenThousandTicksRemainDeterministic() {
        fun run(): Pair<String, List<Job>> {
            val entities = EntityStore(
                nextEntityId = 2,
                initialEntities = listOf(
                    Entity(
                        id = EntityId(1),
                        type = "colonist",
                        position = PositionComponent(TilePosition(2, 2)),
                        needs = NeedsComponent(mapOf("hunger" to 100, "rest" to 100)),
                    ),
                ),
            )
            val jobs = JobBoard()
            val system = NeedsSystem(definitions)
            repeat(10_000) { system.tick(entities, jobs) }
            return entities.require(EntityId(1)).let { entity ->
                entity.needs.toString() to jobs.all()
            }
        }

        assertEquals(run(), run())
    }
}
