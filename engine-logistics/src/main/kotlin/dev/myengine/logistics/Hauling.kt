package dev.myengine.logistics

import dev.myengine.ai.AgentPathPlan
import dev.myengine.ai.AgentPathPlanner
import dev.myengine.ai.HaulPhase
import dev.myengine.ai.HaulDestinationKind
import dev.myengine.ai.JobBoard
import dev.myengine.ai.Job
import dev.myengine.entities.EntityId
import dev.myengine.entities.EntityStore
import dev.myengine.entities.InventoryComponent
import dev.myengine.world.TilePosition
import dev.myengine.world.TileWorld
import dev.myengine.core.StableHash

data class HaulSource(
    val id: String,
    val position: TilePosition,
    val resources: Map<String, Int> = emptyMap(),
    val reservations: Map<String, Int> = emptyMap(),
    val reservationResources: Map<String, String> = emptyMap(),
) {
    init {
        require(id.isNotBlank()) { "Haul source id cannot be blank." }
        resources.forEach { (resourceId, amount) ->
            require(resourceId.isNotBlank() && amount >= 0) { "Invalid haul source resource '$resourceId'." }
        }
        reservations.forEach { (jobId, amount) ->
            require(jobId.isNotBlank() && amount > 0) { "Invalid haul source reservation '$jobId'." }
        }
        reservationResources.forEach { (jobId, resourceId) ->
            require(jobId in reservations && resourceId.isNotBlank() && resourceId in resources) {
                "Invalid haul source reservation resource '$jobId'."
            }
        }
        val knownReserved = reservationResources.entries
            .groupBy({ it.value }, { reservations.getValue(it.key) })
            .mapValues { (_, amounts) -> amounts.sumOf { it.toLong() } }
        require(knownReserved.all { (resourceId, amount) -> amount <= (resources[resourceId] ?: 0).toLong() }) {
            "Haul source reservations cannot exceed their resource quantities."
        }
        val unknownReserved = reservations.keys
            .filter { it !in reservationResources }
            .sumOf { reservations.getValue(it).toLong() }
        require(unknownReserved <= resources.values.sumOf { it.toLong() }) {
            "Legacy haul source reservations cannot exceed source resources."
        }
    }

    fun available(resourceId: String): Int {
        val reserved = reservations.entries.sumOf { (jobId, amount) ->
            if (reservationResources[jobId] == null || reservationResources[jobId] == resourceId) amount else 0
        }
        return (resources[resourceId] ?: 0) - reserved
    }
}

/** Authoritative deterministic item sources, including positioned producer outputs. */
class HaulSourceStore(initialSources: List<HaulSource> = emptyList()) {
    private val sources = sortedMapOf<String, HaulSource>()

    init {
        initialSources.sortedBy { it.id }.forEach { source ->
            require(sources.put(source.id, canonical(source)) == null) { "Duplicate haul source '${source.id}'." }
        }
    }

    fun add(source: HaulSource) {
        require(!sources.containsKey(source.id)) { "Duplicate haul source '${source.id}'." }
        sources[source.id] = canonical(source)
    }

    fun addOutput(sourceId: String, position: TilePosition, resourceId: String, amount: Int) {
        require(amount > 0) { "Produced haul amount must be positive." }
        val current = sources[sourceId]
        if (current == null) {
            sources[sourceId] = HaulSource(sourceId, position, mapOf(resourceId to amount))
        } else {
            require(current.position == position) { "Haul source '$sourceId' changed position." }
            sources[sourceId] = current.copy(resources = current.resources + (resourceId to (current.resources[resourceId] ?: 0) + amount))
        }
    }

    fun get(sourceId: String): HaulSource? = sources[sourceId]

    fun all(): List<HaulSource> = sources.values.toList()

    fun reserve(sourceId: String, jobId: String, resourceId: String, amount: Int): Boolean {
        val source = sources[sourceId] ?: return false
        if (source.reservations.containsKey(jobId) || source.available(resourceId) < amount) return false
        sources[sourceId] = source.copy(
            reservations = source.reservations + (jobId to amount),
            reservationResources = source.reservationResources + (jobId to resourceId),
        )
        return true
    }

    fun hasReservation(jobId: String): Boolean = sources.values.any { jobId in it.reservations }

    fun pickup(sourceId: String, jobId: String, resourceId: String, amount: Int): Boolean {
        val source = sources[sourceId] ?: return false
        if (source.reservations[jobId] != amount ||
            (source.reservationResources[jobId] != null && source.reservationResources[jobId] != resourceId) ||
            source.resources[resourceId] ?: 0 < amount
        ) return false
        val remaining = (source.resources[resourceId] ?: 0) - amount
        val nextResources = if (remaining == 0) source.resources - resourceId else source.resources + (resourceId to remaining)
        sources[sourceId] = source.copy(
            resources = nextResources,
            reservations = source.reservations - jobId,
            reservationResources = source.reservationResources - jobId,
        )
        return true
    }

    fun release(jobId: String) {
        sources.entries.forEach { (id, source) ->
            if (jobId in source.reservations) {
                sources[id] = source.copy(
                    reservations = source.reservations - jobId,
                    reservationResources = source.reservationResources - jobId,
                )
            }
        }
    }

    /** Returns material to its original source after a carried or delivered haul is cancelled. */
    fun refund(sourceId: String, resourceId: String, amount: Int): Boolean {
        require(amount > 0) { "Refund amount must be positive." }
        val source = sources[sourceId] ?: return false
        sources[sourceId] = source.copy(
            resources = source.resources + (resourceId to ((source.resources[resourceId] ?: 0) + amount)),
        )
        return true
    }

    fun appendHash(hash: StableHash) {
        all().forEach { source ->
            hash.add("haul-source").add(source.id).add(source.position.x).add(source.position.y)
            source.resources.toSortedMap().forEach { (id, amount) -> hash.add(id).add(amount) }
            source.reservations.toSortedMap().forEach { (jobId, amount) ->
                hash.add("reservation").add(jobId).add(amount).add(source.reservationResources[jobId] ?: "")
            }
        }
    }

    private fun canonical(source: HaulSource): HaulSource = source.copy(
        resources = source.resources.toSortedMap(),
        reservations = source.reservations.toSortedMap(),
        reservationResources = source.reservationResources.toSortedMap(),
    )
}

data class HaulingReport(
    val completedJobIds: List<String> = emptyList(),
    val releasedJobIds: List<String> = emptyList(),
)

fun interface HaulDestinationSink {
    fun deposit(job: Job, position: TilePosition, resourceId: String, amount: Int): Boolean
}

private class StockpileDestinationSink(private val zones: ZoneStore) : HaulDestinationSink {
    override fun deposit(job: Job, position: TilePosition, resourceId: String, amount: Int): Boolean {
        val spec = job.haul ?: return false
        return spec.destinationKind == HaulDestinationKind.STOCKPILE &&
            zones.deposit(spec.destinationZoneId, position, resourceId, amount)
    }
}

/** Deterministic source-to-stockpile worker execution. */
class HaulingSystem(
    private val pathPlanner: AgentPathPlanner = AgentPathPlanner(),
) {
    fun tick(
        world: TileWorld,
        entities: EntityStore,
        jobs: JobBoard,
        sources: HaulSourceStore,
        zones: ZoneStore,
        workers: Map<String, dev.myengine.content.WorkerContent>,
        destinationSink: HaulDestinationSink = StockpileDestinationSink(zones),
    ): HaulingReport {
        val completed = mutableListOf<String>()
        val released = mutableListOf<String>()
        val releasedThisTick = mutableSetOf<String>()
        entities.all()
            .filter { it.worker != null && it.jobActor != null && it.position != null }
            .sortedBy { it.id }
            .forEach { initial ->
                var worker = entities.require(initial.id)
                val actor = worker.jobActor ?: return@forEach
                val assignedJobId = actor.assignedJobId
                if (assignedJobId != null && jobs.get(assignedJobId)?.haul == null) {
                    // Generic jobs share the same worker actor and are executed immediately
                    // after hauling in the sandbox tick. Leave their assignment untouched so
                    // JobExecutionSystem can advance it in its own deterministic phase.
                    return@forEach
                }
                val job = if (assignedJobId == null) {
                    jobs.assignNext(worker.id, releasedThisTick) { it.haul != null }?.job
                } else {
                    jobs.get(assignedJobId)?.takeIf { it.haul != null && it.status != dev.myengine.ai.JobStatus.DONE && it.status != dev.myengine.ai.JobStatus.FAILED }
                }
                if (job == null) {
                    if (assignedJobId != null) clearAssignment(entities, worker.id, clearCarry = false)
                    return@forEach
                }
                if (assignedJobId == null) {
                    entities.update(worker.id) { it.copy(jobActor = actor.copy(assignedJobId = job.id, workTicks = 0)) }
                    worker = entities.require(worker.id)
                }
                val spec = job.haul ?: return@forEach
                val content = workers[worker.worker?.workerId] ?: run {
                    release(entities, jobs, sources, worker.id, job.id, "unknown_worker", releasedThisTick, released)
                    return@forEach
                }
                if (spec.amount > content.capacity) {
                    release(entities, jobs, sources, worker.id, job.id, "worker_capacity", releasedThisTick, released)
                    return@forEach
                }
                val carriedInventory = worker.inventory
                if (carriedInventory != null && carriedInventory.resources.isNotEmpty()) {
                    if (spec.phase == HaulPhase.TO_SOURCE) {
                        jobs.updateHaulPhase(job.id, HaulPhase.TO_STOCKPILE)
                    }
                } else if (spec.phase == HaulPhase.TO_SOURCE) {
                    if (!sources.hasReservation(job.id) && !sources.reserve(spec.sourceId, job.id, spec.resourceId, spec.amount)) {
                        release(entities, jobs, sources, worker.id, job.id, "source_unavailable", releasedThisTick, released)
                        return@forEach
                    }
                    val source = sources.get(spec.sourceId)
                    if (source == null || !move(world, entities, worker, source.position, content.speedTilesPerTick)) {
                        if (worker.position?.tile != source?.position) return@forEach
                    }
                    worker = entities.require(worker.id)
                    if (worker.position?.tile == source?.position) {
                        if (!sources.pickup(spec.sourceId, job.id, spec.resourceId, spec.amount)) {
                            release(entities, jobs, sources, worker.id, job.id, "source_pickup_failed", releasedThisTick, released)
                            return@forEach
                        }
                        entities.update(worker.id) { it.copy(inventory = InventoryComponent(mapOf(spec.resourceId to spec.amount), content.capacity)) }
                        jobs.start(job.id)
                        jobs.updateHaulPhase(job.id, HaulPhase.TO_STOCKPILE)
                    }
                    return@forEach
                }

                worker = entities.require(worker.id)
                val carry = worker.inventory?.resources?.entries?.singleOrNull()
                if (carry == null || carry.key != spec.resourceId || carry.value != spec.amount) {
                    release(entities, jobs, sources, worker.id, job.id, "invalid_carry", releasedThisTick, released)
                    return@forEach
                }
                if (!move(world, entities, worker, job.target, content.speedTilesPerTick)) return@forEach
                worker = entities.require(worker.id)
                if (worker.position?.tile != job.target) return@forEach
                if (!destinationSink.deposit(job, job.target, spec.resourceId, spec.amount)) {
                    release(entities, jobs, sources, worker.id, job.id, "invalid_stockpile", releasedThisTick, released)
                    return@forEach
                }
                jobs.complete(job.id)
                clearAssignment(entities, worker.id, clearCarry = true)
                completed += job.id
            }
        return HaulingReport(completed, released)
    }

    private fun move(world: TileWorld, entities: EntityStore, entity: dev.myengine.entities.Entity, target: TilePosition, budget: Int): Boolean {
        val current = entity.position?.tile ?: return false
        if (current == target) return true
        val plan = pathPlanner.plan(world, current, target, entity.movement)
        val movement = when (plan) {
            is AgentPathPlan.NoPath -> return false
            is AgentPathPlan.Kept -> plan.movement
            is AgentPathPlan.Repathed -> plan.movement
        }
        var position = current
        var index = movement.pathIndex
        repeat(budget) {
            if (position == target) return@repeat
            val nextIndex = index + 1
            if (nextIndex !in movement.path.indices) return@repeat
            val next = movement.path[nextIndex]
            if (!world.inBounds(next) || !world.canOccupy(next)) return@repeat
            position = next
            index = nextIndex
        }
        entities.update(entity.id) { it.copy(position = dev.myengine.entities.PositionComponent(position), movement = movement.copy(pathIndex = index)) }
        return position == target
    }

    private fun release(
        entities: EntityStore,
        jobs: JobBoard,
        sources: HaulSourceStore,
        workerId: EntityId,
        jobId: String,
        reason: String,
        releasedThisTick: MutableSet<String>,
        released: MutableList<String>,
    ) {
        sources.release(jobId)
        jobs.release(jobId, reason)
        releasedThisTick += jobId
        released += jobId
        clearAssignment(entities, workerId, clearCarry = true)
    }

    private fun clearAssignment(entities: EntityStore, workerId: EntityId, clearCarry: Boolean) {
        entities.update(workerId) { entity ->
            entity.copy(
                movement = null,
                inventory = if (clearCarry) null else entity.inventory,
                jobActor = entity.jobActor?.copy(assignedJobId = null, workTicks = 0),
            )
        }
    }
}
