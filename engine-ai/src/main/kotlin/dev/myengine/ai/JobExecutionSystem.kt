package dev.myengine.ai

import dev.myengine.entities.EntityId
import dev.myengine.entities.EntityStore
import dev.myengine.entities.MovementComponent
import dev.myengine.entities.PositionComponent
import dev.myengine.world.TileWorld

data class JobExecutionReport(
    val completedJobIds: List<String> = emptyList(),
    val releasedJobIds: List<String> = emptyList(),
)

fun interface JobCompletionEffectSink {
    fun emit(worker: EntityId, job: Job, effect: JobCompletionEffect)
}

/**
 * Deterministic simulation system for the generic job-actor contract.
 *
 * Workers are discovered from the entity store and processed by ascending [EntityId]. A worker
 * claims the highest-priority open job, with job id as the stable tie-breaker. Movement is applied
 * directly to simulation-owned entity components; rendering and Android are not involved.
 */
class JobExecutionSystem(
    private val pathPlanner: AgentPathPlanner = AgentPathPlanner(),
    private val completionEffectSink: JobCompletionEffectSink = JobCompletionEffectSink { _, _, _ -> },
) {
    fun tick(
        world: TileWorld,
        entities: EntityStore,
        jobs: JobBoard,
    ): JobExecutionReport {
        val releasedThisTick = mutableSetOf<String>()
        val completed = mutableListOf<String>()
        val released = mutableListOf<String>()

        entities.all()
            .filter { it.jobActor != null && it.position != null }
            .sortedBy { it.id }
            .forEach { initialWorker ->
                var worker = entities.require(initialWorker.id)
                val actor = worker.jobActor ?: return@forEach
                val jobId = actor.assignedJobId
                if (jobId != null && jobId in releasedThisTick) {
                    clearWorker(entities, worker.id)
                    return@forEach
                }
                val job = if (jobId == null) {
                    jobs.assignNext(worker.id, releasedThisTick)?.job
                } else {
                    jobs.get(jobId)?.let { existing ->
                        when {
                            existing.status == JobStatus.OPEN -> jobs.claim(existing.id, worker.id)
                            existing.assignedTo == worker.id &&
                                existing.status != JobStatus.DONE &&
                                existing.status != JobStatus.COMPLETED &&
                                existing.status != JobStatus.FAILED -> existing
                            else -> null
                        }
                    }
                }

                if (job == null) {
                    if (jobId != null) clearWorker(entities, worker.id)
                    return@forEach
                }

                if (jobId == null) {
                    entities.update(worker.id) {
                        it.copy(jobActor = actor.copy(assignedJobId = job.id, workTicks = 0))
                    }
                    worker = entities.require(worker.id)
                }

                if (!world.inBounds(job.target)) {
                    release(entities, jobs, worker.id, job.id, "target_out_of_bounds", releasedThisTick, released)
                    return@forEach
                }

                val position = worker.position?.tile
                if (position == null || !world.inBounds(position)) {
                    release(entities, jobs, worker.id, job.id, "worker_out_of_bounds", releasedThisTick, released)
                    return@forEach
                }

                when (val plan = pathPlanner.plan(world, position, job.target, worker.movement)) {
                    is AgentPathPlan.NoPath -> {
                        release(entities, jobs, worker.id, job.id, plan.reason, releasedThisTick, released)
                        return@forEach
                    }
                    is AgentPathPlan.Kept -> worker = applyMovementPlan(entities, worker, plan.movement)
                    is AgentPathPlan.Repathed -> worker = applyMovementPlan(entities, worker, plan.movement)
                }

                val currentPosition = worker.position?.tile ?: return@forEach
                if (currentPosition != job.target) {
                    moveOneStep(world, entities, jobs, worker, job, releasedThisTick, released)
                    return@forEach
                }

                if (jobs.get(job.id)?.status == JobStatus.CLAIMED || jobs.get(job.id)?.status == JobStatus.ASSIGNED) {
                    jobs.start(job.id)
                }
                val currentActor = entities.require(worker.id).jobActor ?: return@forEach
                val nextWorkTicks = currentActor.workTicks + 1
                if (nextWorkTicks < job.workTicks) {
                    entities.update(worker.id) {
                        it.copy(
                            movement = null,
                            jobActor = currentActor.copy(workTicks = nextWorkTicks),
                        )
                    }
                    return@forEach
                }

                jobs.complete(job.id)
                job.completionEffects
                    .sortedWith(compareBy<JobCompletionEffect> { it.type.id }.thenBy {
                        (it as? JobCompletionEffect.ResourceDelta)?.resourceId.orEmpty()
                    })
                    .forEach { effect -> completionEffectSink.emit(worker.id, job, effect) }
                completed += job.id
                clearWorker(entities, worker.id)
            }

        return JobExecutionReport(completed.toList(), released.toList())
    }

    private fun applyMovementPlan(
        entities: EntityStore,
        worker: dev.myengine.entities.Entity,
        movement: MovementComponent,
    ): dev.myengine.entities.Entity {
        entities.update(worker.id) { it.copy(movement = movement) }
        return entities.require(worker.id)
    }

    private fun moveOneStep(
        world: TileWorld,
        entities: EntityStore,
        jobs: JobBoard,
        worker: dev.myengine.entities.Entity,
        job: Job,
        releasedThisTick: MutableSet<String>,
        released: MutableList<String>,
    ) {
        val movement = worker.movement
        val current = worker.position?.tile
        if (movement == null || current == null || movement.pathIndex + 1 !in movement.path.indices) {
            release(entities, jobs, worker.id, job.id, "invalid_path", releasedThisTick, released)
            return
        }
        val next = movement.path[movement.pathIndex + 1]
        if (!world.inBounds(next) || !world.canOccupy(next)) {
            release(entities, jobs, worker.id, job.id, "path_blocked", releasedThisTick, released)
            return
        }
        entities.update(worker.id) {
            it.copy(
                position = PositionComponent(next),
                movement = movement.copy(pathIndex = movement.pathIndex + 1),
            )
        }
    }

    private fun release(
        entities: EntityStore,
        jobs: JobBoard,
        workerId: EntityId,
        jobId: String,
        reason: String,
        releasedThisTick: MutableSet<String>,
        released: MutableList<String>,
    ) {
        jobs.release(jobId, reason)
        releasedThisTick += jobId
        released += jobId
        clearWorker(entities, workerId)
    }

    private fun clearWorker(entities: EntityStore, workerId: EntityId) {
        val worker = entities.get(workerId) ?: return
        val jobActor = worker.jobActor ?: return
        entities.update(workerId) {
            it.copy(
                movement = null,
                jobActor = jobActor.copy(assignedJobId = null, workTicks = 0),
            )
        }
    }
}
