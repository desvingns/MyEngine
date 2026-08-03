package dev.myengine.render

import dev.myengine.core.Tick
import dev.myengine.world.WorldSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TechTreeSnapshotTest {
    @Test
    fun techTreeProjectionSortsAndDefensivelyCopiesNestedLists() {
        val prerequisites = mutableListOf("z", "a")
        val unlocks = mutableListOf(
            TechUnlockSnapshot("recipe", "zeta"),
            TechUnlockSnapshot("tower", "alpha"),
        )
        val node = TechNodeSnapshot(
            id = "node",
            costResource = "bolt",
            costAmount = 2,
            prerequisites = prerequisites,
            unlocks = unlocks,
            researched = false,
            available = true,
        )
        val nodes = mutableListOf(node)
        val tree = TechTreeSnapshot(nodes)

        prerequisites += "mutated"
        unlocks += TechUnlockSnapshot("building", "late")
        nodes.clear()

        assertEquals(listOf("a", "z"), tree.nodes.single().prerequisites)
        assertEquals(listOf("recipe:zeta", "tower:alpha"), tree.nodes.single().unlocks.map { "${it.type}:${it.id}" })
        assertFailsWith<UnsupportedOperationException> { (tree.nodes as MutableList<TechNodeSnapshot>).add(node) }
        assertFailsWith<UnsupportedOperationException> {
            (tree.nodes.single().prerequisites as MutableList<String>).add("x")
        }
    }

    @Test
    fun engineSnapshotKeepsTechTreeOptionalForLegacyConsumers() {
        val snapshot = EngineSnapshot(
            worldSize = WorldSize(1, 1),
            tiles = emptyList(),
            entities = emptyList(),
            coreHealth = 1,
            debug = DebugOverlay(Tick(0), 0, null, null, null),
        )

        assertEquals(TechTreeSnapshot.EMPTY, snapshot.techTree)
    }
}
