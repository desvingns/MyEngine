package dev.myengine.games.sandbox

import dev.myengine.ai.Job
import dev.myengine.ai.JobCompletionEffect
import dev.myengine.ai.JobStatus
import dev.myengine.entities.Entity
import dev.myengine.entities.EntityId
import dev.myengine.entities.EntityStore
import dev.myengine.entities.JobActorComponent
import dev.myengine.entities.PositionComponent
import dev.myengine.logistics.Inventory
import dev.myengine.world.TilePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SandboxJobExecutionTest {
    @Test
    fun twoWorkersCompetingForOneJobHaveDeterministicWinnerAndReplayHash() {
        val first = runCompetition()
        val second = runCompetition()

        assertEquals(EntityId(1), first.winner)
        assertEquals(first.hashAfterCompletion, second.hashAfterCompletion)
        assertEquals(first.state.inventory, second.state.inventory)
        assertEquals(mapOf("bolt" to 4), first.state.inventory.resources)
        assertEquals(JobStatus.DONE, first.state.jobBoard.get("only-job")?.status)
        assertNull(first.state.entities.require(EntityId(1)).jobActor?.assignedJobId)
        assertNull(first.state.entities.require(EntityId(2)).jobActor?.assignedJobId)
    }

    @Test
    fun sandboxAppliesCompletionResourceDeltasInResourceIdOrder() {
        val registry = SandboxGame.loadRegistry()
        val state = SandboxGame.createInitialState(registry).copy(
            entities = EntityStore(
                nextEntityId = 3,
                initialEntities = listOf(
                    worker(1, TilePosition(1, 1)),
                    worker(2, TilePosition(1, 1)),
                ),
            ),
            producers = emptyList(),
            inventory = Inventory(capacity = 1),
            jobBoard = dev.myengine.ai.JobBoard(
                listOf(
                    Job(
                        id = "a-job",
                        type = "haul",
                        target = TilePosition(1, 1),
                        priority = 1,
                        completionEffects = listOf(JobCompletionEffect.ResourceDelta("zeta", 1)),
                    ),
                    Job(
                        id = "b-job",
                        type = "build",
                        target = TilePosition(1, 1),
                        priority = 1,
                        completionEffects = listOf(JobCompletionEffect.ResourceDelta("alpha", 1)),
                    ),
                ),
            ),
        )

        SandboxRuntime(state).step()

        assertEquals(JobStatus.DONE, state.jobBoard.get("a-job")?.status)
        assertEquals(JobStatus.DONE, state.jobBoard.get("b-job")?.status)
        assertEquals(mapOf("alpha" to 1), state.inventory.resources)
        assertEquals("job_effect_rejected:zeta:1", state.lastCommandOrError)
    }

    @Test
    fun v14RoundtripPreservesInFlightJobBoardActorProgressAndEffects() {
        val registry = SandboxGame.loadRegistry()
        val worker = worker(7, TilePosition(5, 5)).copy(
            jobActor = JobActorComponent(assignedJobId = "in-flight", workTicks = 2),
            movement = dev.myengine.entities.MovementComponent(
                path = listOf(TilePosition(5, 5), TilePosition(6, 5), TilePosition(7, 5)),
                pathIndex = 1,
            ),
        )
        val job = Job(
            id = "in-flight",
            type = "haul",
            target = TilePosition(7, 5),
            priority = 9,
            reservedBy = EntityId(7),
            assignedTo = EntityId(7),
            status = JobStatus.IN_PROGRESS,
            failureReason = null,
            workTicks = 4,
            completionEffects = listOf(JobCompletionEffect.ResourceDelta("bolt", 3)),
        )
        val state = SandboxGame.createInitialState(registry).copy(
            entities = EntityStore(nextEntityId = 8, initialEntities = listOf(worker)),
            producers = emptyList(),
            inventory = Inventory(mapOf("bolt" to 2)),
            jobBoard = dev.myengine.ai.JobBoard(listOf(job)),
        )

        val save = SandboxSaveCodec.encode(state, seed = 41L)
        val restored = SandboxSaveCodec.decode(save, registry)

        assertEquals(22, SandboxSaveCodec.SAVE_VERSION)
        assertEquals("22", saveProperty(save, "saveVersion"))
        assertEquals(job, restored.jobBoard.get("in-flight"))
        assertEquals(worker, restored.entities.require(EntityId(7)))
        assertEquals(state.stableHash(), restored.stableHash())
    }

    @Test
    fun futureSaveVersionIsRejectedAfterV14Bump() {
        val registry = SandboxGame.loadRegistry()
        val valid = SandboxSession.start(registry).save()
        val future = valid.replace(
            "saveVersion=${SandboxSaveCodec.SAVE_VERSION}",
            "saveVersion=${SandboxSaveCodec.SAVE_VERSION + 1}",
        )

        assertTrue(valid != future)
        assertFailsWith<IllegalArgumentException> { SandboxSaveCodec.decode(future, registry) }
        assertFailsWith<IllegalArgumentException> { SandboxSession.restore(future, registry) }
    }

    private fun runCompetition(): CompetitionResult {
        val registry = SandboxGame.loadRegistry()
        val state = SandboxGame.createInitialState(registry).copy(
            entities = EntityStore(
                nextEntityId = 3,
                initialEntities = listOf(
                    worker(2, TilePosition(1, 2)),
                    worker(1, TilePosition(1, 1)),
                ),
            ),
            producers = emptyList(),
            inventory = Inventory(),
            jobBoard = dev.myengine.ai.JobBoard(
                listOf(
                    Job(
                        id = "only-job",
                        type = "haul",
                        target = TilePosition(1, 1),
                        priority = 100,
                        workTicks = 2,
                        completionEffects = listOf(JobCompletionEffect.ResourceDelta("bolt", 4)),
                    ),
                ),
            ),
        )
        val runtime = SandboxRuntime(state)
        runtime.step()
        assertEquals(EntityId(1), state.jobBoard.get("only-job")?.assignedTo)
        assertEquals(JobStatus.IN_PROGRESS, state.jobBoard.get("only-job")?.status)
        runtime.step()
        return CompetitionResult(state, EntityId(1), state.stableHash())
    }

    private fun worker(id: Long, position: TilePosition): Entity = Entity(
        id = EntityId(id),
        type = "worker",
        position = PositionComponent(position),
        jobActor = JobActorComponent(),
    )

    private fun saveProperty(text: String, key: String): String? =
        java.util.Properties().also { it.load(java.io.StringReader(text)) }.getProperty(key)

    private data class CompetitionResult(
        val state: SandboxState,
        val winner: EntityId,
        val hashAfterCompletion: String,
    )
}
