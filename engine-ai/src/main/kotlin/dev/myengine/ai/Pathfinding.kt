package dev.myengine.ai

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

class GridPathfinder {
    fun find(world: TileWorld, request: PathRequest): PathResult {
        if (!world.inBounds(request.from) || !world.inBounds(request.to)) {
            return PathResult.NoPath("start_or_goal_out_of_bounds")
        }
        if (!world.canOccupy(request.from) && request.from != request.to) {
            return PathResult.NoPath("start_blocked")
        }
        if (!world.canOccupy(request.to)) {
            return PathResult.NoPath("goal_blocked")
        }

        val frontier = ArrayDeque<TilePosition>()
        val cameFrom = mutableMapOf<TilePosition, TilePosition?>()
        frontier.add(request.from)
        cameFrom[request.from] = null

        while (frontier.isNotEmpty()) {
            val current = frontier.removeFirst()
            if (current == request.to) break
            current.neighbors4()
                .filter { world.inBounds(it) && world.canOccupy(it) && !cameFrom.containsKey(it) }
                .sorted()
                .forEach {
                    cameFrom[it] = current
                    frontier.add(it)
                }
        }

        if (!cameFrom.containsKey(request.to)) {
            return PathResult.NoPath("unreachable")
        }

        val path = mutableListOf<TilePosition>()
        var cursor: TilePosition? = request.to
        while (cursor != null) {
            path += cursor
            cursor = cameFrom[cursor]
        }
        return PathResult.Found(path.asReversed())
    }
}
