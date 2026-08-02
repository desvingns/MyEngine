package dev.myengine.ai

import dev.myengine.entities.MovementComponent
import dev.myengine.world.TerrainRule
import dev.myengine.world.TilePosition
import dev.myengine.world.TileWorld
import dev.myengine.world.WorldSize
import dev.myengine.world.WorldTile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AgentPathPlannerTest {
    private val terrain = mapOf(
        "floor" to TerrainRule("floor", buildable = true, blocksMovement = false),
        "wall" to TerrainRule("wall", buildable = false, blocksMovement = true),
    )

    @Test
    fun keepsValidStoredPath() {
        val world = TileWorld.filled(WorldSize(5, 2), terrain, "floor")
        val current = TilePosition(0, 0)
        val goal = TilePosition(4, 0)
        val stored = MovementComponent(
            path = listOf(
                TilePosition(0, 0),
                TilePosition(1, 0),
                TilePosition(2, 0),
                TilePosition(3, 0),
                TilePosition(4, 0),
            ),
            pathIndex = 0,
        )

        val plan = AgentPathPlanner().plan(world, current, goal, stored)
        val kept = assertIs<AgentPathPlan.Kept>(plan)

        assertTrue(kept.movement === stored)
    }

    @Test
    fun keepsValidPathFromCurrentIndexAndRepathsDeterministicallyAfterCurrentBlock() {
        val planner = AgentPathPlanner()
        val current = TilePosition(2, 0)
        val goal = TilePosition(4, 0)
        val stored = MovementComponent(
            path = listOf(
                TilePosition(0, 0),
                TilePosition(1, 0),
                current,
                TilePosition(3, 0),
                goal,
            ),
            pathIndex = 2,
        )
        val openWorld = TileWorld.filled(WorldSize(5, 2), terrain, "floor")

        val kept = assertIs<AgentPathPlan.Kept>(planner.plan(openWorld, current, goal, stored))
        assertTrue(kept.movement === stored)
        assertTrue(planner.isValid(openWorld, current, goal, stored))

        val firstBlockedWorld = openWorldWithBlockAfterCurrent()
        val secondBlockedWorld = openWorldWithBlockAfterCurrent()
        val firstRepath = assertIs<AgentPathPlan.Repathed>(
            planner.plan(firstBlockedWorld, current, goal, stored),
        )
        val secondRepath = assertIs<AgentPathPlan.Repathed>(
            planner.plan(secondBlockedWorld, current, goal, stored),
        )

        assertEquals(firstRepath.movement, secondRepath.movement)
        assertEquals(listOf(current, TilePosition(2, 1), TilePosition(3, 1), TilePosition(4, 1), goal), firstRepath.movement.path)
        assertTrue(TilePosition(3, 0) !in firstRepath.movement.path)
        assertTrue(!planner.isValid(firstBlockedWorld, current, goal, stored))
    }

    @Test
    fun blocksNextTileInvalidateStoredRouteAndTriggerDeterministicRepath() {
        val planner = AgentPathPlanner()
        val current = TilePosition(0, 0)
        val goal = TilePosition(4, 0)
        val stored = directPath(goal)

        val firstWorld = worldWithRouteBlocker()
        val secondWorld = worldWithRouteBlocker()
        val firstPlan = planner.plan(firstWorld, current, goal, stored)
        val secondPlan = planner.plan(secondWorld, current, goal, stored)

        val firstRepath = assertIs<AgentPathPlan.Repathed>(firstPlan)
        val secondRepath = assertIs<AgentPathPlan.Repathed>(secondPlan)
        assertEquals(firstRepath.movement, secondRepath.movement)
        assertEquals(current, firstRepath.movement.path.first())
        assertEquals(goal, firstRepath.movement.path.last())
        assertTrue(TilePosition(1, 0) !in firstRepath.movement.path)
        assertTrue(!planner.isValid(firstWorld, current, goal, stored))
    }

    @Test
    fun occupiedCurrentStartIsAllowedForRepath() {
        val world = TileWorld.filled(WorldSize(4, 2), terrain, "floor")
        val current = TilePosition(0, 0)
        val goal = TilePosition(3, 0)
        world.occupy(current, entityId = 42L)

        val plan = AgentPathPlanner().plan(world, current, goal)
        val repathed = assertIs<AgentPathPlan.Repathed>(plan)

        assertEquals(current, repathed.movement.path.first())
        assertEquals(goal, repathed.movement.path.last())
        assertTrue(repathed.movement.path.size > 1)
    }

    @Test
    fun blockedRouteWithNoAlternativeReturnsNoPath() {
        val world = TileWorld.filled(WorldSize(3, 1), terrain, "floor")
        val current = TilePosition(0, 0)
        val goal = TilePosition(2, 0)
        world.setTile(TilePosition(1, 0), WorldTile("wall"))

        val plan = AgentPathPlanner().plan(world, current, goal, directPath(goal))
        val noPath = assertIs<AgentPathPlan.NoPath>(plan)

        assertEquals("unreachable", noPath.reason)
    }

    private fun directPath(goal: TilePosition): MovementComponent = MovementComponent(
        path = listOf(
            TilePosition(0, 0),
            TilePosition(1, 0),
            TilePosition(2, 0),
            TilePosition(3, 0),
            goal,
        ),
        pathIndex = 0,
    )

    private fun worldWithRouteBlocker(): TileWorld {
        val world = TileWorld.filled(WorldSize(5, 2), terrain, "floor")
        world.setTile(TilePosition(1, 0), WorldTile("wall"))
        return world
    }

    private fun openWorldWithBlockAfterCurrent(): TileWorld = TileWorld.filled(
        WorldSize(5, 2),
        terrain,
        "floor",
    ).also { world ->
        world.setTile(TilePosition(3, 0), WorldTile("wall"))
    }
}
