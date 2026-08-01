package dev.myengine.games.sandbox

import dev.myengine.content.ContentPackLoader
import dev.myengine.content.ContentRegistry
import dev.myengine.content.MapContent
import dev.myengine.content.MapWinCondition
import dev.myengine.content.TowerUpgradeTier
import dev.myengine.content.VisualAssetRef
import dev.myengine.content.WaveContent
import dev.myengine.content.HudStringKeys
import dev.myengine.ai.GoalField
import dev.myengine.core.CommandQueue
import dev.myengine.core.CombatEvents
import dev.myengine.core.EngineCommand
import dev.myengine.core.EngineInfo
import dev.myengine.core.HashableState
import dev.myengine.core.RunState
import dev.myengine.core.RunStatus
import dev.myengine.core.RunSummary
import dev.myengine.core.StableHash
import dev.myengine.core.TerminalReason
import dev.myengine.core.Tick
import dev.myengine.core.command.BuildTowerCommand
import dev.myengine.core.command.CallWaveEarlyCommand
import dev.myengine.core.command.SellTowerCommand
import dev.myengine.core.command.SetTowerTargetingModeCommand
import dev.myengine.core.command.TileCoordinate
import dev.myengine.core.command.TargetingMode
import dev.myengine.core.command.UpgradeTowerCommand
import dev.myengine.defense.DefenseRuntime
import dev.myengine.defense.DefenseState
import dev.myengine.defense.TowerPlacementResult
import dev.myengine.entities.AttackComponent
import dev.myengine.entities.Entity
import dev.myengine.entities.EntityId
import dev.myengine.entities.EntityStore
import dev.myengine.entities.HealthComponent
import dev.myengine.entities.MovementComponent
import dev.myengine.entities.PositionComponent
import dev.myengine.entities.StatusEffectComponent
import dev.myengine.entities.TowerComponent
import dev.myengine.logistics.Inventory
import dev.myengine.logistics.Producer
import dev.myengine.logistics.ProducerSystem
import dev.myengine.render.DebugOverlay
import dev.myengine.render.EngineSnapshot
import dev.myengine.render.HudBuildTower
import dev.myengine.render.HudLabels
import dev.myengine.render.HudResourceAmount
import dev.myengine.render.HudSnapshot
import dev.myengine.render.HudTowerInfo
import dev.myengine.render.HudTowerTier
import dev.myengine.render.HudWaveCompositionEntry
import dev.myengine.render.RenderEntity
import dev.myengine.render.RenderAssetRef
import dev.myengine.render.RenderTile
import dev.myengine.storyteller.IncidentDirector
import dev.myengine.world.ResourceNode
import dev.myengine.world.TerrainRule
import dev.myengine.world.TilePosition
import dev.myengine.world.TileWorld
import dev.myengine.world.WorldSize
import dev.myengine.world.WorldTile
import java.io.StringReader
import java.io.StringWriter
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Properties

data class SandboxDescriptor(
    val id: String = "sandbox",
    val engineName: String = EngineInfo.NAME,
)

data class SandboxState(
    var tick: Tick,
    val registry: ContentRegistry,
    val mapId: String,
    val world: TileWorld,
    val entities: EntityStore,
    var inventory: Inventory,
    var producers: List<Producer>,
    var defense: DefenseState,
    var run: RunState = RunState(),
    var lastCommandOrError: String? = null,
) : HashableState {
    override fun appendHash(hash: StableHash) {
        hash.add(tick.value)
        hash.add(registry.manifest.id).add(registry.manifest.version)
        world.appendHash(hash)
        entities.appendHash(hash)
        inventory.appendHash(hash)
        producers.sortedBy { it.id }.forEach { hash.add(it.id).add(it.recipeId).add(it.progressTicks) }
        hash.add(defense.coreHealth)
        defense.spawnedWaveIds.sorted().forEach(hash::add)
        hash.add(defense.metrics.enemiesSpawned)
            .add(defense.metrics.enemiesKilled)
            .add(defense.metrics.enemiesLeaked)
            .add(defense.metrics.coreDamage)
            .add(defense.metrics.towerShots)
        if (run.isTerminal) run.appendHash(hash)
    }

    fun stableHash(): String = StableHash().also(::appendHash).digest()
}

/** Outcome of depositing tower-kill rewards into an inventory: the (possibly full) inventory and any rewards that did not fit. */
internal data class RewardDeposit(val inventory: Inventory, val dropped: Map<String, Int>)

/**
 * Pure, deterministic deposit of content-derived kill rewards into [inventory].
 *
 * Rewards are applied in sorted-key order (replay-stable). A reward that would exceed a set
 * inventory capacity (`canAdd == false`) is NOT force-added — it is reported in [RewardDeposit.dropped]
 * so the caller can surface telemetry instead of silently swallowing it. With the default sandbox
 * inventory (`capacity == null`) nothing is ever dropped.
 */
internal fun depositRewards(inventory: Inventory, rewards: Map<String, Int>): RewardDeposit {
    var next = inventory
    val dropped = linkedMapOf<String, Int>()
    rewards.toSortedMap().forEach { (resource, amount) ->
        if (next.canAdd(resource, amount)) {
            next = next.add(resource, amount)
        } else {
            dropped[resource] = amount
        }
    }
    return RewardDeposit(next, dropped)
}

class SandboxRuntime(
    val state: SandboxState,
    private val defenseRuntime: DefenseRuntime = DefenseRuntime(),
    private val commandQueue: CommandQueue = CommandQueue(),
) {
    private val producerSystem = ProducerSystem(state.registry.recipes)
    private val map = state.registry.requireMap(state.mapId)
    private val spawn = map.primarySpawn.position.let { TilePosition(it.x, it.y) }
    private val spawns = map.spawns.values.map { TilePosition(it.position.x, it.position.y) }.sorted()
    private val core = map.core.let { TilePosition(it.x, it.y) }
    /** Derived cache: rebuilt from authoritative world occupancy after placement and on restore. */
    private var goalField: GoalField = rebuildAfterWalkabilityChange()
    /** Latest transient combat events, replaced every completed simulation tick. */
    private var combatEvents: CombatEvents = CombatEvents.EMPTY
    private val renderPath: List<TilePosition> get() = goalField.pathFrom(spawn)

    /** Returns false without mutating state when the run has already reached its terminal boundary. */
    fun submit(command: EngineCommand): Boolean {
        if (state.run.isTerminal) return false
        commandQueue.submit(command)
        return true
    }

    /** Non-destructive snapshot of the runtime's not-yet-drained pending commands. */
    fun pendingCommands(): List<EngineCommand> = commandQueue.pending()

    /** Submits a command batch subject to the same terminal rejection as [submit]. */
    fun submitAll(commands: List<EngineCommand>) {
        commands.forEach(::submit)
    }

    /**
     * Restore-only path that preserves a serialized pre-terminal queue without reopening command
     * submission. Terminal [step] calls still return before draining these commands.
     */
    internal fun restorePendingCommands(commands: List<EngineCommand>) {
        commands.forEach(commandQueue::submit)
    }

    fun step(ticks: Int = 1) {
        repeat(ticks) {
            if (state.run.isTerminal) return
            state.tick = state.tick.next()
            val commands = commandQueue.drainFor(state.tick)
            commands.forEach(::applyCommand)
            updateProduction()
            state.defense = defenseRuntime.spawnDueWaves(
                state.tick,
                state.defense,
                state.registry,
                state.world,
                state.entities,
                spawn,
                core,
                goalField,
            )
            val statusEffectResult = defenseRuntime.updateStatusEffects(state.registry, state.entities)
            state.defense = state.defense.record(statusEffectResult.metrics)
            val statusEffectDeposit = depositRewards(state.inventory, statusEffectResult.rewards)
            state.inventory = statusEffectDeposit.inventory
            if (statusEffectDeposit.dropped.isNotEmpty()) {
                state.lastCommandOrError = statusEffectDeposit.dropped.entries
                    .joinToString(",", prefix = "reward_dropped:") { "${it.key}:${it.value}" }
            }
            val towerResult = defenseRuntime.updateTowers(state.registry, state.entities, goalField, state.tick)
            combatEvents = towerResult.events
            state.defense = state.defense.record(towerResult.metrics).recordTowerMetrics(towerResult.towerMetrics)
            val deposit = depositRewards(state.inventory, towerResult.rewards)
            state.inventory = deposit.inventory
            if (deposit.dropped.isNotEmpty()) {
                // Non-fatal telemetry: a full inventory is a legitimate game state, not a crash,
                // but earned rewards must not vanish without a trace (surfaces in DebugOverlay).
                state.lastCommandOrError = deposit.dropped.entries
                    .joinToString(",", prefix = "reward_dropped:") { "${it.key}:${it.value}" }
            }
            state.defense = defenseRuntime.updateEnemies(state.registry, state.defense, state.entities, goalField)
            evaluateTerminalState()
            if (state.run.isTerminal) return
            IncidentDirector(state.registry.incidents.values).select(state.defense.metrics.enemiesSpawned, dev.myengine.core.SeededRandom(17))
        }
    }

    fun snapshot(): EngineSnapshot {
        val renderTiles = state.world.positions().map {
            val view = state.world.tileAt(it)
            RenderTile(
                position = it,
                terrainId = view.tile.terrainId,
                buildable = state.world.canBuild(it),
                assetRef = state.registry.tiles[view.tile.terrainId]?.assetRef?.toRenderAssetRef(),
            )
        }
        val renderEntities = state.entities.all().mapNotNull {
            val position = it.position?.tile ?: return@mapNotNull null
            val assetRef = it.tower?.let { towerComponent ->
                val tower = state.registry.towers[towerComponent.towerId]
                val tierAsset = towerComponent.upgradeBranch?.let { branch ->
                    tower?.upgradeTiers[TowerUpgradeTier.key(branch, towerComponent.upgradeTier)]?.assetRef
                }
                (tierAsset ?: tower?.assetRef)?.toRenderAssetRef()
            } ?: when {
                it.type.startsWith("enemy:") -> state.registry.enemies[it.type.substringAfter(':')]?.assetRef?.toRenderAssetRef()
                it.type.startsWith("building:") -> state.registry.buildings[it.type.substringAfter(':')]?.assetRef?.toRenderAssetRef()
                else -> null
            }
            RenderEntity(
                id = it.id.value,
                type = it.type,
                position = position,
                health = it.health?.current,
                towerTier = it.tower?.upgradeTier,
                assetRef = assetRef,
                activeEffectTags = it.statusEffects.sortedBy { effect -> effect.effectId }
                    .map { effect -> effect.effectId },
            )
        }
        return EngineSnapshot(
            worldSize = state.world.size,
            tiles = renderTiles,
            entities = renderEntities,
            path = renderPath,
            coreHealth = state.defense.coreHealth,
            debug = DebugOverlay(
                tick = state.tick,
                entityCount = state.entities.count(),
                wave = state.defense.spawnedWaveIds.maxOrNull(),
                selectedTile = null,
                lastCommandOrError = state.lastCommandOrError,
            ),
            runStatus = state.run.status,
            terminalReason = state.run.terminalReason,
            terminalTick = state.run.terminalTick,
            runSummary = state.run.summary ?: currentRunSummary(),
            hud = hudSnapshot(),
            combatEvents = combatEvents,
        )
    }

    private fun hudSnapshot(): HudSnapshot {
        val registry = state.registry
        fun text(key: String): String = registry.strings[key].orEmpty()
        fun resourceAmount(resourceId: String, amount: Int): HudResourceAmount {
            val resource = registry.requireResource(resourceId)
            return HudResourceAmount(resourceId, text(resource.displayKey), amount)
        }
        fun tier(tier: TowerUpgradeTier): HudTowerTier = HudTowerTier(
            branch = tier.branch,
            tier = tier.tier,
            label = text(tier.displayKey),
            cost = resourceAmount(tier.costResource, tier.costAmount),
            damage = tier.damage,
        )

        val buildTowers = registry.towers.values.sortedBy { it.id }.map { tower ->
            HudBuildTower(
                towerId = tower.id,
                label = text(tower.displayKey),
                cost = resourceAmount(tower.costResource, tower.costAmount),
                tiers = tower.upgradeTiers.values
                    .sortedWith(compareBy<TowerUpgradeTier> { it.branch }.thenBy { it.tier })
                    .map(::tier),
            )
        }
        val towerInfo = state.entities.byTag("tower").sortedBy { it.id.value }.mapNotNull { entity ->
            val component = entity.tower ?: return@mapNotNull null
            val tower = registry.towers[component.towerId] ?: return@mapNotNull null
            val metrics = state.defense.towerMetrics[entity.id.value]
            val upgrades = tower.upgradeTiers.values.filter { candidate ->
                if (component.upgradeBranch == null) {
                    candidate.tier == 1
                } else {
                    candidate.branch == component.upgradeBranch && candidate.tier == component.upgradeTier + 1
                }
            }.sortedWith(compareBy<TowerUpgradeTier> { it.branch }.thenBy { it.tier }).map(::tier)
            HudTowerInfo(
                entityId = entity.id.value,
                towerId = tower.id,
                label = text(tower.displayKey),
                branch = component.upgradeBranch,
                tier = component.upgradeTier,
                damage = entity.attack?.damage ?: tower.damage,
                actualDamage = metrics?.actualDamage ?: 0,
                kills = metrics?.kills ?: 0,
                targetingMode = component.targetingMode,
                availableUpgrades = upgrades,
            )
        }
        val nextWave = nextUnspawnedWave()
        return HudSnapshot(
            labels = HudLabels(
                resources = text(HudStringKeys.RESOURCES),
                wave = text(HudStringKeys.WAVE),
                nextWave = text(HudStringKeys.NEXT_WAVE),
                coreHealth = text(HudStringKeys.CORE_HEALTH),
                build = text(HudStringKeys.BUILD),
                upgrade = text(HudStringKeys.UPGRADE),
                damage = text(HudStringKeys.DAMAGE),
                kills = text(HudStringKeys.KILLS),
                tier = text(HudStringKeys.TIER),
            ),
            resources = registry.resources.values.sortedBy { it.id }
                .map { resourceAmount(it.id, state.inventory.amount(it.id)) },
            wave = state.defense.spawnedWaveIds.size,
            totalWaves = registry.waves.size,
            nextWaveInTicks = nextWave?.let { (it.startTick - state.tick.value).coerceAtLeast(0) },
            nextWaveComposition = nextWave?.spawns?.map { spawn ->
                HudWaveCompositionEntry(enemyId = spawn.enemyId, count = spawn.count)
            } ?: emptyList(),
            coreHealth = state.defense.coreHealth,
            buildTowers = buildTowers,
            towers = towerInfo,
        )
    }

    private fun applyCommand(command: EngineCommand) {
        when (command) {
            is BuildTowerCommand -> buildTower(command)
            is UpgradeTowerCommand -> upgradeTower(command)
            is SellTowerCommand -> sellTower(command)
            is SetTowerTargetingModeCommand -> setTowerTargetingMode(command)
            is CallWaveEarlyCommand -> callWaveEarly(command)
            else -> state.lastCommandOrError = "ignored:${command.type}"
        }
    }

    /**
     * Resolves and starts the previewed wave before scheduled spawning. Rejection checks happen
     * before any authoritative operation: a live enemy or an exhausted schedule leaves entities,
     * inventory, and defense state untouched (the diagnostic string is presentation telemetry).
     * A wave whose scheduled boundary has already arrived is also a no-op: only a genuinely early
     * call (`state.tick < wave.startTick`) is allowed to grant its early-call bonus.
     */
    private fun callWaveEarly(@Suppress("UNUSED_PARAMETER") command: CallWaveEarlyCommand) {
        val wave = nextUnspawnedWave()
        if (wave == null) {
            state.lastCommandOrError = "no_upcoming_wave"
            return
        }
        if (state.tick.value >= wave.startTick) {
            state.lastCommandOrError = "wave_already_due:${wave.id}"
            return
        }
        if (state.entities.byTag("enemy").any { it.health?.isAlive() == true }) {
            state.lastCommandOrError = "wave_active"
            return
        }

        state.defense = defenseRuntime.spawnWave(
            wave = wave,
            state = state.defense,
            registry = state.registry,
            world = state.world,
            entities = state.entities,
            spawn = spawn,
            core = core,
            goalField = goalField,
        )
        val bonus = wave.earlyCallBonus
        if (bonus == null) {
            state.lastCommandOrError = "wave_called:${wave.id}"
            return
        }
        val deposit = depositRewards(state.inventory, mapOf(bonus.resourceId to bonus.amount))
        state.inventory = deposit.inventory
        state.lastCommandOrError = if (deposit.dropped.isEmpty()) {
            "wave_called:${wave.id}"
        } else {
            deposit.dropped.entries.joinToString(",", prefix = "wave_called:${wave.id},reward_dropped:") {
                "${it.key}:${it.value}"
            }
        }
    }

    /** One deterministic next-wave projection shared by command behavior and HUD preview. */
    private fun nextUnspawnedWave(): WaveContent? = state.registry.waves.values
        .filter { it.id !in state.defense.spawnedWaveIds }
        .minWithOrNull(compareBy({ it.startTick }, { it.id }))

    private fun buildTower(command: BuildTowerCommand) {
        val tower = state.registry.towers[command.towerId]
        if (tower == null) {
            state.lastCommandOrError = "unknown_tower:${command.towerId}"
            return
        }
        if (!state.inventory.canRemove(tower.costResource, tower.costAmount)) {
            state.lastCommandOrError = "missing_resource:${tower.costResource}"
            return
        }
        val position = TilePosition(command.position.x, command.position.y)
        when (
            val result = defenseRuntime.placeTower(
                command.towerId,
                position,
                state.registry,
                state.world,
                state.entities,
                spawns,
                core,
            )
        ) {
            is TowerPlacementResult.Placed -> {
                // Tick order is command -> occupancy mutation -> rebuild -> spawn/movement.  Thus
                // this same tick can never use a pre-placement route; future destroy/wall flows
                // must use this hook after their authoritative walkability mutation too.
                goalField = rebuildAfterWalkabilityChange()
                state.inventory = state.inventory.remove(tower.costResource, tower.costAmount)
                state.lastCommandOrError = "placed:${result.entityId.value}"
            }
            is TowerPlacementResult.Rejected -> state.lastCommandOrError = result.reason
        }
    }

    private fun upgradeTower(command: UpgradeTowerCommand) {
        val entityId = EntityId(command.towerEntityId)
        val entity = state.entities.get(entityId)
        val towerComponent = entity?.tower
        if (entity == null || towerComponent == null) {
            state.lastCommandOrError = "unknown_tower_entity:${command.towerEntityId}"
            return
        }
        val tower = state.registry.towers[towerComponent.towerId]
        if (tower == null) {
            state.lastCommandOrError = "unknown_tower:${towerComponent.towerId}"
            return
        }
        val tier = tower.upgradeTiers[TowerUpgradeTier.key(command.branch, command.tier)]
        if (tier == null) {
            state.lastCommandOrError = "unknown_upgrade:${tower.id}:${command.branch}:${command.tier}"
            return
        }
        if (!canApplyUpgrade(towerComponent, command)) {
            val currentBranch = towerComponent.upgradeBranch ?: "none"
            state.lastCommandOrError = "invalid_upgrade_transition:$currentBranch:${towerComponent.upgradeTier}->${command.branch}:${command.tier}"
            return
        }
        if (entity.attack == null) {
            state.lastCommandOrError = "tower_missing_attack:${command.towerEntityId}"
            return
        }
        if (!state.inventory.canRemove(tier.costResource, tier.costAmount)) {
            state.lastCommandOrError = "missing_resource:${tier.costResource}"
            return
        }

        state.entities.update(entityId) {
            it.copy(
                tower = towerComponent.copy(upgradeBranch = tier.branch, upgradeTier = tier.tier),
                attack = AttackComponent(tier.range, tier.damage, tier.cooldownTicks),
            )
        }
        state.inventory = state.inventory.remove(tier.costResource, tier.costAmount)
        state.lastCommandOrError = "upgraded:${command.towerEntityId}:${tier.branch}:${tier.tier}"
    }

    private fun sellTower(command: SellTowerCommand) {
        val entityId = EntityId(command.towerEntityId)
        val entity = state.entities.get(entityId)
        val towerComponent = entity?.tower
        if (entity == null || towerComponent == null) {
            state.lastCommandOrError = "unknown_tower_entity:${command.towerEntityId}"
            return
        }
        val position = entity.position?.tile
        if (position == null) {
            state.lastCommandOrError = "tower_missing_position:${command.towerEntityId}"
            return
        }
        val tower = state.registry.towers[towerComponent.towerId]
        if (tower == null) {
            state.lastCommandOrError = "unknown_tower:${towerComponent.towerId}"
            return
        }
        val refund = calculateSellRefund(tower, towerComponent)
        if (refund == null) {
            state.lastCommandOrError = "invalid_tower_upgrade_state:${command.towerEntityId}"
            return
        }

        var refundedInventory = state.inventory
        for ((resourceId, amount) in refund) {
            if (!refundedInventory.canAdd(resourceId, amount)) {
                state.lastCommandOrError = "refund_capacity:$resourceId"
                return
            }
            refundedInventory = refundedInventory.add(resourceId, amount)
        }

        state.world.clearOccupancy(position, entityId.value)
        state.entities.remove(entityId)
        state.defense = state.defense.copy(towerMetrics = state.defense.towerMetrics - entityId.value)
        goalField = rebuildAfterWalkabilityChange()
        state.inventory = refundedInventory
        state.lastCommandOrError = "sold:${command.towerEntityId}"
    }

    private fun setTowerTargetingMode(command: SetTowerTargetingModeCommand) {
        val entityId = EntityId(command.towerEntityId)
        val entity = state.entities.get(entityId)
        val towerComponent = entity?.tower
        if (entity == null || towerComponent == null) {
            state.lastCommandOrError = "unknown_tower_entity:${command.towerEntityId}"
            return
        }
        state.entities.update(entityId) {
            it.copy(tower = towerComponent.copy(targetingMode = command.targetingMode))
        }
        state.lastCommandOrError = "targeting_mode:${command.towerEntityId}:${command.targetingMode.id}"
    }

    /**
     * Reconstructs only the content costs that were actually applied to this tower. Upgrade
     * transitions are sequential within a branch, so the component's current tier identifies
     * exactly the native tier costs included in the cumulative sell refund.
     */
    private fun calculateSellRefund(tower: dev.myengine.content.TowerContent, component: TowerComponent): Map<String, Int>? {
        val spent = linkedMapOf<String, Long>()

        fun addSpend(resourceId: String, amount: Int): Boolean {
            return try {
                spent[resourceId] = Math.addExact(spent[resourceId] ?: 0L, amount.toLong())
                true
            } catch (_: ArithmeticException) {
                false
            }
        }

        if (!addSpend(tower.costResource, tower.costAmount)) return null
        val upgradeBranch = component.upgradeBranch
        if (upgradeBranch != null) {
            for (tierNumber in 1..component.upgradeTier) {
                val tier = tower.upgradeTiers[TowerUpgradeTier.key(upgradeBranch, tierNumber)] ?: return null
                if (!addSpend(tier.costResource, tier.costAmount)) return null
            }
        }

        val refunds = linkedMapOf<String, Int>()
        for ((resourceId, amount) in spent.toSortedMap()) {
            val refund = try {
                BigDecimal.valueOf(amount)
                    .multiply(tower.sellRefundRatio)
                    .setScale(0, RoundingMode.DOWN)
                    .intValueExact()
            } catch (_: ArithmeticException) {
                return null
            }
            if (refund > 0) refunds[resourceId] = refund
        }
        return refunds
    }

    private fun canApplyUpgrade(current: TowerComponent, command: UpgradeTowerCommand): Boolean {
        val branch = current.upgradeBranch
        return if (branch == null) {
            command.tier == 1
        } else {
            command.branch == branch && command.tier == current.upgradeTier + 1
        }
    }

    /**
     * The terminal decision runs only at this end-of-tick boundary, after spawning, towers,
     * reward deposits, and enemy movement/leaks have all settled. Losses win ties over victory.
     */
    private fun evaluateTerminalState() {
        if (state.run.isTerminal) return
        val terminal = when {
            state.defense.coreHealth <= 0 -> RunStatus.LOST to TerminalReason.CORE_HEALTH_EXHAUSTED
            map.terminalRules.leakBudget?.let { state.defense.metrics.enemiesLeaked >= it } == true ->
                RunStatus.LOST to TerminalReason.LEAK_BUDGET_EXHAUSTED
            map.terminalRules.winCondition == MapWinCondition.FINITE_WAVES &&
                state.registry.waves.isNotEmpty() &&
                state.registry.waves.keys.all(state.defense.spawnedWaveIds::contains) &&
                state.entities.byTag("enemy").isEmpty() -> RunStatus.WON to TerminalReason.ALL_WAVES_CLEARED
            else -> null
        }
        terminal?.let { (status, reason) ->
            state.run = RunState(
                status = status,
                terminalReason = reason,
                terminalTick = state.tick,
                summary = currentRunSummary(),
            )
        }
    }

    private fun currentRunSummary(): RunSummary = RunSummary(
        waves = state.defense.spawnedWaveIds.size,
        kills = state.defense.metrics.enemiesKilled,
        leaks = state.defense.metrics.enemiesLeaked,
        resources = state.inventory.resources.toSortedMap(),
        ticks = state.tick,
    )

    private fun updateProduction() {
        state.producers = state.producers.map { producer ->
            val result = producerSystem.tick(producer, state.inventory)
            state.inventory = result.inventory
            result.producer
        }
    }

    /** Single committed-world cache hook paired with GoalField's prospective placement probe. */
    private fun rebuildAfterWalkabilityChange(): GoalField =
        GoalField.rebuildAfterWalkabilityChange(state.world, core, spawns).field
}

private fun VisualAssetRef.toRenderAssetRef(): RenderAssetRef = RenderAssetRef(path = path, atlasKey = atlasKey)

object SandboxSaveCodec {
    const val SAVE_VERSION: Int = 9

    fun encode(state: SandboxState, seed: Long, pendingCommands: List<EngineCommand> = emptyList()): String {
        val props = Properties()
        props["saveVersion"] = SAVE_VERSION.toString()
        props["engineVersion"] = EngineInfo.SCAFFOLD_PHASE.toString()
        props["packId"] = state.registry.manifest.id
        props["packVersion"] = state.registry.manifest.version
        props["mapId"] = state.mapId
        props["contentVersion"] = state.registry.manifest.version
        props["seed"] = seed.toString()
        props["tick"] = state.tick.value.toString()
        props["runStatus"] = state.run.status.name
        props["terminalReason"] = state.run.terminalReason?.name.orEmpty()
        props["terminalTick"] = state.run.terminalTick?.value?.toString().orEmpty()
        props["runSummary"] = state.run.summary?.let { summary ->
            listOf(summary.waves, summary.kills, summary.leaks, summary.ticks.value).joinToString(",")
        }.orEmpty()
        props["runResources"] = state.run.summary?.resources?.toSortedMap()?.entries
            ?.joinToString(";") { "${it.key}:${it.value}" }
            .orEmpty()
        props["coreHealth"] = state.defense.coreHealth.toString()
        props["spawnedWaves"] = state.defense.spawnedWaveIds.sorted().joinToString(",")
        props["metrics"] = listOf(
            state.defense.metrics.enemiesSpawned,
            state.defense.metrics.enemiesKilled,
            state.defense.metrics.enemiesLeaked,
            state.defense.metrics.coreDamage,
            state.defense.metrics.towerShots,
        ).joinToString(",")
        props["towerMetrics"] = state.defense.towerMetrics.toSortedMap().entries.joinToString(";") { (entityId, metrics) ->
            "$entityId|${metrics.actualDamage}|${metrics.kills}"
        }
        props["inventory"] = state.inventory.resources.toSortedMap().entries.joinToString(";") { "${it.key}:${it.value}" }
        props["producers"] = state.producers.sortedBy { it.id }.joinToString(";") { "${it.id}|${it.recipeId}|${it.progressTicks}" }
        props["nextEntityId"] = state.entities.nextIdSnapshot().toString()
        props["entities"] = state.entities.all().joinToString(";") { entity ->
            val path = entity.movement?.path?.joinToString("/") { "${it.x}:${it.y}" }.orEmpty()
            listOf(
                entity.id.value,
                entity.type,
                entity.position?.tile?.x ?: "",
                entity.position?.tile?.y ?: "",
                entity.health?.current ?: "",
                entity.health?.max ?: "",
                entity.tower?.towerId ?: "",
                entity.tower?.cooldownRemaining ?: "",
                entity.attack?.range ?: "",
                entity.attack?.damage ?: "",
                entity.attack?.cooldownTicks ?: "",
                path,
                entity.movement?.pathIndex ?: "",
                entity.tower?.upgradeBranch ?: "",
                entity.tower?.upgradeTier?.takeIf { entity.tower?.upgradeBranch != null } ?: "",
                entity.tower?.targetingMode?.id ?: "",
                entity.statusEffects.sortedBy { it.effectId }.joinToString(",") { effect ->
                    "${effect.effectId}~${effect.remainingTicks}~${effect.stacks}"
                },
            ).joinToString("|")
        }
        props["pendingCommands"] = pendingCommands.joinToString(";") { cmd ->
            listOf(cmd.type, cmd.id.value, cmd.scheduledTick.value, cmd.actorId ?: "", cmd.stablePayload()).joinToString("|")
        }
        return StringWriter().also { props.store(it, "MyEngine sandbox save") }.toString()
    }

    fun decode(text: String, registry: ContentRegistry): SandboxState {
        val props = Properties().also { it.load(StringReader(text)) }
        val version = requireSupportedSaveVersion(props)
        val mapId = if (version >= 4) {
            require(props.getProperty("packId") == registry.manifest.id) {
                "Save pack '${props.getProperty("packId")}' does not match loaded pack '${registry.manifest.id}'."
            }
            require(props.getProperty("contentVersion") == registry.manifest.version) {
                "Save content version '${props.getProperty("contentVersion")}' does not match loaded content version '${registry.manifest.version}'."
            }
            props.getProperty("mapId")?.takeIf { it.isNotBlank() }
                ?: error("Save version 4+ is missing mapId.")
        } else {
            null
        }
        val state = SandboxGame.createInitialState(registry = registry, mapId = mapId)
        state.tick = Tick(props.getProperty("tick").toLong())
        val metrics = props.getProperty("metrics", "0,0,0,0,0").split(',').map { it.toInt() }
        state.defense = DefenseState(
            coreHealth = props.getProperty("coreHealth").toInt(),
            spawnedWaveIds = props.getProperty("spawnedWaves", "").split(',').filter { it.isNotBlank() }.toSet(),
            metrics = dev.myengine.defense.DefenseMetrics(metrics[0], metrics[1], metrics[2], metrics[3], metrics[4]),
            towerMetrics = if (version >= 6) parseTowerMetrics(props.getProperty("towerMetrics", "")) else emptyMap(),
        )
        state.inventory = Inventory(parseResources(props.getProperty("inventory", "")))
        state.producers = props.getProperty("producers", "")
            .split(';')
            .filter { it.isNotBlank() }
            .map {
                val parts = it.split('|')
                Producer(parts[0], parts[1], parts[2].toInt())
            }
        state.run = if (version >= 5) parseRunState(props) else RunState()
        val entities = parseEntities(props.getProperty("entities", ""), registry, version)
        state.world.positions().forEach { state.world.clearOccupancy(it) }
        val loadedStore = EntityStore(props.getProperty("nextEntityId").toLong(), entities)
        entities.filter { it.tower != null }.forEach { entity ->
            entity.position?.let { state.world.occupy(it.tile, entity.id.value) }
        }
        return state.copy(entities = loadedStore)
    }

    /**
     * Extracts the runtime's not-yet-drained pending commands from a save [text], independent of
     * [decode]'s `SandboxState` reconstruction. Absent on v1 saves (`props.getProperty` defaults
     * to `""`), which parses to an empty list — matching today's v1 behavior with no special-casing.
     */
    fun decodePendingCommands(text: String): List<EngineCommand> {
        val props = Properties().also { it.load(StringReader(text)) }
        val version = requireSupportedSaveVersion(props)
        if (version >= 5) parseRunState(props)
        return parsePendingCommands(props.getProperty("pendingCommands", ""))
    }

    private fun requireSupportedSaveVersion(props: Properties): Int {
        val version = props.getProperty("saveVersion")?.toIntOrNull()
        require(version != null && version in 1..SAVE_VERSION) { "Unsupported save version '$version'." }
        return version
    }

    private fun parseRunState(props: Properties): RunState {
        val status = RunStatus.values().firstOrNull { it.name == props.getProperty("runStatus") }
            ?: error("Save version 5 has an invalid runStatus.")
        if (status == RunStatus.ACTIVE) {
            require(props.getProperty("terminalReason").isNullOrBlank()) { "Active run cannot contain terminalReason." }
            require(props.getProperty("terminalTick").isNullOrBlank()) { "Active run cannot contain terminalTick." }
            require(props.getProperty("runSummary").isNullOrBlank()) { "Active run cannot contain runSummary." }
            require(props.getProperty("runResources").isNullOrBlank()) { "Active run cannot contain runResources." }
            return RunState()
        }

        val reason = TerminalReason.values().firstOrNull { it.name == props.getProperty("terminalReason") }
            ?: error("Terminal run is missing or has an invalid terminalReason.")
        require(
            (status == RunStatus.WON && reason == TerminalReason.ALL_WAVES_CLEARED) ||
                (status == RunStatus.LOST && reason != TerminalReason.ALL_WAVES_CLEARED),
        ) { "Run status and terminal reason do not agree." }
        val terminalTick = props.getProperty("terminalTick")?.toLongOrNull()?.let(::Tick)
            ?: error("Terminal run is missing or has an invalid terminalTick.")
        val summaryValues = props.getProperty("runSummary")?.split(',')?.map { it.toLongOrNull() }
        require(summaryValues?.size == 4 && summaryValues.all { it != null && it >= 0 }) {
            "Terminal run is missing or has an invalid runSummary."
        }
        val summary = RunSummary(
            waves = summaryValues[0]!!.toInt(),
            kills = summaryValues[1]!!.toInt(),
            leaks = summaryValues[2]!!.toInt(),
            resources = parseResources(props.getProperty("runResources", "")).toSortedMap(),
            ticks = Tick(summaryValues[3]!!),
        )
        return RunState(status, reason, terminalTick, summary)
    }

    private fun parsePendingCommands(text: String): List<EngineCommand> =
        text.split(';').filter { it.isNotBlank() }.map { encoded ->
            val parts = encoded.split('|')
            val type = parts[0]
            val id = dev.myengine.core.CommandId(parts[1].toLong())
            val scheduledTick = Tick(parts[2].toLong())
            val actorId = parts[3].toLongOrNull()
            val payload = parts[4]
            if (type == "build_tower") {
                val payloadParts = payload.split(':')
                BuildTowerCommand(id, scheduledTick, payloadParts[0], TileCoordinate(payloadParts[1].toInt(), payloadParts[2].toInt()), actorId)
            } else if (type == "upgrade_tower") {
                val payloadParts = payload.split(':')
                UpgradeTowerCommand(id, scheduledTick, payloadParts[0].toLong(), payloadParts[1], payloadParts[2].toInt(), actorId)
            } else if (type == "sell_tower") {
                SellTowerCommand(id, scheduledTick, payload.toLong(), actorId)
            } else if (type == "set_tower_targeting_mode") {
                val payloadParts = payload.split(':')
                require(payloadParts.size == 2) { "Invalid targeting mode command payload '$payload'." }
                val mode = TargetingMode.fromId(payloadParts[1])
                    ?: error("Unknown targeting mode '${payloadParts[1]}'.")
                SetTowerTargetingModeCommand(id, scheduledTick, payloadParts[0].toLong(), mode, actorId)
            } else if (type == "call_wave_early") {
                CallWaveEarlyCommand(id, scheduledTick, actorId)
            } else {
                dev.myengine.core.TextCommand(id, scheduledTick, type, payload, actorId)
            }
        }

    private fun parseResources(text: String): Map<String, Int> =
        text.split(';').filter { it.isNotBlank() }.associate {
            val parts = it.split(':')
            parts[0] to parts[1].toInt()
        }

    private fun parseTowerMetrics(text: String): Map<Long, dev.myengine.defense.TowerDefenseMetrics> =
        text.split(';').filter { it.isNotBlank() }.associate { encoded ->
            val parts = encoded.split('|')
            require(parts.size == 3) { "Invalid tower metrics entry '$encoded'." }
            val entityId = parts[0].toLong()
            val actualDamage = parts[1].toLong()
            val kills = parts[2].toInt()
            require(entityId > 0 && actualDamage >= 0 && kills >= 0) { "Invalid tower metrics entry '$encoded'." }
            entityId to dev.myengine.defense.TowerDefenseMetrics(actualDamage, kills)
        }.toSortedMap()

    private fun parseEntities(text: String, registry: ContentRegistry, version: Int): List<Entity> =
        text.split(';').filter { it.isNotBlank() }.map { encoded ->
            val parts = encoded.split('|')
            val id = EntityId(parts[0].toLong())
            val type = parts[1]
            val x = parts[2].toIntOrNull()
            val y = parts[3].toIntOrNull()
            val health = parts[4].toIntOrNull()
            val maxHealth = parts[5].toIntOrNull()
            val towerId = parts[6].takeIf { it.isNotBlank() }
            val cooldown = parts[7].toIntOrNull()
            val range = parts[8].toIntOrNull()
            val damage = parts[9].toIntOrNull()
            val cooldownTicks = parts[10].toIntOrNull()
            val path = parts[11].split('/').filter { it.isNotBlank() }.map {
                val xy = it.split(':')
                TilePosition(xy[0].toInt(), xy[1].toInt())
            }
            val pathIndex = parts[12].toIntOrNull()
            val upgradeBranch = parts.getOrNull(13)?.takeIf { it.isNotBlank() }
            val upgradeTier = parts.getOrNull(14)?.toIntOrNull() ?: 0
            val targetingMode = if (towerId == null) {
                TargetingMode.NEAREST
            } else if (version >= 7) {
                TargetingMode.fromId(parts.getOrNull(15).orEmpty())
                    ?: error("Save version 7+ has an invalid targeting mode for tower '$towerId'.")
            } else {
                registry.requireTower(towerId).targetingMode
            }
            val statusEffects = if (version >= 9) {
                parseStatusEffects(parts.getOrNull(16).orEmpty(), registry)
            } else {
                emptyList()
            }
            Entity(
                id = id,
                type = type,
                tags = when {
                    type.startsWith("tower") -> setOf("tower")
                    type.startsWith("enemy") -> setOf("enemy")
                    else -> emptySet()
                },
                position = if (x != null && y != null) PositionComponent(TilePosition(x, y)) else null,
                health = if (health != null && maxHealth != null) HealthComponent(health, maxHealth) else null,
                tower = if (towerId != null && cooldown != null) TowerComponent(towerId, cooldown, upgradeBranch, upgradeTier, targetingMode) else null,
                attack = if (range != null && damage != null && cooldownTicks != null) AttackComponent(range, damage, cooldownTicks) else null,
                statusEffects = statusEffects,
                // Wave routing is a derived GoalField cache.  Older v6 saves can still carry a
                // serialized per-enemy path; discard it and rebuild from restored world occupancy.
                movement = if (type.startsWith("enemy") && x != null && y != null) {
                    MovementComponent()
                } else if (path.isNotEmpty() && pathIndex != null) {
                    MovementComponent(path, pathIndex)
                } else {
                    null
                },
            )
        }

    private fun parseStatusEffects(text: String, registry: ContentRegistry): List<StatusEffectComponent> =
        text.split(',').filter { it.isNotBlank() }.map { encoded ->
            val parts = encoded.split('~')
            require(parts.size == 3) { "Invalid status effect entry '$encoded'." }
            val effectId = parts[0]
            require(effectId.isNotBlank() && registry.effects.containsKey(effectId)) {
                "Unknown status effect '$effectId' in save."
            }
            val remainingTicks = parts[1].toIntOrNull()
                ?: error("Invalid remaining ticks for status effect '$effectId'.")
            val stacks = parts[2].toIntOrNull()
                ?: error("Invalid stacks for status effect '$effectId'.")
            StatusEffectComponent(effectId, remainingTicks, stacks)
        }.sortedBy { it.effectId }
}

data class SandboxScenarioResult(
    val hash: String,
    val snapshot: EngineSnapshot,
    val saveText: String,
    val metrics: dev.myengine.defense.DefenseMetrics,
)

object SandboxGame {
    val descriptor: SandboxDescriptor = SandboxDescriptor()

    fun banner(): String = "${EngineInfo.banner()} / ${descriptor.id}"

    fun loadRegistry(root: Path = contentRoot(), difficultyId: String? = null): ContentRegistry {
        val result = ContentPackLoader.load(root)
        require(result.isValid) { result.errors.joinToString("\n") }
        val registry = result.registry!!
        return difficultyId?.let(registry::resolveDifficulty) ?: registry
    }

    fun createInitialState(
        registry: ContentRegistry = loadRegistry(),
        difficultyId: String? = null,
        mapId: String? = null,
    ): SandboxState {
        val effectiveRegistry = difficultyId?.let(registry::resolveDifficulty) ?: registry
        val map = effectiveRegistry.requireMap(mapId)
        val world = createWorld(effectiveRegistry, map)
        return SandboxState(
            tick = Tick(0),
            registry = effectiveRegistry,
            mapId = map.id,
            world = world,
            entities = EntityStore(),
            inventory = Inventory(mapOf("bolt" to 6)),
            producers = listOf(Producer("generator-1", "bolt-generator")),
            defense = DefenseState(coreHealth = 20),
        )
    }

    fun createRuntime(
        registry: ContentRegistry = loadRegistry(),
        difficultyId: String? = null,
        mapId: String? = null,
    ): SandboxRuntime =
        SandboxRuntime(createInitialState(registry, difficultyId, mapId))

    /**
     * Canonical replay/benchmark scenario. The pulse tower at (30,32) is adjacent to the core and
     * never reaches the (1,1)->(32,32) enemy corridor within the 35-tick budget, so it kills nothing.
     * Its hash is the long-standing baseline; kept stable on purpose. Use [runScriptedKillScenario]
     * to exercise the kill+reward path.
     */
    fun runScriptedScenario(seed: Long = 7, difficultyId: String? = null, mapId: String? = null): SandboxScenarioResult =
        runScriptedScenario(TilePosition(30, 32), seed, difficultyId, mapId)

    /**
     * Second canonical scenario that DOES exercise kills and the reward-deposit path. The pulse
     * tower at (2,2) sits next to the enemy spawn (1,1), so wave enemies die within the 35-tick
     * budget and their content-derived rewards are deposited into the inventory. Proven to kill by
     * SandboxRewardDepositTest / SandboxVerticalSliceTest; its hash is a stable, kill-bearing gate.
     */
    fun runScriptedKillScenario(seed: Long = 7, difficultyId: String? = null, mapId: String? = null): SandboxScenarioResult =
        runScriptedScenario(TilePosition(2, 2), seed, difficultyId, mapId)

    private fun runScriptedScenario(
        towerPosition: TilePosition,
        seed: Long,
        difficultyId: String?,
        mapId: String?,
    ): SandboxScenarioResult {
        val registry = loadRegistry(difficultyId = difficultyId)
        val runtime = createRuntime(registry, mapId = mapId)
        runtime.submit(BuildTowerCommand(dev.myengine.core.CommandId(1), Tick(1), "pulse", TileCoordinate(towerPosition.x, towerPosition.y)))
        runtime.step(35)
        val save = SandboxSaveCodec.encode(runtime.state, seed)
        return SandboxScenarioResult(runtime.state.stableHash(), runtime.snapshot(), save, runtime.state.defense.metrics)
    }

    fun contentRoot(): Path {
        val cwd = Paths.get("").toAbsolutePath()
        val roots = generateSequence(cwd) { it.parent }.take(6).toList()
        val candidates = roots.flatMap {
            listOf(
                it.resolve(Paths.get("content", "sandbox")),
                it.resolve(Paths.get("games", "sandbox", "content", "sandbox")),
            )
        }
        return candidates.firstOrNull(Files::exists)
            ?: cwd.resolve(Paths.get("games", "sandbox", "content", "sandbox"))
    }

    private fun createWorld(registry: ContentRegistry, map: MapContent): TileWorld {
        val terrain = registry.tiles.values.associate {
            it.id to TerrainRule(it.id, it.buildable, it.blocksMovement, it.isCore)
        }
        val worldTiles = map.terrainRows.flatMap { row ->
            row.map { symbol ->
                val mapping = map.terrainMapping.getValue(symbol)
                WorldTile(
                    terrainId = mapping.terrainId,
                    resourceNode = mapping.resourceNode?.let { ResourceNode(it.resourceId, it.amount) },
                )
            }
        }
        return TileWorld(WorldSize(map.width, map.height), terrain, worldTiles)
    }
}
