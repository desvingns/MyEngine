package dev.myengine.logistics

import dev.myengine.core.StableHash

data class Inventory(
    val resources: Map<String, Int> = emptyMap(),
    val capacity: Int? = null,
) {
    init {
        resources.forEach { (id, amount) ->
            require(id.isNotBlank()) { "Resource id cannot be blank." }
            require(amount >= 0) { "Resource amount cannot be negative." }
        }
        require(capacity == null || capacity >= 0) { "Capacity cannot be negative." }
    }

    fun amount(resourceId: String): Int = resources[resourceId] ?: 0

    fun canAdd(resourceId: String, amount: Int): Boolean {
        require(amount >= 0) { "Amount cannot be negative." }
        val total = resources.values.sum()
        return capacity == null || total + amount <= capacity
    }

    fun add(resourceId: String, amount: Int): Inventory {
        require(amount >= 0) { "Amount cannot be negative." }
        require(canAdd(resourceId, amount)) { "Inventory capacity exceeded." }
        return copy(resources = resources + (resourceId to amount(resourceId) + amount))
    }

    fun canRemove(resourceId: String, amount: Int): Boolean {
        require(amount >= 0) { "Amount cannot be negative." }
        return amount(resourceId) >= amount
    }

    fun remove(resourceId: String, amount: Int): Inventory {
        require(canRemove(resourceId, amount)) { "Not enough '$resourceId'." }
        val next = amount(resourceId) - amount
        return copy(resources = if (next == 0) resources - resourceId else resources + (resourceId to next))
    }

    fun appendHash(hash: StableHash) {
        resources.toSortedMap().forEach { (id, amount) -> hash.add(id).add(amount) }
        hash.add(capacity ?: -1)
    }
}
