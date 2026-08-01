package dev.myengine.content

import java.math.BigDecimal
import dev.myengine.core.command.TargetingMode

data class ContentPackManifest(
    val id: String,
    val version: String,
    val schemaVersion: Int,
    val engineMin: String,
    val engineMax: String,
    val locales: List<String>,
    val dependencies: List<String>,
)

interface ContentDefinition {
    val id: String
}

/**
 * Opaque content-owned visual reference. The loader validates [path] and, when present, that
 * [atlasKey] exists in the pack's minimal atlas index; simulation never opens this asset.
 */
data class VisualAssetRef(
    val path: String,
    val atlasKey: String? = null,
) {
    init {
        require(path.isNotBlank()) { "Visual asset path cannot be blank." }
        require(atlasKey == null || atlasKey.isNotBlank()) { "Atlas key cannot be blank." }
    }
}

data class TileContent(
    override val id: String,
    val buildable: Boolean,
    val blocksMovement: Boolean,
    val isCore: Boolean,
    val assetRef: VisualAssetRef? = null,
) : ContentDefinition

data class ResourceContent(
    override val id: String,
    val displayKey: String,
) : ContentDefinition

data class TowerContent(
    override val id: String,
    val range: Int,
    val damage: Int,
    val cooldownTicks: Int,
    val costResource: String,
    val costAmount: Int,
    val sellRefundRatio: BigDecimal,
    val targetingMode: TargetingMode,
    /** Optional Manhattan-radius splash centered on the selected primary target. */
    val splashRadius: Int? = null,
    /** Percentage of base damage lost per Manhattan-distance ring inside [splashRadius]. */
    val falloffPercent: Int = 0,
    val upgradeTiers: Map<String, TowerUpgradeTier> = emptyMap(),
    val displayKey: String = "tower.$id",
    val assetRef: VisualAssetRef? = null,
    val effectId: String? = null,
) : ContentDefinition

enum class StatusEffectKind(val id: String) {
    SLOW("slow"),
    DOT("dot"),
    ;

    companion object {
        fun fromId(id: String): StatusEffectKind? = entries.firstOrNull { it.id == id.trim().lowercase() }
    }
}

enum class StatusEffectStackingRule(val id: String) {
    REFRESH("refresh"),
    STACK("stack"),
    IGNORE("ignore"),
    ;

    companion object {
        fun fromId(id: String): StatusEffectStackingRule? = entries.firstOrNull { it.id == id.trim().lowercase() }
    }
}

typealias StackingRule = StatusEffectStackingRule

/** Optional data-defined effect consumed by the defense simulation. */
data class StatusEffectContent(
    override val id: String,
    val kind: StatusEffectKind,
    val magnitude: Int,
    val durationTicks: Int,
    val stackingRule: StatusEffectStackingRule,
) : ContentDefinition {
    init {
        require(magnitude >= 0) { "Status effect magnitude cannot be negative." }
        require(durationTicks > 0) { "Status effect duration must be positive." }
        if (kind == StatusEffectKind.SLOW) {
            require(magnitude <= 100) { "Slow magnitude cannot exceed 100 percent." }
        } else {
            require(magnitude > 0) { "Damage-over-time magnitude must be positive." }
        }
    }
}

typealias EffectContent = StatusEffectContent

data class TowerUpgradeTier(
    val branch: String,
    val tier: Int,
    val range: Int,
    val damage: Int,
    val cooldownTicks: Int,
    val costResource: String,
    val costAmount: Int,
    val displayKey: String = "tower.upgrade.$branch.$tier",
    val assetRef: VisualAssetRef? = null,
) {
    init {
        require(branch.matches(BRANCH_ID_REGEX)) { "Upgrade branch must match ${BRANCH_ID_REGEX.pattern}." }
        require(tier > 0) { "Upgrade tier must be positive." }
    }

    val key: String = key(branch, tier)

    companion object {
        val BRANCH_ID_REGEX: Regex = Regex("[A-Za-z0-9_-]+")

        fun key(branch: String, tier: Int): String = "$branch:$tier"
    }
}

data class EnemyContent(
    override val id: String,
    val health: Int,
    val speedTilesPerTick: Int,
    val rewardResource: String,
    val rewardAmount: Int,
    val coreDamage: Int,
    val assetRef: VisualAssetRef? = null,
) : ContentDefinition

/** Minimal data-driven building definition; gameplay components are intentionally out of scope. */
data class BuildingContent(
    override val id: String,
    val assetRef: VisualAssetRef? = null,
) : ContentDefinition

data class RecipeContent(
    override val id: String,
    val inputResource: String?,
    val inputAmount: Int,
    val outputResource: String,
    val outputAmount: Int,
    val durationTicks: Int,
) : ContentDefinition

data class WaveSpawn(
    val enemyId: String,
    val count: Int,
)

/** Content-defined reward granted when this wave is called before its scheduled start tick. */
data class WaveEarlyCallBonus(
    val resourceId: String,
    val amount: Int,
) {
    init {
        require(resourceId.isNotBlank()) { "Early-call bonus resource id cannot be blank." }
        require(amount > 0) { "Early-call bonus amount must be positive." }
    }
}

data class WaveContent(
    override val id: String,
    val startTick: Long,
    val spawns: List<WaveSpawn>,
    val earlyCallBonus: WaveEarlyCallBonus? = null,
) : ContentDefinition

data class DifficultyContent(
    override val id: String,
    val healthMult: BigDecimal,
    val countMult: BigDecimal,
    val rewardMult: BigDecimal,
    val goldRateMult: BigDecimal,
) : ContentDefinition

/** A grid coordinate that belongs to content data, independent of a simulation-world implementation. */
data class MapCoordinate(
    val x: Int,
    val y: Int,
)

data class MapResourceNode(
    val resourceId: String,
    val amount: Int,
)

data class MapTerrainSymbol(
    val terrainId: String,
    val resourceNode: MapResourceNode? = null,
)

data class MapSpawn(
    val id: String,
    val position: MapCoordinate,
)

/** Content-owned policy for deciding whether a map's finite wave schedule can end in victory. */
enum class MapWinCondition {
    FINITE_WAVES,
    NO_WIN,
}

/**
 * Data-defined terminal policy for a map. Core-health exhaustion is always a loss; [leakBudget]
 * adds an optional earlier loss threshold. A declared budget must be positive so omitted and zero
 * never have ambiguous meanings.
 */
data class MapTerminalRules(
    val winCondition: MapWinCondition = MapWinCondition.FINITE_WAVES,
    val leakBudget: Int? = null,
) {
    init {
        require(leakBudget == null || leakBudget > 0) { "Leak budget must be positive when declared." }
    }
}

/**
 * Structured nested content for a tile map. Validation belongs to [ContentPackLoader], so this
 * immutable definition is safe to pass to world-owning game modules without leaking JSON types.
 */
data class MapContent(
    override val id: String,
    val width: Int,
    val height: Int,
    val terrainRows: List<String>,
    val terrainMapping: Map<Char, MapTerrainSymbol>,
    val spawns: Map<String, MapSpawn>,
    val core: MapCoordinate,
    val terminalRules: MapTerminalRules = MapTerminalRules(),
) : ContentDefinition {
    /** Deterministic bridge for consumers that presently support a single wave spawn. */
    val primarySpawn: MapSpawn
        get() = spawns.values.minBy { it.id }

    fun symbolAt(position: MapCoordinate): Char = terrainRows[position.y][position.x]
}

data class IncidentContent(
    override val id: String,
    val minThreat: Int,
    val maxThreat: Int,
    val weight: Int,
) : ContentDefinition

data class ContentRegistry(
    val manifest: ContentPackManifest,
    val tiles: Map<String, TileContent>,
    val resources: Map<String, ResourceContent>,
    val towers: Map<String, TowerContent>,
    val enemies: Map<String, EnemyContent>,
    val recipes: Map<String, RecipeContent>,
    val waves: Map<String, WaveContent>,
    val incidents: Map<String, IncidentContent>,
    val strings: Map<String, String>,
    val buildings: Map<String, BuildingContent> = emptyMap(),
    val difficulties: Map<String, DifficultyContent> = emptyMap(),
    val maps: Map<String, MapContent> = emptyMap(),
    val resolvedDifficultyId: String? = null,
    val effects: Map<String, StatusEffectContent> = emptyMap(),
) {
    fun requireTile(id: String): TileContent = tiles[id] ?: error("Unknown tile '$id'.")
    fun requireResource(id: String): ResourceContent = resources[id] ?: error("Unknown resource '$id'.")
    fun requireTower(id: String): TowerContent = towers[id] ?: error("Unknown tower '$id'.")
    fun requireEnemy(id: String): EnemyContent = enemies[id] ?: error("Unknown enemy '$id'.")
    fun requireBuilding(id: String): BuildingContent = buildings[id] ?: error("Unknown building '$id'.")
    fun requireEffect(id: String): StatusEffectContent = effects[id] ?: error("Unknown status effect '$id'.")
    fun requireMap(id: String? = null): MapContent = when {
        id != null -> maps[id] ?: error("Unknown map '$id'.")
        maps.size == 1 -> maps.values.single()
        maps.isEmpty() -> error("This content pack has no maps.json map definitions.")
        else -> error("Map id is required because this content pack defines ${maps.size} maps.")
    }

    fun requireString(key: String): String = strings[key] ?: error("Unknown localization key '$key'.")

    /**
     * Materializes one data-defined difficulty before the registry enters simulation.
     * Existing packs remain unchanged when no difficulty is selected.
     */
    fun resolveDifficulty(id: String): ContentRegistry {
        val alreadyResolved = resolvedDifficultyId
        if (alreadyResolved != null) {
            require(alreadyResolved == id) {
                "Registry already resolved for difficulty '$alreadyResolved', cannot resolve '$id'."
            }
            return this
        }
        val difficulty = difficulties[id] ?: error("Unknown difficulty '$id'.")
        return copy(
            enemies = enemies.mapValues { (_, enemy) ->
                enemy.copy(
                    health = DifficultyScaling.scalePopulation(enemy.health, difficulty.healthMult),
                    rewardAmount = DifficultyScaling.scalePayout(enemy.rewardAmount, difficulty),
                )
            },
            waves = waves.mapValues { (_, wave) ->
                wave.copy(
                    spawns = wave.spawns.map { spawn ->
                        spawn.copy(count = DifficultyScaling.scalePopulation(spawn.count, difficulty.countMult))
                    },
                )
            },
            resolvedDifficultyId = id,
        )
    }
}


object HudStringKeys {
    const val RESOURCES = "hud.resources"
    const val WAVE = "hud.wave"
    const val NEXT_WAVE = "hud.nextWave"
    const val CORE_HEALTH = "hud.coreHealth"
    const val BUILD = "hud.build"
    const val UPGRADE = "hud.upgrade"
    const val DAMAGE = "hud.damage"
    const val KILLS = "hud.kills"
    const val TIER = "hud.tier"

    val required: List<String> = listOf(
        RESOURCES,
        WAVE,
        NEXT_WAVE,
        CORE_HEALTH,
        BUILD,
        UPGRADE,
        DAMAGE,
        KILLS,
        TIER,
    )
}
