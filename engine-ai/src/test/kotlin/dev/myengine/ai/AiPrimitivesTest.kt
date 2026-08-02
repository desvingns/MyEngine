package dev.myengine.ai

import dev.myengine.entities.EntityId
import dev.myengine.world.TerrainRule
import dev.myengine.world.TilePosition
import dev.myengine.world.TileWorld
import dev.myengine.world.WorldSize
import dev.myengine.world.WorldTile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class AiPrimitivesTest {
    private val terrain = mapOf(
        "floor" to TerrainRule("floor", buildable = true, blocksMovement = false),
        "wall" to TerrainRule("wall", buildable = false, blocksMovement = true),
    )

    @Test
    fun pathfinderFindsAroundBlockedTiles() {
        val world = TileWorld.filled(WorldSize(3, 3), terrain, "floor")
        world.setTile(TilePosition(1, 0), WorldTile("wall"))
        val result = GridPathfinder().find(world, PathRequest(TilePosition(0, 0), TilePosition(2, 0)))

        val path = assertIs<PathResult.Found>(result).tiles
        assertEquals(TilePosition(0, 0), path.first())
        assertEquals(TilePosition(2, 0), path.last())
    }

    @Test
    fun pathfinderReturnsNoPathWhenSealed() {
        val world = TileWorld.filled(WorldSize(3, 1), terrain, "floor")
        world.setTile(TilePosition(1, 0), WorldTile("wall"))

        assertIs<PathResult.NoPath>(GridPathfinder().find(world, PathRequest(TilePosition(0, 0), TilePosition(2, 0))))
    }

    @Test
    fun goalFieldPinsSortedNeighborTieOrder() {
        val world = TileWorld.filled(WorldSize(3, 3), terrain, "floor")
        val field = GoalField.build(world, TilePosition(1, 1))

        // TilePosition sorting is y-then-x: the reverse BFS discovers (0,0) first through
        // (1,0), not (0,1). Keep this literal route as the deterministic tie-break contract.
        assertEquals(
            listOf(TilePosition(0, 0), TilePosition(1, 0), TilePosition(1, 1)),
            field.pathFrom(TilePosition(0, 0)),
        )
        assertEquals(TilePosition(1, 0), field.nextStep(TilePosition(0, 0)))
    }

    @Test
    fun jobsAssignByPriorityThenId() {
        val board = JobBoard(
            listOf(
                Job("b", "haul", TilePosition(0, 0), priority = 10),
                Job("a", "build", TilePosition(1, 0), priority = 10),
                Job("c", "mine", TilePosition(2, 0), priority = 1),
            ),
        )

        assertEquals("a", board.assignNext(EntityId(1))?.job?.id)
        assertNull(board.assignNext(EntityId(2))?.takeIf { it.job.id == "a" })
    }

    @Test
    fun claimAcceptsNullOrOwnReservationAndRejectsForeignReservation() {
        val actor = EntityId(1)
        val otherActor = EntityId(2)
        val board = JobBoard(
            listOf(
                Job("free", "haul", TilePosition(0, 0), priority = 1),
                Job(
                    "own",
                    "build",
                    TilePosition(1, 0),
                    priority = 1,
                    reservedBy = actor,
                ),
                Job(
                    "foreign",
                    "mine",
                    TilePosition(2, 0),
                    priority = 1,
                    reservedBy = otherActor,
                ),
            ),
        )

        assertEquals(actor, board.claim("free", actor).reservedBy)
        assertEquals(actor, board.claim("own", actor).reservedBy)

        assertFailsWith<IllegalArgumentException> {
            board.claim("foreign", actor)
        }
        assertEquals(JobStatus.OPEN, board.get("foreign")?.status)
        assertEquals(otherActor, board.get("foreign")?.reservedBy)
        assertNull(board.get("foreign")?.assignedTo)
    }
}
