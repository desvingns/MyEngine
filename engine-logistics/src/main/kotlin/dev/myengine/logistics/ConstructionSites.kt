package dev.myengine.logistics

import dev.myengine.core.StableHash
import dev.myengine.world.TilePosition

/** Authoritative material ledger for one non-blocking construction blueprint. */
data class ConstructionSite(
    val id: String,
    val buildingId: String,
    val position: TilePosition,
    val materialResourceId: String,
    val requiredAmount: Int,
    val deliveredBySource: Map<String, Int> = emptyMap(),
) {
    init {
        require(id.isNotBlank()) { "Construction site id cannot be blank." }
        require(buildingId.isNotBlank()) { "Construction building id cannot be blank." }
        require(materialResourceId.isNotBlank()) { "Construction material resource cannot be blank." }
        require(requiredAmount > 0) { "Construction material requirement must be positive." }
        require(deliveredBySource.keys.all { it.isNotBlank() }) { "Construction source ids cannot be blank." }
        require(deliveredBySource.values.all { it > 0 }) { "Construction delivered amounts must be positive." }
        require(deliveredAmount <= requiredAmount) {
            "Construction delivered materials cannot exceed the requirement."
        }
    }

    val deliveredAmount: Int get() = deliveredBySource.values.sum()
    val remainingAmount: Int get() = requiredAmount - deliveredAmount
}

/** Android-free, sorted construction-site state with source-aware delivery accounting. */
class ConstructionSiteStore(initialSites: List<ConstructionSite> = emptyList()) {
    private val sites = sortedMapOf<String, ConstructionSite>()

    init {
        initialSites.sortedBy { it.id }.forEach(::add)
    }

    fun add(site: ConstructionSite) {
        require(!sites.containsKey(site.id)) { "Duplicate construction site '${site.id}'." }
        require(sites.values.none { it.position == site.position }) {
            "Construction site position ${site.position} is already reserved."
        }
        sites[site.id] = canonical(site)
    }

    fun get(siteId: String): ConstructionSite? = sites[siteId]

    fun all(): List<ConstructionSite> = sites.values.toList()

    fun remove(siteId: String): ConstructionSite? = sites.remove(siteId)

    /** Records a completed haul while retaining the source needed for deterministic cancellation refund. */
    fun deposit(siteId: String, sourceId: String, resourceId: String, amount: Int): Boolean {
        require(sourceId.isNotBlank()) { "Construction source id cannot be blank." }
        require(amount > 0) { "Construction deposit amount must be positive." }
        val site = sites[siteId] ?: return false
        if (site.materialResourceId != resourceId || site.remainingAmount < amount) return false
        sites[siteId] = canonical(
            site.copy(
                deliveredBySource = site.deliveredBySource +
                    (sourceId to ((site.deliveredBySource[sourceId] ?: 0) + amount)),
            ),
        )
        return true
    }

    fun appendHash(hash: StableHash) {
        if (sites.isEmpty()) return
        hash.add("construction-sites")
        all().forEach { site ->
            hash.add(site.id).add(site.buildingId)
                .add(site.position.x).add(site.position.y)
                .add(site.materialResourceId).add(site.requiredAmount)
            site.deliveredBySource.toSortedMap().forEach { (sourceId, amount) ->
                hash.add("delivered").add(sourceId).add(amount)
            }
        }
    }

    private fun canonical(site: ConstructionSite): ConstructionSite = site.copy(
        deliveredBySource = site.deliveredBySource.toSortedMap(),
    )
}
