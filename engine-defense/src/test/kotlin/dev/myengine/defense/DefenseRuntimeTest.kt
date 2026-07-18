package dev.myengine.defense

import dev.myengine.content.ContentPackLoader
import dev.myengine.content.WaveContent
import dev.myengine.content.WaveSpawn
import dev.myengine.core.Tick
import dev.myengine.entities.EntityStore
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

class DefenseRuntimeTest {
    @Test
    fun waveSpawnsAndEnemyLeaksToCore() {
        val registry = testRegistry()
        val terrain = mapOf(
            "floor" to TerrainRule("floor", buildable = true, blocksMovement = false),
            "core" to TerrainRule("core", buildable = false, blocksMovement = false, isCore = true),
        )
        val world = TileWorld.filled(WorldSize(3, 1), terrain, "floor")
        world.setTile(TilePosition(2, 0), WorldTile("core"))
        val entities = EntityStore()
        val runtime = DefenseRuntime()

        var state = DefenseState(coreHealth = 3)
        state = runtime.spawnDueWaves(dev.myengine.core.Tick(1), state, registry, world, entities, TilePosition(0, 0), TilePosition(2, 0))
        state = runtime.updateEnemies(registry, state, entities)
        state = runtime.updateEnemies(registry, state, entities)

        assertEquals(1, state.metrics.enemiesSpawned)
        assertEquals(1, state.metrics.enemiesLeaked)
        assertEquals(2, state.coreHealth)
    }

    @Test
    fun towerTargetsEnemyInRange() {
        val registry = testRegistry()
        val terrain = mapOf("floor" to TerrainRule("floor", buildable = true, blocksMovement = false))
        val world = TileWorld.filled(WorldSize(3, 3), terrain, "floor")
        val entities = EntityStore()
        val runtime = DefenseRuntime()

        assertIs<TowerPlacementResult.Placed>(runtime.placeTower("basic", TilePosition(1, 1), registry, world, entities))
        runtime.spawnDueWaves(dev.myengine.core.Tick(1), DefenseState(3), registry, world, entities, TilePosition(1, 0), TilePosition(1, 2))
        val result = runtime.updateTowers(registry, entities)

        assertEquals(1, result.metrics.towerShots)
        assertEquals(1, result.metrics.enemiesKilled)
    }

    @Test
    fun towersContendingForOneEnemyUseCurrentHealthAndAttributeCappedDamageDeterministically() {
        fun runOnce(): Pair<TowerUpdateResult, List<Long>> {
            val base = testRegistry()
            val registry = base.copy(
                enemies = base.enemies.mapValues { (_, enemy) -> enemy.copy(health = 3) },
            )
            val terrain = mapOf("floor" to TerrainRule("floor", buildable = true, blocksMovement = false))
            val world = TileWorld.filled(WorldSize(3, 5), terrain, "floor")
            val entities = EntityStore()
            val runtime = DefenseRuntime()
            val firstTower = assertIs<TowerPlacementResult.Placed>(
                runtime.placeTower("basic", TilePosition(0, 0), registry, world, entities),
            ).entityId.value
            val secondTower = assertIs<TowerPlacementResult.Placed>(
                runtime.placeTower("basic", TilePosition(2, 0), registry, world, entities),
            ).entityId.value
            runtime.spawnDueWaves(
                Tick(1),
                DefenseState(3),
                registry,
                world,
                entities,
                TilePosition(1, 0),
                TilePosition(1, 4),
            )

            val result = runtime.updateTowers(registry, entities)

            assertTrue(entities.byTag("enemy").isEmpty())
            return result to listOf(firstTower, secondTower)
        }

        val (first, towerIds) = runOnce()
        val (second, repeatedTowerIds) = runOnce()
        val enemy = testRegistry().enemies.getValue("scout").copy(health = 3)

        assertEquals(towerIds, repeatedTowerIds)
        assertEquals(first, second)
        assertEquals(2, first.metrics.towerShots)
        assertEquals(1, first.metrics.enemiesKilled)
        assertEquals(mapOf(enemy.rewardResource to enemy.rewardAmount), first.rewards)
        assertEquals(towerIds, first.towerMetrics.keys.toList())
        assertEquals(TowerDefenseMetrics(actualDamage = 2, kills = 0), first.towerMetrics.getValue(towerIds[0]))
        assertEquals(TowerDefenseMetrics(actualDamage = 1, kills = 1), first.towerMetrics.getValue(towerIds[1]))
        assertEquals(enemy.health.toLong(), first.towerMetrics.values.sumOf { it.actualDamage })
        assertEquals(1, first.towerMetrics.values.sumOf { it.kills })
    }

    @Test
    fun killedEnemyDepositsContentDerivedReward() {
        val registry = testRegistry()
        val terrain = mapOf("floor" to TerrainRule("floor", buildable = true, blocksMovement = false))
        val world = TileWorld.filled(WorldSize(3, 3), terrain, "floor")
        val entities = EntityStore()
        val runtime = DefenseRuntime()

        // Content-derived expectations: prove conservation, not a magic number.
        val scout = registry.enemies["scout"]!!
        val rewardResource = scout.rewardResource
        val rewardAmount = scout.rewardAmount

        assertIs<TowerPlacementResult.Placed>(runtime.placeTower("basic", TilePosition(1, 1), registry, world, entities))
        runtime.spawnDueWaves(dev.myengine.core.Tick(1), DefenseState(3), registry, world, entities, TilePosition(1, 0), TilePosition(1, 2))
        val result = runtime.updateTowers(registry, entities)

        // Exactly one kill in this scenario, so the summed reward equals a single rewardAmount.
        assertEquals(1, result.metrics.enemiesKilled)
        assertEquals(mapOf(rewardResource to rewardAmount), result.rewards)
    }

    @Test
    fun leakedEnemyYieldsNoReward() {
        val registry = testRegistry()
        val terrain = mapOf(
            "floor" to TerrainRule("floor", buildable = true, blocksMovement = false),
            "core" to TerrainRule("core", buildable = false, blocksMovement = false, isCore = true),
        )
        val world = TileWorld.filled(WorldSize(3, 1), terrain, "floor")
        world.setTile(TilePosition(2, 0), WorldTile("core"))
        val entities = EntityStore()
        val runtime = DefenseRuntime()

        // No towers placed: the enemy walks to the core and leaks via updateEnemies.
        var state = DefenseState(coreHealth = 3)
        state = runtime.spawnDueWaves(dev.myengine.core.Tick(1), state, registry, world, entities, TilePosition(0, 0), TilePosition(2, 0))
        state = runtime.updateEnemies(registry, state, entities)
        state = runtime.updateEnemies(registry, state, entities)

        // Sanity: the enemy did leak (mirrors waveSpawnsAndEnemyLeaksToCore setup).
        assertEquals(1, state.metrics.enemiesLeaked)

        // A leak produces no kill and therefore no reward.
        val result = runtime.updateTowers(registry, entities)
        assertEquals(0, result.metrics.enemiesKilled)
        assertTrue(result.rewards.isEmpty())
    }

    @Test
    fun dueAndOverdueWavesSpawnInStableIdOrder() {
        val base = testRegistry()
        val alpha = base.enemies.getValue("scout").copy(id = "alpha")
        val zeta = base.enemies.getValue("scout").copy(id = "zeta")
        val registry = base.copy(
            enemies = mapOf(alpha.id to alpha, zeta.id to zeta),
            waves = linkedMapOf(
                "z-overdue" to WaveContent("z-overdue", 1, listOf(WaveSpawn(zeta.id, 1))),
                "a-due" to WaveContent("a-due", 7, listOf(WaveSpawn(alpha.id, 1))),
            ),
        )
        val terrain = mapOf("floor" to TerrainRule("floor", buildable = true, blocksMovement = false))
        val world = TileWorld.filled(WorldSize(3, 1), terrain, "floor")
        val entities = EntityStore()

        val state = DefenseRuntime().spawnDueWaves(
            Tick(7),
            DefenseState(coreHealth = 3),
            registry,
            world,
            entities,
            TilePosition(0, 0),
            TilePosition(2, 0),
        )

        assertEquals(setOf("a-due", "z-overdue"), state.spawnedWaveIds)
        assertEquals(2, state.metrics.enemiesSpawned)
        assertEquals(listOf("enemy:alpha", "enemy:zeta"), entities.byTag("enemy").map { it.type })
    }

    @Test
    fun placementThatSealsNamedSpawnIsRejectedWithoutWorldOrEntityMutation() {
        val registry = testRegistry()
        val terrain = mapOf(
            "floor" to TerrainRule("floor", buildable = true, blocksMovement = false),
            "wall" to TerrainRule("wall", buildable = false, blocksMovement = true),
            "core" to TerrainRule("core", buildable = false, blocksMovement = false, isCore = true),
        )
        // The only route from entry (0,0) to core (2,0) crosses (1,0). (2,1) remains a
        // harmless buildable control tile so we can prove a rejected placement did not consume id 1.
        val world = TileWorld(
            WorldSize(3, 2),
            terrain,
            listOf(
                WorldTile("floor"), WorldTile("floor"), WorldTile("core"),
                WorldTile("wall"), WorldTile("wall"), WorldTile("floor"),
            ),
        )
        val entities = EntityStore()
        val runtime = DefenseRuntime()
        val blocked = TilePosition(1, 0)

        val rejected = runtime.placeTower(
            "basic",
            blocked,
            registry,
            world,
            entities,
            spawns = listOf(TilePosition(0, 0)),
            core = TilePosition(2, 0),
        )

        assertEquals(TowerPlacementResult.Rejected("blocks_spawn_path"), rejected)
        assertTrue(world.canBuild(blocked), "rejected placement must not occupy the candidate tile")
        assertEquals(0, entities.count(), "rejected placement must not create an entity")

        val placed = assertIs<TowerPlacementResult.Placed>(
            runtime.placeTower(
                "basic",
                TilePosition(2, 1),
                registry,
                world,
                entities,
                spawns = listOf(TilePosition(0, 0)),
                core = TilePosition(2, 0),
            ),
        )
        assertEquals(1L, placed.entityId.value, "rejection must not consume the next entity id")
    }

    @Test
    fun placementThatOnlyCutsSecondaryNamedSpawnIsRejectedWithoutMutation() {
        val registry = testRegistry()
        val terrain = mapOf(
            "floor" to TerrainRule("floor", buildable = true, blocksMovement = false),
            "wall" to TerrainRule("wall", buildable = false, blocksMovement = true),
            "core" to TerrainRule("core", buildable = false, blocksMovement = false, isCore = true),
        )
        // Primary reaches the core along the top row. Secondary can only climb through (2,1),
        // so a primary-only check would accept this placement while the all-spawn contract rejects it.
        val world = TileWorld(
            WorldSize(5, 3),
            terrain,
            listOf(
                WorldTile("floor"), WorldTile("floor"), WorldTile("floor"), WorldTile("floor"), WorldTile("core"),
                WorldTile("wall"), WorldTile("wall"), WorldTile("floor"), WorldTile("wall"), WorldTile("wall"),
                WorldTile("floor"), WorldTile("floor"), WorldTile("floor"), WorldTile("wall"), WorldTile("wall"),
            ),
        )
        val entities = EntityStore()
        val candidate = TilePosition(2, 1)

        val result = DefenseRuntime().placeTower(
            "basic",
            candidate,
            registry,
            world,
            entities,
            spawns = listOf(TilePosition(0, 0), TilePosition(0, 2)),
            core = TilePosition(4, 0),
        )

        assertEquals(TowerPlacementResult.Rejected("blocks_spawn_path"), result)
        assertTrue(world.canBuild(candidate), "secondary-spawn rejection must not occupy the candidate tile")
        assertEquals(0, entities.count(), "secondary-spawn rejection must not create an entity")
        assertEquals(1L, entities.nextIdSnapshot(), "secondary-spawn rejection must not consume an id")
    }

    private fun testRegistry() = Files.createTempDirectory("myengine-defense-test").let { root ->
        root.resolve("manifest.properties").writeText(
            """
            id=defense-test
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
            basic.damage=2
            basic.cooldownTicks=1
            basic.costResource=bolt
            basic.costAmount=1
            basic.sellRefundRatio=0.5
            basic.targetingMode=nearest
            """.trimIndent(),
        )
        root.resolve("enemies.properties").writeText(
            """
            scout.health=2
            scout.speedTilesPerTick=1
            scout.rewardResource=bolt
            scout.rewardAmount=1
            scout.coreDamage=1
            """.trimIndent(),
        )
        root.resolve("recipes.properties").writeText("gen.outputResource=bolt\ngen.outputAmount=1\ngen.durationTicks=1\n")
        root.resolve("waves.properties").writeText("w1.startTick=1\nw1.spawns=scout:1\n")
        root.resolve("incidents.properties").writeText("spark.minThreat=0\nspark.maxThreat=3\nspark.weight=1\n")
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
        ContentPackLoader.load(root).registry ?: error("test registry invalid")
    }
}
