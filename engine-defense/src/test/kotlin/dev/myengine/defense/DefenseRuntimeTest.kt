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
            basic.range=2
            basic.damage=2
            basic.cooldownTicks=1
            basic.costResource=bolt
            basic.costAmount=1
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
        root.resolve("strings.properties").writeText("resource.bolt=Bolt\n")
        ContentPackLoader.load(root).registry ?: error("test registry invalid")
    }
}
