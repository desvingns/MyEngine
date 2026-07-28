package dev.myengine.render

import dev.myengine.core.RunStatus
import dev.myengine.core.RunSummary
import dev.myengine.core.TerminalReason
import dev.myengine.core.Tick
import dev.myengine.core.command.TargetingMode
import dev.myengine.world.TilePosition
import dev.myengine.world.WorldSize

/** Opaque path/key pair carried to a platform render consumer without asset decoding. */
data class RenderAssetRef(
    val path: String,
    val atlasKey: String? = null,
)

data class RenderTile(
    val position: TilePosition,
    val terrainId: String,
    val buildable: Boolean,
    val assetRef: RenderAssetRef? = null,
)

data class RenderEntity(
    val id: Long,
    val type: String,
    val position: TilePosition,
    val health: Int? = null,
    val towerTier: Int? = null,
    val assetRef: RenderAssetRef? = null,
)

data class DebugOverlay(
    val tick: Tick,
    val entityCount: Int,
    val wave: String?,
    val selectedTile: TilePosition?,
    val lastCommandOrError: String?,
)

data class HudLabels(
    val resources: String,
    val wave: String,
    val nextWave: String,
    val coreHealth: String,
    val build: String,
    val upgrade: String,
    val damage: String,
    val kills: String,
    val tier: String,
)

data class HudResourceAmount(
    val resourceId: String,
    val label: String,
    val amount: Int,
)

/** Immutable content projection of one enemy entry in the next-wave preview. */
data class HudWaveCompositionEntry(
    val enemyId: String,
    val count: Int,
)

data class HudTowerTier(
    val branch: String,
    val tier: Int,
    val label: String,
    val cost: HudResourceAmount,
    val damage: Int,
)

data class HudBuildTower(
    val towerId: String,
    val label: String,
    val cost: HudResourceAmount,
    val tiers: List<HudTowerTier>,
)

data class HudTowerInfo(
    val entityId: Long,
    val towerId: String,
    val label: String,
    val branch: String?,
    val tier: Int,
    val damage: Int,
    val actualDamage: Long,
    val kills: Int,
    val targetingMode: TargetingMode,
    val availableUpgrades: List<HudTowerTier>,
)

data class HudSnapshot(
    val labels: HudLabels,
    val resources: List<HudResourceAmount>,
    val wave: Int,
    val totalWaves: Int,
    val nextWaveInTicks: Long?,
    val nextWaveComposition: List<HudWaveCompositionEntry>,
    val coreHealth: Int,
    val buildTowers: List<HudBuildTower>,
    val towers: List<HudTowerInfo>,
) {
    companion object {
        val EMPTY = HudSnapshot(
            labels = HudLabels("", "", "", "", "", "", "", "", ""),
            resources = emptyList(),
            wave = 0,
            totalWaves = 0,
            nextWaveInTicks = null,
            nextWaveComposition = emptyList(),
            coreHealth = 0,
            buildTowers = emptyList(),
            towers = emptyList(),
        )
    }
}

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
    val hud: HudSnapshot = HudSnapshot.EMPTY,
)
