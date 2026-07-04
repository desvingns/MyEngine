package dev.myengine.render

import dev.myengine.core.CommandId
import dev.myengine.core.EngineCommand
import dev.myengine.core.Tick
import dev.myengine.world.TilePosition
import dev.myengine.world.WorldSize

data class RenderTile(
    val position: TilePosition,
    val terrainId: String,
    val buildable: Boolean,
)

data class RenderEntity(
    val id: Long,
    val type: String,
    val position: TilePosition,
    val health: Int? = null,
)

data class DebugOverlay(
    val tick: Tick,
    val entityCount: Int,
    val wave: String?,
    val selectedTile: TilePosition?,
    val lastCommandOrError: String?,
)

data class EngineSnapshot(
    val worldSize: WorldSize,
    val tiles: List<RenderTile>,
    val entities: List<RenderEntity>,
    val coreHealth: Int,
    val debug: DebugOverlay,
)

data class BuildTowerCommand(
    override val id: CommandId,
    override val scheduledTick: Tick,
    val towerId: String,
    val position: TilePosition,
    override val actorId: Long? = null,
) : EngineCommand {
    override val type: String = "build_tower"
    override fun stablePayload(): String = "$towerId:${position.x}:${position.y}"
}
