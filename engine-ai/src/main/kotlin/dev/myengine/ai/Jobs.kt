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

enum class HaulPhase {
    TO_SOURCE,
    TO_STOCKPILE,
}

enum class HaulDestinationKind {
    STOCKPILE,
    CONSTRUCTION,
}

/** Typed payload for a deterministic source -> destination job. */
data class HaulJobSpec(
    val sourceId: String,
    val resourceId: String,
    val amount: Int,
    val destinationZoneId: String,
    val phase: HaulPhase = HaulPhase.TO_SOURCE,
    val destinationKind: HaulDestinationKind = HaulDestinationKind.STOCKPILE,
) {
    init {
        require(sourceId.isNotBlank()) { "Haul source id cannot be blank." }
        require(resourceId.isNotBlank()) { "Haul resource id cannot be blank." }
        require(amount > 0) { "Haul amount must be positive." }
        require(destinationZoneId.isNotBlank()) { "Haul destination zone id cannot be blank." }
    }
}

enum class JobEffectType(val id: String) {
    RESOURCE_DELTA("resource_delta"),
    SPAWN_BUILDING("spawn_building"),
    NEED_RECOVERY("need_recovery"),
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

    data class SpawnBuilding(
        val buildingId: String,
        val siteId: String,
    ) : JobCompletionEffect {
        override val type: JobEffectType = JobEffectType.SPAWN_BUILDING

        init {
            require(buildingId.isNotBlank()) { "Building id cannot be blank." }
            require(siteId.isNotBlank()) { "Construction site id cannot be blank." }
        }
    }

    data class NeedRecovery(
        val needId: String,
        val amount: Int,
        val targetEntityId: EntityId? = null,
    ) : JobCompletionEffect {
        override val type: JobEffectType = JobEffectType.NEED_RECOVERY

        init {
            require(needId.isNotBlank()) { "Need id cannot be blank." }
            require(amount > 0) { "Need recovery amount must be positive." }
        }
    }
}

/** Canonical effect key used by hashes, save encoding, and completion application. */
fun JobCompletionEffect.stableSortKey(): String = when (this) {
    is JobCompletionEffect.ResourceDelta ->
        listOf(type.id, resourceId, amount).joinToString("\u0000")
    is JobCompletionEffect.SpawnBuilding ->
        listOf(type.id, siteId, buildingId).joinToString("\u0000")
    is JobCompletionEffect.NeedRecovery ->
        listOf(type.id, needId, amount, targetEntityId?.value ?: -1L).joinToString("\u0000")
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
    val haul: HaulJobSpec? = null,
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

    fun assignNext(
        actor: EntityId,
        excludedJobIds: Set<String> = emptySet(),
        eligible: (Job) -> Boolean = { true },
    ): JobAssignment? {
        val candidate = jobs.values
            .filter {
                it.id !in excludedJobIds &&
                    eligible(it) &&
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

    fun updateHaulPhase(jobId: String, phase: HaulPhase): Job {
        val current = jobs[jobId] ?: error("Unknown job '$jobId'.")
        val haul = current.haul ?: error("Job '$jobId' is not a haul job.")
        return current.copy(haul = haul.copy(phase = phase)).also { jobs[jobId] = it }
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

    /** Removes an unclaimed job during authoritative designation cancellation. */
    fun remove(jobId: String): Job? = jobs.remove(jobId)

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
            job.haul?.let { haul ->
                hash.add("haul").add(haul.sourceId).add(haul.resourceId).add(haul.amount)
                    .add(haul.destinationZoneId).add(haul.phase.name).add(haul.destinationKind.name)
            }
            job.completionEffects.sortedBy { it.stableSortKey() }
                .forEach { effect ->
                    hash.add(effect.type.id)
                    when (effect) {
                        is JobCompletionEffect.ResourceDelta -> hash.add(effect.resourceId).add(effect.amount)
                        is JobCompletionEffect.SpawnBuilding -> hash.add(effect.buildingId).add(effect.siteId)
                        is JobCompletionEffect.NeedRecovery -> hash.add(effect.needId).add(effect.amount).add(effect.targetEntityId?.value ?: -1L)
                    }
                }
        }
    }

    private fun update(jobId: String, transform: (Job) -> Job) {
        val current = jobs[jobId] ?: error("Unknown job '$jobId'.")
        jobs[jobId] = transform(current)
    }
}
