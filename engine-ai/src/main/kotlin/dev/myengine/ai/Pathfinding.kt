package dev.myengine.ai

import dev.myengine.world.AStarPathfinder
import dev.myengine.world.AStarPathResult
import dev.myengine.world.TilePosition
import dev.myengine.world.TileWorld

data class PathRequest(
    val from: TilePosition,
    val to: TilePosition,
)

sealed class PathResult {
    data class Found(val tiles: List<TilePosition>) : PathResult()
    data class NoPath(val reason: String) : PathResult()
}

class GridPathfinder(
    private val pathfinder: AStarPathfinder = AStarPathfinder(),
) {
    fun find(
        world: TileWorld,
        request: PathRequest,
        allowOccupiedStart: Boolean = false,
    ): PathResult = when (val result = pathfinder.find(world, request.from, request.to, allowOccupiedStart)) {
        is AStarPathResult.Found -> PathResult.Found(result.tiles)
        is AStarPathResult.NoPath -> PathResult.NoPath(result.reason)
    }
}

/**
 * A reverse breadth-first field rooted at one walkable goal.  The first predecessor assigned by
 * the sorted BFS frontier is retained as the next tile, so equal-cost routes have a stable tie
 * result without every moving entity retaining its own path.
 */
class GoalField private constructor(
    val goal: TilePosition,
    private val distances: Map<TilePosition, Int>,
    private val nextSteps: Map<TilePosition, TilePosition>,
) {
    /** The number of tiles that can reach [goal], including the goal itself. */
    val reachableTileCount: Int get() = distances.size

    fun distanceAt(position: TilePosition): Int? = distances[position]

    fun canReach(position: TilePosition): Boolean = position in distances

    fun isGoal(position: TilePosition): Boolean = position == goal

    /**
     * Returns the pinned next tile toward [goal], or null for the goal and unreachable tiles.
     */
    fun nextStep(position: TilePosition): TilePosition? = nextSteps[position]

    /** Materializes the field route for presentation/debug consumers; simulation uses [nextStep]. */
    fun pathFrom(start: TilePosition): List<TilePosition> {
        if (!canReach(start)) return emptyList()
        val path = mutableListOf(start)
        var cursor = start
        while (!isGoal(cursor)) {
            cursor = nextStep(cursor) ?: return emptyList()
            path += cursor
        }
        return path
    }

    /** Result of rebuilding after a committed or prospective walkability mutation. */
    data class WalkabilityRebuild(
        val field: GoalField,
        val unreachableSpawns: List<TilePosition>,
    ) {
        val keepsAllSpawnsReachable: Boolean get() = unreachableSpawns.isEmpty()
    }

    companion object {
        /**
         * Builds a deterministic goal field.  Neighbor coordinates are sorted by [TilePosition]'s
         * y-then-x ordering before enqueue, which pins both BFS discovery and equal-cost ties.
         * [additionalBlocked] lets callers validate a prospective placement without mutating world
         * occupancy.
         */
        fun build(
            world: TileWorld,
            goal: TilePosition,
            additionalBlocked: TilePosition? = null,
        ): GoalField {
            if (!world.inBounds(goal) || !isWalkable(world, goal, additionalBlocked)) {
                return GoalField(goal, emptyMap(), emptyMap())
            }

            val frontier = ArrayDeque<TilePosition>()
            val distances = mutableMapOf(goal to 0)
            val nextSteps = mutableMapOf<TilePosition, TilePosition>()
            frontier.add(goal)

            while (frontier.isNotEmpty()) {
                val current = frontier.removeFirst()
                val distance = distances.getValue(current)
                current.neighbors4()
                    .sorted()
                    .filter { neighbor ->
                        world.inBounds(neighbor) &&
                            isWalkable(world, neighbor, additionalBlocked) &&
                            neighbor !in distances
                    }
                    .forEach { neighbor ->
                        distances[neighbor] = distance + 1
                        nextSteps[neighbor] = current
                        frontier.add(neighbor)
                    }
            }
            return GoalField(goal, distances.toMap(), nextSteps.toMap())
        }

        /**
         * The single route-cache hook for walkability changes.  Use [additionalBlocked] to probe
         * an uncommitted blocker; omit it immediately after a committed change (such as a future
         * destroy or wall operation).  Spawn iteration is sorted so diagnostics stay stable.
         */
        fun rebuildAfterWalkabilityChange(
            world: TileWorld,
            goal: TilePosition,
            spawns: List<TilePosition>,
            additionalBlocked: TilePosition? = null,
        ): WalkabilityRebuild {
            val field = build(world, goal, additionalBlocked)
            return WalkabilityRebuild(
                field = field,
                unreachableSpawns = spawns.sorted().filterNot(field::canReach),
            )
        }

        private fun isWalkable(world: TileWorld, position: TilePosition, additionalBlocked: TilePosition?): Boolean =
            position != additionalBlocked && world.canOccupy(position)
    }
}
