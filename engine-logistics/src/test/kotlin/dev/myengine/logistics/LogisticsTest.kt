package dev.myengine.logistics

import dev.myengine.content.RecipeContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LogisticsTest {
    @Test
    fun inventoryCannotGoNegative() {
        val inventory = Inventory(mapOf("ore" to 1))

        assertTrue(inventory.canRemove("ore", 1))
        assertFalse(inventory.canRemove("ore", 2))
    }

    @Test
    fun producerConsumesAndProducesDeterministically() {
        val recipe = RecipeContent("smelt", "ore", 1, "plate", 2, durationTicks = 2)
        val system = ProducerSystem(mapOf(recipe.id to recipe))
        var producer = Producer("p1", "smelt")
        var inventory = Inventory(mapOf("ore" to 1))

        system.tick(producer, inventory).also {
            producer = it.producer
            inventory = it.inventory
            assertFalse(it.completed)
            assertEquals(0, inventory.amount("ore"))
        }
        system.tick(producer, inventory).also {
            assertTrue(it.completed)
            assertEquals(2, it.inventory.amount("plate"))
        }
    }
}
