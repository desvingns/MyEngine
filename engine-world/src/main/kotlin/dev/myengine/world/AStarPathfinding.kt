package dev.myengine.world

import java.util.PriorityQueue

/** The result of a deterministic point-to-point A* search over [TileWorld]. */
sealed class AStarPathResult {
    data class Found(val tiles: List<TilePosition>) : AStarPathResult()
    data class NoPath(val reason: String) : AStarPathResult()
}

/**
 * A pure four-neighbor A* pathfinder for uniform-cost tile worlds.
 *
 * The open set is ordered by `(f, row-major tile index)`. Neighbors are visited in the stable
 * y-then-x order already defined by [TilePosition], and equal-cost discoveries retain the first
 * predecessor. These rules make both successful paths and failure reasons reproducible.
 */
class AStarPathfinder {
    private data class OpenEntry(
        val position: TilePosition,
        val g: Int,
        val f: Int,
        val tileIndex: Int,
    )

    /**
     * Finds a path from [from] to [to]. [allowOccupiedStart] is intended for a future moving
     * actor whose own occupancy marks its current tile; terrain blocking is never ignored.
     */
    fun find(
        world: TileWorld,
        from: TilePosition,
        to: TilePosition,
        allowOccupiedStart: Boolean = false,
    ): AStarPathResult {
        if (!world.inBounds(from) || !world.inBounds(to)) {
            return AStarPathResult.NoPath("start_or_goal_out_of_bounds")
        }

        val startTerrain = world.tileAt(from).terrain
        if (startTerrain.blocksMovement) {
            return if (from == to) {
                AStarPathResult.NoPath("goal_blocked")
            } else {
                AStarPathResult.NoPath("start_blocked")
            }
        }
        if (!allowOccupiedStart && !world.canOccupy(from) && from != to) {
            return AStarPathResult.NoPath("start_blocked")
        }
        if (!world.canOccupy(to) && !(allowOccupiedStart && from == to)) {
            return AStarPathResult.NoPath("goal_blocked")
        }
        if (from == to) return AStarPathResult.Found(listOf(from))

        val openSet = PriorityQueue<OpenEntry>(compareBy({ it.f }, { it.tileIndex }))
        val cameFrom = mutableMapOf<TilePosition, TilePosition?>()
        val gScore = mutableMapOf<TilePosition, Int>()

        gScore[from] = 0
        cameFrom[from] = null
        openSet += OpenEntry(from, g = 0, f = from.manhattanDistance(to), tileIndex = rowMajorIndex(world, from))

        while (openSet.isNotEmpty()) {
            val currentEntry = openSet.remove()
            val current = currentEntry.position
            if (currentEntry.g != gScore[current]) continue
            if (current == to) break

            current.neighbors4()
                .sorted()
                .filter { neighbor ->
                    world.inBounds(neighbor) && world.canOccupy(neighbor)
                }
                .forEach { neighbor ->
                    val tentativeG = currentEntry.g + STEP_COST
                    val knownG = gScore[neighbor]
                    // Strictly lower g preserves the first predecessor on equal-cost ties.
                    if (knownG == null || tentativeG < knownG) {
                        gScore[neighbor] = tentativeG
                        cameFrom[neighbor] = current
                        openSet += OpenEntry(
                            position = neighbor,
                            g = tentativeG,
                            f = tentativeG + neighbor.manhattanDistance(to),
                            tileIndex = rowMajorIndex(world, neighbor),
                        )
                    }
                }
        }

        if (to !in cameFrom) return AStarPathResult.NoPath("unreachable")

        val path = mutableListOf<TilePosition>()
        var cursor: TilePosition? = to
        while (cursor != null) {
            path += cursor
            cursor = cameFrom[cursor]
        }
        return AStarPathResult.Found(path.asReversed())
    }

    private fun rowMajorIndex(world: TileWorld, position: TilePosition): Int =
        position.y * world.size.width + position.x

    private companion object {
        const val STEP_COST = 1
    }
}
