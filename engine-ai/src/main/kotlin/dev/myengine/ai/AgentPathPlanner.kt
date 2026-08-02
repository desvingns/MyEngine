package dev.myengine.ai

import dev.myengine.entities.MovementComponent
import dev.myengine.world.TilePosition
import dev.myengine.world.TileWorld

/** The minimal decision boundary for a future job-actor movement system. */
sealed class AgentPathPlan {
    data class Kept(val movement: MovementComponent) : AgentPathPlan()
    data class Repathed(val movement: MovementComponent) : AgentPathPlan()
    data class NoPath(val reason: String) : AgentPathPlan()
}

/**
 * Reuses a stored job-actor path while it remains valid and deterministically replans it after a
 * walkability change. This is deliberately not a tick system and does not affect wave enemies;
 * callers decide when to apply the returned [MovementComponent].
 */
class AgentPathPlanner(
    private val pathfinder: GridPathfinder = GridPathfinder(),
) {
    fun plan(
        world: TileWorld,
        current: TilePosition,
        goal: TilePosition,
        stored: MovementComponent? = null,
    ): AgentPathPlan {
        if (stored != null && isValid(world, current, goal, stored)) {
            return AgentPathPlan.Kept(stored)
        }

        return when (val result = pathfinder.find(
            world = world,
            request = PathRequest(current, goal),
            allowOccupiedStart = true,
        )) {
            is PathResult.Found -> AgentPathPlan.Repathed(
                MovementComponent(path = result.tiles, pathIndex = 0),
            )
            is PathResult.NoPath -> AgentPathPlan.NoPath(result.reason)
        }
    }

    /** Returns false when a stored path no longer matches the current world or actor position. */
    fun isValid(
        world: TileWorld,
        current: TilePosition,
        goal: TilePosition,
        movement: MovementComponent,
    ): Boolean {
        if (!world.inBounds(current) || !world.inBounds(goal)) return false
        if (movement.path.isEmpty() || movement.pathIndex !in movement.path.indices) return false
        if (movement.path[movement.pathIndex] != current || movement.path.last() != goal) return false
        if (world.tileAt(current).terrain.blocksMovement) return false

        val remaining = movement.path.subList(movement.pathIndex, movement.path.size)
        return remaining.indices.all { index ->
            val position = remaining[index]
            world.inBounds(position) && (index == 0 || isOpenStep(world, remaining[index - 1], position))
        }
    }

    private fun isOpenStep(world: TileWorld, from: TilePosition, to: TilePosition): Boolean =
        from.manhattanDistance(to) == 1 && world.canOccupy(to)
}
