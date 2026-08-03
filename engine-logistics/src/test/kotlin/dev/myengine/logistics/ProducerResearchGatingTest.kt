package dev.myengine.logistics

import dev.myengine.content.RecipeContent
import kotlin.test.Test
import kotlin.test.assertEquals

class ProducerResearchGatingTest {
    private val recipe = RecipeContent(
        id = "generator",
        inputResource = "ore",
        inputAmount = 1,
        outputResource = "bolt",
        outputAmount = 2,
        durationTicks = 2,
    )

    @Test
    fun lockedProducerLeavesProducerAndInventoryUnchanged() {
        val producer = Producer("p1", "generator", progressTicks = 1)
        val inventory = Inventory(mapOf("ore" to 1, "bolt" to 3))
        val result = ProducerSystem(mapOf(recipe.id to recipe)) { false }.tick(producer, inventory)

        assertEquals(producer, result.producer)
        assertEquals(inventory, result.inventory)
        assertEquals(false, result.completed)
    }

    @Test
    fun unlockedProducerPreservesExistingInputAndOutputBehavior() {
        val producer = Producer("p1", "generator")
        val inventory = Inventory(mapOf("ore" to 1))
        val system = ProducerSystem(mapOf(recipe.id to recipe)) { true }

        val inProgress = system.tick(producer, inventory)
        val completed = system.tick(inProgress.producer, inProgress.inventory)

        assertEquals(1, inProgress.producer.progressTicks)
        assertEquals(emptyMap(), inProgress.inventory.resources)
        assertEquals(true, completed.completed)
        assertEquals(0, completed.producer.progressTicks)
        assertEquals(mapOf("bolt" to 2), completed.inventory.resources)
    }
}
