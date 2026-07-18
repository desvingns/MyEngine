package dev.myengine.defense

import dev.myengine.ai.GoalField
import dev.myengine.core.command.TargetingMode
import dev.myengine.entities.Entity
import dev.myengine.world.TilePosition

/** Pure, deterministic in-range enemy selector for [TargetingMode]. */
object TargetSelector {
    /**
     * Unreachable enemies are excluded for every mode. Ties always resolve to the lower entity id.
     * FIRST is closest to the goal and LAST is farthest from it, as measured by [GoalField].
     */
    fun select(
        mode: TargetingMode,
        towerPosition: TilePosition,
        range: Int,
        enemies: Iterable<Entity>,
        goalField: GoalField,
    ): Entity? = enemies.asSequence()
        .mapNotNull { enemy ->
            val position = enemy.position?.tile ?: return@mapNotNull null
            val health = enemy.health ?: return@mapNotNull null
            val goalDistance = goalField.distanceAt(position) ?: return@mapNotNull null
            if (!health.isAlive() || towerPosition.manhattanDistance(position) > range) return@mapNotNull null
            Candidate(enemy, towerPosition.manhattanDistance(position), goalDistance, health.current)
        }
        .sortedWith(comparator(mode))
        .firstOrNull()
        ?.entity

    private fun comparator(mode: TargetingMode): Comparator<Candidate> = when (mode) {
        TargetingMode.FIRST -> compareBy<Candidate> { it.goalDistance }
        TargetingMode.LAST -> compareByDescending<Candidate> { it.goalDistance }
        TargetingMode.NEAREST -> compareBy<Candidate> { it.towerDistance }
        TargetingMode.STRONGEST -> compareByDescending<Candidate> { it.health }
        TargetingMode.WEAKEST -> compareBy<Candidate> { it.health }
    }.thenBy { it.entity.id.value }

    private data class Candidate(
        val entity: Entity,
        val towerDistance: Int,
        val goalDistance: Int,
        val health: Int,
    )
}
