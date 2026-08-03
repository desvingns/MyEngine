package dev.myengine.ai

import dev.myengine.content.NeedContent
import dev.myengine.entities.EntityId
import dev.myengine.entities.EntityStore
import dev.myengine.entities.NeedsComponent

data class NeedsTickReport(
    val enqueuedJobIds: List<String> = emptyList(),
)

/**
 * Deterministic need decay and threshold-job producer. Entities and definitions are processed in
 * stable id order; one threshold episode produces one uniquely numbered job.
 */
class NeedsSystem(
    private val definitions: Map<String, NeedContent>,
) {
    fun tick(entities: EntityStore, jobs: JobBoard): NeedsTickReport {
        val enqueued = mutableListOf<String>()
        entities.all()
            .filter { it.needs != null && it.position != null }
            .sortedBy { it.id }
            .forEach { entity ->
                val current = entity.needs ?: return@forEach
                var next = current.withLevels(
                    current.levels + definitions.keys.associateWith { current.level(it) },
                )
                definitions.toSortedMap().forEach { (needId, definition) ->
                    val before = current.level(needId)
                    val after = (before - definition.decayPerTick).coerceAtLeast(0)
                    next = next.withLevels(next.levels + (needId to after))
                    val crossing = (before > definition.threshold && after <= definition.threshold) ||
                        (before <= definition.threshold && (current.triggerCounts[needId] ?: 0) == 0)
                    if (!crossing || hasActiveJob(jobs, entity.id, needId)) return@forEach

                    val trigger = next.triggerCounts[needId] ?: 0
                    val jobId = needJobId(entity.id, needId, trigger)
                    jobs.add(
                        Job(
                            id = jobId,
                            type = definition.jobType,
                            target = entity.position!!.tile,
                            priority = definition.priority,
                            workTicks = 1,
                            completionEffects = listOf(
                                JobCompletionEffect.NeedRecovery(
                                    needId = needId,
                                    amount = definition.recoveryAmount,
                                    targetEntityId = entity.id,
                                ),
                            ),
                        ),
                    )
                    next = next.copy(triggerCounts = next.triggerCounts + (needId to trigger + 1))
                    enqueued += jobId
                }
                entities.update(entity.id) { it.copy(needs = next) }
            }
        return NeedsTickReport(enqueued.toList())
    }

    private fun hasActiveJob(jobs: JobBoard, entityId: EntityId, needId: String): Boolean = jobs.all().any { job ->
        job.completionEffects.any { effect ->
            effect is JobCompletionEffect.NeedRecovery &&
                effect.targetEntityId == entityId &&
                effect.needId == needId
        } && job.status in setOf(JobStatus.OPEN, JobStatus.CLAIMED, JobStatus.IN_PROGRESS, JobStatus.ASSIGNED)
    }
}

fun needJobId(entityId: EntityId, needId: String, trigger: Int): String = "need:${entityId.value}:$needId:$trigger"
