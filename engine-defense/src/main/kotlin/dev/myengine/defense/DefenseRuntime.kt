package dev.myengine.defense

import dev.myengine.ai.GridPathfinder
import dev.myengine.ai.PathRequest
import dev.myengine.ai.PathResult
import dev.myengine.content.ContentRegistry
import dev.myengine.content.EnemyContent
import dev.myengine.content.TowerContent
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

data class DefenseState(
    val coreHealth: Int,
    val spawnedWaveIds: Set<String> = emptySet(),
    val metrics: DefenseMetrics = DefenseMetrics(),
) {
    fun record(metrics: DefenseMetrics): DefenseState = copy(metrics = this.metrics.plus(metrics))
}

sealed class TowerPlacementResult {
    data class Placed(val entityId: EntityId) : TowerPlacementResult()
    data class Rejected(val reason: String) : TowerPlacementResult()
}

data class TowerUpdateResult(
    val metrics: DefenseMetrics,
    val rewards: Map<String, Int> = emptyMap(),
)

class DefenseRuntime(private val pathfinder: GridPathfinder = GridPathfinder()) {
    fun placeTower(
        towerId: String,
        position: TilePosition,
        registry: ContentRegistry,
        world: TileWorld,
        entities: EntityStore,
    ): TowerPlacementResult {
        val tower = registry.towers[towerId] ?: return TowerPlacementResult.Rejected("unknown_tower")
        if (!world.canBuild(position)) return TowerPlacementResult.Rejected("tile_not_buildable")
        val entity = entities.create("tower:$towerId", setOf("tower")) { id ->
            Entity(
                id = id,
                type = "tower:$towerId",
                tags = setOf("tower"),
                position = PositionComponent(position),
                tower = TowerComponent(towerId),
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
    ): DefenseState {
        var nextState = state
        registry.waves.values
            .filter { it.startTick <= tick.value && it.id !in state.spawnedWaveIds }
            .sortedBy { it.id }
            .forEach { wave ->
                wave.spawns.forEach { spawnDef ->
                    val enemy = registry.requireEnemy(spawnDef.enemyId)
                    repeat(spawnDef.count) {
                        spawnEnemy(enemy, world, entities, spawn, core)
                    }
                    nextState = nextState.record(DefenseMetrics(enemiesSpawned = spawnDef.count))
                }
                nextState = nextState.copy(spawnedWaveIds = nextState.spawnedWaveIds + wave.id)
            }
        return nextState
    }

    fun updateTowers(registry: ContentRegistry, entities: EntityStore): TowerUpdateResult {
        var metrics = DefenseMetrics()
        val rewards = mutableMapOf<String, Int>()
        val enemiesById = entities.byTag("enemy").associateBy { it.id }
        entities.byTag("tower").sortedBy { it.id.value }.forEach { towerEntity ->
            val towerComponent = towerEntity.tower ?: return@forEach
            val attack = towerEntity.attack ?: return@forEach
            if (towerComponent.cooldownRemaining > 0) {
                entities.update(towerEntity.id) { it.copy(tower = towerComponent.copy(cooldownRemaining = towerComponent.cooldownRemaining - 1)) }
                return@forEach
            }
            val towerPosition = towerEntity.position?.tile ?: return@forEach
            val target = enemiesById.values
                .filter { enemy ->
                    val position = enemy.position?.tile ?: return@filter false
                    val health = enemy.health ?: return@filter false
                    health.isAlive() && towerPosition.manhattanDistance(position) <= attack.range
                }
                .sortedWith(compareBy<Entity> { towerPosition.manhattanDistance(it.position!!.tile) }.thenBy { it.id.value })
                .firstOrNull()
                ?: return@forEach

            val health = target.health ?: return@forEach
            val damaged = health.damage(attack.damage)
            metrics = metrics.plus(DefenseMetrics(towerShots = 1))
            if (damaged.isAlive()) {
                entities.update(target.id) { it.copy(health = damaged) }
            } else {
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
        return TowerUpdateResult(metrics, rewards)
    }

    fun updateEnemies(registry: ContentRegistry, state: DefenseState, entities: EntityStore): DefenseState {
        var nextState = state
        entities.byTag("enemy").sortedBy { it.id.value }.forEach { enemy ->
            val movement = enemy.movement ?: return@forEach
            val nextIndex = movement.pathIndex + 1
            if (nextIndex >= movement.path.lastIndex) {
                val enemyDefinition = registry.enemies[enemy.type.substringAfter("enemy:")]
                val damage = enemyDefinition?.coreDamage ?: 1
                entities.markRemove(enemy.id)
                nextState = nextState.copy(coreHealth = (nextState.coreHealth - damage).coerceAtLeast(0))
                    .record(DefenseMetrics(enemiesLeaked = 1, coreDamage = damage))
            } else {
                val nextPosition = movement.path[nextIndex]
                entities.update(enemy.id) {
                    it.copy(
                        position = PositionComponent(nextPosition),
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
    ) {
        val path = when (val result = pathfinder.find(world, PathRequest(spawn, core))) {
            is PathResult.Found -> result.tiles
            is PathResult.NoPath -> listOf(spawn)
        }
        entities.create("enemy:${enemy.id}", setOf("enemy")) { id ->
            Entity(
                id = id,
                type = "enemy:${enemy.id}",
                tags = setOf("enemy"),
                position = PositionComponent(spawn),
                health = HealthComponent(enemy.health, enemy.health),
                movement = MovementComponent(path = path, pathIndex = 0),
            )
        }
    }
}
