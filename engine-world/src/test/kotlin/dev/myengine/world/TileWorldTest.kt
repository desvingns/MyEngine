package dev.myengine.world

import dev.myengine.core.StableHash
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TileWorldTest {
    private val terrain = mapOf(
        "floor" to TerrainRule("floor", buildable = true, blocksMovement = false),
        "wall" to TerrainRule("wall", buildable = false, blocksMovement = true),
        "core" to TerrainRule("core", buildable = false, blocksMovement = false, isCore = true),
    )

    @Test
    fun buildabilityHonorsTerrainOccupancyAndResources() {
        val world = TileWorld.filled(WorldSize(3, 3), terrain, "floor")
        world.setTile(TilePosition(1, 0), WorldTile("wall"))
        world.setTile(TilePosition(0, 1), WorldTile("floor", ResourceNode("ore", 10)))
        world.occupy(TilePosition(2, 2), 99)

        assertTrue(world.canBuild(TilePosition(0, 0)))
        assertFalse(world.canBuild(TilePosition(1, 0)))
        assertFalse(world.canBuild(TilePosition(0, 1)))
        assertFalse(world.canBuild(TilePosition(2, 2)))
    }

    @Test
    fun corePositionsAreStable() {
        val world = TileWorld.filled(WorldSize(3, 3), terrain, "floor")
        world.setTile(TilePosition(2, 1), WorldTile("core"))

        assertEquals(listOf(TilePosition(2, 1)), world.corePositions())
    }

    @Test
    fun hashChangesWhenTileChanges() {
        val first = TileWorld.filled(WorldSize(2, 2), terrain, "floor")
        val second = TileWorld.filled(WorldSize(2, 2), terrain, "floor")
        second.setTile(TilePosition(1, 1), WorldTile("wall"))

        assertNotEquals(
            StableHash().also(first::appendHash).digest(),
            StableHash().also(second::appendHash).digest(),
        )
    }
}
