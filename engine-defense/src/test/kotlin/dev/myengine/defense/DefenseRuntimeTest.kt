package dev.myengine.defense

import dev.myengine.ai.GoalField
import dev.myengine.content.ContentPackLoader
import dev.myengine.content.StatusEffectContent
import dev.myengine.content.StatusEffectKind
import dev.myengine.content.StatusEffectStackingRule
import dev.myengine.content.WaveContent
import dev.myengine.content.WaveModifier
import dev.myengine.content.WaveSpawn
import dev.myengine.core.Tick
import dev.myengine.core.HitEvent
import dev.myengine.core.ShotEvent
import dev.myengine.entities.Entity
import dev.myengine.entities.EntityId
import dev.myengine.entities.EntityStore
import dev.myengine.entities.HealthComponent
import dev.myengine.entities.PositionComponent
import dev.myengine.entities.MovementComponent
import dev.myengine.entities.StatusEffectComponent
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

            val result = runtime.updateTowers(
                registry = registry,
                entities = entities,
                goalField = GoalField.build(world, TilePosition(1, 4)),
            )

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
    fun indexedTargetingSkipsDeadAndRemovedEnemies() {
        val registry = testRegistry()
        val terrain = mapOf("floor" to TerrainRule("floor", buildable = true, blocksMovement = false))
        val world = TileWorld.filled(WorldSize(5, 5), terrain, "floor")
        val entities = EntityStore()
        val runtime = DefenseRuntime()
        val tower = assertIs<TowerPlacementResult.Placed>(
            runtime.placeTower("basic", TilePosition(1, 1), registry, world, entities),
        ).entityId.value
        val dead = Entity(
            id = EntityId(20),
            type = "enemy:scout",
            tags = setOf("enemy"),
            position = PositionComponent(TilePosition(2, 1)),
            health = HealthComponent(0, 2),
        )
        val alive = Entity(
            id = EntityId(30),
            type = "enemy:scout",
            tags = setOf("enemy"),
            position = PositionComponent(TilePosition(1, 2)),
            health = HealthComponent(5, 5),
        )
        val removed = Entity(
            id = EntityId(40),
            type = "enemy:scout",
            tags = setOf("enemy"),
            position = PositionComponent(TilePosition(0, 1)),
            health = HealthComponent(5, 5),
        )
        entities.upsert(dead)
        entities.upsert(alive)
        entities.upsert(removed)
        entities.remove(removed.id)

        val result = runtime.updateTowers(
            registry = registry,
            entities = entities,
            goalField = GoalField.build(world, TilePosition(4, 4)),
            tick = Tick(3),
        )

        assertEquals(listOf(ShotEvent(tower, alive.id.value, Tick(3))), result.events.shots)
        assertEquals(listOf(HitEvent(tower, alive.id.value, Tick(3))), result.events.hits)
        assertEquals(3, entities.require(alive.id).health?.current)
        assertEquals(0, result.metrics.enemiesKilled)
        assertEquals(null, entities.get(removed.id))
    }

    @Test
    fun splashUsesStableIdOrderIntegerFalloffAndAttributesExactRewardsToTheFiringTower() {
        fun runOnce(): TowerUpdateResult {
            val base = testRegistry()
            val registry = base.copy(
                towers = base.towers.mapValues { (_, tower) ->
                    tower.copy(damage = 5, splashRadius = 2, falloffPercent = 50)
                },
            )
            val terrain = mapOf("floor" to TerrainRule("floor", buildable = true, blocksMovement = false))
            val world = TileWorld.filled(WorldSize(4, 2), terrain, "floor")
            val entities = EntityStore()
            val runtime = DefenseRuntime()
            val tower = assertIs<TowerPlacementResult.Placed>(
                runtime.placeTower("basic", TilePosition(0, 0), registry, world, entities),
            ).entityId

            // Deliberately insert in non-id order: AoE application and event order must still be id-stable.
            listOf(
                Entity(EntityId(11), "enemy:scout", setOf("enemy"), PositionComponent(TilePosition(1, 0)), HealthComponent(5, 5)),
                Entity(EntityId(4), "enemy:scout", setOf("enemy"), PositionComponent(TilePosition(3, 0)), HealthComponent(9, 9)),
                Entity(EntityId(7), "enemy:scout", setOf("enemy"), PositionComponent(TilePosition(1, 1)), HealthComponent(2, 2)),
                Entity(EntityId(5), "enemy:scout", setOf("enemy"), PositionComponent(TilePosition(2, 0)), HealthComponent(2, 2)),
            ).forEach(entities::upsert)

            val result = runtime.updateTowers(registry, entities, tick = Tick(8))

            assertEquals(listOf(4L), entities.byTag("enemy").map { it.id.value })
            assertEquals(9, entities.require(EntityId(4)).health!!.current, "distance-two damage must truncate to zero")
            assertEquals(
                listOf(
                    ShotEvent(tower.value, 11, Tick(8)),
                ),
                result.events.shots,
            )
            assertEquals(
                listOf(
                    HitEvent(tower.value, 5, Tick(8)),
                    HitEvent(tower.value, 7, Tick(8)),
                    HitEvent(tower.value, 11, Tick(8)),
                ),
                result.events.hits,
            )
            return result
        }

        val first = runOnce()
        val second = runOnce()
        val reward = testRegistry().requireEnemy("scout")

        assertEquals(first, second)
        assertEquals(1, first.metrics.towerShots)
        assertEquals(3, first.metrics.enemiesKilled)
        assertEquals(mapOf(reward.rewardResource to reward.rewardAmount * 3), first.rewards)
        assertEquals(TowerDefenseMetrics(actualDamage = 9, kills = 3), first.towerMetrics.getValue(1L))
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
    fun statusEffectsApplyInStableOrderAndDotUsesTheExistingRewardPath() {
        val base = testRegistry()
        val registry = base.copy(
            effects = mapOf(
                "burn" to StatusEffectContent(
                    "burn", StatusEffectKind.DOT, magnitude = 1, durationTicks = 2,
                    stackingRule = StatusEffectStackingRule.STACK,
                ),
                "slow" to StatusEffectContent(
                    "slow", StatusEffectKind.SLOW, magnitude = 100, durationTicks = 2,
                    stackingRule = StatusEffectStackingRule.REFRESH,
                ),
                "ignore" to StatusEffectContent(
                    "ignore", StatusEffectKind.SLOW, magnitude = 10, durationTicks = 4,
                    stackingRule = StatusEffectStackingRule.IGNORE,
                ),
            ),
            towers = base.towers + ("basic" to base.requireTower("basic").copy(effectId = "burn")),
        )
        val world = TileWorld.filled(
            WorldSize(3, 3),
            mapOf("floor" to TerrainRule("floor", buildable = true, blocksMovement = false)),
            "floor",
        )
        val entities = EntityStore()
        val runtime = DefenseRuntime()
        val tower = assertIs<TowerPlacementResult.Placed>(
            runtime.placeTower("basic", TilePosition(1, 1), registry, world, entities),
        )
        val enemy = Entity(
            id = EntityId(20),
            type = "enemy:scout",
            tags = setOf("enemy"),
            position = PositionComponent(TilePosition(2, 1)),
            health = HealthComponent(3, 3),
            movement = MovementComponent(listOf(TilePosition(2, 1), TilePosition(2, 2)), 0),
        )
        entities.upsert(enemy)

        val towerResult = runtime.updateTowers(registry, entities, tick = Tick(1))
        assertEquals(1, towerResult.metrics.towerShots)
        assertEquals(listOf(StatusEffectComponent("burn", 2)), entities.require(enemy.id).statusEffects)

        runtime.applyStatusEffect(registry, entities, enemy.id, "burn")
        runtime.applyStatusEffect(registry, entities, enemy.id, "slow")
        runtime.applyStatusEffect(registry, entities, enemy.id, "ignore")
        runtime.applyStatusEffect(registry, entities, enemy.id, "ignore")
        val active = entities.require(enemy.id).statusEffects
        assertEquals(listOf("burn", "ignore", "slow"), active.map { it.effectId })
        assertEquals(2, active.first { it.effectId == "burn" }.stacks)
        assertEquals(4, active.first { it.effectId == "ignore" }.remainingTicks)

        val effectResult = runtime.updateStatusEffects(registry, entities)
        assertEquals(1, effectResult.metrics.enemiesKilled)
        assertEquals(mapOf("bolt" to 1), effectResult.rewards)
        assertTrue(entities.byTag("enemy").isEmpty())
        assertEquals(tower.entityId.value, towerResult.towerMetrics.keys.single())
    }

    @Test
    fun fullSlowPreventsMovementWithoutChangingTheAuthoritativePath() {
        val base = testRegistry()
        val registry = base.copy(
            effects = mapOf(
                "slow" to StatusEffectContent(
                    "slow", StatusEffectKind.SLOW, magnitude = 100, durationTicks = 2,
                    stackingRule = StatusEffectStackingRule.REFRESH,
                ),
            ),
        )
        val world = TileWorld.filled(
            WorldSize(3, 1),
            mapOf("floor" to TerrainRule("floor", buildable = true, blocksMovement = false)),
            "floor",
        )
        val entities = EntityStore()
        val enemy = Entity(
            id = EntityId(7),
            type = "enemy:scout",
            tags = setOf("enemy"),
            position = PositionComponent(TilePosition(0, 0)),
            health = HealthComponent(5, 5),
            movement = MovementComponent(listOf(TilePosition(0, 0), TilePosition(1, 0), TilePosition(2, 0)), 0),
            statusEffects = listOf(StatusEffectComponent("slow", 2)),
        )
        entities.upsert(enemy)

        val state = DefenseRuntime().updateEnemies(
            registry,
            DefenseState(coreHealth = 5),
            entities,
            goalField = null,
        )

        assertEquals(TilePosition(0, 0), entities.require(enemy.id).position?.tile)
        assertEquals(0, state.metrics.enemiesLeaked)
    }

    @Test
    fun partialSlowUsesTheDocumentedIntegerFloorForOneTileBaseSpeed() {
        val base = testRegistry()
        val registry = base.copy(
            effects = mapOf(
                "slow" to StatusEffectContent(
                    "slow", StatusEffectKind.SLOW, magnitude = 50, durationTicks = 2,
                    stackingRule = StatusEffectStackingRule.REFRESH,
                ),
            ),
        )
        val world = TileWorld.filled(
            WorldSize(3, 1),
            mapOf("floor" to TerrainRule("floor", buildable = true, blocksMovement = false)),
            "floor",
        )
        val entities = EntityStore()
        val enemy = Entity(
            id = EntityId(8),
            type = "enemy:scout",
            tags = setOf("enemy"),
            position = PositionComponent(TilePosition(0, 0)),
            health = HealthComponent(5, 5),
            movement = MovementComponent(listOf(TilePosition(0, 0), TilePosition(1, 0)), 0),
            statusEffects = listOf(StatusEffectComponent("slow", 2)),
        )
        entities.upsert(enemy)

        DefenseRuntime().updateEnemies(registry, DefenseState(coreHealth = 5), entities)

        assertEquals(TilePosition(0, 0), entities.require(enemy.id).position?.tile)
    }

    @Test
    fun dotOnNonEnemyDoesNotCreateEnemyKillMetricsOrRemoveTheEntity() {
        val base = testRegistry()
        val registry = base.copy(
            effects = mapOf(
                "burn" to StatusEffectContent(
                    "burn", StatusEffectKind.DOT, magnitude = 5, durationTicks = 1,
                    stackingRule = StatusEffectStackingRule.REFRESH,
                ),
            ),
        )
        val entities = EntityStore()
        val tower = Entity(
            id = EntityId(9),
            type = "tower:basic",
            tags = setOf("tower"),
            health = HealthComponent(2, 2),
            statusEffects = listOf(StatusEffectComponent("burn", 1)),
        )
        entities.upsert(tower)

        val result = DefenseRuntime().updateStatusEffects(registry, entities)

        assertEquals(DefenseMetrics(), result.metrics)
        assertEquals(tower.copy(statusEffects = emptyList()), entities.require(tower.id))
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
    fun multiSpawnWaveSortsRoutesAndPreservesWaveSpawnAndInstanceOrderDeterministically() {
        val base = testRegistry()
        val firstEnemy = base.requireEnemy("scout").copy(id = "alpha")
        val secondEnemy = base.requireEnemy("scout").copy(id = "omega")
        val registry = base.copy(
            enemies = mapOf(firstEnemy.id to firstEnemy, secondEnemy.id to secondEnemy),
            waves = mapOf(
                "multi" to WaveContent(
                    id = "multi",
                    startTick = 1,
                    // The authored WaveSpawn order is omega x2, then alpha x1.
                    spawns = listOf(WaveSpawn("omega", 2), WaveSpawn("alpha", 1)),
                    // The authored route order is intentionally unsorted.
                    spawnSelection = listOf("zulu", "alpha"),
                ),
            ),
        )

        fun runOnce(): Pair<List<Triple<Long, String, TilePosition>>, DefenseState> {
            val world = TileWorld.filled(
                WorldSize(4, 1),
                mapOf("floor" to TerrainRule("floor", buildable = true, blocksMovement = false)),
                "floor",
            )
            val entities = EntityStore()
            val state = DefenseRuntime().spawnDueWaves(
                tick = Tick(1),
                state = DefenseState(coreHealth = 10),
                registry = registry,
                world = world,
                entities = entities,
                spawn = TilePosition(0, 0),
                core = TilePosition(3, 0),
                spawnRoutes = linkedMapOf(
                    "zulu" to TilePosition(0, 0),
                    "alpha" to TilePosition(2, 0),
                ),
            )
            return entities.all().map { entity ->
                Triple(entity.id.value, entity.type, requireNotNull(entity.position).tile)
            } to state
        }

        val first = runOnce()
        val second = runOnce()

        assertEquals(first, second, "The same multi-spawn wave must replay identically.")
        assertEquals(
            listOf(
                Triple(1L, "enemy:omega", TilePosition(2, 0)),
                Triple(2L, "enemy:omega", TilePosition(2, 0)),
                Triple(3L, "enemy:alpha", TilePosition(2, 0)),
                Triple(4L, "enemy:omega", TilePosition(0, 0)),
                Triple(5L, "enemy:omega", TilePosition(0, 0)),
                Triple(6L, "enemy:alpha", TilePosition(0, 0)),
            ),
            first.first,
        )
        assertEquals(6, first.second.metrics.enemiesSpawned)
        assertEquals(setOf("multi"), first.second.spawnedWaveIds)
    }

    @Test
    fun bossAndWaveScalingApplyInStableSpawnOrderAndPersistEffectiveRewardStats() {
        val base = testRegistry()
        val boss = base.enemies.getValue("scout").copy(
            health = 10,
            speedTilesPerTick = 2,
            rewardAmount = 5,
            isBoss = true,
            healthScalePercent = 150,
            speedScalePercent = 200,
            rewardScalePercent = 150,
        )
        val registry = base.copy(
            enemies = mapOf(boss.id to boss),
            waves = mapOf(
                "boss-wave" to WaveContent(
                    id = "boss-wave",
                    startTick = 1,
                    spawns = listOf(WaveSpawn(boss.id, 3)),
                    modifiers = listOf(
                        WaveModifier(healthPercent = 200, speedPercent = 50, count = 1),
                        WaveModifier(healthPercent = 100, speedPercent = 150, count = 1),
                    ),
                ),
            ),
        )
        val terrain = mapOf("floor" to TerrainRule("floor", buildable = true, blocksMovement = false))
        val world = TileWorld.filled(WorldSize(4, 1), terrain, "floor")
        val entities = EntityStore()

        DefenseRuntime().spawnDueWaves(
            Tick(1), DefenseState(coreHealth = 10), registry, world, entities,
            TilePosition(0, 0), TilePosition(3, 0),
        )

        val spawned = entities.byTag("enemy")
        assertEquals(listOf(30, 15, 15), spawned.map { it.health!!.max })
        assertEquals(listOf(2, 6, 4), spawned.map { it.enemy!!.speedTilesPerTick })
        assertEquals(listOf(8, 8, 8), spawned.map { it.enemy!!.rewardAmount })
        assertTrue(spawned.all { it.enemy!!.isBoss })
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
