package dev.myengine.core.command

import dev.myengine.core.CommandId
import dev.myengine.core.EngineCommand
import dev.myengine.core.Tick

private val ZONE_ID_REGEX = Regex("[A-Za-z0-9_-]+")

private fun requireZoneId(id: String, label: String): String {
    require(id.matches(ZONE_ID_REGEX)) {
        "$label must match ${ZONE_ID_REGEX.pattern}."
    }
    return id
}

private fun normalizedTiles(tiles: List<TileCoordinate>): List<TileCoordinate> {
    require(tiles.isNotEmpty()) { "Zone must contain at least one tile." }
    require(tiles.all { it.x >= 0 && it.y >= 0 }) { "Zone tile coordinates must be non-negative." }
    require(tiles.distinct().size == tiles.size) { "Zone tiles must be unique." }
    return tiles.sortedWith(compareBy<TileCoordinate> { it.y }.thenBy { it.x })
}

private fun normalizedResourceIds(resourceIds: Set<String>): List<String> {
    require(resourceIds.all { it.matches(ZONE_ID_REGEX) }) {
        "Stockpile resource ids must match ${ZONE_ID_REGEX.pattern}."
    }
    return resourceIds.sorted()
}

/** Render-free request to create a new stockpile zone. */
data class DefineStockpileZoneCommand(
    override val id: CommandId,
    override val scheduledTick: Tick,
    val zoneId: String,
    val tiles: List<TileCoordinate>,
    val allowedResourceIds: Set<String> = emptySet(),
    override val actorId: Long? = null,
) : EngineCommand {
    init {
        requireZoneId(zoneId, "Stockpile zone id")
        normalizedTiles(tiles)
        normalizedResourceIds(allowedResourceIds)
    }

    override val type: String = "define_stockpile_zone"
    override fun stablePayload(): String = ZoneCommandPayload.encode(zoneId, tiles, allowedResourceIds)
}

/** Render-free request to replace an existing stockpile zone definition. */
data class UpdateStockpileZoneCommand(
    override val id: CommandId,
    override val scheduledTick: Tick,
    val zoneId: String,
    val tiles: List<TileCoordinate>,
    val allowedResourceIds: Set<String> = emptySet(),
    override val actorId: Long? = null,
) : EngineCommand {
    init {
        requireZoneId(zoneId, "Stockpile zone id")
        normalizedTiles(tiles)
        normalizedResourceIds(allowedResourceIds)
    }

    override val type: String = "update_stockpile_zone"
    override fun stablePayload(): String = ZoneCommandPayload.encode(zoneId, tiles, allowedResourceIds)
}

/** Render-free request to remove a stockpile zone. */
data class RemoveStockpileZoneCommand(
    override val id: CommandId,
    override val scheduledTick: Tick,
    val zoneId: String,
    override val actorId: Long? = null,
) : EngineCommand {
    init { requireZoneId(zoneId, "Stockpile zone id") }

    override val type: String = "remove_stockpile_zone"
    override fun stablePayload(): String = zoneId
}

/** Render-free request to designate one content resource node for a harvest job. */
data class DesignateHarvestNodeCommand(
    override val id: CommandId,
    override val scheduledTick: Tick,
    val designationId: String,
    val resourceId: String,
    val position: TileCoordinate,
    override val actorId: Long? = null,
) : EngineCommand {
    init {
        requireZoneId(designationId, "Harvest designation id")
        require(resourceId.matches(ZONE_ID_REGEX)) {
            "Harvest resource id must match ${ZONE_ID_REGEX.pattern}."
        }
        require(position.x >= 0 && position.y >= 0) { "Harvest tile coordinates must be non-negative." }
    }

    override val type: String = "designate_harvest_node"
    override fun stablePayload(): String =
        "$designationId:$resourceId:${position.x}:${position.y}"
}

/** Render-free request to cancel a harvest designation. */
data class RemoveHarvestDesignationCommand(
    override val id: CommandId,
    override val scheduledTick: Tick,
    val designationId: String,
    override val actorId: Long? = null,
) : EngineCommand {
    init { requireZoneId(designationId, "Harvest designation id") }

    override val type: String = "remove_harvest_designation"
    override fun stablePayload(): String = designationId
}

private object ZoneCommandPayload {
    fun encode(zoneId: String, tiles: List<TileCoordinate>, resourceIds: Set<String>): String =
        buildString {
            append(zoneId)
            append(':')
            append(tiles.sortedWith(compareBy<TileCoordinate> { it.y }.thenBy { it.x })
                .joinToString(",") { "${it.x}.${it.y}" })
            append(':')
            append(resourceIds.sorted().joinToString(","))
        }
}
