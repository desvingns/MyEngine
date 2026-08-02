package dev.myengine.logistics

import dev.myengine.core.StableHash
import dev.myengine.world.TilePosition
import java.util.Collections

data class StockpileZone(
    val id: String,
    val tiles: List<TilePosition>,
    val allowedResourceIds: Set<String> = emptySet(),
    val storedResources: Map<String, Int> = emptyMap(),
) {
    init {
        require(id.isNotBlank()) { "Stockpile zone id cannot be blank." }
        require(tiles.isNotEmpty()) { "Stockpile zone must contain at least one tile." }
        require(tiles.distinct().size == tiles.size) { "Stockpile zone tiles must be unique." }
        require(allowedResourceIds.all { it.isNotBlank() }) { "Stockpile resource ids cannot be blank." }
        require(storedResources.keys.all { it.isNotBlank() }) { "Stockpile resource ids cannot be blank." }
        require(storedResources.values.all { it >= 0 }) { "Stockpile amounts cannot be negative." }
    }

    val normalizedTiles: List<TilePosition> = Collections.unmodifiableList(tiles.distinct().sorted())
    val normalizedResourceIds: Set<String> =
        Collections.unmodifiableSet(allowedResourceIds.toSortedSet())
}

data class HarvestDesignation(
    val id: String,
    val resourceId: String,
    val position: TilePosition,
    val jobId: String = jobIdFor(id),
) {
    init {
        require(id.isNotBlank()) { "Harvest designation id cannot be blank." }
        require(resourceId.isNotBlank()) { "Harvest resource id cannot be blank." }
        require(jobId.isNotBlank()) { "Harvest job id cannot be blank." }
    }

    companion object {
        fun jobIdFor(designationId: String): String = "harvest-node:$designationId"
    }
}

/**
 * Authoritative, Android-free store for player-authored zones and designations.
 * Stockpiles cannot overlap one another; harvest designations are a separate kind and may overlap.
 */
class ZoneStore(
    initialStockpiles: List<StockpileZone> = emptyList(),
    initialHarvestDesignations: List<HarvestDesignation> = emptyList(),
) {
    private val stockpiles = sortedMapOf<String, StockpileZone>()
    private val harvestDesignations = sortedMapOf<String, HarvestDesignation>()

    init {
        initialStockpiles.sortedBy { it.id }.forEach(::defineStockpile)
        initialHarvestDesignations.sortedBy { it.id }.forEach(::addHarvestDesignation)
    }

    fun defineStockpile(zone: StockpileZone) {
        require(!stockpiles.containsKey(zone.id)) { "Duplicate stockpile zone '${zone.id}'." }
        validateNoStockpileOverlap(zone)
        stockpiles[zone.id] = canonical(zone)
    }

    fun updateStockpile(zone: StockpileZone) {
        require(stockpiles.containsKey(zone.id)) { "Unknown stockpile zone '${zone.id}'." }
        validateNoStockpileOverlap(zone, excludingId = zone.id)
        stockpiles[zone.id] = canonical(zone.copy(storedResources = stockpiles.getValue(zone.id).storedResources))
    }

    fun removeStockpile(id: String): Boolean = stockpiles.remove(id) != null

    fun addHarvestDesignation(designation: HarvestDesignation) {
        require(!harvestDesignations.containsKey(designation.id)) {
            "Duplicate harvest designation '${designation.id}'."
        }
        require(harvestDesignations.values.none { it.position == designation.position }) {
            "Harvest tile ${designation.position} is already designated."
        }
        harvestDesignations[designation.id] = designation
    }

    fun removeHarvestDesignation(id: String): HarvestDesignation? = harvestDesignations.remove(id)

    fun stockpile(id: String): StockpileZone? = stockpiles[id]

    fun harvestDesignation(id: String): HarvestDesignation? = harvestDesignations[id]

    fun allStockpiles(): List<StockpileZone> = stockpiles.values.toList()

    fun allHarvestDesignations(): List<HarvestDesignation> = harvestDesignations.values.toList()

    fun deposit(zoneId: String, position: TilePosition, resourceId: String, amount: Int): Boolean {
        require(amount > 0) { "Deposit amount must be positive." }
        val zone = stockpiles[zoneId] ?: return false
        if (position !in zone.normalizedTiles) return false
        if (zone.normalizedResourceIds.isNotEmpty() && resourceId !in zone.normalizedResourceIds) return false
        stockpiles[zoneId] = canonical(zone.copy(
            storedResources = zone.storedResources + (resourceId to (zone.storedResources[resourceId] ?: 0) + amount),
        ))
        return true
    }

    fun appendHash(hash: StableHash) {
        allStockpiles().forEach { zone ->
            hash.add("stockpile").add(zone.id)
            zone.normalizedTiles.forEach { tile -> hash.add(tile.x).add(tile.y) }
            zone.normalizedResourceIds.forEach { hash.add(it) }
            zone.storedResources.toSortedMap().forEach { (resourceId, amount) ->
                hash.add("stored").add(resourceId).add(amount)
            }
        }
        allHarvestDesignations().forEach { designation ->
            hash.add("harvest").add(designation.id).add(designation.resourceId)
                .add(designation.position.x).add(designation.position.y).add(designation.jobId)
        }
    }

    private fun validateNoStockpileOverlap(zone: StockpileZone, excludingId: String? = null) {
        val occupied = stockpiles.values
            .filter { it.id != excludingId }
            .flatMap { it.normalizedTiles }
            .toSet()
        require(zone.normalizedTiles.none(occupied::contains)) {
            "Stockpile zone '${zone.id}' overlaps another stockpile."
        }
    }

    private fun canonical(zone: StockpileZone): StockpileZone = zone.copy(
        tiles = zone.normalizedTiles,
        allowedResourceIds = zone.normalizedResourceIds,
        storedResources = zone.storedResources.toSortedMap(),
    )
}
