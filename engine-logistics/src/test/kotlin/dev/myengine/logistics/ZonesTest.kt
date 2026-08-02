package dev.myengine.logistics

import dev.myengine.world.TilePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ZonesTest {
    @Test
    fun zoneStoreNormalizesTilesResourcesAndIdsDeterministically() {
        val store = ZoneStore(
            initialStockpiles = listOf(
                StockpileZone("z-2", listOf(TilePosition(3, 2), TilePosition(1, 1)), setOf("zinc", "bolt")),
                StockpileZone("z-1", listOf(TilePosition(4, 0))),
            ),
        )

        assertEquals(listOf("z-1", "z-2"), store.allStockpiles().map { it.id })
        assertEquals(listOf(TilePosition(1, 1), TilePosition(3, 2)), store.stockpile("z-2")!!.normalizedTiles)
        assertEquals(setOf("bolt", "zinc"), store.stockpile("z-2")!!.normalizedResourceIds)
    }

    @Test
    fun sameKindStockpileOverlapIsRejectedButHarvestOverlapIsAllowed() {
        val store = ZoneStore()
        store.defineStockpile(StockpileZone("first", listOf(TilePosition(5, 5))))

        assertFailsWith<IllegalArgumentException> {
            store.defineStockpile(StockpileZone("second", listOf(TilePosition(5, 5))))
        }

        store.addHarvestDesignation(HarvestDesignation("node-1", "bolt", TilePosition(5, 5)))
        assertEquals(TilePosition(5, 5), store.harvestDesignation("node-1")!!.position)
    }

    @Test
    fun duplicateHarvestTileAndIdAreRejected() {
        val store = ZoneStore()
        store.addHarvestDesignation(HarvestDesignation("node-1", "bolt", TilePosition(5, 5)))

        assertFailsWith<IllegalArgumentException> {
            store.addHarvestDesignation(HarvestDesignation("node-1", "bolt", TilePosition(6, 5)))
        }
        assertFailsWith<IllegalArgumentException> {
            store.addHarvestDesignation(HarvestDesignation("node-2", "bolt", TilePosition(5, 5)))
        }
    }
}
