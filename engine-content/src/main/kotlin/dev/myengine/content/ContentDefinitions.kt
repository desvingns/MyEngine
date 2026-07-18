package dev.myengine.content

import java.math.BigDecimal

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

data class TileContent(
    override val id: String,
    val buildable: Boolean,
    val blocksMovement: Boolean,
    val isCore: Boolean,
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
    val upgradeTiers: Map<String, TowerUpgradeTier> = emptyMap(),
) : ContentDefinition

data class TowerUpgradeTier(
    val branch: String,
    val tier: Int,
    val range: Int,
    val damage: Int,
    val cooldownTicks: Int,
    val costResource: String,
    val costAmount: Int,
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

data class WaveContent(
    override val id: String,
    val startTick: Long,
    val spawns: List<WaveSpawn>,
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
    val difficulties: Map<String, DifficultyContent> = emptyMap(),
    val maps: Map<String, MapContent> = emptyMap(),
    val resolvedDifficultyId: String? = null,
) {
    fun requireTile(id: String): TileContent = tiles[id] ?: error("Unknown tile '$id'.")
    fun requireResource(id: String): ResourceContent = resources[id] ?: error("Unknown resource '$id'.")
    fun requireTower(id: String): TowerContent = towers[id] ?: error("Unknown tower '$id'.")
    fun requireEnemy(id: String): EnemyContent = enemies[id] ?: error("Unknown enemy '$id'.")
    fun requireMap(id: String? = null): MapContent = when {
        id != null -> maps[id] ?: error("Unknown map '$id'.")
        maps.size == 1 -> maps.values.single()
        maps.isEmpty() -> error("This content pack has no maps.json map definitions.")
        else -> error("Map id is required because this content pack defines ${maps.size} maps.")
    }

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
