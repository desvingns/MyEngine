package dev.myengine.entities

import dev.myengine.core.StableHash

class EntityStore(
    nextEntityId: Long = 1,
    initialEntities: List<Entity> = emptyList(),
) {
    private var nextEntityId = nextEntityId
    private val entities = sortedMapOf<EntityId, Entity>()
    private val pendingRemovals = sortedSetOf<EntityId>()

    init {
        initialEntities.sortedBy { it.id.value }.forEach { entities[it.id] = it }
        val maxExisting = initialEntities.maxOfOrNull { it.id.value } ?: 0
        this.nextEntityId = maxOf(this.nextEntityId, maxExisting + 1)
    }

    fun create(type: String, tags: Set<String> = emptySet(), configure: (EntityId) -> Entity): Entity {
        val id = EntityId(nextEntityId++)
        val entity = configure(id).copy(id = id, type = type, tags = tags)
        entities[id] = entity
        return entity
    }

    fun upsert(entity: Entity) {
        entities[entity.id] = entity
        nextEntityId = maxOf(nextEntityId, entity.id.value + 1)
    }

    fun get(id: EntityId): Entity? = entities[id]

    fun require(id: EntityId): Entity = entities[id] ?: error("Unknown entity $id.")

    fun update(id: EntityId, transform: (Entity) -> Entity) {
        val current = require(id)
        entities[id] = transform(current).copy(id = id)
    }

    fun markRemove(id: EntityId) {
        pendingRemovals += id
    }

    /** Removes an entity immediately at a command boundary before later systems observe the store. */
    fun remove(id: EntityId): Entity? {
        pendingRemovals -= id
        return entities.remove(id)
    }

    fun flushRemovals() {
        pendingRemovals.forEach(entities::remove)
        pendingRemovals.clear()
    }

    fun all(): List<Entity> = entities.values.toList()

    fun byTag(tag: String): List<Entity> = all().filter { tag in it.tags }

    fun count(): Int = entities.size

    fun nextIdSnapshot(): Long = nextEntityId

    fun appendHash(hash: StableHash) {
        hash.add(nextEntityId)
        all().forEach { it.appendHash(hash) }
    }

    fun toSaveRecords(): List<EntitySaveRecord> = all().map {
        EntitySaveRecord(
            id = it.id.value,
            type = it.type,
            x = it.position?.tile?.x,
            y = it.position?.tile?.y,
            health = it.health?.current,
            maxHealth = it.health?.max,
        )
    }
}
