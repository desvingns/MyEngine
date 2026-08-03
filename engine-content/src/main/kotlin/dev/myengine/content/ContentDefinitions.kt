package dev.myengine.content

import java.math.BigDecimal
import dev.myengine.core.GameplayEventType
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

/** Opaque pack-relative audio reference resolved only by a platform presentation consumer. */
data class SoundRef(
    val path: String,
) {
    init {
        require(path.isNotBlank()) { "Sound path cannot be blank." }
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

data class DamageTypeContent(
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
    val damageTypeId: String? = null,
    /** Optional structure health used when an enemy pack enables structure attacks. */
    val maxHealth: Int = 10,
) : ContentDefinition {
    init {
        require(maxHealth > 0) { "Tower max health must be positive." }
    }
}

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
    /** When true, a blocked enemy attacks an adjacent live tower or building. */
    val attacksStructures: Boolean = false,
    val assetRef: VisualAssetRef? = null,
    val isElite: Boolean = false,
    val isBoss: Boolean = false,
    val healthScalePercent: Int = 100,
    val speedScalePercent: Int = 100,
    val rewardScalePercent: Int = 100,
    val resists: Map<String, Int> = emptyMap(),
) : ContentDefinition {
    init {
        require(!(isElite && isBoss)) { "An enemy cannot be both elite and boss." }
        require(healthScalePercent in 1..10_000) { "Enemy health scale must be between 1 and 10000 percent." }
        require(speedScalePercent in 1..10_000) { "Enemy speed scale must be between 1 and 10000 percent." }
        require(rewardScalePercent in 1..10_000) { "Enemy reward scale must be between 1 and 10000 percent." }
    }
}

/** Data-defined worker capabilities used by the deterministic hauling slice. */
data class WorkerContent(
    override val id: String,
    val speedTilesPerTick: Int,
    val capacity: Int,
) : ContentDefinition {
    init {
        require(speedTilesPerTick > 0) { "Worker speed must be positive." }
        require(capacity > 0) { "Worker capacity must be positive." }
    }
}

/** Data-defined colonist need policy consumed by the deterministic needs system. */
data class NeedContent(
    override val id: String,
    val decayPerTick: Int,
    val threshold: Int,
    val recoveryAmount: Int,
    val jobType: String,
    val priority: Int,
    val displayKey: String = "need.$id",
) : ContentDefinition {
    init {
        require(decayPerTick > 0) { "Need decay must be positive." }
        require(threshold in 0..100) { "Need threshold must be between 0 and 100." }
        require(recoveryAmount > 0) { "Need recovery amount must be positive." }
        require(jobType.isNotBlank()) { "Need job type cannot be blank." }
        require(priority >= 0) { "Need priority cannot be negative." }
        require(displayKey.isNotBlank()) { "Need display key cannot be blank." }
    }
}

/** Data-driven 1x1 wall definition for the player-placed blocker slice. */
data class BuildingContent(
    override val id: String,
    val costResource: String,
    val costAmount: Int,
    val maxHealth: Int,
    val footprintWidth: Int,
    val footprintHeight: Int,
    val sellRefundRatio: BigDecimal,
    val displayKey: String,
    val assetRef: VisualAssetRef? = null,
    val buildWorkTicks: Int = 1,
    /** Optional output-only recipe; when present this building is a resource extractor. */
    val producerRecipeId: String? = null,
) : ContentDefinition {
    init {
        require(costResource.isNotBlank()) { "Building cost resource cannot be blank." }
        require(costAmount > 0) { "Building cost must be positive." }
        require(maxHealth > 0) { "Building max health must be positive." }
        require(footprintWidth == 1 && footprintHeight == 1) { "Only 1x1 building footprints are supported." }
        require(sellRefundRatio >= BigDecimal.ZERO && sellRefundRatio <= BigDecimal.ONE) {
            "Building sell refund ratio must be between 0 and 1."
        }
        require(displayKey.isNotBlank()) { "Building display key cannot be blank." }
        require(buildWorkTicks > 0) { "Building work ticks must be positive." }
        require(producerRecipeId == null || producerRecipeId.isNotBlank()) {
            "Producer recipe id cannot be blank."
        }
    }
}

data class RecipeContent(
    override val id: String,
    val inputResource: String?,
    val inputAmount: Int,
    val outputResource: String,
    val outputAmount: Int,
    val durationTicks: Int,
) : ContentDefinition

enum class TechUnlockType(val id: String) {
    TOWER("tower"),
    BUILDING("building"),
    RECIPE("recipe"),
    ;

    companion object {
        fun fromId(id: String): TechUnlockType? = entries.firstOrNull { it.id == id.trim().lowercase() }
    }
}

data class TechUnlockRef(
    val type: TechUnlockType,
    val id: String,
) {
    init {
        require(id.isNotBlank()) { "Tech unlock id cannot be blank." }
    }

    val stableKey: String get() = "${type.id}:$id"
}

data class TechNodeContent(
    override val id: String,
    val costResource: String,
    val costAmount: Int,
    val prerequisites: List<String> = emptyList(),
    val unlocks: List<TechUnlockRef> = emptyList(),
) : ContentDefinition {
    init {
        require(id.isNotBlank()) { "Tech node id cannot be blank." }
        require(costResource.isNotBlank()) { "Tech node cost resource cannot be blank." }
        require(costAmount > 0) { "Tech node cost must be positive." }
        require(prerequisites.all(String::isNotBlank)) { "Tech node prerequisites cannot be blank." }
    }
}

data class WaveSpawn(
    val enemyId: String,
    val count: Int,
)

/** A deterministic modifier covering [count] consecutive enemies in a wave's spawn order. */
data class WaveModifier(
    val healthPercent: Int,
    val speedPercent: Int,
    val count: Int,
    val rewardPercent: Int = 100,
) {
    init {
        require(healthPercent > 0) { "Wave health percentage must be positive." }
        require(speedPercent > 0) { "Wave speed percentage must be positive." }
        require(healthPercent <= 10_000) { "Wave health percentage cannot exceed 10000." }
        require(speedPercent <= 10_000) { "Wave speed percentage cannot exceed 10000." }
        require(count > 0) { "Wave modifier count must be positive." }
        require(rewardPercent > 0) { "Wave reward percentage must be positive." }
        require(rewardPercent <= 10_000) { "Wave reward percentage cannot exceed 10000." }
    }
}

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
    val modifiers: List<WaveModifier> = emptyList(),
    /** Null means that the wave uses every named spawn on the selected map. */
    val spawnSelection: List<String>? = null,
    /** Additional population scaling materialized for generated waves; finite waves keep 100. */
    val healthScalePercent: Long = 100L,
    /** Additional reward scaling materialized for generated waves; finite waves keep 100. */
    val rewardScalePercent: Long = 100L,
) : ContentDefinition

data class EndlessWaveComposition(
    val spawns: List<WaveSpawn>,
) {
    init {
        require(spawns.isNotEmpty()) { "Endless wave composition cannot be empty." }
        require(spawns.all { it.enemyId.isNotBlank() && it.count > 0 }) {
            "Endless wave composition entries must have a non-blank enemy id and positive count."
        }
    }
}

/** Optional pack-owned schedule for waves generated after the finite schedule. */
data class EndlessWaveContent(
    val startTick: Long,
    val intervalTicks: Long,
    val compositionCycle: List<EndlessWaveComposition>,
    val countGrowthPercent: Int,
    val healthGrowthPercent: Int,
    val rewardGrowthPercent: Int,
    val spawnSelection: List<String>? = null,
) {
    init {
        require(startTick >= 0) { "Endless wave start tick cannot be negative." }
        require(intervalTicks > 0) { "Endless wave interval must be positive." }
        require(compositionCycle.isNotEmpty()) { "Endless wave composition cycle cannot be empty." }
        require(countGrowthPercent in 1..10_000) { "Endless count growth must be between 1 and 10000 percent." }
        require(healthGrowthPercent in 1..10_000) { "Endless health growth must be between 1 and 10000 percent." }
        require(rewardGrowthPercent in 1..10_000) { "Endless reward growth must be between 1 and 10000 percent." }
        require(spawnSelection == null || spawnSelection.isNotEmpty()) {
            "Endless spawn selection cannot be empty when declared."
        }
    }
}

data class DifficultyContent(
    override val id: String,
    val healthMult: BigDecimal,
    val countMult: BigDecimal,
    val rewardMult: BigDecimal,
    val goldRateMult: BigDecimal,
) : ContentDefinition

enum class IncidentEffectType(val id: String) {
    SPAWN_WAVE("spawn_wave"),
    RESOURCE_EVENT("resource_event"),
    MODIFIER("modifier"),
    ;

    companion object {
        fun fromId(id: String): IncidentEffectType? = entries.firstOrNull {
            it.id == id.trim().lowercase().replace('-', '_')
        }
    }
}

/** Content-only effect descriptors. Execution belongs to the consuming simulation, not content. */
sealed interface IncidentEffectDescriptor {
    val type: IncidentEffectType

    data class SpawnWave(val waveId: String) : IncidentEffectDescriptor {
        override val type: IncidentEffectType = IncidentEffectType.SPAWN_WAVE

        init {
            require(waveId.isNotBlank()) { "Incident spawn-wave id cannot be blank." }
        }
    }

    data class ResourceEvent(val resourceId: String, val amount: Int) : IncidentEffectDescriptor {
        override val type: IncidentEffectType = IncidentEffectType.RESOURCE_EVENT

        init {
            require(resourceId.isNotBlank()) { "Incident resource id cannot be blank." }
            require(amount > 0) { "Incident resource amount must be positive." }
        }
    }

    data class Modifier(val modifierId: String, val amount: Int, val durationTicks: Int) : IncidentEffectDescriptor {
        override val type: IncidentEffectType = IncidentEffectType.MODIFIER

        init {
            require(modifierId.isNotBlank()) { "Incident modifier id cannot be blank." }
            require(amount > 0) { "Incident modifier amount must be positive." }
            require(durationTicks > 0) { "Incident modifier duration must be positive." }
        }
    }
}

typealias SpawnWaveEffectDescriptor = IncidentEffectDescriptor.SpawnWave
typealias ResourceEventEffectDescriptor = IncidentEffectDescriptor.ResourceEvent
typealias ModifierEffectDescriptor = IncidentEffectDescriptor.Modifier

/** A grid coordinate that belongs to content data, independent of a simulation-world implementation. */
data class MapCoordinate(
    val x: Int,
    val y: Int,
)

data class MapResourceNode(
    val resourceId: String,
    val amount: Int,
    val infinite: Boolean = false,
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
    /** Tick at which the periodic selection window opens. */
    val cadenceStartTick: Long = 0,
    /** Positive interval enables selection; zero preserves legacy no-cadence content as inert. */
    val cadenceIntervalTicks: Int = 0,
    /** Optional inclusive end of the cadence window. */
    val cadenceEndTick: Long? = null,
    /** Optional explicit pacing window; legacy min/max threat remain the defaults. */
    val pacingMinThreat: Int = minThreat,
    val pacingMaxThreat: Int = maxThreat,
    val cooldownTicks: Int = 0,
    val effects: List<IncidentEffectDescriptor> = emptyList(),
) : ContentDefinition {
    init {
        require(minThreat >= 0) { "Incident minimum threat cannot be negative." }
        require(maxThreat >= 0) { "Incident maximum threat cannot be negative." }
        require(weight > 0) { "Incident weight must be positive." }
        require(cadenceStartTick >= 0) { "Incident cadence start must be non-negative." }
        require(cadenceIntervalTicks >= 0) { "Incident cadence interval must be non-negative." }
        require(cadenceEndTick == null || cadenceEndTick >= 0) { "Incident cadence end cannot be negative." }
        require(pacingMinThreat >= 0) { "Incident pacing minimum threat cannot be negative." }
        require(pacingMaxThreat >= 0) { "Incident pacing maximum threat cannot be negative." }
        require(cooldownTicks >= 0) { "Incident cooldown must be non-negative." }
    }

    val cadenceTicks: Int get() = cadenceIntervalTicks
    val pacingWindow: IntRange get() = pacingMinThreat..pacingMaxThreat
}

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
    val sounds: Map<GameplayEventType, SoundRef> = emptyMap(),
    val endlessWave: EndlessWaveContent? = null,
    val damageTypes: Map<String, DamageTypeContent> = emptyMap(),
    val workers: Map<String, WorkerContent> = emptyMap(),
    val needs: Map<String, NeedContent> = emptyMap(),
    val techNodes: Map<String, TechNodeContent> = emptyMap(),
) {
    /** Alias kept for callers that refer to the optional pack feature as simply `endless`. */
    val endless: EndlessWaveContent? get() = endlessWave
    /** Alias for consumers that refer to the optional graph as the tech tree. */
    val techTree: Map<String, TechNodeContent> get() = techNodes
    fun requireTile(id: String): TileContent = tiles[id] ?: error("Unknown tile '$id'.")
    fun requireResource(id: String): ResourceContent = resources[id] ?: error("Unknown resource '$id'.")
    fun requireTower(id: String): TowerContent = towers[id] ?: error("Unknown tower '$id'.")
    fun requireEnemy(id: String): EnemyContent = enemies[id] ?: error("Unknown enemy '$id'.")
    fun requireDamageType(id: String): DamageTypeContent = damageTypes[id] ?: error("Unknown damage type '$id'.")
    fun requireBuilding(id: String): BuildingContent = buildings[id] ?: error("Unknown building '$id'.")
    fun requireEffect(id: String): StatusEffectContent = effects[id] ?: error("Unknown status effect '$id'.")
    fun requireWorker(id: String): WorkerContent = workers[id] ?: error("Unknown worker '$id'.")
    fun requireNeed(id: String): NeedContent = needs[id] ?: error("Unknown need '$id'.")
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
            endlessWave = endlessWave?.copy(
                compositionCycle = endlessWave.compositionCycle.map { composition ->
                    composition.copy(
                        spawns = composition.spawns.map { spawn ->
                            spawn.copy(count = DifficultyScaling.scalePopulation(spawn.count, difficulty.countMult))
                        },
                    )
                },
            ),
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
