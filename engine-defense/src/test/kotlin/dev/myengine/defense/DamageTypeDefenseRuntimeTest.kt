package dev.myengine.defense

import dev.myengine.content.ContentPackLoader
import dev.myengine.content.ContentRegistry
import dev.myengine.core.HitEvent
import dev.myengine.entities.Entity
import dev.myengine.entities.EntityId
import dev.myengine.entities.EntityStore
import dev.myengine.entities.HealthComponent
import dev.myengine.entities.PositionComponent
import dev.myengine.world.TerrainRule
import dev.myengine.world.TilePosition
import dev.myengine.world.TileWorld
import dev.myengine.world.WorldSize
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DamageTypeDefenseRuntimeTest {
    @Test
    fun directHitAppliesTypedResistance() {
        val registry = typedRegistry(resistance = 50)
        val world = testWorld()
        val entities = EntityStore()
        val tower = assertIs<TowerPlacementResult.Placed>(
            DefenseRuntime().placeTower("basic", TilePosition(0, 0), registry, world, entities),
        )
        entities.upsert(enemy(2, TilePosition(1, 0), health = 10))

        val result = DefenseRuntime().updateTowers(registry, entities, tick = dev.myengine.core.Tick(1))

        assertEquals(7, entities.require(EntityId(2)).health!!.current)
        assertEquals(listOf(HitEvent(tower.entityId.value, 2, dev.myengine.core.Tick(1))), result.events.hits)
    }

    @Test
    fun splashHitAppliesFalloffAndResistanceToEachTarget() {
        val base = typedRegistry(resistance = 50)
        val towerContent = base.requireTower("basic").copy(splashRadius = 1, falloffPercent = 50)
        val registry = base.copy(towers = mapOf(towerContent.id to towerContent))
        val world = testWorld()
        val entities = EntityStore()
        val tower = assertIs<TowerPlacementResult.Placed>(
            DefenseRuntime().placeTower("basic", TilePosition(0, 0), registry, world, entities),
        )
        entities.upsert(enemy(2, TilePosition(1, 0), health = 10))
        entities.upsert(enemy(3, TilePosition(1, 1), health = 10))

        val result = DefenseRuntime().updateTowers(registry, entities, tick = dev.myengine.core.Tick(2))

        // Primary: floor(7 * 50 / 100) = 3; secondary ring: floor(7 * 50 * 50 / 10_000) = 1.
        assertEquals(7, entities.require(EntityId(2)).health!!.current)
        assertEquals(9, entities.require(EntityId(3)).health!!.current)
        assertEquals(
            listOf(
                HitEvent(tower.entityId.value, 2, dev.myengine.core.Tick(2)),
                HitEvent(tower.entityId.value, 3, dev.myengine.core.Tick(2)),
            ),
            result.events.hits,
        )
    }

    @Test
    fun fullResistanceDoesNotProduceHitEventOrHealthChange() {
        val registry = typedRegistry(resistance = 100)
        val world = testWorld()
        val entities = EntityStore()
        val tower = assertIs<TowerPlacementResult.Placed>(
            DefenseRuntime().placeTower("basic", TilePosition(0, 0), registry, world, entities),
        )
        entities.upsert(enemy(2, TilePosition(1, 0), health = 10))

        val result = DefenseRuntime().updateTowers(registry, entities, tick = dev.myengine.core.Tick(3))

        assertEquals(10, entities.require(EntityId(2)).health!!.current)
        assertTrue(result.events.hits.isEmpty())
        assertEquals(1, result.metrics.towerShots)
        assertEquals(tower.entityId.value, result.events.shots.single().sourceEntityId)
    }

    @Test
    fun legacyTowerWithoutDamageTypeKeepsDirectDamageBehavior() {
        val registry = legacyRegistry()
        val world = testWorld()
        val entities = EntityStore()
        val tower = assertIs<TowerPlacementResult.Placed>(
            DefenseRuntime().placeTower("basic", TilePosition(0, 0), registry, world, entities),
        )
        assertEquals(null, entities.require(tower.entityId).attack!!.damageTypeId)
        entities.upsert(enemy(2, TilePosition(1, 0), health = 10))

        val result = DefenseRuntime().updateTowers(registry, entities, tick = dev.myengine.core.Tick(4))

        assertEquals(3, entities.require(EntityId(2)).health!!.current)
        assertEquals(1, result.events.hits.size)
    }

    private fun enemy(id: Long, position: TilePosition, health: Int): Entity = Entity(
        id = EntityId(id),
        type = "enemy:scout",
        tags = setOf("enemy"),
        position = PositionComponent(position),
        health = HealthComponent(health, health),
    )

    private fun testWorld(): TileWorld = TileWorld.filled(
        WorldSize(4, 3),
        mapOf("floor" to TerrainRule("floor", buildable = true, blocksMovement = false)),
        "floor",
    )

    private fun typedRegistry(resistance: Int): ContentRegistry = loadRegistry(resistance, typed = true)

    private fun legacyRegistry(): ContentRegistry = loadRegistry(0, typed = false)

    private fun loadRegistry(resistance: Int, typed: Boolean): ContentRegistry = Files.createTempDirectory("myengine-damage-types-defense").let { root ->
        root.resolve("manifest.properties").writeText(
            """
            id=damage-types-defense
            version=0.1.0
            schemaVersion=1
            engineMin=0.0.1
            engineMax=0.1.x
            locales=en
            dependencies=
            """.trimIndent(),
        )
        root.resolve("tiles.properties").writeText("floor.buildable=true\nfloor.blocksMovement=false\n")
        if (typed) root.resolve("damage-types.properties").writeText("arcane.displayKey=damage.arcane\n")
        root.resolve("resources.properties").writeText("bolt.displayKey=resource.bolt\n")
        root.resolve("towers.properties").writeText(
            """
            basic.displayKey=tower.basic
            basic.range=3
            basic.damage=7
            basic.cooldownTicks=1
            basic.costResource=bolt
            basic.costAmount=1
            basic.sellRefundRatio=0.5
            basic.targetingMode=nearest
            ${if (typed) "basic.damageTypeId=arcane" else ""}
            """.trimIndent(),
        )
        root.resolve("enemies.properties").writeText(
            """
            scout.health=10
            scout.speedTilesPerTick=1
            scout.rewardResource=bolt
            scout.rewardAmount=1
            scout.coreDamage=1
            ${if (typed) "scout.resist.arcane=$resistance" else ""}
            """.trimIndent(),
        )
        root.resolve("recipes.properties").writeText("generator.outputResource=bolt\ngenerator.outputAmount=1\ngenerator.durationTicks=1\n")
        root.resolve("waves.properties").writeText("wave1.startTick=1\nwave1.spawns=scout:1\n")
        root.resolve("incidents.properties").writeText("spark.minThreat=0\nspark.maxThreat=3\nspark.weight=1\n")
        root.resolve("strings.properties").writeText(
            """
            resource.bolt=Bolt
            tower.basic=Basic tower
            ${if (typed) "damage.arcane=Arcane damage" else ""}
            hud.resources=Resources
            hud.wave=Wave
            hud.nextWave=Next wave
            hud.coreHealth=Core
            hud.build=Build
            hud.upgrade=Upgrade
            hud.damage=Damage
            hud.kills=Kills
            hud.tier=Tier
            """.trimIndent(),
        )
        ContentPackLoader.load(root).registry ?: error(ContentPackLoader.load(root).errors.joinToString("\n"))
    }
}
