package dev.myengine.entities

import dev.myengine.core.StableHash
import dev.myengine.world.TilePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class EntityStoreTest {
    @Test
    fun idsAndIterationAreStable() {
        val store = EntityStore()
        store.create("enemy") { id -> Entity(id, "ignored", position = PositionComponent(TilePosition(2, 0))) }
        store.create("tower") { id -> Entity(id, "ignored", position = PositionComponent(TilePosition(1, 0))) }

        assertEquals(listOf(1L, 2L), store.all().map { it.id.value })
    }

    @Test
    fun removalDuringTickIsDeferredUntilFlush() {
        val store = EntityStore()
        val entity = store.create("enemy") { id -> Entity(id, "ignored") }

        store.markRemove(entity.id)

        assertEquals(1, store.count())
        store.flushRemovals()
        assertEquals(0, store.count())
    }

    @Test
    fun hashChangesWhenComponentChanges() {
        val first = EntityStore()
        val second = EntityStore()
        val a = first.create("enemy") { id -> Entity(id, "ignored", health = HealthComponent(3, 3)) }
        second.upsert(a.copy(health = HealthComponent(2, 3)))

        assertNotEquals(
            StableHash().also(first::appendHash).digest(),
            StableHash().also(second::appendHash).digest(),
        )
    }
}
