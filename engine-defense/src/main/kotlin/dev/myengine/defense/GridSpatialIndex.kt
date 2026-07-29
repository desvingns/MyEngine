package dev.myengine.defense

import dev.myengine.entities.Entity
import dev.myengine.entities.EntityId
import dev.myengine.entities.EntityStore
import dev.myengine.world.TilePosition

/**
 * Non-persisted, defense-local spatial index for entities with tile positions.
 *
 * Buckets retain entity ids rather than entity snapshots. Queries resolve those ids through the
 * current store so health changes made by an earlier tower in the same update pass remain visible
 * to later targeting and splash filters. The index is deliberately rebuilt at the system
 * boundary; movement systems do not mutate positions while tower updates are in progress, so no
 * public EntityStore mutation hook is required.
 */
internal class GridSpatialIndex private constructor(
    private val buckets: Map<TilePosition, List<EntityId>>,
) {
    /** Returns candidates in stable entity-id order; callers retain authoritative post-filters. */
    fun query(center: TilePosition, radius: Int, entities: EntityStore): List<Entity> {
        if (radius < 0) return emptyList()

        val candidateIds = mutableListOf<EntityId>()
        for (dy in -radius..radius) {
            val horizontalRadius = radius - kotlin.math.abs(dy)
            for (dx in -horizontalRadius..horizontalRadius) {
                buckets[TilePosition(center.x + dx, center.y + dy)]?.let(candidateIds::addAll)
            }
        }
        return candidateIds
            .sortedBy { it.value }
            .mapNotNull(entities::get)
    }

    companion object {
        fun build(entities: Iterable<Entity>): GridSpatialIndex {
            val buckets = mutableMapOf<TilePosition, MutableList<EntityId>>()
            entities.forEach { entity ->
                val position = entity.position?.tile ?: return@forEach
                buckets.getOrPut(position, ::mutableListOf).add(entity.id)
            }
            return GridSpatialIndex(
                buckets.mapValues { (_, ids) -> ids.sortedBy(EntityId::value) },
            )
        }
    }
}
