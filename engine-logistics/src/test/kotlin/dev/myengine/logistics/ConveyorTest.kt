package dev.myengine.logistics

import dev.myengine.content.BeltDirectionContent
import dev.myengine.content.BeltGeometryContent
import dev.myengine.content.BuildingContent
import dev.myengine.world.TilePosition
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConveyorTest {
    @Test
    fun straightAndCornerContentConvertToBeltLines() {
        val straightContent = beltContent(
            id = "straight",
            geometry = BeltGeometryContent.STRAIGHT,
            direction = BeltDirectionContent.EAST,
            ticksPerCell = 2,
        )
        val cornerContent = beltContent(
            id = "corner",
            geometry = BeltGeometryContent.CORNER,
            direction = BeltDirectionContent.SOUTH,
            ticksPerCell = 3,
        )

        val straightLine = BeltLine(
            id = straightContent.id,
            cells = listOf(
                straightContent.toBeltCell(TilePosition(2, 2))!!,
                straightContent.toBeltCell(TilePosition(3, 2))!!,
            ),
            ticksPerCell = straightContent.beltTicksPerCell!!,
        )
        val cornerLine = BeltLine(
            id = cornerContent.id,
            cells = listOf(cornerContent.toBeltCell(TilePosition(4, 2))!!),
            ticksPerCell = cornerContent.beltTicksPerCell!!,
        )

        assertEquals(BeltGeometry.STRAIGHT, straightLine.cells[0].geometry)
        assertEquals(BeltDirection.EAST, straightLine.cells[0].direction)
        assertEquals(2, straightLine.ticksPerCell)
        assertEquals(BeltGeometry.CORNER, cornerLine.cells.single().geometry)
        assertEquals(BeltDirection.SOUTH, cornerLine.cells.single().direction)
        assertEquals(3, cornerLine.ticksPerCell)
    }

    @Test
    fun ticksPerCellAndSinkToSourceOrderingAdvanceAChainDeterministically() {
        val belt = BeltLine(
            id = "chain",
            cells = cells(3),
            ticksPerCell = 2,
            items = listOf(
                BeltItem("source-item", "ore", 1, cellIndex = 0, progressTicks = 1),
                BeltItem("middle-item", "ore", 1, cellIndex = 1, progressTicks = 1),
            ),
        )
        val system = BeltTransportSystem()

        val first = system.tick(
            state = BeltTransportState(listOf(belt)),
            pull = { null },
            push = { _, _ -> false },
        ).state.belts.single()
        assertEquals(2, first.itemAt(2)?.cellIndex)
        assertEquals("middle-item", first.itemAt(2)?.id)
        assertEquals("source-item", first.itemAt(1)?.id)
        assertEquals(0, first.itemAt(1)?.progressTicks)
        assertEquals(0, first.itemAt(2)?.progressTicks)

        val second = system.tick(
            state = BeltTransportState(listOf(first)),
            pull = { null },
            push = { _, _ -> false },
        ).state.belts.single()
        assertEquals(1, second.itemAt(1)?.progressTicks)
        assertEquals(1, second.itemAt(2)?.progressTicks)

        val thirdResult = system.tick(
            state = BeltTransportState(listOf(second)),
            pull = { null },
            push = { _, item -> item.id == "middle-item" },
        )
        assertEquals(listOf("middle-item"), thirdResult.delivered.map { it.id })
        assertEquals("source-item", thirdResult.state.belts.single().itemAt(2)?.id)
        assertEquals(0, thirdResult.state.belts.single().itemAt(2)?.progressTicks)
    }

    @Test
    fun fullBeltAppliesBackpressureWithoutPullingOrReorderingItems() {
        val items = listOf(
            BeltItem("a", "ore", 1, cellIndex = 0, progressTicks = 2),
            BeltItem("b", "ore", 1, cellIndex = 1, progressTicks = 2),
            BeltItem("c", "ore", 1, cellIndex = 2, progressTicks = 2),
        )
        var pullCalls = 0

        val result = BeltTransportSystem().tick(
            state = BeltTransportState(listOf(BeltLine("full", cells(3), ticksPerCell = 2, items = items))),
            pull = {
                pullCalls += 1
                BeltItem("unexpected", "ore", 1, cellIndex = 0)
            },
            push = { _, _ -> false },
        )

        assertEquals(0, pullCalls)
        assertEquals(items, result.state.belts.single().items)
        assertTrue(result.delivered.isEmpty())
        assertTrue(result.pulled.isEmpty())
    }

    @Test
    fun beltIdsAndPullOrderAreCanonicalAcrossRepeatedRuns() {
        fun run(): Pair<List<String>, BeltTransportState> {
            val calls = mutableListOf<String>()
            val result = BeltTransportSystem().tick(
                state = BeltTransportState(
                    listOf(
                        BeltLine("zeta", cells(1), ticksPerCell = 1),
                        BeltLine("alpha", cells(1), ticksPerCell = 1),
                    ),
                ),
                pull = { belt ->
                    calls += belt.id
                    null
                },
                push = { _, _ -> false },
            )
            return calls to result.state
        }

        val first = run()
        val second = run()

        assertEquals(listOf("alpha", "zeta"), first.first)
        assertEquals(first, second)
        assertFalse(first.second.belts.any { it.items.isNotEmpty() })
    }

    private fun beltContent(
        id: String,
        geometry: BeltGeometryContent,
        direction: BeltDirectionContent,
        ticksPerCell: Int,
    ) = BuildingContent(
        id = id,
        costResource = "bolt",
        costAmount = 1,
        maxHealth = 1,
        footprintWidth = 1,
        footprintHeight = 1,
        sellRefundRatio = BigDecimal("0.5"),
        displayKey = "building.$id",
        beltGeometry = geometry,
        beltDirection = direction,
        beltTicksPerCell = ticksPerCell,
    )

    private fun cells(count: Int): List<BeltCell> = (0 until count).map { index ->
        BeltCell(TilePosition(index, 0), BeltGeometry.STRAIGHT, BeltDirection.EAST)
    }
}
