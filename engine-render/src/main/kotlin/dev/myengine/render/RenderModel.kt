package dev.myengine.render

import dev.myengine.core.RunStatus
import dev.myengine.core.RunSummary
import dev.myengine.core.TerminalReason
import dev.myengine.core.Tick
import dev.myengine.core.CombatEvents
import dev.myengine.core.command.TargetingMode
import dev.myengine.world.TilePosition
import dev.myengine.world.WorldSize
import java.util.Collections

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

class RenderEntity(
    val id: Long,
    val type: String,
    val position: TilePosition,
    val health: Int? = null,
    val towerTier: Int? = null,
    val assetRef: RenderAssetRef? = null,
    activeEffectTags: List<String> = emptyList(),
    val isBoss: Boolean = false,
) {
    val activeEffectTags: List<String> = Collections.unmodifiableList(activeEffectTags.toList().sorted())

    override fun equals(other: Any?): Boolean = other is RenderEntity &&
        id == other.id &&
        type == other.type &&
        position == other.position &&
        health == other.health &&
        towerTier == other.towerTier &&
        assetRef == other.assetRef &&
        activeEffectTags == other.activeEffectTags &&
        isBoss == other.isBoss

    override fun hashCode(): Int = listOf(id, type, position, health, towerTier, assetRef, activeEffectTags, isBoss).hashCode()

    override fun toString(): String = "RenderEntity(id=$id, type='$type', position=$position, health=$health, " +
        "towerTier=$towerTier, assetRef=$assetRef, activeEffectTags=$activeEffectTags, isBoss=$isBoss)"
}

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

enum class RenderZoneKind {
    STOCKPILE,
    HARVEST_DESIGNATION,
}

/** Immutable presentation-only zone overlay data. */
class RenderZone(
    val id: String,
    val kind: RenderZoneKind,
    tiles: List<TilePosition>,
    allowedResourceIds: List<String> = emptyList(),
    val resourceId: String? = null,
    val jobId: String? = null,
) {
    val tiles: List<TilePosition> = Collections.unmodifiableList(tiles.toList().sorted())
    val allowedResourceIds: List<String> = Collections.unmodifiableList(allowedResourceIds.toList().sorted())

    override fun equals(other: Any?): Boolean = other is RenderZone &&
        id == other.id && kind == other.kind && tiles == other.tiles &&
        allowedResourceIds == other.allowedResourceIds && resourceId == other.resourceId && jobId == other.jobId

    override fun hashCode(): Int = listOf(id, kind, tiles, allowedResourceIds, resourceId, jobId).hashCode()

    override fun toString(): String = "RenderZone(id='$id', kind=$kind, tiles=$tiles, " +
        "allowedResourceIds=$allowedResourceIds, resourceId=$resourceId, jobId=$jobId)"
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
    /** Transient immutable combat events emitted during the latest simulation tick. */
    val combatEvents: CombatEvents = CombatEvents.EMPTY,
    val zones: List<RenderZone> = emptyList(),
)
