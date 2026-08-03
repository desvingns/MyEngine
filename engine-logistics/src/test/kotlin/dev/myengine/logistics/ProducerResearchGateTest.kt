package dev.myengine.logistics

import dev.myengine.content.RecipeContent
import kotlin.test.Test
import kotlin.test.assertEquals

class ProducerResearchGateTest {
    private val recipe = RecipeContent(
        id = "generator",
        inputResource = "ore",
        inputAmount = 2,
        outputResource = "metal",
        outputAmount = 1,
        durationTicks = 2,
    )

    @Test
    fun unavailableRecipeDoesNotConsumeInputOrAdvanceProgress() {
        val producer = Producer("p", recipe.id, progressTicks = 1)
        val inventory = Inventory(mapOf("ore" to 2))

        val result = ProducerSystem(mapOf(recipe.id to recipe), isRecipeAvailable = { false })
            .tick(producer, inventory)

        assertEquals(producer, result.producer)
        assertEquals(inventory, result.inventory)
        assertEquals(false, result.completed)
    }

    @Test
    fun availableRecipeKeepsExistingProductionBehavior() {
        val result = ProducerSystem(mapOf(recipe.id to recipe), isRecipeAvailable = { true })
            .tick(Producer("p", recipe.id), Inventory(mapOf("ore" to 2)))

        assertEquals(Producer("p", recipe.id, progressTicks = 1), result.producer)
        assertEquals(emptyMap(), result.inventory.resources)
        assertEquals(false, result.completed)
    }
}
