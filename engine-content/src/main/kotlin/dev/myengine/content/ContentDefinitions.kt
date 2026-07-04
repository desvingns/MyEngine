package dev.myengine.content

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
) : ContentDefinition

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
) {
    fun requireTile(id: String): TileContent = tiles[id] ?: error("Unknown tile '$id'.")
    fun requireResource(id: String): ResourceContent = resources[id] ?: error("Unknown resource '$id'.")
    fun requireTower(id: String): TowerContent = towers[id] ?: error("Unknown tower '$id'.")
    fun requireEnemy(id: String): EnemyContent = enemies[id] ?: error("Unknown enemy '$id'.")
}
