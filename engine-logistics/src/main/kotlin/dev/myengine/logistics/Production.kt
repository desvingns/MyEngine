package dev.myengine.logistics

import dev.myengine.content.RecipeContent
import dev.myengine.world.TilePosition

data class Producer(
    val id: String,
    val recipeId: String,
    val progressTicks: Int = 0,
    val position: TilePosition? = null,
    val resourceNodePosition: TilePosition? = null,
)

data class ProductionSource(
    val resourceId: String,
    val availableAmount: Int,
    val infinite: Boolean = false,
) {
    init {
        require(resourceId.isNotBlank()) { "Production source resource cannot be blank." }
        require(availableAmount >= 0) { "Production source amount cannot be negative." }
    }
}

data class ProductionResult(
    val producer: Producer,
    val inventory: Inventory,
    val completed: Boolean,
    val producedAmount: Int = 0,
)

class ProducerSystem(
    private val recipes: Map<String, RecipeContent>,
    private val isRecipeAvailable: (String) -> Boolean = { true },
) {
    fun tick(producer: Producer, inventory: Inventory): ProductionResult {
        val recipe = recipes[producer.recipeId] ?: error("Unknown recipe '${producer.recipeId}'.")
        if (!isRecipeAvailable(recipe.id)) return ProductionResult(producer, inventory, completed = false)
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

    /** Output-only production against a finite or infinite resource node. */
    fun tick(producer: Producer, inventory: Inventory, source: ProductionSource): ProductionResult {
        val recipe = recipes[producer.recipeId] ?: error("Unknown recipe '${producer.recipeId}'.")
        require(recipe.inputResource == null && recipe.inputAmount == 0) {
            "Extractor production recipes must not require input resources."
        }
        if (source.resourceId != recipe.outputResource || source.availableAmount == 0) {
            return ProductionResult(producer, inventory, completed = false)
        }
        val result = tick(producer, inventory)
        if (!result.completed) return result
        val produced = if (source.infinite) recipe.outputAmount else minOf(recipe.outputAmount, source.availableAmount)
        val withoutFullBatch = result.inventory.remove(recipe.outputResource, recipe.outputAmount)
        return result.copy(
            inventory = if (produced == 0) withoutFullBatch else withoutFullBatch.add(recipe.outputResource, produced),
            producedAmount = produced,
        )
    }
}
