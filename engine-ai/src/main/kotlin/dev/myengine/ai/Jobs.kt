package dev.myengine.ai

import dev.myengine.core.StableHash
import dev.myengine.entities.EntityId
import dev.myengine.world.TilePosition

enum class JobStatus {
    OPEN,
    CLAIMED,
    IN_PROGRESS,
    DONE,
    /** Legacy name accepted when reading callers that used the pre-execution model. */
    ASSIGNED,
    /** Legacy terminal name; new execution code writes [DONE]. */
    COMPLETED,
    FAILED,
}

enum class JobEffectType(val id: String) {
    RESOURCE_DELTA("resource_delta"),
}

sealed interface JobCompletionEffect {
    val type: JobEffectType

    data class ResourceDelta(
        val resourceId: String,
        val amount: Int,
    ) : JobCompletionEffect {
        override val type: JobEffectType = JobEffectType.RESOURCE_DELTA

        init {
            require(resourceId.isNotBlank()) { "Resource id cannot be blank." }
        }
    }
}

data class Job(
    val id: String,
    val type: String,
    val target: TilePosition,
    val priority: Int,
    val reservedBy: EntityId? = null,
    val assignedTo: EntityId? = null,
    val status: JobStatus = JobStatus.OPEN,
    val failureReason: String? = null,
    val workTicks: Int = 1,
    val completionEffects: List<JobCompletionEffect> = emptyList(),
) {
    init {
        require(id.isNotBlank()) { "Job id cannot be blank." }
        require(type.isNotBlank()) { "Job type cannot be blank." }
        require(workTicks > 0) { "Job work ticks must be positive." }
    }
}

data class JobAssignment(
    val actor: EntityId,
    val job: Job,
)

class JobBoard(initialJobs: List<Job> = emptyList()) {
    private val jobs = sortedMapOf<String, Job>()

    init {
        initialJobs.forEach { jobs[it.id] = it }
    }

    fun add(job: Job) {
        require(!jobs.containsKey(job.id)) { "Duplicate job '${job.id}'." }
        jobs[job.id] = job
    }

    fun assignNext(actor: EntityId, excludedJobIds: Set<String> = emptySet()): JobAssignment? {
        val candidate = jobs.values
            .filter {
                it.id !in excludedJobIds &&
                    it.status == JobStatus.OPEN &&
                    (it.reservedBy == null || it.reservedBy == actor)
            }
            .sortedWith(compareByDescending<Job> { it.priority }.thenBy { it.id })
            .firstOrNull()
            ?: return null
        val assigned = candidate.copy(
            status = JobStatus.CLAIMED,
            assignedTo = actor,
            reservedBy = actor,
            failureReason = null,
        )
        jobs[assigned.id] = assigned
        return JobAssignment(actor, assigned)
    }

    fun claim(jobId: String, actor: EntityId): Job {
        val current = jobs[jobId] ?: error("Unknown job '$jobId'.")
        require(current.status == JobStatus.OPEN || current.status == JobStatus.CLAIMED || current.status == JobStatus.ASSIGNED) {
            "Job '$jobId' cannot be claimed from ${current.status}."
        }
        require(current.assignedTo == null || current.assignedTo == actor) {
            "Job '$jobId' is assigned to ${current.assignedTo}."
        }
        require(current.reservedBy == null || current.reservedBy == actor) {
            "Job '$jobId' is reserved by ${current.reservedBy}."
        }
        return current.copy(
            status = JobStatus.CLAIMED,
            assignedTo = actor,
            reservedBy = actor,
            failureReason = null,
        ).also { jobs[jobId] = it }
    }

    fun start(jobId: String): Job {
        val current = jobs[jobId] ?: error("Unknown job '$jobId'.")
        require(current.status == JobStatus.CLAIMED || current.status == JobStatus.ASSIGNED) {
            "Job '$jobId' cannot start from ${current.status}."
        }
        return current.copy(status = JobStatus.IN_PROGRESS).also { jobs[jobId] = it }
    }

    fun complete(jobId: String) {
        update(jobId) {
            it.copy(
                status = JobStatus.DONE,
                reservedBy = null,
                assignedTo = null,
            )
        }
    }

    fun fail(jobId: String, reason: String) {
        update(jobId) {
            it.copy(
                status = JobStatus.FAILED,
                reservedBy = null,
                assignedTo = null,
                failureReason = reason,
            )
        }
    }

    /** Returns an interrupted job to the queue without allowing the caller to reclaim it yet. */
    fun release(jobId: String, reason: String): Job {
        val current = jobs[jobId] ?: error("Unknown job '$jobId'.")
        return current.copy(
            status = JobStatus.OPEN,
            reservedBy = null,
            assignedTo = null,
            failureReason = reason,
        ).also { jobs[jobId] = it }
    }

    fun get(jobId: String): Job? = jobs[jobId]

    fun all(): List<Job> = jobs.values.toList()

    fun appendHash(hash: StableHash) {
        if (jobs.isEmpty()) return
        hash.add("jobs")
        all().forEach { job ->
            hash.add(job.id)
                .add(job.type)
                .add(job.target.x)
                .add(job.target.y)
                .add(job.priority)
                .add(job.reservedBy?.value ?: -1L)
                .add(job.assignedTo?.value ?: -1L)
                .add(job.status.name)
                .add(job.failureReason ?: "")
                .add(job.workTicks)
            job.completionEffects
                .sortedWith(compareBy<JobCompletionEffect> { it.type.id }.thenBy {
                    (it as? JobCompletionEffect.ResourceDelta)?.resourceId.orEmpty()
                })
                .forEach { effect ->
                    hash.add(effect.type.id)
                    when (effect) {
                        is JobCompletionEffect.ResourceDelta -> hash.add(effect.resourceId).add(effect.amount)
                    }
                }
        }
    }

    private fun update(jobId: String, transform: (Job) -> Job) {
        val current = jobs[jobId] ?: error("Unknown job '$jobId'.")
        jobs[jobId] = transform(current)
    }
}
