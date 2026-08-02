package dev.myengine.core.command

import dev.myengine.core.CommandId
import dev.myengine.core.Tick
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BuildingCommandsTest {
    @Test
    fun placeAndRemoveBuildingCommandsExposeStablePayloads() {
        val place = PlaceBuildingCommand(CommandId(7), Tick(12), "wall", TileCoordinate(4, 5), actorId = 99L)
        val remove = RemoveBuildingCommand(CommandId(8), Tick(13), 123L, actorId = 100L)

        assertEquals("place_building", place.type)
        assertEquals("wall:4:5", place.stablePayload())
        assertEquals(99L, place.actorId)
        assertEquals("remove_building", remove.type)
        assertEquals("123", remove.stablePayload())
        assertEquals(100L, remove.actorId)
    }

    @Test
    fun buildingCommandsRejectInvalidIdentity() {
        assertFailsWith<IllegalArgumentException> {
            PlaceBuildingCommand(CommandId(1), Tick(1), "", TileCoordinate(1, 1))
        }
        assertFailsWith<IllegalArgumentException> {
            RemoveBuildingCommand(CommandId(1), Tick(1), 0L)
        }
    }
}
