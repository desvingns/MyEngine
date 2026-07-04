package dev.myengine.logistics

import dev.myengine.content.RecipeContent

data class Producer(
    val id: String,
    val recipeId: String,
    val progressTicks: Int = 0,
)

data class ProductionResult(
    val producer: Producer,
    val inventory: Inventory,
    val completed: Boolean,
)

class ProducerSystem(private val recipes: Map<String, RecipeContent>) {
    fun tick(producer: Producer, inventory: Inventory): ProductionResult {
        val recipe = recipes[producer.recipeId] ?: error("Unknown recipe '${producer.recipeId}'.")
        val inputResource = recipe.inputResource
        if (inputResource != null && producer.progressTicks == 0 && !inventory.canRemove(inputResource, recipe.inputAmount)) {
            return ProductionResult(producer, inventory, completed = false)
        }

        val afterInput = if (inputResource != null && producer.progressTicks == 0 && recipe.inputAmount > 0) {
            inventory.remove(inputResource, recipe.inputAmount)
        } else {
            inventory
        }

        val nextProgress = producer.progressTicks + 1
        if (nextProgress < recipe.durationTicks) {
            return ProductionResult(producer.copy(progressTicks = nextProgress), afterInput, completed = false)
        }

        val afterOutput = afterInput.add(recipe.outputResource, recipe.outputAmount)
        return ProductionResult(producer.copy(progressTicks = 0), afterOutput, completed = true)
    }
}
