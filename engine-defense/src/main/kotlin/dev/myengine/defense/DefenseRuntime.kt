package dev.myengine.defense

import dev.myengine.ai.GoalField
import dev.myengine.ai.GridPathfinder
import dev.myengine.ai.PathRequest
import dev.myengine.ai.PathResult
import dev.myengine.content.ContentRegistry
import dev.myengine.content.EnemyContent
import dev.myengine.content.TowerContent
import dev.myengine.content.WaveContent
import dev.myengine.core.Tick
import dev.myengine.entities.AttackComponent
import dev.myengine.entities.Entity
import dev.myengine.entities.EntityId
import dev.myengine.entities.EntityStore
import dev.myengine.entities.HealthComponent
import dev.myengine.entities.MovementComponent
import dev.myengine.entities.PositionComponent
import dev.myengine.entities.TowerComponent
import dev.myengine.world.TilePosition
import dev.myengine.world.TileWorld

data class DefenseMetrics(
    val enemiesSpawned: Int = 0,
    val enemiesKilled: Int = 0,
    val enemiesLeaked: Int = 0,
    val coreDamage: Int = 0,
    val towerShots: Int = 0,
) {
    fun plus(other: DefenseMetrics): DefenseMetrics = DefenseMetrics(
        enemiesSpawned = enemiesSpawned + other.enemiesSpawned,
        enemiesKilled = enemiesKilled + other.enemiesKilled,
        enemiesLeaked = enemiesLeaked + other.enemiesLeaked,
        coreDamage = coreDamage + other.coreDamage,
        towerShots = towerShots + other.towerShots,
    )
}

data class TowerDefenseMetrics(
    val actualDamage: Long = 0,
    val kills: Int = 0,
) {
    fun plus(other: TowerDefenseMetrics): TowerDefenseMetrics = TowerDefenseMetrics(
        actualDamage = actualDamage + other.actualDamage,
        kills = kills + other.kills,
    )
}

data class DefenseState(
    val coreHealth: Int,
    val spawnedWaveIds: Set<String> = emptySet(),
    val metrics: DefenseMetrics = DefenseMetrics(),
    val towerMetrics: Map<Long, TowerDefenseMetrics> = emptyMap(),
) {
    fun record(metrics: DefenseMetrics): DefenseState = copy(metrics = this.metrics.plus(metrics))

    fun recordTowerMetrics(metrics: Map<Long, TowerDefenseMetrics>): DefenseState {
        if (metrics.isEmpty()) return this
        val next = towerMetrics.toMutableMap()
        metrics.toSortedMap().forEach { (towerId, delta) ->
            next[towerId] = (next[towerId] ?: TowerDefenseMetrics()).plus(delta)
        }
        return copy(towerMetrics = next.toSortedMap())
    }
}

sealed class TowerPlacementResult {
    data class Placed(val entityId: EntityId) : TowerPlacementResult()
    data class Rejected(val reason: String) : TowerPlacementResult()
}

data class TowerUpdateResult(
    val metrics: DefenseMetrics,
    val rewards: Map<String, Int> = emptyMap(),
    val towerMetrics: Map<Long, TowerDefenseMetrics> = emptyMap(),
)

class DefenseRuntime(private val pathfinder: GridPathfinder = GridPathfinder()) {
    fun placeTower(
        towerId: String,
        position: TilePosition,
        registry: ContentRegistry,
        world: TileWorld,
        entities: EntityStore,
        spawns: List<TilePosition> = emptyList(),
        core: TilePosition? = null,
    ): TowerPlacementResult {
        val tower = registry.towers[towerId] ?: return TowerPlacementResult.Rejected("unknown_tower")
        if (!world.canBuild(position)) return TowerPlacementResult.Rejected("tile_not_buildable")
        if (entities.byTag("enemy").any { enemy ->
                enemy.position?.tile == position && enemy.health?.isAlive() == true
            }
        ) {
            return TowerPlacementResult.Rejected("occupied_by_enemy")
        }
        if (core != null) {
            val prospective = GoalField.rebuildAfterWalkabilityChange(
                world = world,
                goal = core,
                spawns = spawns,
                additionalBlocked = position,
            )
            if (!prospective.keepsAllSpawnsReachable) {
                return TowerPlacementResult.Rejected("blocks_spawn_path")
            }
        }
        val entity = entities.create("tower:$towerId", setOf("tower")) { id ->
            Entity(
                id = id,
                type = "tower:$towerId",
                tags = setOf("tower"),
                position = PositionComponent(position),
                tower = TowerComponent(towerId, targetingMode = tower.targetingMode),
                attack = AttackComponent(tower.range, tower.damage, tower.cooldownTicks),
            )
        }
        world.occupy(position, entity.id.value)
        return TowerPlacementResult.Placed(entity.id)
    }

    fun spawnDueWaves(
        tick: Tick,
        state: DefenseState,
        registry: ContentRegistry,
        world: TileWorld,
        entities: EntityStore,
        spawn: TilePosition,
        core: TilePosition,
        goalField: GoalField? = null,
    ): DefenseState {
        var nextState = state
        registry.waves.values
            .filter { it.startTick <= tick.value && it.id !in state.spawnedWaveIds }
            .sortedBy { it.id }
            .forEach { wave ->
                nextState = spawnWave(wave, nextState, registry, world, entities, spawn, core, goalField)
            }
        return nextState
    }

    /**
     * Spawns one wave exactly once and records its id after all content spawn entries have been
     * applied. Callers use this same helper for scheduled and command-triggered waves, so both
     * paths share entity-id, metric, and spawned-id ordering.
     */
    fun spawnWave(
        wave: WaveContent,
        state: DefenseState,
        registry: ContentRegistry,
        world: TileWorld,
        entities: EntityStore,
        spawn: TilePosition,
        core: TilePosition,
        goalField: GoalField? = null,
    ): DefenseState {
        if (wave.id in state.spawnedWaveIds) return state
        var nextState = state
        wave.spawns.forEach { spawnDef ->
            val enemy = registry.requireEnemy(spawnDef.enemyId)
            repeat(spawnDef.count) {
                spawnEnemy(enemy, world, entities, spawn, core, goalField)
            }
            nextState = nextState.record(DefenseMetrics(enemiesSpawned = spawnDef.count))
        }
        return nextState.copy(spawnedWaveIds = nextState.spawnedWaveIds + wave.id)
    }

    fun updateTowers(
        registry: ContentRegistry,
        entities: EntityStore,
        goalField: GoalField? = null,
    ): TowerUpdateResult {
        var metrics = DefenseMetrics()
        val rewards = mutableMapOf<String, Int>()
        val towerMetrics = mutableMapOf<Long, TowerDefenseMetrics>()
        entities.byTag("tower").sortedBy { it.id.value }.forEach { towerEntity ->
            val towerComponent = towerEntity.tower ?: return@forEach
            val attack = towerEntity.attack ?: return@forEach
            if (towerComponent.cooldownRemaining > 0) {
                entities.update(towerEntity.id) { it.copy(tower = towerComponent.copy(cooldownRemaining = towerComponent.cooldownRemaining - 1)) }
                return@forEach
            }
            val towerPosition = towerEntity.position?.tile ?: return@forEach
            // Query the store for every firing tower: earlier towers may already have changed
            // health or killed a target during this same deterministic update pass.
            val target = if (goalField != null) {
                TargetSelector.select(towerComponent.targetingMode, towerPosition, attack.range, entities.byTag("enemy"), goalField)
            } else {
                // Compatibility for direct defense-module callers predating goal-field routing.
                entities.byTag("enemy")
                    .filter { enemy ->
                        val position = enemy.position?.tile ?: return@filter false
                        val health = enemy.health ?: return@filter false
                        health.isAlive() && towerPosition.manhattanDistance(position) <= attack.range
                    }
                    .sortedWith(compareBy<Entity> { towerPosition.manhattanDistance(it.position!!.tile) }.thenBy { it.id.value })
                    .firstOrNull()
            } ?: return@forEach

            val health = target.health ?: return@forEach
            val damaged = health.damage(attack.damage)
            metrics = metrics.plus(DefenseMetrics(towerShots = 1))
            val towerDelta = TowerDefenseMetrics(
                actualDamage = minOf(attack.damage, health.current).toLong(),
                kills = if (damaged.isAlive()) 0 else 1,
            )
            towerMetrics[towerEntity.id.value] = (towerMetrics[towerEntity.id.value] ?: TowerDefenseMetrics()).plus(towerDelta)
            // Persist even terminal damage until the final flush so subsequent towers observe the
            // dead health and cannot double-count damage, kills, or content rewards.
            entities.update(target.id) { it.copy(health = damaged) }
            if (!damaged.isAlive()) {
                entities.markRemove(target.id)
                metrics = metrics.plus(DefenseMetrics(enemiesKilled = 1))
                val enemyId = target.type.substringAfter("enemy:")
                val enemy = registry.enemies[enemyId]
                if (enemy != null && enemy.rewardAmount > 0) {
                    rewards[enemy.rewardResource] = (rewards[enemy.rewardResource] ?: 0) + enemy.rewardAmount
                }
            }
            entities.update(towerEntity.id) { it.copy(tower = towerComponent.copy(cooldownRemaining = attack.cooldownTicks)) }
        }
        entities.flushRemovals()
        return TowerUpdateResult(metrics, rewards, towerMetrics.toSortedMap())
    }

    fun updateEnemies(
        registry: ContentRegistry,
        state: DefenseState,
        entities: EntityStore,
        goalField: GoalField? = null,
    ): DefenseState {
        var nextState = state
        entities.byTag("enemy").sortedBy { it.id.value }.forEach { enemy ->
            val movement = enemy.movement ?: return@forEach
            val position = enemy.position?.tile ?: return@forEach
            val nextPosition = goalField?.nextStep(position)
            val reachesCore = if (goalField != null) goalField.isGoal(position) else {
                val nextIndex = movement.pathIndex + 1
                nextIndex >= movement.path.lastIndex
            }
            if (reachesCore) {
                val enemyDefinition = registry.enemies[enemy.type.substringAfter("enemy:")]
                val damage = enemyDefinition?.coreDamage ?: 1
                entities.markRemove(enemy.id)
                nextState = nextState.copy(coreHealth = (nextState.coreHealth - damage).coerceAtLeast(0))
                    .record(DefenseMetrics(enemiesLeaked = 1, coreDamage = damage))
            } else if (goalField != null && nextPosition != null) {
                entities.update(enemy.id) {
                    it.copy(position = PositionComponent(nextPosition))
                }
            } else if (goalField == null) {
                val nextIndex = movement.pathIndex + 1
                val legacyNextPosition = movement.path[nextIndex]
                entities.update(enemy.id) {
                    it.copy(
                        position = PositionComponent(legacyNextPosition),
                        movement = movement.copy(pathIndex = nextIndex),
                    )
                }
            }
        }
        entities.flushRemovals()
        return nextState
    }

    private fun spawnEnemy(
        enemy: EnemyContent,
        world: TileWorld,
        entities: EntityStore,
        spawn: TilePosition,
        core: TilePosition,
        goalField: GoalField?,
    ) {
        val movement = if (goalField != null) {
            MovementComponent()
        } else {
            val path = when (val result = pathfinder.find(world, PathRequest(spawn, core))) {
                is PathResult.Found -> result.tiles
                is PathResult.NoPath -> listOf(spawn)
            }
            MovementComponent(path = path, pathIndex = 0)
        }
        entities.create("enemy:${enemy.id}", setOf("enemy")) { id ->
            Entity(
                id = id,
                type = "enemy:${enemy.id}",
                tags = setOf("enemy"),
                position = PositionComponent(spawn),
                health = HealthComponent(enemy.health, enemy.health),
                movement = movement,
            )
        }
    }
}
