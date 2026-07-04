package dev.myengine.ai

import dev.myengine.entities.EntityId
import dev.myengine.world.TilePosition

enum class JobStatus {
    OPEN,
    ASSIGNED,
    COMPLETED,
    FAILED,
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
)

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

    fun assignNext(actor: EntityId): JobAssignment? {
        val candidate = jobs.values
            .filter { it.status == JobStatus.OPEN && (it.reservedBy == null || it.reservedBy == actor) }
            .sortedWith(compareByDescending<Job> { it.priority }.thenBy { it.id })
            .firstOrNull()
            ?: return null
        val assigned = candidate.copy(status = JobStatus.ASSIGNED, assignedTo = actor, reservedBy = actor)
        jobs[assigned.id] = assigned
        return JobAssignment(actor, assigned)
    }

    fun complete(jobId: String) {
        update(jobId) { it.copy(status = JobStatus.COMPLETED) }
    }

    fun fail(jobId: String, reason: String) {
        update(jobId) { it.copy(status = JobStatus.FAILED, failureReason = reason) }
    }

    fun all(): List<Job> = jobs.values.toList()

    private fun update(jobId: String, transform: (Job) -> Job) {
        val current = jobs[jobId] ?: error("Unknown job '$jobId'.")
        jobs[jobId] = transform(current)
    }
}
