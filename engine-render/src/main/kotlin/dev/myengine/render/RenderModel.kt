package dev.myengine.render

import dev.myengine.core.RunStatus
import dev.myengine.core.RunSummary
import dev.myengine.core.TerminalReason
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
    val towerTier: Int? = null,
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
    val path: List<TilePosition> = emptyList(),
    val coreHealth: Int,
    val debug: DebugOverlay,
    val runStatus: RunStatus = RunStatus.ACTIVE,
    val terminalReason: TerminalReason? = null,
    val terminalTick: Tick? = null,
    val runSummary: RunSummary = RunSummary(),
)
