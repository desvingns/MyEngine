package dev.myengine.entities

import dev.myengine.core.StableHash
import dev.myengine.core.command.TargetingMode
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
    val worker: WorkerComponent? = null,
    val enemy: EnemyComponent? = null,
    val statusEffects: List<StatusEffectComponent> = emptyList(),
) {
    init {
        require(statusEffects.map { it.effectId }.distinct().size == statusEffects.size) {
            "An entity cannot contain duplicate status effect ids."
        }
    }

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
        worker?.let {
            hash.add("worker")
            it.appendHash(hash)
        }
        enemy?.appendHash(hash)
        statusEffects.sortedBy { it.effectId }.forEach { effect ->
            hash.add("status-effect")
            effect.appendHash(hash)
        }
    }
}

/** Effective immutable state captured when an enemy is spawned, including rank markers. */
data class EnemyComponent(
    val enemyId: String,
    val speedTilesPerTick: Int,
    val coreDamage: Int,
    val rewardResource: String,
    val rewardAmount: Int,
    val isElite: Boolean = false,
    val isBoss: Boolean = false,
) {
    init {
        require(enemyId.isNotBlank()) { "Enemy id cannot be blank." }
        require(speedTilesPerTick > 0) { "Enemy speed must be positive." }
        require(coreDamage > 0) { "Enemy core damage must be positive." }
        require(rewardResource.isNotBlank()) { "Enemy reward resource cannot be blank." }
        require(rewardAmount >= 0) { "Enemy reward cannot be negative." }
        require(!(isElite && isBoss)) { "An enemy cannot be both elite and boss." }
    }

    fun appendHash(hash: StableHash) {
        hash.add(enemyId)
            .add(speedTilesPerTick)
            .add(coreDamage)
            .add(rewardResource)
            .add(rewardAmount)
            .add(isElite)
            .add(isBoss)
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
    val upgradeBranch: String? = null,
    val upgradeTier: Int = 0,
    val targetingMode: TargetingMode = TargetingMode.NEAREST,
) {
    init {
        require(upgradeBranch == null || upgradeBranch.isNotBlank()) { "Upgrade branch cannot be blank." }
        require(upgradeTier >= 0) { "Upgrade tier cannot be negative." }
        require((upgradeBranch == null) == (upgradeTier == 0)) { "Upgrade branch and tier must be set together." }
    }

    fun appendHash(hash: StableHash) {
        hash.add(towerId).add(cooldownRemaining)
        if (upgradeBranch != null) {
            hash.add("upgrade").add(upgradeBranch).add(upgradeTier)
        }
        hash.add("targeting").add(targetingMode.id)
    }
}

data class AttackComponent(
    val range: Int,
    val damage: Int,
    val cooldownTicks: Int,
    /** Static content metadata; it is intentionally not persisted or included in stable hashes. */
    val damageTypeId: String? = null,
) {
    fun appendHash(hash: StableHash) {
        hash.add(range).add(damage).add(cooldownTicks)
    }
}

data class JobActorComponent(
    val assignedJobId: String? = null,
    val workTicks: Int = 0,
) {
    init {
        require(workTicks >= 0) { "Completed job work ticks cannot be negative." }
    }

    fun appendHash(hash: StableHash) {
        hash.add(assignedJobId ?: "").add(workTicks)
    }
}

/** Stable identity for content-defined worker capabilities. Carry lives in [InventoryComponent]. */
data class WorkerComponent(
    val workerId: String,
) {
    init {
        require(workerId.isNotBlank()) { "Worker id cannot be blank." }
    }

    fun appendHash(hash: StableHash) = hash.add(workerId)
}

data class StatusEffectComponent(
    val effectId: String,
    val remainingTicks: Int,
    val stacks: Int = 1,
) {
    init {
        require(effectId.isNotBlank()) { "Status effect id cannot be blank." }
        require(remainingTicks > 0) { "Remaining status-effect ticks must be positive." }
        require(stacks > 0) { "Status-effect stacks must be positive." }
    }

    fun appendHash(hash: StableHash) {
        hash.add(effectId).add(remainingTicks).add(stacks)
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
