package dev.myengine.world

import java.util.ArrayDeque
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AStarPathfindingTest {
    private val terrain = mapOf(
        "floor" to TerrainRule("floor", buildable = true, blocksMovement = false),
        "wall" to TerrainRule("wall", buildable = false, blocksMovement = true),
    )

    @Test
    fun openSetTieBreakIsDeterministicAndRepeatedResultsAreStable() {
        val world = TileWorld.filled(WorldSize(3, 3), terrain, "floor")
        val from = TilePosition(0, 0)
        val to = TilePosition(2, 2)
        val expected = listOf(
            TilePosition(0, 0),
            TilePosition(1, 0),
            TilePosition(2, 0),
            TilePosition(2, 1),
            TilePosition(2, 2),
        )
        val pathfinder = AStarPathfinder()

        repeat(20) {
            val result = assertIs<AStarPathResult.Found>(pathfinder.find(world, from, to))
            assertEquals(expected, result.tiles)
            assertEquals(4, pathCost(result.tiles))
        }
    }

    @Test
    fun equalGDiscoveryKeepsFirstPredecessorAfterFThenRowMajorTieBreak() {
        val world = TileWorld.filled(WorldSize(3, 3), terrain, "floor")
        // The direct top-right step is blocked, so the route must pass through (1,1).
        world.setTile(TilePosition(2, 0), WorldTile("wall"))

        // From (0,0), (1,0) and (0,1) both have f=3; row-major indices 1 and 3
        // select (1,0) first. That discovers (1,1) with g=2. (0,1) later reaches
        // (1,1) with the same g, and the strict lower-g update keeps (1,0) as
        // the first predecessor.
        val expected = listOf(
            TilePosition(0, 0),
            TilePosition(1, 0),
            TilePosition(1, 1),
            TilePosition(2, 1),
        )

        val result = assertIs<AStarPathResult.Found>(
            AStarPathfinder().find(world, TilePosition(0, 0), TilePosition(2, 1)),
        )

        assertEquals(expected, result.tiles)
        assertEquals(3, pathCost(result.tiles))
    }

    @Test
    fun boundariesBlockedEndpointsSameTileAndOccupiedStartFollowContract() {
        val pathfinder = AStarPathfinder()
        val world = TileWorld.filled(WorldSize(3, 3), terrain, "floor")

        assertNoPath(
            pathfinder.find(world, TilePosition(-1, 0), TilePosition(2, 2)),
            "start_or_goal_out_of_bounds",
        )
        assertNoPath(
            pathfinder.find(world, TilePosition(0, 0), TilePosition(3, 2)),
            "start_or_goal_out_of_bounds",
        )

        world.setTile(TilePosition(0, 0), WorldTile("wall"))
        assertNoPath(
            pathfinder.find(world, TilePosition(0, 0), TilePosition(2, 2)),
            "start_blocked",
        )

        world.setTile(TilePosition(0, 0), WorldTile("floor"))
        world.setTile(TilePosition(2, 2), WorldTile("wall"))
        assertNoPath(
            pathfinder.find(world, TilePosition(0, 0), TilePosition(2, 2)),
            "goal_blocked",
        )

        world.setTile(TilePosition(2, 2), WorldTile("floor"))
        val sameTile = assertIs<AStarPathResult.Found>(
            pathfinder.find(world, TilePosition(1, 1), TilePosition(1, 1)),
        )
        assertEquals(listOf(TilePosition(1, 1)), sameTile.tiles)

        val occupiedStart = TilePosition(0, 0)
        world.occupy(occupiedStart, entityId = 7L)
        assertNoPath(
            pathfinder.find(world, occupiedStart, TilePosition(2, 0)),
            "start_blocked",
        )
        val allowedOccupiedStart = assertIs<AStarPathResult.Found>(
            pathfinder.find(world, occupiedStart, TilePosition(2, 0), allowOccupiedStart = true),
        )
        assertEquals(occupiedStart, allowedOccupiedStart.tiles.first())
        assertEquals(TilePosition(2, 0), allowedOccupiedStart.tiles.last())
        assertValidPath(world, allowedOccupiedStart.tiles, allowOccupiedStart = true)
    }

    @Test
    fun sealedMapReturnsNoPath() {
        val world = TileWorld.filled(WorldSize(3, 3), terrain, "floor")
        val start = TilePosition(0, 0)
        val goal = TilePosition(2, 2)
        listOf(TilePosition(1, 0), TilePosition(0, 1)).forEach { position ->
            world.setTile(position, WorldTile("wall"))
        }

        assertNoPath(AStarPathfinder().find(world, start, goal), "unreachable")
    }

    @Test
    fun seededDifferentialMatchesIndependentBfsOnUniformCostMaps() {
        var reachableCases = 0
        var unreachableCases = 0

        repeat(128) { seed ->
            val random = Random(seed)
            val width = 4 + random.nextInt(7)
            val height = 4 + random.nextInt(7)
            val from = TilePosition(random.nextInt(width), random.nextInt(height))
            val to = TilePosition(random.nextInt(width), random.nextInt(height))
            val blocked = buildBlockedSet(width, height, random)
            blocked.remove(from)
            blocked.remove(to)
            val world = worldWithBlockedTiles(width, height, blocked)

            val expectedDistance = bfsDistance(width, height, blocked, from, to)
            when (val actual = AStarPathfinder().find(world, from, to)) {
                is AStarPathResult.Found -> {
                    reachableCases += 1
                    assertTrue(expectedDistance != null, "seed=$seed should be unreachable")
                    assertEquals(expectedDistance, pathCost(actual.tiles), "seed=$seed")
                    assertEquals(expectedDistance!! + 1, actual.tiles.size, "seed=$seed")
                    assertValidPath(world, actual.tiles)
                }

                is AStarPathResult.NoPath -> {
                    unreachableCases += 1
                    assertEquals(null, expectedDistance, "seed=$seed returned ${actual.reason}")
                }
            }
        }

        assertTrue(reachableCases > 0, "seeded corpus must contain reachable maps")
        assertTrue(unreachableCases > 0, "seeded corpus must contain unreachable maps")
    }

    @Test
    fun canonical64x64WorstCaseSearchEmitsMachineReadableMetric() {
        // A fully open corner-to-corner map is a tie-heavy workload: every monotone route has the
        // same integer cost, so the deterministic open-set ordering is exercised broadly.
        val width = 64
        val height = 64
        val world = TileWorld.filled(WorldSize(width, height), terrain, "floor")
        val from = TilePosition(0, 0)
        val to = TilePosition(width - 1, height - 1)

        val started = System.nanoTime()
        val result = assertIs<AStarPathResult.Found>(AStarPathfinder().find(world, from, to))
        val elapsedNanos = System.nanoTime() - started
        assertValidPath(world, result.tiles)

        val metric =
            "{" +
                "\"algorithm\":\"a_star\"," +
                "\"scenario\":\"canonical-64x64\"," +
                "\"width\":$width," +
                "\"height\":$height," +
                "\"start_x\":${from.x}," +
                "\"start_y\":${from.y}," +
                "\"goal_x\":${to.x}," +
                "\"goal_y\":${to.y}," +
                "\"reachable\":true," +
                "\"path_tiles\":${result.tiles.size}," +
                "\"path_cost\":${pathCost(result.tiles)}," +
                "\"search_ns\":$elapsedNanos" +
                "}"
        println(metric)

        val fields = parseFlatJsonObject(metric)
        assertEquals(
            setOf(
                "algorithm",
                "scenario",
                "width",
                "height",
                "start_x",
                "start_y",
                "goal_x",
                "goal_y",
                "reachable",
                "path_tiles",
                "path_cost",
                "search_ns",
            ),
            fields.keys,
        )
        assertEquals("a_star", fields.getValue("algorithm"))
        assertEquals("canonical-64x64", fields.getValue("scenario"))
        assertEquals("64", fields.getValue("width"))
        assertEquals("64", fields.getValue("height"))
        assertEquals("127", fields.getValue("path_tiles"))
        assertEquals("126", fields.getValue("path_cost"))
        assertTrue(fields.getValue("search_ns").toLong() >= 0L)
    }

    private fun buildBlockedSet(width: Int, height: Int, random: Random): MutableSet<TilePosition> =
        (0 until height).flatMap { y ->
            (0 until width).mapNotNull { x ->
                if (random.nextInt(100) < 24) TilePosition(x, y) else null
            }
        }.toMutableSet()

    private fun worldWithBlockedTiles(
        width: Int,
        height: Int,
        blocked: Set<TilePosition>,
    ): TileWorld = TileWorld(
        size = WorldSize(width, height),
        terrainRules = terrain,
        tiles = (0 until height).flatMap { y ->
            (0 until width).map { x ->
                WorldTile(if (TilePosition(x, y) in blocked) "wall" else "floor")
            }
        },
    )

    private fun bfsDistance(
        width: Int,
        height: Int,
        blocked: Set<TilePosition>,
        from: TilePosition,
        to: TilePosition,
    ): Int? {
        val frontier = ArrayDeque<TilePosition>()
        val distances = mutableMapOf<TilePosition, Int>(from to 0)
        frontier.add(from)

        while (frontier.isNotEmpty()) {
            val current = frontier.removeFirst()
            if (current == to) return distances.getValue(current)
            for (neighbor in current.neighbors4()) {
                if (neighbor.x !in 0 until width || neighbor.y !in 0 until height) continue
                if (neighbor in blocked || neighbor in distances) continue
                distances[neighbor] = distances.getValue(current) + 1
                frontier.addLast(neighbor)
            }
        }
        return null
    }

    private fun assertValidPath(
        world: TileWorld,
        path: List<TilePosition>,
        allowOccupiedStart: Boolean = false,
    ) {
        assertTrue(path.isNotEmpty())
        path.forEachIndexed { index, position ->
            assertTrue(world.inBounds(position), "path contains out-of-bounds tile $position")
            assertTrue(
                world.canOccupy(position) || (allowOccupiedStart && index == 0),
                "path contains blocked tile $position",
            )
        }
        path.zipWithNext().forEach { (from, to) ->
            assertEquals(1, from.manhattanDistance(to), "path contains a non-neighbor step")
        }
    }

    private fun pathCost(path: List<TilePosition>): Int = path.size - 1

    private fun assertNoPath(result: AStarPathResult, reason: String) {
        val noPath = assertIs<AStarPathResult.NoPath>(result)
        assertEquals(reason, noPath.reason)
    }

    private fun parseFlatJsonObject(json: String): Map<String, String> {
        assertTrue(json.startsWith("{") && json.endsWith("}"))
        return json.removePrefix("{").removeSuffix("}").split(',').associate { entry ->
            val (rawKey, rawValue) = entry.split(':', limit = 2)
            rawKey.removeSurrounding("\"") to rawValue.removeSurrounding("\"")
        }
    }
}
