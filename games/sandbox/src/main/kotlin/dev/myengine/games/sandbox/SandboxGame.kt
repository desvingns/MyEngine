package dev.myengine.games.sandbox

import dev.myengine.content.ContentPackLoader
import dev.myengine.content.ContentRegistry
import dev.myengine.content.DamageTypeContent
import dev.myengine.content.EndlessWaveGenerator
import dev.myengine.content.IncidentEffectDescriptor
import dev.myengine.content.MapContent
import dev.myengine.content.MapWinCondition
import dev.myengine.content.GeneratedMap
import dev.myengine.content.ProceduralMapGenerator
import dev.myengine.content.ProceduralMapParameters
import dev.myengine.content.TowerUpgradeTier
import dev.myengine.content.VisualAssetRef
import dev.myengine.content.WaveContent
import dev.myengine.content.HudStringKeys
import dev.myengine.ai.GoalField
import dev.myengine.ai.Job
import dev.myengine.ai.JobBoard
import dev.myengine.ai.JobCompletionEffect
import dev.myengine.ai.JobCompletionEffectSink
import dev.myengine.ai.JobExecutionSystem
import dev.myengine.ai.JobStatus
import dev.myengine.ai.NeedsSystem
import dev.myengine.ai.HaulDestinationKind
import dev.myengine.ai.stableSortKey
import dev.myengine.logistics.HaulingSystem
import dev.myengine.core.CommandQueue
import dev.myengine.core.CombatEvents
import dev.myengine.core.EngineCommand
import dev.myengine.core.EngineInfo
import dev.myengine.core.GameplayEvent
import dev.myengine.core.HashableState
import dev.myengine.core.RunState
import dev.myengine.core.RunStatus
import dev.myengine.core.RunSummary
import dev.myengine.core.SeededRandom
import dev.myengine.core.StableHash
import dev.myengine.core.TerminalReason
import dev.myengine.core.Tick
import dev.myengine.core.MovementMode
import dev.myengine.core.command.BuildTowerCommand
import dev.myengine.core.command.CancelBlueprintCommand
import dev.myengine.core.command.PlaceBlueprintCommand
import dev.myengine.core.command.PlaceBuildingCommand
import dev.myengine.core.command.RemoveBuildingCommand
import dev.myengine.core.command.CallWaveEarlyCommand
import dev.myengine.core.command.DefineStockpileZoneCommand
import dev.myengine.core.command.DesignateHarvestNodeCommand
import dev.myengine.core.command.RemoveHarvestDesignationCommand
import dev.myengine.core.command.RemoveStockpileZoneCommand
import dev.myengine.core.command.ResearchCommand
import dev.myengine.core.command.SellTowerCommand
import dev.myengine.core.command.SetTowerTargetingModeCommand
import dev.myengine.core.command.TileCoordinate
import dev.myengine.core.command.TargetingMode
import dev.myengine.core.command.UpgradeTowerCommand
import dev.myengine.core.command.UpdateStockpileZoneCommand
import dev.myengine.defense.DefenseRuntime
import dev.myengine.defense.DefenseState
import dev.myengine.defense.TowerPlacementResult
import dev.myengine.entities.AttackComponent
import dev.myengine.entities.Entity
import dev.myengine.entities.EntityId
import dev.myengine.entities.EntityStore
import dev.myengine.entities.EnemyComponent
import dev.myengine.entities.HealthComponent
import dev.myengine.entities.InventoryComponent
import dev.myengine.entities.JobActorComponent
import dev.myengine.entities.NeedsComponent
import dev.myengine.entities.WorkerComponent
import dev.myengine.entities.MovementComponent
import dev.myengine.entities.PositionComponent
import dev.myengine.entities.StatusEffectComponent
import dev.myengine.entities.TowerComponent
import dev.myengine.logistics.Inventory
import dev.myengine.logistics.Producer
import dev.myengine.logistics.ProducerSystem
import dev.myengine.logistics.ProductionSource
import dev.myengine.logistics.HarvestDesignation
import dev.myengine.logistics.HaulSource
import dev.myengine.logistics.HaulSourceStore
import dev.myengine.logistics.ConstructionSite
import dev.myengine.logistics.ConstructionSiteStore
import dev.myengine.logistics.HaulDestinationSink
import dev.myengine.logistics.BeltItem
import dev.myengine.logistics.BeltCell
import dev.myengine.logistics.BeltLine
import dev.myengine.logistics.BeltTransportState
import dev.myengine.logistics.BeltTransportSystem
import dev.myengine.logistics.StockpileZone
import dev.myengine.logistics.ZoneStore
import dev.myengine.render.DebugOverlay
import dev.myengine.render.EngineSnapshot
import dev.myengine.render.HudBuildTower
import dev.myengine.render.HudLabels
import dev.myengine.render.HudNeedBar
import dev.myengine.render.HudResourceAmount
import dev.myengine.render.HudSnapshot
import dev.myengine.render.HudTowerInfo
import dev.myengine.render.HudTowerTier
import dev.myengine.render.HudWaveCompositionEntry
import dev.myengine.render.RenderEntity
import dev.myengine.render.RenderAssetRef
import dev.myengine.render.RenderZone
import dev.myengine.render.RenderZoneKind
import dev.myengine.render.RenderTile
import dev.myengine.render.TechNodeSnapshot
import dev.myengine.render.TechTreeSnapshot
import dev.myengine.render.TechUnlockSnapshot
import dev.myengine.storyteller.IncidentDirector
import dev.myengine.storyteller.IncidentDirectorState
import dev.myengine.storyteller.IncidentExecution
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
import java.util.Collections

data class SandboxDescriptor(
    val id: String = "sandbox",
    val engineName: String = EngineInfo.NAME,
)

data class SandboxIncidentModifier(
    val amount: Int,
    val remainingTicks: Int,
) {
    init {
        require(amount > 0) { "Incident modifier amount must be positive." }
        require(remainingTicks > 0) { "Incident modifier duration must be positive." }
    }
}

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
    var randomCursor: Long = Long.MIN_VALUE,
    var incidentState: IncidentDirectorState = IncidentDirectorState(),
    var incidentModifiers: Map<String, SandboxIncidentModifier> = emptyMap(),
    var jobBoard: JobBoard = JobBoard(),
    var zones: ZoneStore = ZoneStore(),
    var haulSources: HaulSourceStore = HaulSourceStore(),
    var constructionSites: ConstructionSiteStore = ConstructionSiteStore(),
    var belts: BeltTransportState = BeltTransportState(),
    var researchedTechIds: Set<String> = emptySet(),
) : HashableState {
    override fun appendHash(hash: StableHash) {
        hash.add(tick.value)
        hash.add(registry.manifest.id).add(registry.manifest.version)
        world.appendHash(hash)
        entities.appendHash(hash)
        jobBoard.appendHash(hash)
        zones.appendHash(hash)
        haulSources.appendHash(hash)
        constructionSites.appendHash(hash)
        belts.appendHash(hash)
        if (registry.techNodes.isNotEmpty()) {
            researchedTechIds.sorted().forEach { hash.add("researched-tech").add(it) }
        }
        inventory.appendHash(hash)
        producers.sortedBy { it.id }.forEach {
            hash.add(it.id).add(it.recipeId).add(it.progressTicks)
            it.position?.let { position -> hash.add("producer-position").add(position.x).add(position.y) }
            it.resourceNodePosition?.let { position ->
                hash.add("producer-node").add(position.x).add(position.y)
            }
        }
        hash.add(defense.coreHealth)
        defense.spawnedWaveIds.sorted().forEach(hash::add)
        hash.add(defense.metrics.enemiesSpawned)
            .add(defense.metrics.enemiesKilled)
            .add(defense.metrics.enemiesLeaked)
            .add(defense.metrics.coreDamage)
            .add(defense.metrics.towerShots)
        hash.add("rng-cursor").add(randomCursor)
        val canonicalIncidentState = incidentState.canonical()
        canonicalIncidentState.cooldownUntil.forEach { (id, tick) -> hash.add("cooldown").add(id).add(tick) }
        hash.add(canonicalIncidentState.lastSelectionTick ?: -1L)
            .add(canonicalIncidentState.lastSelectionId ?: "")
        canonicalIncidentState.executions.forEach { execution ->
            hash.add("incident").add(execution.tick).add(execution.incidentId).add(execution.threat)
            execution.effects.forEach { effect ->
                hash.add(effect.type.id)
                when (effect) {
                    is IncidentEffectDescriptor.SpawnWave -> hash.add(effect.waveId)
                    is IncidentEffectDescriptor.ResourceEvent -> hash.add(effect.resourceId).add(effect.amount)
                    is IncidentEffectDescriptor.Modifier -> hash.add(effect.modifierId).add(effect.amount).add(effect.durationTicks)
                }
            }
        }
        incidentModifiers.toSortedMap().forEach { (id, modifier) ->
            hash.add("incident-modifier").add(id).add(modifier.amount).add(modifier.remainingTicks)
        }
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
    seed: Long = 7L,
) {
    private val producerSystem = ProducerSystem(state.registry.recipes) { recipeId ->
        isUnlockAvailable(dev.myengine.content.TechUnlockType.RECIPE, recipeId)
    }
    private data class PendingJobEffect(val worker: EntityId, val job: Job, val effect: JobCompletionEffect)

    private val jobEffects = mutableListOf<PendingJobEffect>()
    private val needsSystem = NeedsSystem(state.registry.needs)
    private val jobExecutionSystem = JobExecutionSystem(
        completionEffectSink = JobCompletionEffectSink { worker, job, effect ->
            jobEffects += PendingJobEffect(worker, job, effect)
        },
        eligibleJob = { it.haul == null },
    )
    private val haulingSystem = HaulingSystem()
    private val beltTransportSystem = BeltTransportSystem()
    private val map = state.registry.requireMap(state.mapId)
    private val spawn = map.primarySpawn.position.let { TilePosition(it.x, it.y) }
    private val spawnRoutes: Map<String, TilePosition> = map.spawns.toSortedMap()
        .mapValues { (_, mapSpawn) -> TilePosition(mapSpawn.position.x, mapSpawn.position.y) }
        .toSortedMap()
    private val spawns = spawnRoutes.values.toList()
    private val core = map.core.let { TilePosition(it.x, it.y) }
    /** Derived cache: rebuilt from authoritative world occupancy after placement and on restore. */
    private var goalField: GoalField = rebuildAfterWalkabilityChange()
    /** Air routes intentionally ignore terrain and occupancy blockers, so this cache is static. */
    private var airGoalField: GoalField = GoalField.buildIgnoringBlockers(state.world, core)
    /** Latest transient combat events, replaced every completed simulation tick. */
    private var combatEvents: CombatEvents = CombatEvents.EMPTY
    private val simulationRandom = SeededRandom.fromSnapshot(
        state.randomCursor.takeIf { it != Long.MIN_VALUE } ?: seed,
    )
    private val incidentDirector = IncidentDirector(
        incidents = state.registry.incidents.values,
        random = simulationRandom,
        initialState = state.incidentState,
    )
    private val incidentInterpreter = SandboxIncidentEffectInterpreter(defenseRuntime)
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
            advanceIncidentModifiers()
            val commandEvents = mutableListOf<GameplayEvent>()
            val scheduledWaveEvents = mutableListOf<GameplayEvent>()
            val incidentWaveEvents = mutableListOf<GameplayEvent>()
            val commands = commandQueue.drainFor(state.tick)
            commands.forEach { applyCommand(it, commandEvents) }
            needsSystem.tick(state.entities, state.jobBoard)
            ensureConstructionJobs()
            executeHauling()
            ensureConstructionJobs()
            executeJobs(commandEvents)
            updateProduction()
            updateBelts()
            state.defense = defenseRuntime.spawnDueWaves(
                state.tick,
                state.defense,
                state.registry,
                state.world,
                state.entities,
                spawn,
                core,
                goalField,
                eventSink = scheduledWaveEvents,
                spawnRoutes = spawnRoutes,
                random = simulationRandom,
                airGoalField = airGoalField,
            )
            state.randomCursor = simulationRandom.snapshot()
            val statusEffectResult = defenseRuntime.updateStatusEffects(
                registry = state.registry,
                entities = state.entities,
                tick = state.tick,
            )
            state.defense = state.defense.record(statusEffectResult.metrics)
            val statusEffectDeposit = depositRewards(state.inventory, statusEffectResult.rewards)
            state.inventory = statusEffectDeposit.inventory
            if (statusEffectDeposit.dropped.isNotEmpty()) {
                state.lastCommandOrError = statusEffectDeposit.dropped.entries
                    .joinToString(",", prefix = "reward_dropped:") { "${it.key}:${it.value}" }
            }
            val towerResult = defenseRuntime.updateTowers(
                state.registry,
                state.entities,
                goalField,
                state.tick,
                airGoalField,
            )
            state.defense = state.defense.record(towerResult.metrics).recordTowerMetrics(towerResult.towerMetrics)
            val deposit = depositRewards(state.inventory, towerResult.rewards)
            state.inventory = deposit.inventory
            if (deposit.dropped.isNotEmpty()) {
                // Non-fatal telemetry: a full inventory is a legitimate game state, not a crash,
                // but earned rewards must not vanish without a trace (surfaces in DebugOverlay).
                state.lastCommandOrError = deposit.dropped.entries
                    .joinToString(",", prefix = "reward_dropped:") { "${it.key}:${it.value}" }
            }
            state.defense = defenseRuntime.updateEnemies(
                registry = state.registry,
                state = state.defense,
                entities = state.entities,
                goalField = goalField,
                world = state.world,
                onStructureDestroyed = { goalField = rebuildAfterWalkabilityChange() },
                airGoalField = airGoalField,
            )
            evaluateTerminalState()
            if (state.run.isTerminal) {
                combatEvents = aggregateGameplayEvents(
                    commandEvents,
                    scheduledWaveEvents,
                    statusEffectResult.events,
                    towerResult.events,
                    incidentWaveEvents,
                )
                return
            }
            val directorBefore = state.incidentState
            val randomBefore = simulationRandom.snapshot()
            val selection = incidentDirector.select(state.tick.value, state.defense.metrics.enemiesSpawned)
            state.randomCursor = simulationRandom.snapshot()
            state.incidentState = incidentDirector.state()
            if (selection != null) {
                val application = incidentInterpreter.apply(
                    selection = selection,
                    state = state,
                    spawn = spawn,
                    core = core,
                    goalField = goalField,
                    airGoalField = airGoalField,
                    tick = state.tick,
                    eventSink = incidentWaveEvents,
                    spawnRoutes = spawnRoutes,
                )
                if (application.applied) {
                    state.lastCommandOrError = "incident_applied:${selection.incidentId}"
                } else {
                    incidentDirector.restore(directorBefore)
                    simulationRandom.restore(randomBefore)
                    state.randomCursor = randomBefore
                    state.incidentState = directorBefore
                    state.lastCommandOrError = "incident_rejected:${application.reason}"
                }
            }
            combatEvents = aggregateGameplayEvents(
                commandEvents,
                scheduledWaveEvents,
                statusEffectResult.events,
                towerResult.events,
                incidentWaveEvents,
            )
        }
    }

    private fun aggregateGameplayEvents(
        commandEvents: List<GameplayEvent>,
        scheduledWaveEvents: List<GameplayEvent>,
        statusEvents: List<GameplayEvent>,
        towerEvents: CombatEvents,
        incidentWaveEvents: List<GameplayEvent>,
    ): CombatEvents {
        val ordered = buildList {
            addAll(commandEvents)
            addAll(scheduledWaveEvents.sortedWith(compareBy<GameplayEvent> { it.contentId.orEmpty() }.thenBy { it.type.id }))
            addAll(statusEvents)
            addAll(towerEvents.gameplayEvents)
            addAll(incidentWaveEvents.sortedWith(compareBy<GameplayEvent> { it.contentId.orEmpty() }.thenBy { it.type.id }))
        }.mapIndexed { ordinal, event -> event.copy(ordinal = ordinal) }
        return CombatEvents(
            shots = towerEvents.shots,
            hits = towerEvents.hits,
            gameplayEvents = Collections.unmodifiableList(ordered),
        )
    }

    private fun advanceIncidentModifiers() {
        state.incidentModifiers = state.incidentModifiers.toSortedMap()
            .mapNotNull { (id, modifier) ->
                if (modifier.remainingTicks <= 1) null
                else id to modifier.copy(remainingTicks = modifier.remainingTicks - 1)
            }
            .toMap()
    }

    private fun executeJobs(eventSink: MutableList<GameplayEvent>) {
        jobEffects.clear()
        jobExecutionSystem.tick(state.world, state.entities, state.jobBoard)
        jobEffects
            .sortedWith(
                compareBy<PendingJobEffect> { it.effect.stableSortKey() }.thenBy { it.job.id },
            )
            .forEach { pending ->
                when (val effect = pending.effect) {
                    is JobCompletionEffect.ResourceDelta -> when {
                        effect.amount >= 0 && state.inventory.canAdd(effect.resourceId, effect.amount) -> {
                            state.inventory = state.inventory.add(effect.resourceId, effect.amount)
                        }
                        effect.amount < 0 && state.inventory.canRemove(effect.resourceId, -effect.amount) -> {
                            state.inventory = state.inventory.remove(effect.resourceId, -effect.amount)
                        }
                        else -> {
                            state.lastCommandOrError = "job_effect_rejected:${effect.resourceId}:${effect.amount}"
                        }
                    }
                    is JobCompletionEffect.SpawnBuilding ->
                        spawnCompletedBuilding(pending.job, effect, eventSink)
                    is JobCompletionEffect.NeedRecovery -> {
                        val target = effect.targetEntityId ?: pending.worker
                        state.entities.update(target) { entity ->
                            entity.copy(needs = entity.needs?.recover(effect.needId, effect.amount))
                        }
                    }
                }
            }
    }

    private fun executeHauling() {
        haulingSystem.tick(
            world = state.world,
            entities = state.entities,
            jobs = state.jobBoard,
            sources = state.haulSources,
            zones = state.zones,
            workers = state.registry.workers,
            destinationSink = HaulDestinationSink { job, position, resourceId, amount ->
                val spec = job.haul ?: return@HaulDestinationSink false
                when (spec.destinationKind) {
                    HaulDestinationKind.STOCKPILE ->
                        state.zones.deposit(spec.destinationZoneId, position, resourceId, amount)
                    HaulDestinationKind.CONSTRUCTION ->
                        state.constructionSites.deposit(spec.destinationZoneId, spec.sourceId, resourceId, amount)
                }
            },
        )
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
                isBoss = it.enemy?.isBoss == true,
            )
        }
        val renderZones = Collections.unmodifiableList(buildList {
            state.zones.allStockpiles().forEach { zone ->
                add(
                    RenderZone(
                        id = zone.id,
                        kind = RenderZoneKind.STOCKPILE,
                        tiles = zone.normalizedTiles,
                        allowedResourceIds = zone.normalizedResourceIds.toList(),
                    ),
                )
            }
            state.zones.allHarvestDesignations().forEach { designation ->
                add(
                    RenderZone(
                        id = designation.id,
                        kind = RenderZoneKind.HARVEST_DESIGNATION,
                        tiles = listOf(designation.position),
                        resourceId = designation.resourceId,
                        jobId = designation.jobId,
                    ),
                )
            }
        })
        val techTree = TechTreeSnapshot(
            state.registry.techNodes.values.sortedBy { it.id }.map { node ->
                val researched = node.id in state.researchedTechIds
                TechNodeSnapshot(
                    id = node.id,
                    costResource = node.costResource,
                    costAmount = node.costAmount,
                    prerequisites = node.prerequisites,
                    unlocks = node.unlocks.map { unlock -> TechUnlockSnapshot(unlock.type.id, unlock.id) },
                    researched = researched,
                    available = !researched && node.prerequisites.all { it in state.researchedTechIds },
                )
            },
        )
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
            zones = renderZones,
            techTree = techTree,
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
        val needBars = state.entities.all()
            .filter { it.needs != null }
            .sortedBy { it.id.value }
            .flatMap { entity ->
                val needs = entity.needs ?: return@flatMap emptyList()
                state.registry.needs.toSortedMap().map { (needId, definition) ->
                    HudNeedBar(
                        entityId = entity.id.value,
                        needId = needId,
                        label = text(definition.displayKey).ifBlank { needId },
                        value = needs.level(needId),
                        threshold = definition.threshold,
                    )
                }
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
            needBars = needBars,
        )
    }

    private fun applyCommand(command: EngineCommand, eventSink: MutableList<GameplayEvent>) {
        when (command) {
            is ResearchCommand -> research(command)
            is BuildTowerCommand -> buildTower(command, eventSink)
            is PlaceBlueprintCommand -> placeBlueprint(command)
            is PlaceBuildingCommand -> placeBuilding(command, eventSink)
            is UpgradeTowerCommand -> upgradeTower(command)
            is SellTowerCommand -> sellTower(command, eventSink)
            is RemoveBuildingCommand -> removeBuilding(command, eventSink)
            is CancelBlueprintCommand -> cancelBlueprint(command)
            is SetTowerTargetingModeCommand -> setTowerTargetingMode(command)
            is CallWaveEarlyCommand -> callWaveEarly(command, eventSink)
            is DefineStockpileZoneCommand -> defineStockpileZone(command)
            is UpdateStockpileZoneCommand -> updateStockpileZone(command)
            is RemoveStockpileZoneCommand -> removeStockpileZone(command)
            is DesignateHarvestNodeCommand -> designateHarvestNode(command)
            is RemoveHarvestDesignationCommand -> removeHarvestDesignation(command)
            else -> state.lastCommandOrError = "ignored:${command.type}"
        }
    }

    private fun isUnlockAvailable(type: dev.myengine.content.TechUnlockType, targetId: String): Boolean {
        val refs = state.registry.techNodes.values
            .flatMap { node -> node.unlocks.map { node.id to it } }
            .filter { (_, ref) -> ref.type == type && ref.id == targetId }
        return refs.isEmpty() || refs.any { (nodeId, _) -> nodeId in state.researchedTechIds }
    }

    private fun research(command: ResearchCommand) {
        val node = state.registry.techNodes[command.nodeId]
        if (node == null) {
            state.lastCommandOrError = "unknown_tech:${command.nodeId}"
            return
        }
        if (command.nodeId in state.researchedTechIds) {
            state.lastCommandOrError = "already_researched:${command.nodeId}"
            return
        }
        if (node.prerequisites.any { it !in state.researchedTechIds }) {
            state.lastCommandOrError = "missing_prerequisite:${command.nodeId}"
            return
        }
        if (!state.inventory.canRemove(node.costResource, node.costAmount)) {
            state.lastCommandOrError = "missing_resource:${node.costResource}"
            return
        }
        state.inventory = state.inventory.remove(node.costResource, node.costAmount)
        state.researchedTechIds = (state.researchedTechIds + command.nodeId).toSortedSet()
        state.lastCommandOrError = "researched:${command.nodeId}"
    }

    private fun defineStockpileZone(command: DefineStockpileZoneCommand) {
        val positionTiles = command.tiles.map(::toTilePosition)
        if (positionTiles.any { !state.world.inBounds(it) }) {
            state.lastCommandOrError = "zone_tile_out_of_bounds:${command.zoneId}"
            return
        }
        if (command.allowedResourceIds.any { it !in state.registry.resources }) {
            state.lastCommandOrError = "unknown_stockpile_resource:${command.zoneId}"
            return
        }
        try {
            state.zones.defineStockpile(
                StockpileZone(command.zoneId, positionTiles, command.allowedResourceIds),
            )
            state.lastCommandOrError = "stockpile_defined:${command.zoneId}"
        } catch (error: IllegalArgumentException) {
            state.lastCommandOrError = "stockpile_rejected:${command.zoneId}:${error.message}"
        }
    }

    private fun updateStockpileZone(command: UpdateStockpileZoneCommand) {
        val positionTiles = command.tiles.map(::toTilePosition)
        if (positionTiles.any { !state.world.inBounds(it) }) {
            state.lastCommandOrError = "zone_tile_out_of_bounds:${command.zoneId}"
            return
        }
        if (command.allowedResourceIds.any { it !in state.registry.resources }) {
            state.lastCommandOrError = "unknown_stockpile_resource:${command.zoneId}"
            return
        }
        try {
            state.zones.updateStockpile(
                StockpileZone(command.zoneId, positionTiles, command.allowedResourceIds),
            )
            state.lastCommandOrError = "stockpile_updated:${command.zoneId}"
        } catch (error: IllegalArgumentException) {
            state.lastCommandOrError = "stockpile_rejected:${command.zoneId}:${error.message}"
        }
    }

    private fun removeStockpileZone(command: RemoveStockpileZoneCommand) {
        state.lastCommandOrError = if (state.zones.removeStockpile(command.zoneId)) {
            "stockpile_removed:${command.zoneId}"
        } else {
            "unknown_stockpile:${command.zoneId}"
        }
    }

    private fun designateHarvestNode(command: DesignateHarvestNodeCommand) {
        val position = toTilePosition(command.position)
        if (!state.world.inBounds(position)) {
            state.lastCommandOrError = "harvest_tile_out_of_bounds:${command.designationId}"
            return
        }
        if (command.resourceId !in state.registry.resources) {
            state.lastCommandOrError = "unknown_harvest_resource:${command.resourceId}"
            return
        }
        val resourceNode = state.world.tileAt(position).tile.resourceNode
        if (resourceNode?.resourceId != command.resourceId) {
            state.lastCommandOrError = "harvest_resource_mismatch:${command.designationId}"
            return
        }
        val jobId = HarvestDesignation.jobIdFor(command.designationId)
        if (state.zones.harvestDesignation(command.designationId) != null || state.jobBoard.get(jobId) != null) {
            state.lastCommandOrError = "duplicate_harvest_designation:${command.designationId}"
            return
        }
        if (state.zones.allHarvestDesignations().any { it.position == position }) {
            state.lastCommandOrError = "duplicate_harvest_tile:${position.x}:${position.y}"
            return
        }
        val designation = HarvestDesignation(command.designationId, command.resourceId, position, jobId)
        state.zones.addHarvestDesignation(designation)
        state.jobBoard.add(
            Job(
                id = jobId,
                type = "harvest_node",
                target = position,
                priority = 0,
            ),
        )
        state.lastCommandOrError = "harvest_designated:${command.designationId}"
    }

    private fun removeHarvestDesignation(command: RemoveHarvestDesignationCommand) {
        val designation = state.zones.harvestDesignation(command.designationId)
        if (designation == null) {
            state.lastCommandOrError = "unknown_harvest_designation:${command.designationId}"
            return
        }
        val job = state.jobBoard.get(designation.jobId)
        if (job?.status == JobStatus.OPEN && job.assignedTo == null) {
            state.jobBoard.remove(designation.jobId)
        }
        state.zones.removeHarvestDesignation(command.designationId)
        state.lastCommandOrError = "harvest_designation_removed:${command.designationId}"
    }

    private fun toTilePosition(coordinate: TileCoordinate): TilePosition =
        TilePosition(coordinate.x, coordinate.y)

    /**
     * Resolves and starts the previewed wave before scheduled spawning. Rejection checks happen
     * before any authoritative operation: a live enemy or an exhausted schedule leaves entities,
     * inventory, and defense state untouched (the diagnostic string is presentation telemetry).
     * A wave whose scheduled boundary has already arrived is also a no-op: only a genuinely early
     * call (`state.tick < wave.startTick`) is allowed to grant its early-call bonus.
     */
    private fun callWaveEarly(
        @Suppress("UNUSED_PARAMETER") command: CallWaveEarlyCommand,
        eventSink: MutableList<GameplayEvent>,
    ) {
        val wave = nextUnspawnedWave(consumeRandom = true)
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
            airGoalField = airGoalField,
            tick = state.tick,
            eventSink = eventSink,
            spawnRoutes = spawnRoutes,
        )
        state.randomCursor = simulationRandom.snapshot()
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
    private fun nextUnspawnedWave(consumeRandom: Boolean = false): WaveContent? {
        val finite = state.registry.waves.values
            .filter { it.id !in state.defense.spawnedWaveIds }
            .minWithOrNull(compareBy<WaveContent> { it.startTick }.thenBy { it.id })
        val endlessCandidate = state.registry.endlessWave?.let { config ->
            var waveNumber = 1
            while (EndlessWaveGenerator.idFor(waveNumber) in state.defense.spawnedWaveIds) {
                if (waveNumber == Int.MAX_VALUE) return@let null
                waveNumber += 1
            }
            Triple(config, waveNumber, EndlessWaveGenerator.startTickFor(config, waveNumber))
        }
        val finiteWins = finite != null && (
            endlessCandidate == null ||
                finite.startTick < endlessCandidate.third ||
                (finite.startTick == endlessCandidate.third && finite.id < EndlessWaveGenerator.idFor(endlessCandidate.second))
            )
        if (finiteWins) return finite
        val (config, waveNumber, _) = endlessCandidate ?: return finite
        val random = if (consumeRandom) simulationRandom else SeededRandom.fromSnapshot(simulationRandom.snapshot())
        return EndlessWaveGenerator.generate(config, waveNumber, random)
    }

    private fun buildTower(command: BuildTowerCommand, eventSink: MutableList<GameplayEvent>) {
        val tower = state.registry.towers[command.towerId]
        if (tower == null) {
            state.lastCommandOrError = "unknown_tower:${command.towerId}"
            return
        }
        if (!isUnlockAvailable(dev.myengine.content.TechUnlockType.TOWER, command.towerId)) {
            state.lastCommandOrError = "locked_tower:${command.towerId}"
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
                eventSink += GameplayEvent(
                    tick = state.tick,
                    type = dev.myengine.core.GameplayEventType.BUILD,
                    sourceEntityId = command.actorId,
                    targetEntityId = result.entityId.value,
                    contentId = command.towerId,
                )
            }
            is TowerPlacementResult.Rejected -> state.lastCommandOrError = result.reason
        }
    }

    private fun placeBlueprint(command: PlaceBlueprintCommand) {
        val building = state.registry.buildings[command.buildingId]
        if (building == null) {
            state.lastCommandOrError = "unknown_building:${command.buildingId}"
            return
        }
        if (!isUnlockAvailable(dev.myengine.content.TechUnlockType.BUILDING, command.buildingId)) {
            state.lastCommandOrError = "locked_building:${command.buildingId}"
            return
        }
        if (building.footprintWidth != 1 || building.footprintHeight != 1) {
            state.lastCommandOrError = "unsupported_building_footprint:${command.buildingId}"
            return
        }
        val position = toTilePosition(command.position)
        if (!state.world.inBounds(position)) {
            state.lastCommandOrError = "position_out_of_bounds"
            return
        }
        val resourceNodePosition = extractorNodePosition(building, position)
        if (!canPlaceBuilding(building, position, resourceNodePosition)) {
            state.lastCommandOrError = "tile_not_buildable"
            return
        }
        if (state.entities.byTag("enemy").any { enemy ->
                enemy.position?.tile == position && enemy.health?.isAlive() == true
            }
        ) {
            state.lastCommandOrError = "occupied_by_enemy"
            return
        }
        if (state.constructionSites.all().any { it.position == position }) {
            state.lastCommandOrError = "duplicate_construction_site:${position.x}:${position.y}"
            return
        }
        val prospective = GoalField.rebuildAfterWalkabilityChange(
            world = state.world,
            goal = core,
            spawns = spawns,
            additionalBlocked = position,
        )
        if (!prospective.keepsAllSpawnsReachable) {
            state.lastCommandOrError = "blocks_spawn_path"
            return
        }
        val siteId = constructionSiteId(command.id.value)
        try {
            state.constructionSites.add(
                ConstructionSite(
                    id = siteId,
                    buildingId = building.id,
                    position = position,
                    materialResourceId = building.costResource,
                    requiredAmount = building.costAmount,
                ),
            )
            state.lastCommandOrError = "blueprint_placed:$siteId"
        } catch (error: IllegalArgumentException) {
            state.lastCommandOrError = "blueprint_rejected:$siteId:${error.message}"
        }
    }

    private fun ensureConstructionJobs() {
        state.constructionSites.all().forEach { site ->
            val building = state.registry.buildings[site.buildingId] ?: return@forEach
            val constructionJobs = state.jobBoard.all().filter { job ->
                val spec = job.haul
                spec?.destinationKind == HaulDestinationKind.CONSTRUCTION &&
                    spec.destinationZoneId == site.id
            }
            if (site.remainingAmount > 0) {
                constructionJobs
                    .filter { it.status == JobStatus.FAILED || (it.status == JobStatus.OPEN && it.failureReason in setOf("source_unavailable", "source_pickup_failed")) }
                    .forEach { failed ->
                        state.haulSources.release(failed.id)
                        state.jobBoard.remove(failed.id)
                    }
                val active = state.jobBoard.all().any { job ->
                    val spec = job.haul
                    spec?.destinationKind == HaulDestinationKind.CONSTRUCTION &&
                        spec.destinationZoneId == site.id &&
                        job.status in setOf(JobStatus.OPEN, JobStatus.CLAIMED, JobStatus.IN_PROGRESS)
                }
                if (!active) {
                    val source = state.haulSources.all()
                        .sortedBy { it.id }
                        .firstOrNull { it.available(site.materialResourceId) >= site.remainingAmount }
                    if (source != null) {
                        val jobId = constructionHaulJobId(site.id)
                        if (state.jobBoard.get(jobId) == null) {
                            state.jobBoard.add(
                                Job(
                                    id = jobId,
                                    type = "construction_haul",
                                    target = site.position,
                                    priority = 100,
                                    haul = dev.myengine.ai.HaulJobSpec(
                                        sourceId = source.id,
                                        resourceId = site.materialResourceId,
                                        amount = site.remainingAmount,
                                        destinationZoneId = site.id,
                                        destinationKind = HaulDestinationKind.CONSTRUCTION,
                                    ),
                                ),
                            )
                        }
                    }
                }
            } else {
                val buildJobId = constructionBuildJobId(site.id)
                val buildJob = state.jobBoard.get(buildJobId)
                if (buildJob == null || buildJob.status == JobStatus.FAILED || buildJob.status == JobStatus.DONE) {
                    if (buildJob != null) state.jobBoard.remove(buildJobId)
                    state.jobBoard.add(
                        Job(
                            id = buildJobId,
                            type = "construction_build",
                            target = site.position,
                            priority = 100,
                            workTicks = building.buildWorkTicks,
                            completionEffects = listOf(JobCompletionEffect.SpawnBuilding(building.id, site.id)),
                        ),
                    )
                }
            }
        }
    }

    private fun spawnCompletedBuilding(
        job: Job,
        effect: JobCompletionEffect.SpawnBuilding,
        eventSink: MutableList<GameplayEvent>,
    ) {
        val site = state.constructionSites.get(effect.siteId)
        val building = state.registry.buildings[effect.buildingId]
        if (site == null || building == null || site.buildingId != building.id || site.position != job.target || site.remainingAmount != 0) {
            state.jobBoard.remove(job.id)
            state.lastCommandOrError = "construction_completion_rejected:${effect.siteId}"
            return
        }
        val resourceNodePosition = extractorNodePosition(building, site.position)
        if (!canPlaceBuilding(building, site.position, resourceNodePosition) ||
            state.entities.byTag("enemy").any { it.position?.tile == site.position && it.health?.isAlive() == true }
        ) {
            state.jobBoard.remove(job.id)
            state.lastCommandOrError = "construction_tile_unavailable:${effect.siteId}"
            return
        }
        val entity = state.entities.create("building:${building.id}", setOf("building")) { id ->
            Entity(
                id = id,
                type = "building:${building.id}",
                tags = setOf("building"),
                position = PositionComponent(site.position),
                health = HealthComponent(building.maxHealth, building.maxHealth),
            )
        }
        state.world.occupy(site.position, entity.id.value)
        addExtractorProducer(building, entity.id.value, site.position, resourceNodePosition)
        goalField = rebuildAfterWalkabilityChange()
        state.constructionSites.remove(site.id)
        state.jobBoard.all()
            .filter { job ->
                val spec = job.haul
                spec?.destinationKind == HaulDestinationKind.CONSTRUCTION && spec.destinationZoneId == site.id
            }
            .forEach { state.jobBoard.remove(it.id) }
        state.jobBoard.remove(job.id)
        state.lastCommandOrError = "built:${entity.id.value}"
        eventSink += GameplayEvent(
            tick = state.tick,
            type = dev.myengine.core.GameplayEventType.BUILD,
            targetEntityId = entity.id.value,
            contentId = building.id,
        )
    }

    private fun cancelBlueprint(command: CancelBlueprintCommand) {
        val site = state.constructionSites.get(command.siteId)
        if (site == null) {
            state.lastCommandOrError = "unknown_construction_site:${command.siteId}"
            return
        }
        val jobs = state.jobBoard.all().filter { job ->
            val spec = job.haul
            (spec?.destinationKind == HaulDestinationKind.CONSTRUCTION && spec.destinationZoneId == site.id) ||
                job.id == constructionBuildJobId(site.id)
        }
        val carried = jobs.mapNotNull { job ->
            val spec = job.haul ?: return@mapNotNull null
            val actorId = job.assignedTo ?: job.reservedBy ?: return@mapNotNull null
            val worker = state.entities.get(actorId) ?: return@mapNotNull null
            val amount = worker.inventory?.resources?.get(spec.resourceId)
                ?.takeIf { it >= spec.amount }
                ?: return@mapNotNull null
            Triple(job, worker, amount)
        }
        val refundBySource = linkedMapOf<String, Int>()
        site.deliveredBySource.forEach { (sourceId, amount) -> refundBySource[sourceId] = (refundBySource[sourceId] ?: 0) + amount }
        carried.forEach { (job, _, amount) ->
            val sourceId = job.haul!!.sourceId
            refundBySource[sourceId] = (refundBySource[sourceId] ?: 0) + amount
        }
        if (refundBySource.keys.any { state.haulSources.get(it) == null }) {
            state.lastCommandOrError = "refund_source_missing:${command.siteId}"
            return
        }
        refundBySource.forEach { (sourceId, amount) ->
            check(state.haulSources.refund(sourceId, site.materialResourceId, amount))
        }
        jobs.forEach { job ->
            state.haulSources.release(job.id)
            val carriedJob = carried.firstOrNull { it.first.id == job.id }
            if (carriedJob != null) {
                val spec = requireNotNull(carriedJob.first.haul)
                val amount = carriedJob.third
                state.entities.update(carriedJob.second.id) {
                    val nextInventory = it.inventory?.let { inventory ->
                        val nextResources = inventory.resources.toMutableMap()
                        val remaining = (nextResources[spec.resourceId] ?: 0) - amount
                        if (remaining > 0) nextResources[spec.resourceId] = remaining
                        else nextResources.remove(spec.resourceId)
                        nextResources.takeIf { resources -> resources.isNotEmpty() }?.let { resources ->
                            inventory.copy(resources = resources)
                        }
                    }
                    it.copy(
                        movement = null,
                        inventory = nextInventory,
                        jobActor = it.jobActor?.copy(assignedJobId = null, workTicks = 0),
                    )
                }
            } else {
                job.assignedTo?.let { actorId ->
                    state.entities.update(actorId) {
                        it.copy(
                            movement = null,
                            jobActor = it.jobActor?.copy(assignedJobId = null, workTicks = 0),
                        )
                    }
                }
            }
            state.jobBoard.remove(job.id)
        }
        state.constructionSites.remove(site.id)
        state.lastCommandOrError = "blueprint_cancelled:${site.id}"
    }

    private fun constructionSiteId(commandId: Long): String = "construction:$commandId"

    private fun constructionHaulJobId(siteId: String): String = "construction-haul:$siteId"

    private fun constructionBuildJobId(siteId: String): String = "construction-build:$siteId"

    private fun extractorNodePosition(
        building: dev.myengine.content.BuildingContent,
        position: TilePosition,
    ): TilePosition? {
        val recipeId = building.producerRecipeId ?: return null
        val recipe = state.registry.recipes[recipeId] ?: return null
        return state.world.extractorNode(position, recipe.outputResource)
    }

    private fun canPlaceBuilding(
        building: dev.myengine.content.BuildingContent,
        position: TilePosition,
        resourceNodePosition: TilePosition?,
    ): Boolean {
        if (building.producerRecipeId == null) return state.world.canBuild(position)
        if (resourceNodePosition == null) return false
        val view = state.world.tileAt(position)
        return if (resourceNodePosition == position) {
            view.tile.occupiedBy == null && !view.terrain.blocksMovement
        } else {
            state.world.canBuild(position)
        }
    }

    private fun addExtractorProducer(
        building: dev.myengine.content.BuildingContent,
        entityId: Long,
        position: TilePosition,
        resourceNodePosition: TilePosition?,
    ) {
        val recipeId = building.producerRecipeId ?: return
        require(resourceNodePosition != null) { "Extractor '${building.id}' requires a resource node." }
        state.producers = (state.producers + Producer(
            id = "extractor:$entityId",
            recipeId = recipeId,
            position = position,
            resourceNodePosition = resourceNodePosition,
        )).sortedBy { it.id }
    }

    /** Keeps a building's producer source reachable by ENG-004 hauling when the building occupies its own tile. */
    private fun extractorOutputPosition(producerPosition: TilePosition): TilePosition {
        val view = state.world.tileAt(producerPosition)
        if (!view.terrain.blocksMovement && view.tile.occupiedBy == null) return producerPosition
        return producerPosition.neighbors4().sorted().firstOrNull { candidate ->
            state.world.inBounds(candidate) && state.world.canOccupy(candidate)
        } ?: producerPosition
    }

    private fun placeBuilding(command: PlaceBuildingCommand, eventSink: MutableList<GameplayEvent>) {
        val building = state.registry.buildings[command.buildingId]
        if (building == null) {
            state.lastCommandOrError = "unknown_building:${command.buildingId}"
            return
        }
        if (!isUnlockAvailable(dev.myengine.content.TechUnlockType.BUILDING, command.buildingId)) {
            state.lastCommandOrError = "locked_building:${command.buildingId}"
            return
        }
        if (building.footprintWidth != 1 || building.footprintHeight != 1) {
            state.lastCommandOrError = "unsupported_building_footprint:${command.buildingId}"
            return
        }
        if (!state.inventory.canRemove(building.costResource, building.costAmount)) {
            state.lastCommandOrError = "missing_resource:${building.costResource}"
            return
        }
        val position = TilePosition(command.position.x, command.position.y)
        if (!state.world.inBounds(position)) {
            state.lastCommandOrError = "position_out_of_bounds"
            return
        }
        val resourceNodePosition = extractorNodePosition(building, position)
        if (!canPlaceBuilding(building, position, resourceNodePosition)) {
            state.lastCommandOrError = "tile_not_buildable"
            return
        }
        if (state.entities.byTag("enemy").any { enemy ->
                enemy.position?.tile == position && enemy.health?.isAlive() == true
            }
        ) {
            state.lastCommandOrError = "occupied_by_enemy"
            return
        }
        val prospective = GoalField.rebuildAfterWalkabilityChange(
            world = state.world,
            goal = core,
            spawns = spawns,
            additionalBlocked = position,
        )
        if (!prospective.keepsAllSpawnsReachable) {
            state.lastCommandOrError = "blocks_spawn_path"
            return
        }

        val entity = state.entities.create("building:${building.id}", setOf("building")) { id ->
            Entity(
                id = id,
                type = "building:${building.id}",
                tags = setOf("building"),
                position = PositionComponent(position),
                health = HealthComponent(building.maxHealth, building.maxHealth),
            )
        }
        state.world.occupy(position, entity.id.value)
        addExtractorProducer(building, entity.id.value, position, resourceNodePosition)
        goalField = rebuildAfterWalkabilityChange()
        state.inventory = state.inventory.remove(building.costResource, building.costAmount)
        state.lastCommandOrError = "placed:${entity.id.value}"
        eventSink += GameplayEvent(
            tick = state.tick,
            type = dev.myengine.core.GameplayEventType.BUILD,
            sourceEntityId = command.actorId,
            targetEntityId = entity.id.value,
            contentId = building.id,
        )
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
                attack = AttackComponent(
                    tier.range,
                    tier.damage,
                    tier.cooldownTicks,
                    entity.attack?.damageTypeId ?: tower.damageTypeId,
                ),
            )
        }
        state.inventory = state.inventory.remove(tier.costResource, tier.costAmount)
        state.lastCommandOrError = "upgraded:${command.towerEntityId}:${tier.branch}:${tier.tier}"
    }

    private fun sellTower(command: SellTowerCommand, eventSink: MutableList<GameplayEvent>) {
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
        eventSink += GameplayEvent(
            tick = state.tick,
            type = dev.myengine.core.GameplayEventType.SELL,
            sourceEntityId = command.actorId,
            targetEntityId = command.towerEntityId,
            contentId = tower.id,
        )
    }

    private fun removeBuilding(command: RemoveBuildingCommand, eventSink: MutableList<GameplayEvent>) {
        val entityId = EntityId(command.buildingEntityId)
        val entity = state.entities.get(entityId)
        if (entity == null || "building" !in entity.tags || !entity.type.startsWith("building:")) {
            state.lastCommandOrError = "unknown_building_entity:${command.buildingEntityId}"
            return
        }
        val position = entity.position?.tile
        if (position == null) {
            state.lastCommandOrError = "building_missing_position:${command.buildingEntityId}"
            return
        }
        val buildingId = entity.type.substringAfter(':')
        val building = state.registry.buildings[buildingId]
        if (building == null) {
            state.lastCommandOrError = "unknown_building:$buildingId"
            return
        }
        val extractorProducerId = "extractor:${entityId.value}"
        val extractorSource = state.haulSources.get("producer:$extractorProducerId")
        if (extractorSource != null && (extractorSource.resources.isNotEmpty() || extractorSource.reservations.isNotEmpty())) {
            state.lastCommandOrError = "extractor_output_pending:${entityId.value}"
            return
        }
        val refund = calculateBuildingRefund(building)
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
        state.producers = state.producers.filterNot { it.id == extractorProducerId }
        extractorSource?.let { state.haulSources.remove(it.id) }
        goalField = rebuildAfterWalkabilityChange()
        state.inventory = refundedInventory
        state.lastCommandOrError = "removed:${command.buildingEntityId}"
        eventSink += GameplayEvent(
            tick = state.tick,
            type = dev.myengine.core.GameplayEventType.SELL,
            sourceEntityId = command.actorId,
            targetEntityId = command.buildingEntityId,
            contentId = building.id,
        )
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

    private fun calculateBuildingRefund(building: dev.myengine.content.BuildingContent): Map<String, Int> {
        val refund = BigDecimal.valueOf(building.costAmount.toLong())
            .multiply(building.sellRefundRatio)
            .setScale(0, RoundingMode.DOWN)
            .intValueExact()
        return if (refund > 0) mapOf(building.costResource to refund) else emptyMap()
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
        state.producers = state.producers.sortedBy { it.id }.map { producer ->
            val recipe = state.registry.recipes[producer.recipeId]
            val nodePosition = producer.resourceNodePosition
            val result = if (nodePosition != null && recipe != null) {
                val node = state.world.tileAt(nodePosition).tile.resourceNode
                if (node == null) {
                    dev.myengine.logistics.ProductionResult(producer, Inventory(), completed = false)
                } else {
                    producerSystem.tick(
                        producer,
                        Inventory(),
                        ProductionSource(node.resourceId, node.amount, node.infinite),
                    )
                }
            } else {
                producerSystem.tick(producer, state.inventory)
            }
            val producerPosition = producer.position
            if (nodePosition != null) {
                if (result.producedAmount > 0 && producerPosition != null && recipe != null) {
                    val extracted = state.world.extractResource(nodePosition, result.producedAmount)
                    if (extracted > 0) {
                        state.haulSources.addOutput(
                            sourceId = "producer:${producer.id}",
                            position = extractorOutputPosition(producerPosition),
                            resourceId = recipe.outputResource,
                            amount = extracted,
                        )
                    }
                }
            } else {
                state.inventory = if (result.completed && producerPosition != null && recipe != null) {
                    state.haulSources.addOutput(
                        sourceId = "producer:${producer.id}",
                        position = producerPosition,
                        resourceId = recipe.outputResource,
                        amount = recipe.outputAmount,
                    )
                    result.inventory.remove(recipe.outputResource, recipe.outputAmount)
                } else {
                    result.inventory
                }
            }
            result.producer
        }
    }

    /** Moves producer output through persisted belts after production and before combat systems. */
    private fun updateBelts() {
        val result = beltTransportSystem.tick(
            state = state.belts,
            pull = pull@{ belt ->
                val sourceId = belt.inputSourceId ?: return@pull null
                val source = state.haulSources.get(sourceId) ?: return@pull null
                val resourceId = source.resources.toSortedMap()
                    .entries
                    .firstOrNull { entry -> entry.value > 0 && source.available(entry.key) > 0 }
                    ?.key
                    ?: return@pull null
                if (!state.haulSources.takeOutput(sourceId, resourceId, 1)) return@pull null
                BeltItem(
                    id = "${belt.id}:${state.tick.value}",
                    resourceId = resourceId,
                    amount = 1,
                    cellIndex = 0,
                )
            },
            push = push@{ belt, item ->
                val destinationId = belt.destinationEntityId
                if (destinationId == null) {
                    if (!state.inventory.canAdd(item.resourceId, item.amount)) return@push false
                    state.inventory = state.inventory.add(item.resourceId, item.amount)
                    return@push true
                }
                val entity = state.entities.get(EntityId(destinationId)) ?: return@push false
                val inventory = entity.inventory ?: InventoryComponent()
                val next = Inventory(inventory.resources, inventory.capacity)
                if (!next.canAdd(item.resourceId, item.amount)) return@push false
                state.entities.update(entity.id) {
                    it.copy(inventory = InventoryComponent(next.add(item.resourceId, item.amount).resources, next.capacity))
                }
                true
            },
        )
        state.belts = result.state
    }

    /** Single committed-world cache hook paired with GoalField's prospective placement probe. */
    private fun rebuildAfterWalkabilityChange(): GoalField =
        GoalField.rebuildAfterWalkabilityChange(state.world, core, spawns).field
}

private fun VisualAssetRef.toRenderAssetRef(): RenderAssetRef = RenderAssetRef(path = path, atlasKey = atlasKey)

object SandboxSaveCodec {
    const val SAVE_VERSION: Int = 21

    fun encode(state: SandboxState, seed: Long, pendingCommands: List<EngineCommand> = emptyList()): String {
        val props = Properties()
        props["saveVersion"] = SAVE_VERSION.toString()
        props["engineVersion"] = EngineInfo.SCAFFOLD_PHASE.toString()
        props["packId"] = state.registry.manifest.id
        props["packVersion"] = state.registry.manifest.version
        props["mapId"] = state.mapId
        props["contentVersion"] = state.registry.manifest.version
        props["seed"] = seed.toString()
        props["randomCursor"] = state.randomCursor.toString()
        props["incidentCooldowns"] = state.incidentState.cooldownUntil.toSortedMap().entries
            .joinToString(";") { (id, tick) -> "${encodeToken(id)}:$tick" }
        props["incidentLastSelection"] = listOf(
            state.incidentState.lastSelectionTick?.toString().orEmpty(),
            encodeToken(state.incidentState.lastSelectionId.orEmpty()),
        ).joinToString(":")
        props["incidentExecutions"] = state.incidentState.executions.joinToString(";") { execution ->
            listOf(
                execution.tick,
                encodeToken(execution.incidentId),
                execution.threat,
                execution.effects.joinToString(",", transform = ::encodeIncidentEffect),
            ).joinToString("~")
        }
        props["incidentModifiers"] = state.incidentModifiers.toSortedMap().entries.joinToString(";") { (id, modifier) ->
            "${encodeToken(id)}:${modifier.amount}:${modifier.remainingTicks}"
        }
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
        props["resourceNodes"] = state.world.positions().mapNotNull { position ->
            state.world.tileAt(position).tile.resourceNode?.let { node ->
                listOf(
                    position.x,
                    position.y,
                    encodeToken(node.resourceId),
                    node.amount,
                    node.infinite,
                ).joinToString("|")
            }
        }.joinToString(";")
        props["researchedTechIds"] = state.researchedTechIds.sorted().joinToString(",") { encodeToken(it) }
        props["producers"] = state.producers.sortedBy { it.id }.joinToString(";") {
            listOf(
                it.id,
                it.recipeId,
                it.progressTicks,
                it.position?.x ?: "",
                it.position?.y ?: "",
                it.resourceNodePosition?.x ?: "",
                it.resourceNodePosition?.y ?: "",
            ).joinToString("|")
        }
        props["jobs"] = state.jobBoard.all().joinToString(";") { job -> encodeJob(job) }
        props["stockpileZones"] = state.zones.allStockpiles().joinToString(";") { zone ->
            listOf(
                encodeToken(zone.id),
                zone.normalizedTiles.joinToString(",") { "${it.x}:${it.y}" },
                zone.normalizedResourceIds.joinToString(",", transform = ::encodeToken),
                zone.storedResources.toSortedMap().entries.joinToString(",") { (id, amount) -> "${encodeToken(id)}:$amount" },
            ).joinToString("|")
        }
        props["harvestDesignations"] = state.zones.allHarvestDesignations().joinToString(";") { designation ->
            listOf(
                encodeToken(designation.id),
                encodeToken(designation.resourceId),
                designation.position.x,
                designation.position.y,
                encodeToken(designation.jobId),
            ).joinToString("|")
        }
        props["haulSources"] = state.haulSources.all().joinToString(";") { source ->
            listOf(
                encodeToken(source.id), source.position.x, source.position.y,
                source.resources.toSortedMap().entries.joinToString(",") { (id, amount) -> "${encodeToken(id)}:$amount" },
                source.reservations.toSortedMap().entries.joinToString(",") { (jobId, amount) ->
                    "${encodeToken(jobId)}:$amount:${encodeToken(source.reservationResources[jobId].orEmpty())}"
                },
            ).joinToString("|")
        }
        props["constructionSites"] = state.constructionSites.all().joinToString(";") { site ->
            listOf(
                encodeToken(site.id),
                encodeToken(site.buildingId),
                site.position.x,
                site.position.y,
                encodeToken(site.materialResourceId),
                site.requiredAmount,
                site.deliveredBySource.toSortedMap().entries.joinToString(",") { (sourceId, amount) ->
                    "${encodeToken(sourceId)}:$amount"
                },
            ).joinToString("|")
        }
        props["belts"] = state.belts.canonical().belts.joinToString(";") { belt ->
            listOf(
                encodeToken(belt.id),
                belt.ticksPerCell,
                encodeToken(belt.inputSourceId.orEmpty()),
                belt.destinationEntityId ?: "",
                belt.cells.joinToString("~") { cell ->
                    listOf(cell.position.x, cell.position.y, cell.geometry.name, cell.direction.name).joinToString(":")
                },
                belt.items.sortedWith(compareBy<BeltItem> { it.cellIndex }.thenBy { it.id }).joinToString("~") { item ->
                    listOf(
                        encodeToken(item.id),
                        encodeToken(item.resourceId),
                        item.amount,
                        item.cellIndex,
                        item.progressTicks,
                    ).joinToString(":")
                },
            ).joinToString("|")
        }
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
                entity.enemy?.let { enemy ->
                    listOf(
                        encodeToken(enemy.enemyId),
                        enemy.speedTilesPerTick,
                        enemy.coreDamage,
                        encodeToken(enemy.rewardResource),
                        enemy.rewardAmount,
                        enemy.isElite,
                        enemy.isBoss,
                        enemy.movementMode.id,
                    ).joinToString("~")
                }.orEmpty(),
                if (entity.jobActor != null) "1" else "",
                entity.jobActor?.assignedJobId?.let(::encodeToken).orEmpty(),
                entity.jobActor?.workTicks?.toString().orEmpty(),
                if (entity.inventory != null) "1" else "",
                entity.inventory?.resources?.toSortedMap()?.entries?.joinToString("~") { (id, amount) -> "${encodeToken(id)}:$amount" }.orEmpty(),
                entity.inventory?.capacity?.toString().orEmpty(),
                if (entity.worker != null) "1" else "",
                entity.worker?.workerId?.let(::encodeToken).orEmpty(),
                if (entity.needs != null) "1" else "",
                entity.needs?.levels?.toSortedMap()?.entries?.joinToString("~") { (id, value) ->
                    "${encodeToken(id)}:$value"
                }.orEmpty(),
                entity.needs?.triggerCounts?.toSortedMap()?.entries?.joinToString("~") { (id, count) ->
                    "${encodeToken(id)}:$count"
                }.orEmpty(),
            ).joinToString("|")
        }
        props["pendingCommands"] = pendingCommands.joinToString(";") { cmd ->
            val stablePayload = if (cmd is ResearchCommand) encodeToken(cmd.nodeId) else cmd.stablePayload()
            listOf(cmd.type, cmd.id.value, cmd.scheduledTick.value, cmd.actorId ?: "", stablePayload).joinToString("|")
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
        val seed = props.getProperty("seed")?.toLongOrNull() ?: 7L
        state.randomCursor = if (version >= 10) {
            props.getProperty("randomCursor")?.toLongOrNull()
                ?: error("Save version 10 is missing randomCursor.")
        } else {
            SeededRandom(seed).snapshot()
        }
        val metrics = props.getProperty("metrics", "0,0,0,0,0").split(',').map { it.toInt() }
        state.defense = DefenseState(
            coreHealth = props.getProperty("coreHealth").toInt(),
            spawnedWaveIds = props.getProperty("spawnedWaves", "").split(',').filter { it.isNotBlank() }.toSet(),
            metrics = dev.myengine.defense.DefenseMetrics(metrics[0], metrics[1], metrics[2], metrics[3], metrics[4]),
            towerMetrics = if (version >= 6) parseTowerMetrics(props.getProperty("towerMetrics", "")) else emptyMap(),
        )
        state.inventory = Inventory(parseResources(props.getProperty("inventory", "")))
        if (version >= 19) restoreResourceNodes(state, props.getProperty("resourceNodes", ""))
        state.researchedTechIds = if (version >= 18) {
            parseResearchedTechIds(props.getProperty("researchedTechIds", ""), registry)
        } else emptySet()
        state.producers = props.getProperty("producers", "")
            .split(';')
            .filter { it.isNotBlank() }
            .map {
                val parts = it.split('|')
                Producer(
                    parts[0], parts[1], parts[2].toInt(),
                    if (version >= 15 && parts.getOrNull(3)?.isNotBlank() == true && parts.getOrNull(4)?.isNotBlank() == true) {
                        TilePosition(parts[3].toInt(), parts[4].toInt())
                    } else null,
                    if (version >= 19 && parts.getOrNull(5)?.isNotBlank() == true && parts.getOrNull(6)?.isNotBlank() == true) {
                        TilePosition(parts[5].toInt(), parts[6].toInt())
                    } else null,
                )
            }
        state.run = if (version >= 5) parseRunState(props) else RunState()
        state.incidentState = if (version >= 10) parseIncidentState(props) else IncidentDirectorState()
        state.incidentModifiers = if (version >= 10) parseIncidentModifiers(props) else emptyMap()
        state.jobBoard = if (version >= 13) parseJobs(props.getProperty("jobs", "")) else JobBoard()
        state.zones = if (version >= 14) parseZones(props, registry, state.world, state.jobBoard) else ZoneStore()
        state.haulSources = if (version >= 15) parseHaulSources(props.getProperty("haulSources", ""), version) else HaulSourceStore()
        state.constructionSites = if (version >= 16) {
            parseConstructionSites(props.getProperty("constructionSites", ""), registry, state.world, state.haulSources)
        } else ConstructionSiteStore()
        state.belts = if (version >= 20) parseBelts(props.getProperty("belts", "")) else BeltTransportState()
        val entities = parseEntities(props.getProperty("entities", ""), registry, version)
        state.world.positions().forEach { state.world.clearOccupancy(it) }
        val loadedStore = EntityStore(props.getProperty("nextEntityId").toLong(), entities)
        entities.filter { it.tower != null || "building" in it.tags }.forEach { entity ->
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

    private fun encodeJob(job: Job): String = listOf(
        encodeToken(job.id),
        encodeToken(job.type),
        job.target.x,
        job.target.y,
        job.priority,
        job.reservedBy?.value ?: "",
        job.assignedTo?.value ?: "",
        job.status.name,
        encodeToken(job.failureReason.orEmpty()),
        job.workTicks,
        job.completionEffects
            .sortedBy { it.stableSortKey() }
            .joinToString("~") { effect ->
                when (effect) {
                    is JobCompletionEffect.ResourceDelta ->
                        listOf(effect.type.id, encodeToken(effect.resourceId), effect.amount).joinToString(":")
                    is JobCompletionEffect.SpawnBuilding ->
                        listOf(effect.type.id, encodeToken(effect.buildingId), encodeToken(effect.siteId)).joinToString(":")
                    is JobCompletionEffect.NeedRecovery ->
                        listOf(
                            effect.type.id,
                            encodeToken(effect.needId),
                            effect.amount,
                            effect.targetEntityId?.value ?: "",
                        ).joinToString(":")
                }
            },
        job.haul?.let { haul ->
            listOf(
                encodeToken(haul.sourceId), encodeToken(haul.resourceId), haul.amount,
                encodeToken(haul.destinationZoneId), haul.phase.name,
                haul.destinationKind.name,
            ).joinToString(":")
        }.orEmpty(),
    ).joinToString("|")

    private fun parseJobs(text: String): JobBoard = JobBoard(
        text.split(';')
            .filter { it.isNotBlank() }
            .map { encoded ->
                val parts = encoded.split('|')
                require(parts.size == 11 || parts.size == 12) { "Invalid job entry '$encoded'." }
                val reservedBy = parts[5].toLongOrNull()?.let(::EntityId)
                val assignedTo = parts[6].toLongOrNull()?.let(::EntityId)
                val effects = parts[10].split('~').filter { it.isNotBlank() }.map { effectText ->
                    val effectParts = effectText.split(':')
                    require(
                        effectParts[0] in setOf("resource_delta", "spawn_building", "need_recovery") &&
                            (effectParts.size == 3 || (effectParts[0] == "need_recovery" && effectParts.size == 4)),
                    ) {
                        "Invalid job completion effect '$effectText'."
                    }
                    if (effectParts[0] == "resource_delta") {
                        JobCompletionEffect.ResourceDelta(
                            resourceId = decodeToken(effectParts[1]),
                            amount = effectParts[2].toIntOrNull()
                                ?: error("Invalid resource delta amount in '$effectText'."),
                        )
                    } else if (effectParts[0] == "spawn_building") {
                        JobCompletionEffect.SpawnBuilding(
                            buildingId = decodeToken(effectParts[1]),
                            siteId = decodeToken(effectParts[2]),
                        )
                    } else {
                        JobCompletionEffect.NeedRecovery(
                            needId = decodeToken(effectParts[1]),
                            amount = effectParts[2].toIntOrNull()
                                ?: error("Invalid need recovery amount in '$effectText'."),
                            targetEntityId = effectParts.getOrNull(3)?.takeIf { it.isNotBlank() }?.toLongOrNull()?.let(::EntityId),
                        )
                    }
                }
                Job(
                    id = decodeToken(parts[0]),
                    type = decodeToken(parts[1]),
                    target = TilePosition(parts[2].toInt(), parts[3].toInt()),
                    priority = parts[4].toInt(),
                    reservedBy = reservedBy,
                    assignedTo = assignedTo,
                    status = JobStatus.valueOf(parts[7]),
                    failureReason = decodeToken(parts[8]).takeIf { it.isNotBlank() },
                    workTicks = parts[9].toInt(),
                    completionEffects = effects,
                    haul = parts.getOrNull(11)?.takeIf { it.isNotBlank() }?.let { haulText ->
                        val haulParts = haulText.split(':')
                        require(haulParts.size == 5 || haulParts.size == 6) { "Invalid haul job payload '$haulText'." }
                        dev.myengine.ai.HaulJobSpec(
                            sourceId = decodeToken(haulParts[0]),
                            resourceId = decodeToken(haulParts[1]),
                            amount = haulParts[2].toInt(),
                            destinationZoneId = decodeToken(haulParts[3]),
                            phase = dev.myengine.ai.HaulPhase.valueOf(haulParts[4]),
                            destinationKind = haulParts.getOrNull(5)?.let(dev.myengine.ai.HaulDestinationKind::valueOf)
                                ?: dev.myengine.ai.HaulDestinationKind.STOCKPILE,
                        )
                    },
                )
            },
    )

    private fun parseZones(
        props: Properties,
        registry: ContentRegistry,
        world: TileWorld,
        jobs: JobBoard,
    ): ZoneStore {
        val stockpiles = props.getProperty("stockpileZones", "")
            .split(';')
            .filter { it.isNotBlank() }
            .map { encoded ->
                val parts = encoded.split('|')
                require(parts.size == 3 || parts.size == 4) { "Invalid stockpile zone entry '$encoded'." }
                val tiles = parts[1].split(',').filter { it.isNotBlank() }.map { tileText ->
                    val xy = tileText.split(':')
                    require(xy.size == 2) { "Invalid stockpile tile '$tileText'." }
                    TilePosition(xy[0].toInt(), xy[1].toInt()).also {
                        require(world.inBounds(it)) { "Stockpile tile $it is outside the saved world." }
                    }
                }
                val resourceIds = parts[2].split(',').filter { it.isNotBlank() }.map(::decodeToken).toSet()
                resourceIds.forEach { resourceId ->
                    require(resourceId in registry.resources) { "Unknown stockpile resource '$resourceId' in save." }
                }
                val stored = if (parts.size == 4) parseEncodedResources(parts[3], "stockpile") else emptyMap()
                stored.keys.forEach { resourceId -> require(resourceId in registry.resources) { "Unknown stockpile resource '$resourceId' in save." } }
                StockpileZone(decodeToken(parts[0]), tiles, resourceIds, stored)
            }
        val designations = props.getProperty("harvestDesignations", "")
            .split(';')
            .filter { it.isNotBlank() }
            .map { encoded ->
                val parts = encoded.split('|')
                require(parts.size == 5) { "Invalid harvest designation entry '$encoded'." }
                val designationId = decodeToken(parts[0])
                val resourceId = decodeToken(parts[1])
                val position = TilePosition(parts[2].toInt(), parts[3].toInt())
                require(world.inBounds(position)) { "Harvest tile $position is outside the saved world." }
                require(resourceId in registry.resources) { "Unknown harvest resource '$resourceId' in save." }
                require(world.tileAt(position).tile.resourceNode?.resourceId == resourceId) {
                    "Harvest designation '$designationId' does not match its resource node."
                }
                val jobId = decodeToken(parts[4])
                require(jobId == HarvestDesignation.jobIdFor(designationId)) {
                    "Harvest designation '$designationId' has a non-deterministic job id."
                }
                require(jobs.get(jobId) != null) {
                    "Harvest designation '$designationId' is missing job '$jobId'."
                }
                HarvestDesignation(designationId, resourceId, position, jobId)
            }
        return ZoneStore(stockpiles, designations)
    }

    private fun parseHaulSources(text: String, version: Int): HaulSourceStore = HaulSourceStore(
        text.split(';').filter { it.isNotBlank() }.map { encoded ->
            val parts = encoded.split('|')
            require(parts.size == 5 || (version >= 16 && parts.size == 6)) { "Invalid haul source entry '$encoded'." }
            val (reservations, reservationResources) = parseEncodedReservations(parts[4], "haul reservation")
            HaulSource(
                id = decodeToken(parts[0]),
                position = TilePosition(parts[1].toInt(), parts[2].toInt()),
                resources = parseEncodedResources(parts[3], "haul source"),
                reservations = reservations,
                reservationResources = reservationResources,
            )
        },
    )

    private fun parseConstructionSites(
        text: String,
        registry: ContentRegistry,
        world: TileWorld,
        sources: HaulSourceStore,
    ): ConstructionSiteStore = ConstructionSiteStore(
        text.split(';').filter { it.isNotBlank() }.map { encoded ->
            val parts = encoded.split('|')
            require(parts.size == 7) { "Invalid construction site entry '$encoded'." }
            val siteId = decodeToken(parts[0])
            val buildingId = decodeToken(parts[1])
            val position = TilePosition(parts[2].toInt(), parts[3].toInt())
            require(world.inBounds(position)) { "Construction site $siteId is outside the saved world." }
            val building = registry.buildings[buildingId]
                ?: error("Unknown construction building '$buildingId' in save.")
            val resourceId = decodeToken(parts[4])
            require(resourceId == building.costResource) {
                "Construction site '$siteId' material does not match building '$buildingId'."
            }
            require(parts[5].toIntOrNull() == building.costAmount) {
                "Construction site '$siteId' requirement does not match building '$buildingId'."
            }
            val delivered = parseEncodedResources(parts[6], "construction delivery")
            delivered.keys.forEach { sourceId ->
                require(sources.get(sourceId) != null) {
                    "Unknown construction source '$sourceId' in save."
                }
            }
            ConstructionSite(
                id = siteId,
                buildingId = buildingId,
                position = position,
                materialResourceId = resourceId,
                requiredAmount = parts[5].toInt(),
                deliveredBySource = delivered,
            )
        },
    )

    private fun parseBelts(text: String): BeltTransportState = BeltTransportState(
        text.split(';').filter { it.isNotBlank() }.map { encoded ->
            val parts = encoded.split('|')
            require(parts.size == 6) { "Invalid belt entry '$encoded'." }
            val destinationEntityId = parts[3].toLongOrNull()?.also { require(it > 0) {
                "Invalid belt destination entity id in '$encoded'."
            } }
            val cells = parts[4].split('~').filter { it.isNotBlank() }.map { cellText ->
                val cellParts = cellText.split(':')
                require(cellParts.size == 4) { "Invalid belt cell entry '$cellText'." }
                BeltCell(
                    position = TilePosition(cellParts[0].toInt(), cellParts[1].toInt()),
                    geometry = dev.myengine.logistics.BeltGeometry.valueOf(cellParts[2]),
                    direction = dev.myengine.logistics.BeltDirection.valueOf(cellParts[3]),
                )
            }
            val items = parts[5].split('~').filter { it.isNotBlank() }.map { itemText ->
                val itemParts = itemText.split(':')
                require(itemParts.size == 5) { "Invalid belt item entry '$itemText'." }
                BeltItem(
                    id = decodeToken(itemParts[0]),
                    resourceId = decodeToken(itemParts[1]),
                    amount = itemParts[2].toInt(),
                    cellIndex = itemParts[3].toInt(),
                    progressTicks = itemParts[4].toInt(),
                )
            }
            BeltLine(
                id = decodeToken(parts[0]),
                cells = cells,
                ticksPerCell = parts[1].toInt(),
                items = items,
                inputSourceId = decodeToken(parts[2]).takeIf { it.isNotBlank() },
                destinationEntityId = destinationEntityId,
            )
        },
    ).canonical()

    private fun parseEncodedResources(text: String, label: String): Map<String, Int> =
        text.split(',').filter { it.isNotBlank() }.associate { encoded ->
            val parts = encoded.split(':')
            require(parts.size == 2) { "Invalid $label resource entry '$encoded'." }
            val amount = parts[1].toIntOrNull()
            require(amount != null && amount >= 0) { "Invalid $label resource amount '$encoded'." }
            decodeToken(parts[0]) to amount
        }

    private fun parseEncodedReservations(text: String, label: String): Pair<Map<String, Int>, Map<String, String>> {
        val reservations = linkedMapOf<String, Int>()
        val reservationResources = linkedMapOf<String, String>()
        text.split(',').filter { it.isNotBlank() }.forEach { encoded ->
            val parts = encoded.split(':')
            require(parts.size == 2 || parts.size == 3) { "Invalid $label resource entry '$encoded'." }
            val amount = parts[1].toIntOrNull()
                ?: error("Invalid $label amount '$encoded'.")
            val jobId = decodeToken(parts[0])
            reservations[jobId] = amount
            if (parts.size == 3 && parts[2].isNotBlank()) reservationResources[jobId] = decodeToken(parts[2])
        }
        return reservations to reservationResources
    }

    private fun parseIncidentState(props: Properties): IncidentDirectorState {
        val cooldowns = props.getProperty("incidentCooldowns", "")
            .split(';')
            .filter { it.isNotBlank() }
            .associate { encoded ->
                val parts = encoded.split(':')
                require(parts.size == 2) { "Invalid incident cooldown entry '$encoded'." }
                decodeToken(parts[0]) to parts[1].toLongOrNull().let {
                    require(it != null && it >= 0) { "Invalid incident cooldown tick in '$encoded'." }
                    it
                }
            }
            .toSortedMap()
        val last = props.getProperty("incidentLastSelection", ":").split(':')
        require(last.size == 2) { "Invalid incident last-selection state." }
        val lastTick = last[0].toLongOrNull()?.takeIf { it >= 0 }
        val lastId = decodeToken(last[1]).takeIf { it.isNotBlank() }
        val executions = props.getProperty("incidentExecutions", "")
            .split(';')
            .filter { it.isNotBlank() }
            .map { encoded ->
                val parts = encoded.split('~')
                require(parts.size == 4) { "Invalid incident execution entry '$encoded'." }
                val tick = parts[0].toLongOrNull()
                require(tick != null && tick >= 0) { "Invalid incident execution tick '$encoded'." }
                val threat = parts[2].toIntOrNull()
                require(threat != null && threat >= 0) { "Invalid incident execution threat '$encoded'." }
                IncidentExecution(
                    tick = tick,
                    incidentId = decodeToken(parts[1]).also { require(it.isNotBlank()) },
                    threat = threat,
                    effects = parts[3].split(',').filter { it.isNotBlank() }.map(::decodeIncidentEffect),
                )
            }
        return IncidentDirectorState(cooldowns, lastTick, lastId, executions).canonical()
    }

    private fun parseIncidentModifiers(props: Properties): Map<String, SandboxIncidentModifier> =
        props.getProperty("incidentModifiers", "")
            .split(';')
            .filter { it.isNotBlank() }
            .associate { encoded ->
                val parts = encoded.split(':')
                require(parts.size == 3) { "Invalid incident modifier entry '$encoded'." }
                val amount = parts[1].toIntOrNull()
                val remaining = parts[2].toIntOrNull()
                require(amount != null && remaining != null) { "Invalid incident modifier values '$encoded'." }
                decodeToken(parts[0]) to SandboxIncidentModifier(amount, remaining)
            }
            .toSortedMap()

    private fun encodeIncidentEffect(effect: IncidentEffectDescriptor): String = when (effect) {
        is IncidentEffectDescriptor.SpawnWave -> "spawn_wave|${encodeToken(effect.waveId)}"
        is IncidentEffectDescriptor.ResourceEvent -> "resource_event|${encodeToken(effect.resourceId)}|${effect.amount}"
        is IncidentEffectDescriptor.Modifier -> "modifier|${encodeToken(effect.modifierId)}|${effect.amount}|${effect.durationTicks}"
    }

    private fun decodeIncidentEffect(encoded: String): IncidentEffectDescriptor {
        val parts = encoded.split('|')
        return when (parts.firstOrNull()) {
            "spawn_wave" -> {
                require(parts.size == 2) { "Invalid spawn-wave incident effect '$encoded'." }
                IncidentEffectDescriptor.SpawnWave(decodeToken(parts[1]))
            }
            "resource_event" -> {
                require(parts.size == 3) { "Invalid resource incident effect '$encoded'." }
                IncidentEffectDescriptor.ResourceEvent(decodeToken(parts[1]), parts[2].toInt())
            }
            "modifier" -> {
                require(parts.size == 4) { "Invalid modifier incident effect '$encoded'." }
                IncidentEffectDescriptor.Modifier(decodeToken(parts[1]), parts[2].toInt(), parts[3].toInt())
            }
            else -> error("Unknown incident effect '$encoded'.")
        }
    }

    private fun encodeToken(value: String): String =
        java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())

    private fun decodeToken(value: String): String =
        java.util.Base64.getUrlDecoder().decode(value).toString(Charsets.UTF_8)

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
            if (type == "research") {
                ResearchCommand(id, scheduledTick, decodeToken(payload), actorId)
            } else if (type == "build_tower") {
                val payloadParts = payload.split(':')
                BuildTowerCommand(id, scheduledTick, payloadParts[0], TileCoordinate(payloadParts[1].toInt(), payloadParts[2].toInt()), actorId)
            } else if (type == "place_building") {
                val payloadParts = payload.split(':')
                require(payloadParts.size == 3) { "Invalid place-building command payload '$payload'." }
                PlaceBuildingCommand(id, scheduledTick, payloadParts[0], TileCoordinate(payloadParts[1].toInt(), payloadParts[2].toInt()), actorId)
            } else if (type == "place_blueprint") {
                val payloadParts = payload.split(':')
                require(payloadParts.size == 3) { "Invalid place-blueprint command payload '$payload'." }
                PlaceBlueprintCommand(
                    id,
                    scheduledTick,
                    payloadParts[0],
                    TileCoordinate(payloadParts[1].toInt(), payloadParts[2].toInt()),
                    actorId,
                )
            } else if (type == "upgrade_tower") {
                val payloadParts = payload.split(':')
                UpgradeTowerCommand(id, scheduledTick, payloadParts[0].toLong(), payloadParts[1], payloadParts[2].toInt(), actorId)
            } else if (type == "sell_tower") {
                SellTowerCommand(id, scheduledTick, payload.toLong(), actorId)
            } else if (type == "remove_building") {
                RemoveBuildingCommand(id, scheduledTick, payload.toLong(), actorId)
            } else if (type == "cancel_blueprint") {
                CancelBlueprintCommand(id, scheduledTick, payload, actorId)
            } else if (type == "set_tower_targeting_mode") {
                val payloadParts = payload.split(':')
                require(payloadParts.size == 2) { "Invalid targeting mode command payload '$payload'." }
                val mode = TargetingMode.fromId(payloadParts[1])
                    ?: error("Unknown targeting mode '${payloadParts[1]}'.")
                SetTowerTargetingModeCommand(id, scheduledTick, payloadParts[0].toLong(), mode, actorId)
            } else if (type == "call_wave_early") {
                CallWaveEarlyCommand(id, scheduledTick, actorId)
            } else if (type == "define_stockpile_zone") {
                val zone = parseStockpileCommandPayload(payload)
                DefineStockpileZoneCommand(id, scheduledTick, zone.first, zone.second, zone.third, actorId)
            } else if (type == "update_stockpile_zone") {
                val zone = parseStockpileCommandPayload(payload)
                UpdateStockpileZoneCommand(id, scheduledTick, zone.first, zone.second, zone.third, actorId)
            } else if (type == "remove_stockpile_zone") {
                RemoveStockpileZoneCommand(id, scheduledTick, payload, actorId)
            } else if (type == "designate_harvest_node") {
                val payloadParts = payload.split(':')
                require(payloadParts.size == 4) { "Invalid harvest designation command payload '$payload'." }
                DesignateHarvestNodeCommand(
                    id = id,
                    scheduledTick = scheduledTick,
                    designationId = payloadParts[0],
                    resourceId = payloadParts[1],
                    position = TileCoordinate(payloadParts[2].toInt(), payloadParts[3].toInt()),
                    actorId = actorId,
                )
            } else if (type == "remove_harvest_designation") {
                RemoveHarvestDesignationCommand(id, scheduledTick, payload, actorId)
            } else {
                dev.myengine.core.TextCommand(id, scheduledTick, type, payload, actorId)
            }
        }

    private fun restoreResourceNodes(state: SandboxState, text: String) {
        text.split(';').filter { it.isNotBlank() }.forEach { encoded ->
            val parts = encoded.split('|')
            require(parts.size == 5) { "Invalid resource node entry '$encoded'." }
            val position = TilePosition(parts[0].toInt(), parts[1].toInt())
            require(state.world.inBounds(position)) { "Resource node $position is outside the saved world." }
            val current = state.world.tileAt(position).tile.resourceNode
                ?: error("Save resource node $position does not exist in the loaded map.")
            val resourceId = decodeToken(parts[2])
            require(resourceId in state.registry.resources) { "Unknown resource '$resourceId' in save." }
            require(resourceId == current.resourceId) {
                "Save resource node '$resourceId' does not match the loaded map at $position."
            }
            val amount = parts[3].toIntOrNull()
                ?: error("Invalid resource node amount '$encoded'.")
            val infinite = parts[4].toBooleanStrictOrNull()
                ?: error("Invalid resource node infinite flag '$encoded'.")
            require(amount >= 0) { "Resource node amount cannot be negative." }
            state.world.setTile(
                position,
                state.world.tileAt(position).tile.copy(
                    resourceNode = ResourceNode(resourceId, amount, infinite),
                ),
            )
        }
    }

    private fun parseResearchedTechIds(text: String, registry: ContentRegistry): Set<String> {
        val ids = text.split(',').filter { it.isNotBlank() }.map(::decodeToken).toSortedSet()
        ids.forEach { id -> require(id in registry.techNodes) { "Unknown researched tech '$id' in save." } }
        return ids
    }

    private fun parseStockpileCommandPayload(
        payload: String,
    ): Triple<String, List<TileCoordinate>, Set<String>> {
        val parts = payload.split(':')
        require(parts.size == 3) { "Invalid stockpile zone command payload '$payload'." }
        val tiles = parts[1].split(',').filter { it.isNotBlank() }.map { tileText ->
            val xy = tileText.split('.')
            require(xy.size == 2) { "Invalid stockpile tile '$tileText'." }
            TileCoordinate(xy[0].toInt(), xy[1].toInt())
        }
        val resourceIds = parts[2].split(',').filter { it.isNotBlank() }.toSet()
        return Triple(parts[0], tiles, resourceIds)
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
            val enemy = if (version >= 11) {
                parseEnemyComponent(parts.getOrNull(17).orEmpty(), type, version)
            } else {
                null
            }
            val jobActorPresent = version >= 13 && parts.getOrNull(18) == "1"
            val assignedJobId = parts.getOrNull(19)?.takeIf { it.isNotBlank() }?.let(::decodeToken)
            val workTicks = parts.getOrNull(20)?.toIntOrNull() ?: 0
            val inventoryPresent = version >= 15 && parts.getOrNull(21) == "1"
            val entityInventory = if (inventoryPresent) {
                InventoryComponent(
                    resources = parseEncodedResources(parts.getOrNull(22).orEmpty(), "worker inventory"),
                    capacity = parts.getOrNull(23)?.toIntOrNull(),
                )
            } else null
            val workerPresent = version >= 15 && parts.getOrNull(24) == "1"
            val workerComponent = if (workerPresent) {
                WorkerComponent(decodeToken(parts.getOrNull(25).orEmpty())).also {
                    require(it.workerId in registry.workers) { "Unknown worker '${it.workerId}' in save." }
                }
            } else null
            val needsPresent = version >= 17 && parts.getOrNull(26) == "1"
            val needs = if (needsPresent) {
                parseNeeds(
                    levelsText = parts.getOrNull(27).orEmpty(),
                    triggersText = parts.getOrNull(28).orEmpty(),
                    registry = registry,
                )
            } else null
            Entity(
                id = id,
                type = type,
                tags = when {
                    type.startsWith("tower") -> setOf("tower")
                    type.startsWith("enemy") -> setOf("enemy")
                    type.startsWith("building:") -> setOf("building")
                    type.startsWith("worker:") -> setOf("worker")
                    else -> emptySet()
                },
                position = if (x != null && y != null) PositionComponent(TilePosition(x, y)) else null,
                health = if (health != null && maxHealth != null) {
                    HealthComponent(health, maxHealth)
                } else if (towerId != null && registry.enemies.values.any { it.attacksStructures }) {
                    val towerHealth = registry.requireTower(towerId).maxHealth
                    HealthComponent(towerHealth, towerHealth)
                } else null,
                tower = if (towerId != null && cooldown != null) TowerComponent(towerId, cooldown, upgradeBranch, upgradeTier, targetingMode) else null,
                attack = if (range != null && damage != null && cooldownTicks != null) AttackComponent(range, damage, cooldownTicks) else null,
                inventory = entityInventory,
                enemy = enemy,
                jobActor = if (jobActorPresent) JobActorComponent(assignedJobId, workTicks) else null,
                worker = workerComponent,
                needs = needs,
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

    private fun parseNeeds(levelsText: String, triggersText: String, registry: ContentRegistry): NeedsComponent {
        fun parse(text: String, label: String): Map<String, Int> = text
            .split('~')
            .filter { it.isNotBlank() }
            .associate { encoded ->
                val parts = encoded.split(':')
                require(parts.size == 2) { "Invalid $label entry '$encoded'." }
                val id = decodeToken(parts[0])
                require(id in registry.needs) { "Unknown need '$id' in save." }
                val value = parts[1].toIntOrNull()
                    ?: error("Invalid $label value '$encoded'.")
                id to value
            }
        return NeedsComponent(
            levels = parse(levelsText, "need level"),
            triggerCounts = parse(triggersText, "need trigger"),
        )
    }

    private fun parseEnemyComponent(text: String, type: String, version: Int): EnemyComponent? {
        if (text.isBlank()) return null
        require(type.startsWith("enemy:")) { "Enemy component is only valid on an enemy entity." }
        val parts = text.split('~')
        val expectedPartCount = if (version >= 21) 8 else 7
        require(parts.size == expectedPartCount) { "Invalid enemy component entry '$text'." }
        val enemyId = decodeToken(parts[0])
        require(type == "enemy:$enemyId") { "Enemy component id '$enemyId' does not match entity type '$type'." }
        val speed = parts[1].toIntOrNull()
        val coreDamage = parts[2].toIntOrNull()
        val rewardAmount = parts[4].toIntOrNull()
        require(speed != null && coreDamage != null && rewardAmount != null) { "Invalid enemy component values '$text'." }
        val movementMode = parts.getOrNull(7)?.takeIf { it.isNotBlank() }?.let {
            MovementMode.fromId(it) ?: error("Invalid enemy movement mode '$it'.")
        } ?: MovementMode.GROUND
        return EnemyComponent(
            enemyId = enemyId,
            speedTilesPerTick = speed,
            coreDamage = coreDamage,
            rewardResource = decodeToken(parts[3]),
            rewardAmount = rewardAmount,
            movementMode = movementMode,
            isElite = parts[5].toBooleanStrictOrNull() ?: error("Invalid elite flag in '$text'."),
            isBoss = parts[6].toBooleanStrictOrNull() ?: error("Invalid boss flag in '$text'."),
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

    /** Generates a map from the selected content map without mutating the loaded registry. */
    fun generateProceduralMap(
        registry: ContentRegistry = loadRegistry(),
        seed: Long = 7L,
        wallDensityPercent: Int = 18,
        maxAttempts: Int = 16,
    ): GeneratedMap {
        val template = registry.requireMap()
        val parameters = ProceduralMapParameters.fromContentMap(
            map = template,
            tiles = registry.tiles,
            wallDensityPercent = wallDensityPercent,
            maxAttempts = maxAttempts,
        )
        return ProceduralMapGenerator.generate(seed, parameters)
    }

    /** Creates a sandbox runtime backed by a generated map; the existing save seed carries provenance. */
    fun createProceduralRuntime(
        registry: ContentRegistry = loadRegistry(),
        seed: Long = 7L,
        wallDensityPercent: Int = 18,
        maxAttempts: Int = 16,
    ): SandboxRuntime {
        val generated = generateProceduralMap(registry, seed, wallDensityPercent, maxAttempts)
        val generatedRegistry = registry.copy(maps = registry.maps + (generated.map.id to generated.map))
        return createRuntime(generatedRegistry, mapId = generated.map.id, seed = seed)
    }

    fun createInitialState(
        registry: ContentRegistry = loadRegistry(),
        difficultyId: String? = null,
        mapId: String? = null,
        seed: Long = 7L,
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
            randomCursor = SeededRandom(seed).snapshot(),
        )
    }

    fun createRuntime(
        registry: ContentRegistry = loadRegistry(),
        difficultyId: String? = null,
        mapId: String? = null,
        seed: Long = 7L,
    ): SandboxRuntime =
        SandboxRuntime(createInitialState(registry, difficultyId, mapId, seed), seed = seed)

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

    /**
     * Typed-damage replay scenario. The typed registry is assembled in memory so the scenario does
     * not change the legacy sandbox pack or its canonical replay hashes. Resistance is static
     * content metadata and therefore does not enter the save format.
     */
    fun runScriptedResistScenario(seed: Long = 7, resistPercent: Int = 50): SandboxScenarioResult {
        require(resistPercent in 0..100) { "Resistance must be between 0 and 100 percent." }
        return runScriptedTypedScenario(
            registry = typedReplayRegistry(resistPercent),
            towerPosition = TilePosition(2, 2),
            seed = seed,
        )
    }

    /** Control replay for the same typed scenario with no resistance. */
    fun runScriptedUnresistedScenario(seed: Long = 7): SandboxScenarioResult =
        runScriptedResistScenario(seed = seed, resistPercent = 0)

    private fun runScriptedScenario(
        towerPosition: TilePosition,
        seed: Long,
        difficultyId: String?,
        mapId: String?,
    ): SandboxScenarioResult {
        val registry = loadRegistry(difficultyId = difficultyId)
        return runScriptedTypedScenario(registry, towerPosition, seed, mapId)
    }

    private fun runScriptedTypedScenario(
        registry: ContentRegistry,
        towerPosition: TilePosition,
        seed: Long,
        mapId: String? = null,
    ): SandboxScenarioResult {
        val runtime = createRuntime(registry, mapId = mapId, seed = seed)
        runtime.submit(BuildTowerCommand(dev.myengine.core.CommandId(1), Tick(1), "pulse", TileCoordinate(towerPosition.x, towerPosition.y)))
        runtime.step(35)
        val save = SandboxSaveCodec.encode(runtime.state, seed)
        return SandboxScenarioResult(runtime.state.stableHash(), runtime.snapshot(), save, runtime.state.defense.metrics)
    }

    private fun typedReplayRegistry(resistPercent: Int): ContentRegistry {
        val base = loadRegistry()
        val damageTypeId = "arcane"
        return base.copy(
            strings = base.strings + ("damage.arcane" to "Arcane"),
            damageTypes = mapOf(damageTypeId to DamageTypeContent(damageTypeId, "damage.arcane")),
            towers = base.towers.mapValues { (_, tower) ->
                tower.copy(damageTypeId = damageTypeId)
            },
            enemies = base.enemies.mapValues { (_, enemy) ->
                enemy.copy(resists = mapOf(damageTypeId to resistPercent))
            },
        )
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
                    resourceNode = mapping.resourceNode?.let { ResourceNode(it.resourceId, it.amount, it.infinite) },
                )
            }
        }
        return TileWorld(WorldSize(map.width, map.height), terrain, worldTiles)
    }
}
