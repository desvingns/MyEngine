package dev.myengine.entities

import dev.myengine.core.StableHash
import dev.myengine.world.TilePosition

@JvmInline
value class EntityId(val value: Long) : Comparable<EntityId> {
    init {
        require(value > 0) { "EntityId must be positive." }
    }

    override fun compareTo(other: EntityId): Int = value.compareTo(other.value)
}

data class Entity(
    val id: EntityId,
    val type: String,
    val tags: Set<String> = emptySet(),
    val position: PositionComponent? = null,
    val health: HealthComponent? = null,
    val movement: MovementComponent? = null,
    val inventory: InventoryComponent? = null,
    val tower: TowerComponent? = null,
    val attack: AttackComponent? = null,
    val jobActor: JobActorComponent? = null,
) {
    fun appendHash(hash: StableHash) {
        hash.add(id.value).add(type)
        tags.sorted().forEach(hash::add)
        position?.appendHash(hash) ?: hash.add("no-position")
        health?.appendHash(hash) ?: hash.add("no-health")
        movement?.appendHash(hash) ?: hash.add("no-movement")
        inventory?.appendHash(hash) ?: hash.add("no-inventory")
        tower?.appendHash(hash) ?: hash.add("no-tower")
        attack?.appendHash(hash) ?: hash.add("no-attack")
        jobActor?.appendHash(hash) ?: hash.add("no-job")
    }
}

data class PositionComponent(val tile: TilePosition) {
    fun appendHash(hash: StableHash) {
        hash.add(tile.x).add(tile.y)
    }
}

data class HealthComponent(val current: Int, val max: Int) {
    init {
        require(max > 0) { "Max health must be positive." }
        require(current in 0..max) { "Current health must be between 0 and max." }
    }

    fun damage(amount: Int): HealthComponent = copy(current = (current - amount).coerceAtLeast(0))
    fun isAlive(): Boolean = current > 0

    fun appendHash(hash: StableHash) {
        hash.add(current).add(max)
    }
}

data class MovementComponent(
    val path: List<TilePosition> = emptyList(),
    val pathIndex: Int = 0,
) {
    fun appendHash(hash: StableHash) {
        path.forEach { hash.add(it.x).add(it.y) }
        hash.add(pathIndex)
    }
}

data class InventoryComponent(
    val resources: Map<String, Int> = emptyMap(),
    val capacity: Int? = null,
) {
    fun appendHash(hash: StableHash) {
        resources.toSortedMap().forEach { (id, amount) -> hash.add(id).add(amount) }
        hash.add(capacity ?: -1)
    }
}

data class TowerComponent(
    val towerId: String,
    val cooldownRemaining: Int = 0,
) {
    fun appendHash(hash: StableHash) {
        hash.add(towerId).add(cooldownRemaining)
    }
}

data class AttackComponent(
    val range: Int,
    val damage: Int,
    val cooldownTicks: Int,
) {
    fun appendHash(hash: StableHash) {
        hash.add(range).add(damage).add(cooldownTicks)
    }
}

data class JobActorComponent(
    val assignedJobId: String? = null,
) {
    fun appendHash(hash: StableHash) {
        hash.add(assignedJobId ?: "")
    }
}

data class EntitySaveRecord(
    val id: Long,
    val type: String,
    val x: Int?,
    val y: Int?,
    val health: Int?,
    val maxHealth: Int?,
)
