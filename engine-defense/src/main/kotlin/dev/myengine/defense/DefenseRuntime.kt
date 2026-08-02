package dev.myengine.defense

import dev.myengine.ai.GoalField
import dev.myengine.ai.GridPathfinder
import dev.myengine.ai.PathRequest
import dev.myengine.ai.PathResult
import dev.myengine.content.ContentRegistry
import dev.myengine.content.EnemyContent
import dev.myengine.content.WaveModifier
import dev.myengine.content.effectiveStats
import dev.myengine.content.StatusEffectKind
import dev.myengine.content.StatusEffectStackingRule
import dev.myengine.content.TowerContent
import dev.myengine.content.WaveContent
import dev.myengine.core.Tick
import dev.myengine.core.CombatEvents
import dev.myengine.core.GameplayEvent
import dev.myengine.core.GameplayEventType
import dev.myengine.core.HitEvent
import dev.myengine.core.ShotEvent
import dev.myengine.entities.AttackComponent
import dev.myengine.entities.Entity
import dev.myengine.entities.EntityId
import dev.myengine.entities.EntityStore
import dev.myengine.entities.EnemyComponent
import dev.myengine.entities.HealthComponent
import dev.myengine.entities.MovementComponent
import dev.myengine.entities.PositionComponent
import dev.myengine.entities.StatusEffectComponent
import dev.myengine.entities.TowerComponent
import dev.myengine.world.TilePosition
import dev.myengine.world.TileWorld
import java.util.Collections

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
    val events: CombatEvents = CombatEvents.EMPTY,
)

data class StatusEffectUpdateResult(
    val metrics: DefenseMetrics = DefenseMetrics(),
    val rewards: Map<String, Int> = emptyMap(),
    val events: List<GameplayEvent> = emptyList(),
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
        eventSink: MutableList<GameplayEvent>? = null,
    ): DefenseState {
        var nextState = state
        registry.waves.values
            .filter { it.startTick <= tick.value && it.id !in state.spawnedWaveIds }
            .sortedBy { it.id }
            .forEach { wave ->
                nextState = spawnWave(
                    wave = wave,
                    state = nextState,
                    registry = registry,
                    world = world,
                    entities = entities,
                    spawn = spawn,
                    core = core,
                    goalField = goalField,
                    tick = tick,
                    eventSink = eventSink,
                )
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
        tick: Tick = Tick(0),
        eventSink: MutableList<GameplayEvent>? = null,
    ): DefenseState {
        if (wave.id in state.spawnedWaveIds) return state
        var nextState = state
        var enemyOrdinal = 0
        wave.spawns.forEach { spawnDef ->
            val enemy = registry.requireEnemy(spawnDef.enemyId)
            repeat(spawnDef.count) {
                val modifier = waveModifierAt(wave.modifiers, enemyOrdinal)
                spawnEnemy(enemy, modifier, world, entities, spawn, core, goalField)
                enemyOrdinal += 1
            }
            nextState = nextState.record(DefenseMetrics(enemiesSpawned = spawnDef.count))
        }
        eventSink?.add(GameplayEvent(tick = tick, type = GameplayEventType.WAVE_START, contentId = wave.id))
        return nextState.copy(spawnedWaveIds = nextState.spawnedWaveIds + wave.id)
    }

    private fun waveModifierAt(modifiers: List<WaveModifier>, enemyOrdinal: Int): WaveModifier? {
        var covered = 0
        modifiers.forEach { modifier ->
            if (enemyOrdinal >= covered && enemyOrdinal < covered + modifier.count) return modifier
            covered += modifier.count
        }
        return null
    }

    /**
     * Advances effects that were already active at the start of this tick. New effects are
     * applied by [updateTowers] after tower damage, so their first DoT tick is deterministic on
     * the following tick. Expiration and DoT traversal are ordered by entity id, then effect id.
     */
    fun updateStatusEffects(
        registry: ContentRegistry,
        entities: EntityStore,
        tick: Tick = Tick(0),
    ): StatusEffectUpdateResult {
        var metrics = DefenseMetrics()
        val rewards = mutableMapOf<String, Int>()
        val events = mutableListOf<GameplayEvent>()
        entities.all().sortedBy { it.id.value }.forEach { entity ->
            if (entity.statusEffects.isEmpty()) return@forEach
            var current = entities.get(entity.id) ?: return@forEach
            val nextEffects = mutableListOf<StatusEffectComponent>()
            entity.statusEffects.sortedBy { it.effectId }.forEach { active ->
                if (current.health?.isAlive() != true) return@forEach
                val definition = registry.requireEffect(active.effectId)
                if (definition.kind == StatusEffectKind.DOT && current.tags.contains("enemy")) {
                    val damage = (definition.magnitude.toLong() * active.stacks.toLong())
                        .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    val health = current.health ?: return@forEach
                    val damaged = health.damage(damage)
                    val killed = !damaged.isAlive()
                    val actualDamage = minOf(damage, health.current)
                    current = current.copy(health = damaged)
                    entities.update(entity.id) { it.copy(health = damaged) }
                    if (killed) {
                        metrics = metrics.plus(DefenseMetrics(enemiesKilled = 1))
                        val enemyId = current.type.substringAfter("enemy:")
                        (current.enemy?.takeIf { it.enemyId == enemyId } ?: registry.enemies[enemyId]?.toEnemyComponent())?.let { enemyDefinition ->
                            if (enemyDefinition.rewardAmount > 0) {
                                rewards[enemyDefinition.rewardResource] =
                                    (rewards[enemyDefinition.rewardResource] ?: 0) + enemyDefinition.rewardAmount
                            }
                        }
                        entities.markRemove(entity.id)
                        events += GameplayEvent(
                            tick = tick,
                            type = GameplayEventType.DEATH,
                            targetEntityId = entity.id.value,
                            contentId = enemyId,
                        )
                    }
                    if (actualDamage < 0) error("Status effect damage cannot be negative.")
                }
                if (current.health?.isAlive() == true && active.remainingTicks > 1) {
                    nextEffects += active.copy(remainingTicks = active.remainingTicks - 1)
                }
            }
            if (current.health?.isAlive() == true) {
                entities.update(entity.id) { it.copy(statusEffects = nextEffects.sortedBy { effect -> effect.effectId }) }
            }
        }
        entities.flushRemovals()
        return StatusEffectUpdateResult(
            metrics = metrics,
            rewards = rewards.toSortedMap(),
            events = Collections.unmodifiableList(events.toList()),
        )
    }

    /** Applies one content-defined effect after the current tower damage phase. */
    fun applyStatusEffect(
        registry: ContentRegistry,
        entities: EntityStore,
        targetId: EntityId,
        effectId: String,
    ): Boolean {
        val definition = registry.requireEffect(effectId)
        val current = entities.get(targetId) ?: return false
        if (current.health?.isAlive() == false) return false
        val existing = current.statusEffects.firstOrNull { it.effectId == effectId }
        val next = when {
            existing == null -> StatusEffectComponent(effectId, definition.durationTicks)
            definition.stackingRule == StatusEffectStackingRule.IGNORE -> existing
            definition.stackingRule == StatusEffectStackingRule.REFRESH ->
                existing.copy(remainingTicks = definition.durationTicks, stacks = 1)
            else -> existing.copy(
                remainingTicks = definition.durationTicks,
                stacks = if (existing.stacks == Int.MAX_VALUE) Int.MAX_VALUE else existing.stacks + 1,
            )
        }
        if (existing == next) return true
        val effects = current.statusEffects
            .filterNot { it.effectId == effectId }
            .plus(next)
            .sortedBy { it.effectId }
        entities.update(targetId) { it.copy(statusEffects = effects) }
        return true
    }

    fun updateTowers(
        registry: ContentRegistry,
        entities: EntityStore,
        goalField: GoalField? = null,
        tick: Tick = Tick(0),
    ): TowerUpdateResult {
        var metrics = DefenseMetrics()
        val rewards = mutableMapOf<String, Int>()
        val towerMetrics = mutableMapOf<Long, TowerDefenseMetrics>()
        val shots = mutableListOf<ShotEvent>()
        val hits = mutableListOf<HitEvent>()
        val gameplayEvents = mutableListOf<GameplayEvent>()
        val pendingEffects = mutableListOf<PendingStatusEffect>()
        // This cache is intentionally scoped to one deterministic tower-update pass. It is
        // rebuilt before any tower can change health, and query results resolve ids through the
        // live store so later towers observe earlier damage/removals exactly as before.
        val enemyIndex = GridSpatialIndex.build(entities.byTag("enemy"))
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
                TargetSelector.select(
                    towerComponent.targetingMode,
                    towerPosition,
                    attack.range,
                    enemyIndex.query(towerPosition, attack.range, entities),
                    goalField,
                )
            } else {
                // Compatibility for direct defense-module callers predating goal-field routing.
                enemyIndex.query(towerPosition, attack.range, entities)
                    .filter { enemy ->
                        val position = enemy.position?.tile ?: return@filter false
                        val health = enemy.health ?: return@filter false
                        health.isAlive() && towerPosition.manhattanDistance(position) <= attack.range
                    }
                    .sortedWith(compareBy<Entity> { towerPosition.manhattanDistance(it.position!!.tile) }.thenBy { it.id.value })
                    .firstOrNull()
            } ?: return@forEach

            metrics = metrics.plus(DefenseMetrics(towerShots = 1))
            shots += ShotEvent(towerEntity.id.value, target.id.value, tick)
            gameplayEvents += GameplayEvent(
                tick = tick,
                type = GameplayEventType.SHOT,
                sourceEntityId = towerEntity.id.value,
                targetEntityId = target.id.value,
                contentId = towerComponent.towerId,
            )
            val targetPosition = target.position?.tile ?: return@forEach
            val towerContent = registry.requireTower(towerComponent.towerId)
            val targets = splashTargets(
                primaryTarget = target,
                primaryPosition = targetPosition,
                splashRadius = towerContent.splashRadius,
                enemyIndex = enemyIndex,
                entities = entities,
            )
            targets.forEach { damagedTarget ->
                val health = damagedTarget.health ?: return@forEach
                if (!health.isAlive()) return@forEach
                val position = damagedTarget.position?.tile ?: return@forEach
                val damage = splashDamage(
                    baseDamage = attack.damage,
                    distance = targetPosition.manhattanDistance(position),
                    falloffPercent = towerContent.falloffPercent,
                )
                if (damage <= 0) return@forEach

                val damaged = health.damage(damage)
                val towerDelta = TowerDefenseMetrics(
                    actualDamage = minOf(damage, health.current).toLong(),
                    kills = if (damaged.isAlive()) 0 else 1,
                )
                towerMetrics[towerEntity.id.value] = (towerMetrics[towerEntity.id.value] ?: TowerDefenseMetrics()).plus(towerDelta)
                hits += HitEvent(towerEntity.id.value, damagedTarget.id.value, tick)
                gameplayEvents += GameplayEvent(
                    tick = tick,
                    type = GameplayEventType.HIT,
                    sourceEntityId = towerEntity.id.value,
                    targetEntityId = damagedTarget.id.value,
                    contentId = towerComponent.towerId,
                )
                // Persist even terminal damage until the final flush so subsequent towers observe the
                // dead health and cannot double-count damage, kills, or content rewards.
                entities.update(damagedTarget.id) { it.copy(health = damaged) }
                if (!damaged.isAlive()) {
                    entities.markRemove(damagedTarget.id)
                    gameplayEvents += GameplayEvent(
                        tick = tick,
                        type = GameplayEventType.DEATH,
                        sourceEntityId = towerEntity.id.value,
                        targetEntityId = damagedTarget.id.value,
                        contentId = damagedTarget.type.substringAfter("enemy:"),
                    )
                    metrics = metrics.plus(DefenseMetrics(enemiesKilled = 1))
                    val enemyId = damagedTarget.type.substringAfter("enemy:")
                    val enemy = damagedTarget.enemy?.takeIf { it.enemyId == enemyId } ?: registry.enemies[enemyId]?.toEnemyComponent()
                    if (enemy != null && enemy.rewardAmount > 0) {
                        rewards[enemy.rewardResource] = (rewards[enemy.rewardResource] ?: 0) + enemy.rewardAmount
                    }
                }
            }
            towerContent.effectId?.let { effectId ->
                if (entities.get(target.id)?.health?.isAlive() == true) {
                    pendingEffects += PendingStatusEffect(target.id, effectId, towerEntity.id)
                }
            }
            entities.update(towerEntity.id) { it.copy(tower = towerComponent.copy(cooldownRemaining = attack.cooldownTicks)) }
        }
        pendingEffects
            .sortedWith(compareBy<PendingStatusEffect> { it.targetId.value }.thenBy { it.effectId }.thenBy { it.sourceTowerId.value })
            .forEach { pending ->
                applyStatusEffect(registry, entities, pending.targetId, pending.effectId)
            }
        entities.flushRemovals()
        return TowerUpdateResult(
            metrics = metrics,
            rewards = rewards.toSortedMap(),
            towerMetrics = towerMetrics.toSortedMap(),
            events = CombatEvents(
                shots = Collections.unmodifiableList(shots.toList()),
                hits = Collections.unmodifiableList(hits.toList()),
                gameplayEvents = Collections.unmodifiableList(gameplayEvents.toList()),
            ),
        )
    }

    private fun splashTargets(
        primaryTarget: Entity,
        primaryPosition: TilePosition,
        splashRadius: Int?,
        enemyIndex: GridSpatialIndex,
        entities: EntityStore,
    ): List<Entity> = when (splashRadius) {
        null -> listOf(primaryTarget)
        else -> enemyIndex.query(primaryPosition, splashRadius, entities)
            .asSequence()
            .filter { enemy ->
                val position = enemy.position?.tile
                val health = enemy.health
                position != null && health?.isAlive() == true && primaryPosition.manhattanDistance(position) <= splashRadius
            }
            .sortedBy { it.id.value }
            .toList()
    }

    /**
     * Integer-only AoE rule: damage loses [falloffPercent] percent of base damage per Manhattan
     * ring, then truncates toward zero. Zero-damage outer-ring candidates produce no hit event.
     */
    private fun splashDamage(baseDamage: Int, distance: Int, falloffPercent: Int): Int {
        val remainingPercent = (100 - distance * falloffPercent).coerceAtLeast(0)
        return ((baseDamage.toLong() * remainingPercent) / 100L).toInt()
    }

    private data class PendingStatusEffect(
        val targetId: EntityId,
        val effectId: String,
        val sourceTowerId: EntityId,
    )

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
            val speed = effectiveMovementSpeed(registry, enemy)
            val reachesCore = if (goalField != null) goalField.isGoal(position) else {
                val nextIndex = movement.pathIndex + speed
                nextIndex >= movement.path.lastIndex
            }
            if (reachesCore) {
                val enemyDefinition = registry.enemies[enemy.type.substringAfter("enemy:")]
                val damage = enemy.enemy?.coreDamage ?: enemyDefinition?.coreDamage ?: 1
                entities.markRemove(enemy.id)
                nextState = nextState.copy(coreHealth = (nextState.coreHealth - damage).coerceAtLeast(0))
                    .record(DefenseMetrics(enemiesLeaked = 1, coreDamage = damage))
            } else if (speed > 0 && goalField != null) {
                var nextPosition = position
                repeat(speed) {
                    nextPosition = goalField.nextStep(nextPosition) ?: return@repeat
                }
                if (nextPosition != position) {
                    entities.update(enemy.id) { it.copy(position = PositionComponent(nextPosition)) }
                }
            } else if (speed > 0) {
                val nextIndex = movement.pathIndex + speed
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

    private fun effectiveMovementSpeed(registry: ContentRegistry, enemy: Entity): Int {
        val enemyId = enemy.type.substringAfter("enemy:")
        val baseSpeed = enemy.enemy?.speedTilesPerTick ?: registry.enemies[enemyId]?.speedTilesPerTick ?: 1
        val slowPercent = enemy.statusEffects
            .sortedBy { it.effectId }
            .fold(0L) { accumulated, active ->
                val definition = registry.requireEffect(active.effectId)
                if (definition.kind != StatusEffectKind.SLOW || accumulated >= 100L) {
                    accumulated
                } else {
                    val contribution = definition.magnitude.toLong() * active.stacks.toLong()
                    (accumulated + contribution).coerceAtMost(100L)
                }
            }
            .coerceIn(0L, 100L)
        return ((baseSpeed.toLong() * (100L - slowPercent)) / 100L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    private fun spawnEnemy(
        enemy: EnemyContent,
        waveModifier: WaveModifier?,
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
        val stats = enemy.effectiveStats(waveModifier)
        val enemyComponent = EnemyComponent(
            enemyId = enemy.id,
            speedTilesPerTick = stats.speedTilesPerTick,
            coreDamage = enemy.coreDamage,
            rewardResource = enemy.rewardResource,
            rewardAmount = stats.rewardAmount,
            isElite = enemy.isElite,
            isBoss = enemy.isBoss,
        ).takeIf {
            enemy.isElite || enemy.isBoss ||
                enemy.healthScalePercent != 100 || enemy.speedScalePercent != 100 ||
                enemy.rewardScalePercent != 100 || waveModifier != null
        }
        entities.create("enemy:${enemy.id}", setOf("enemy")) { id ->
            Entity(
                id = id,
                type = "enemy:${enemy.id}",
                tags = setOf("enemy"),
                position = PositionComponent(spawn),
                health = HealthComponent(stats.health, stats.health),
                movement = movement,
                enemy = enemyComponent,
            )
        }
    }
}

private fun EnemyContent.toEnemyComponent(): EnemyComponent = EnemyComponent(
    enemyId = id,
    speedTilesPerTick = speedTilesPerTick,
    coreDamage = coreDamage,
    rewardResource = rewardResource,
    rewardAmount = rewardAmount,
    isElite = isElite,
    isBoss = isBoss,
)
