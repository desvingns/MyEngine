package dev.myengine.core.command

import dev.myengine.core.CommandId
import dev.myengine.core.EngineCommand
import dev.myengine.core.Tick

/** Render-free request to place a content-defined 1x1 wall. */
data class PlaceBuildingCommand(
    override val id: CommandId,
    override val scheduledTick: Tick,
    val buildingId: String,
    val position: TileCoordinate,
    override val actorId: Long? = null,
) : EngineCommand {
    init {
        require(buildingId.isNotBlank()) { "Building id cannot be blank." }
    }

    override val type: String = "place_building"
    override fun stablePayload(): String = "$buildingId:${position.x}:${position.y}"
}

/** Render-free request to remove a placed building and receive its deterministic refund. */
data class RemoveBuildingCommand(
    override val id: CommandId,
    override val scheduledTick: Tick,
    val buildingEntityId: Long,
    override val actorId: Long? = null,
) : EngineCommand {
    init {
        require(buildingEntityId > 0) { "Building entity id must be positive." }
    }

    override val type: String = "remove_building"
    override fun stablePayload(): String = buildingEntityId.toString()
}
