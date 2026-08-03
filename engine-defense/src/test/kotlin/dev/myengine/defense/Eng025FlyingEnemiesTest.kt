package dev.myengine.defense

import dev.myengine.ai.GoalField
import dev.myengine.content.ContentPackLoader
import dev.myengine.content.ContentRegistry
import dev.myengine.content.EnemyContent
import dev.myengine.content.WaveContent
import dev.myengine.content.WaveSpawn
import dev.myengine.core.MovementMode
import dev.myengine.core.Tick
import dev.myengine.entities.Entity
import dev.myengine.entities.EntityId
import dev.myengine.entities.EntityStore
import dev.myengine.entities.EnemyComponent
import dev.myengine.entities.HealthComponent
import dev.myengine.entities.MovementComponent
import dev.myengine.entities.PositionComponent
import dev.myengine.world.TerrainRule
import dev.myengine.world.TilePosition
import dev.myengine.world.TileWorld
import dev.myengine.world.WorldSize
import dev.myengine.world.WorldTile
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class Eng025FlyingEnemiesTest {
    @Test
    fun airEnemyUsesBlockerIgnoringFieldWhileGroundEnemyStaysBlocked() {
        val base = registry()
        val air = base.requireEnemy("air").copy(movementMode = MovementMode.AIR)
        val ground = base.requireEnemy("ground")
        val registry = base.copy(
            enemies = mapOf(air.id to air, ground.id to ground),
            waves = mapOf("air-wave" to WaveContent("air-wave", 1, listOf(WaveSpawn(air.id, 1)))),
        )
        val terrain = mapOf(
            "floor" to TerrainRule("floor", buildable = true, blocksMovement = false),
            "wall" to TerrainRule("wall", buildable = false, blocksMovement = true),
            "core" to TerrainRule("core", buildable = false, blocksMovement = false, isCore = true),
        )
        fun world() = TileWorld(
            WorldSize(3, 1),
            terrain,
            listOf(WorldTile("floor"), WorldTile("wall"), WorldTile("core")),
        )
        val groundField = GoalField.build(world(), TilePosition(2, 0))
        val airField = GoalField.buildIgnoringBlockers(world(), TilePosition(2, 0))

        val airEntities = EntityStore()
        DefenseRuntime().spawnWave(
            wave = registry.waves.getValue("air-wave"),
            state = DefenseState(10),
            registry = registry,
            world = world(),
            entities = airEntities,
            spawn = TilePosition(0, 0),
            core = TilePosition(2, 0),
            goalField = groundField,
            airGoalField = airField,
        )
        DefenseRuntime().updateEnemies(registry, DefenseState(10), airEntities, groundField, world(), airGoalField = airField)
        assertEquals(TilePosition(1, 0), airEntities.all().single().position!!.tile)
        assertEquals(MovementMode.AIR, airEntities.all().single().enemy!!.movementMode)

        val groundEntities = EntityStore()
        val groundEntity = groundEntities.create("enemy:ground", setOf("enemy")) { id ->
            Entity(
                id = id,
                type = "enemy:ground",
                tags = setOf("enemy"),
                position = PositionComponent(TilePosition(0, 0)),
                health = HealthComponent(2, 2),
                movement = MovementComponent(),
                enemy = EnemyComponent("ground", 1, 1, "bolt", 1),
            )
        }
        DefenseRuntime().updateEnemies(registry, DefenseState(10), groundEntities, groundField, world(), airGoalField = airField)
        assertEquals(TilePosition(0, 0), groundEntities.require(groundEntity.id).position!!.tile)
    }

    @Test
    fun towerCapabilityFlagsFilterAirAndGroundTargetsDeterministically() {
        val base = registry()
        val registry = base.copy(
            towers = mapOf("ground-only" to base.requireTower("basic").copy(canTargetAir = false)),
            enemies = mapOf(
                "air" to base.requireEnemy("air").copy(movementMode = MovementMode.AIR),
                "ground" to base.requireEnemy("ground"),
            ),
        )
        val world = TileWorld.filled(
            WorldSize(3, 1),
            mapOf("floor" to TerrainRule("floor", buildable = true, blocksMovement = false)),
            "floor",
        )
        val entities = EntityStore()
        val tower = assertIs<TowerPlacementResult.Placed>(
            DefenseRuntime().placeTower("ground-only", TilePosition(1, 0), registry, world, entities),
        )
        entities.upsert(enemyEntity(2, "air", MovementMode.AIR))
        entities.upsert(enemyEntity(3, "ground", MovementMode.GROUND).copy(position = PositionComponent(TilePosition(2, 0))))

        val groundField = GoalField.build(world, TilePosition(2, 0))
        val airField = GoalField.buildIgnoringBlockers(world, TilePosition(2, 0))
        DefenseRuntime().updateTowers(registry, entities, groundField, Tick(1), airField)

        assertEquals(2, entities.require(EntityId(2)).health!!.current)
        assertEquals(1, entities.require(EntityId(3)).health!!.current)
        assertEquals(tower.entityId.value, 1L)
    }

    private fun enemyEntity(id: Long, enemyId: String, mode: MovementMode) = Entity(
        id = EntityId(id),
        type = "enemy:$enemyId",
        tags = setOf("enemy"),
        position = PositionComponent(TilePosition(0, 0)),
        health = HealthComponent(2, 2),
        movement = MovementComponent(),
        enemy = EnemyComponent(enemyId, 1, 1, "bolt", 1, movementMode = mode),
    )

    private fun registry(): ContentRegistry = Files.createTempDirectory("myengine-eng025-defense").also { root ->
        root.resolve("manifest.properties").writeText(
            """
            id=eng025-defense
            version=0.1
            schemaVersion=1
            engineMin=0.0.1
            engineMax=0.1.x
            locales=en
            dependencies=
            """.trimIndent(),
        )
        root.resolve("tiles.properties").writeText("floor.buildable=true\nfloor.blocksMovement=false\n")
        root.resolve("resources.properties").writeText("bolt.displayKey=resource.bolt\n")
        root.resolve("towers.properties").writeText(
            """
            basic.displayKey=tower.basic
            basic.range=2
            basic.damage=1
            basic.cooldownTicks=1
            basic.costResource=bolt
            basic.costAmount=1
            basic.sellRefundRatio=0.5
            """.trimIndent(),
        )
        root.resolve("enemies.properties").writeText(
            """
            air.health=2
            air.speedTilesPerTick=1
            air.rewardResource=bolt
            air.rewardAmount=1
            air.coreDamage=1
            air.movementMode=air
            ground.health=2
            ground.speedTilesPerTick=1
            ground.rewardResource=bolt
            ground.rewardAmount=1
            ground.coreDamage=1
            """.trimIndent(),
        )
        root.resolve("recipes.properties").writeText("generator.outputResource=bolt\ngenerator.outputAmount=1\ngenerator.durationTicks=1\n")
        root.resolve("waves.properties").writeText("w1.startTick=1\nw1.spawns=ground:1\n")
        root.resolve("strings.properties").writeText(
            """
            resource.bolt=Bolt
            tower.basic=Basic
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
    }.let { root -> ContentPackLoader.load(root).registry!! }
}
