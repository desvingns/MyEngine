package dev.myengine.core.command

import dev.myengine.core.CommandId
import dev.myengine.core.CommandQueue
import dev.myengine.core.Tick
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ZoneCommandsTest {
    @Test
    fun zoneCommandsExposeStablePayloadsAndIdentity() {
        val define = DefineStockpileZoneCommand(
            id = CommandId(7), scheduledTick = Tick(12), zoneId = "ore-yard",
            tiles = listOf(TileCoordinate(4, 5), TileCoordinate(2, 1)),
            allowedResourceIds = setOf("zinc", "bolt"), actorId = 99L,
        )
        val update = UpdateStockpileZoneCommand(
            id = CommandId(8), scheduledTick = Tick(13), zoneId = "ore-yard",
            tiles = listOf(TileCoordinate(2, 1)), allowedResourceIds = setOf("bolt"),
        )
        val remove = RemoveStockpileZoneCommand(CommandId(9), Tick(14), "ore-yard")
        val designate = DesignateHarvestNodeCommand(
            id = CommandId(10), scheduledTick = Tick(15), designationId = "node-1",
            resourceId = "bolt", position = TileCoordinate(5, 5), actorId = 100L,
        )
        val removeDesignation = RemoveHarvestDesignationCommand(CommandId(11), Tick(16), "node-1")

        assertEquals("define_stockpile_zone", define.type)
        assertEquals("ore-yard:2.1,4.5:bolt,zinc", define.stablePayload())
        assertEquals(99L, define.actorId)
        assertEquals("update_stockpile_zone", update.type)
        assertEquals("ore-yard:2.1:bolt", update.stablePayload())
        assertEquals("remove_stockpile_zone", remove.type)
        assertEquals("ore-yard", remove.stablePayload())
        assertEquals("designate_harvest_node", designate.type)
        assertEquals("node-1:bolt:5:5", designate.stablePayload())
        assertEquals("remove_harvest_designation", removeDesignation.type)
        assertEquals("node-1", removeDesignation.stablePayload())
    }

    @Test
    fun zoneCommandsRejectInvalidIdentity() {
        assertFailsWith<IllegalArgumentException> {
            DefineStockpileZoneCommand(CommandId(1), Tick(1), "", listOf(TileCoordinate(1, 1)))
        }
        assertFailsWith<IllegalArgumentException> {
            DefineStockpileZoneCommand(CommandId(1), Tick(1), "zone", emptyList())
        }
        assertFailsWith<IllegalArgumentException> {
            DefineStockpileZoneCommand(CommandId(1), Tick(1), "zone", listOf(TileCoordinate(-1, 1)))
        }
        assertFailsWith<IllegalArgumentException> {
            DesignateHarvestNodeCommand(CommandId(1), Tick(1), "node", "", TileCoordinate(1, 1))
        }
        assertFailsWith<IllegalArgumentException> {
            DesignateHarvestNodeCommand(CommandId(1), Tick(1), "node", "bolt", TileCoordinate(-1, 1))
        }
        assertFailsWith<IllegalArgumentException> {
            RemoveHarvestDesignationCommand(CommandId(1), Tick(1), "bad.id")
        }
    }

    @Test
    fun zoneCommandsKeepCommandQueueOrderingDeterministic() {
        val queue = CommandQueue()
        queue.submit(RemoveStockpileZoneCommand(CommandId(9), Tick(2), "z"))
        queue.submit(DefineStockpileZoneCommand(CommandId(3), Tick(2), "z", listOf(TileCoordinate(1, 1))))
        queue.submit(DesignateHarvestNodeCommand(CommandId(7), Tick(2), "d", "bolt", TileCoordinate(5, 5)))
        queue.submit(UpdateStockpileZoneCommand(CommandId(1), Tick(1), "z", listOf(TileCoordinate(1, 1))))

        assertEquals(
            listOf(CommandId(1), CommandId(3), CommandId(7), CommandId(9)),
            queue.drainFor(Tick(2)).map { it.id },
        )
    }
}
