package dev.myengine.games.sandbox

import dev.myengine.content.ContentPackLoader
import dev.myengine.content.ContentRegistry
import dev.myengine.core.CommandQueue
import dev.myengine.core.EngineCommand
import dev.myengine.core.EngineInfo
import dev.myengine.core.HashableState
import dev.myengine.core.StableHash
import dev.myengine.core.Tick
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
import dev.myengine.entities.TowerComponent
import dev.myengine.logistics.Inventory
import dev.myengine.logistics.Producer
import dev.myengine.logistics.ProducerSystem
import dev.myengine.render.BuildTowerCommand
import dev.myengine.render.DebugOverlay
import dev.myengine.render.EngineSnapshot
import dev.myengine.render.RenderEntity
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
    val world: TileWorld,
    val entities: EntityStore,
    var inventory: Inventory,
    var producers: List<Producer>,
    var defense: DefenseState,
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
    private val spawn = TilePosition(1, 1)
    private val core = TilePosition(32, 32)

    fun submit(command: EngineCommand) {
        commandQueue.submit(command)
    }

    /** Non-destructive snapshot of the runtime's not-yet-drained pending commands. */
    fun pendingCommands(): List<EngineCommand> = commandQueue.pending()

    /** Loads previously-decoded commands into this runtime's queue, preserving their original ids/ticks. */
    fun submitAll(commands: List<EngineCommand>) {
        commands.forEach(::submit)
    }

    fun step(ticks: Int = 1) {
        repeat(ticks) {
            state.tick = state.tick.next()
            val commands = commandQueue.drainFor(state.tick)
            commands.forEach(::applyCommand)
            updateProduction()
            state.defense = defenseRuntime.spawnDueWaves(state.tick, state.defense, state.registry, state.world, state.entities, spawn, core)
            val towerResult = defenseRuntime.updateTowers(state.registry, state.entities)
            state.defense = state.defense.record(towerResult.metrics)
            val deposit = depositRewards(state.inventory, towerResult.rewards)
            state.inventory = deposit.inventory
            if (deposit.dropped.isNotEmpty()) {
                // Non-fatal telemetry: a full inventory is a legitimate game state, not a crash,
                // but earned rewards must not vanish without a trace (surfaces in DebugOverlay).
                state.lastCommandOrError = deposit.dropped.entries
                    .joinToString(",", prefix = "reward_dropped:") { "${it.key}:${it.value}" }
            }
            state.defense = defenseRuntime.updateEnemies(state.registry, state.defense, state.entities)
            IncidentDirector(state.registry.incidents.values).select(state.defense.metrics.enemiesSpawned, dev.myengine.core.SeededRandom(17))
        }
    }

    fun snapshot(): EngineSnapshot {
        val renderTiles = state.world.positions().map {
            val view = state.world.tileAt(it)
            RenderTile(it, view.tile.terrainId, state.world.canBuild(it))
        }
        val renderEntities = state.entities.all().mapNotNull {
            val position = it.position?.tile ?: return@mapNotNull null
            RenderEntity(it.id.value, it.type, position, it.health?.current)
        }
        return EngineSnapshot(
            worldSize = state.world.size,
            tiles = renderTiles,
            entities = renderEntities,
            coreHealth = state.defense.coreHealth,
            debug = DebugOverlay(
                tick = state.tick,
                entityCount = state.entities.count(),
                wave = state.defense.spawnedWaveIds.maxOrNull(),
                selectedTile = null,
                lastCommandOrError = state.lastCommandOrError,
            ),
        )
    }

    private fun applyCommand(command: EngineCommand) {
        when (command) {
            is BuildTowerCommand -> buildTower(command)
            else -> state.lastCommandOrError = "ignored:${command.type}"
        }
    }

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
        when (val result = defenseRuntime.placeTower(command.towerId, command.position, state.registry, state.world, state.entities)) {
            is TowerPlacementResult.Placed -> {
                state.inventory = state.inventory.remove(tower.costResource, tower.costAmount)
                state.lastCommandOrError = "placed:${result.entityId.value}"
            }
            is TowerPlacementResult.Rejected -> state.lastCommandOrError = result.reason
        }
    }

    private fun updateProduction() {
        state.producers = state.producers.map { producer ->
            val result = producerSystem.tick(producer, state.inventory)
            state.inventory = result.inventory
            result.producer
        }
    }
}

object SandboxSaveCodec {
    const val SAVE_VERSION: Int = 2

    fun encode(state: SandboxState, seed: Long, pendingCommands: List<EngineCommand> = emptyList()): String {
        val props = Properties()
        props["saveVersion"] = SAVE_VERSION.toString()
        props["engineVersion"] = EngineInfo.SCAFFOLD_PHASE.toString()
        props["packId"] = state.registry.manifest.id
        props["packVersion"] = state.registry.manifest.version
        props["seed"] = seed.toString()
        props["tick"] = state.tick.value.toString()
        props["coreHealth"] = state.defense.coreHealth.toString()
        props["spawnedWaves"] = state.defense.spawnedWaveIds.sorted().joinToString(",")
        props["metrics"] = listOf(
            state.defense.metrics.enemiesSpawned,
            state.defense.metrics.enemiesKilled,
            state.defense.metrics.enemiesLeaked,
            state.defense.metrics.coreDamage,
            state.defense.metrics.towerShots,
        ).joinToString(",")
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
            ).joinToString("|")
        }
        props["pendingCommands"] = pendingCommands.joinToString(";") { cmd ->
            listOf(cmd.type, cmd.id.value, cmd.scheduledTick.value, cmd.actorId ?: "", cmd.stablePayload()).joinToString("|")
        }
        return StringWriter().also { props.store(it, "MyEngine sandbox save") }.toString()
    }

    fun decode(text: String, registry: ContentRegistry): SandboxState {
        val props = Properties().also { it.load(StringReader(text)) }
        val version = props.getProperty("saveVersion")?.toIntOrNull()
        require(version == 1 || version == 2) { "Unsupported save version '$version'." }
        val state = SandboxGame.createInitialState(registry)
        state.tick = Tick(props.getProperty("tick").toLong())
        val metrics = props.getProperty("metrics", "0,0,0,0,0").split(',').map { it.toInt() }
        state.defense = DefenseState(
            coreHealth = props.getProperty("coreHealth").toInt(),
            spawnedWaveIds = props.getProperty("spawnedWaves", "").split(',').filter { it.isNotBlank() }.toSet(),
            metrics = dev.myengine.defense.DefenseMetrics(metrics[0], metrics[1], metrics[2], metrics[3], metrics[4]),
        )
        state.inventory = Inventory(parseResources(props.getProperty("inventory", "")))
        state.producers = props.getProperty("producers", "")
            .split(';')
            .filter { it.isNotBlank() }
            .map {
                val parts = it.split('|')
                Producer(parts[0], parts[1], parts[2].toInt())
            }
        val entities = parseEntities(props.getProperty("entities", ""))
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
        return parsePendingCommands(props.getProperty("pendingCommands", ""))
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
                BuildTowerCommand(id, scheduledTick, payloadParts[0], TilePosition(payloadParts[1].toInt(), payloadParts[2].toInt()), actorId)
            } else {
                dev.myengine.core.TextCommand(id, scheduledTick, type, payload, actorId)
            }
        }

    private fun parseResources(text: String): Map<String, Int> =
        text.split(';').filter { it.isNotBlank() }.associate {
            val parts = it.split(':')
            parts[0] to parts[1].toInt()
        }

    private fun parseEntities(text: String): List<Entity> =
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
                tower = if (towerId != null && cooldown != null) TowerComponent(towerId, cooldown) else null,
                attack = if (range != null && damage != null && cooldownTicks != null) AttackComponent(range, damage, cooldownTicks) else null,
                movement = if (path.isNotEmpty() && pathIndex != null) MovementComponent(path, pathIndex) else null,
            )
        }
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

    fun loadRegistry(root: Path = contentRoot()): ContentRegistry {
        val result = ContentPackLoader.load(root)
        require(result.isValid) { result.errors.joinToString("\n") }
        return result.registry!!
    }

    fun createInitialState(registry: ContentRegistry = loadRegistry()): SandboxState {
        val world = createWorld(registry)
        return SandboxState(
            tick = Tick(0),
            registry = registry,
            world = world,
            entities = EntityStore(),
            inventory = Inventory(mapOf("bolt" to 6)),
            producers = listOf(Producer("generator-1", "bolt-generator")),
            defense = DefenseState(coreHealth = 20),
        )
    }

    fun createRuntime(registry: ContentRegistry = loadRegistry()): SandboxRuntime =
        SandboxRuntime(createInitialState(registry))

    /**
     * Canonical replay/benchmark scenario. The pulse tower at (30,32) is adjacent to the core and
     * never reaches the (1,1)->(32,32) enemy corridor within the 35-tick budget, so it kills nothing.
     * Its hash is the long-standing baseline; kept stable on purpose. Use [runScriptedKillScenario]
     * to exercise the kill+reward path.
     */
    fun runScriptedScenario(seed: Long = 7): SandboxScenarioResult =
        runScriptedScenario(TilePosition(30, 32), seed)

    /**
     * Second canonical scenario that DOES exercise kills and the reward-deposit path. The pulse
     * tower at (2,2) sits next to the enemy spawn (1,1), so wave enemies die within the 35-tick
     * budget and their content-derived rewards are deposited into the inventory. Proven to kill by
     * SandboxRewardDepositTest / SandboxVerticalSliceTest; its hash is a stable, kill-bearing gate.
     */
    fun runScriptedKillScenario(seed: Long = 7): SandboxScenarioResult =
        runScriptedScenario(TilePosition(2, 2), seed)

    private fun runScriptedScenario(towerPosition: TilePosition, seed: Long): SandboxScenarioResult {
        val registry = loadRegistry()
        val runtime = createRuntime(registry)
        runtime.submit(BuildTowerCommand(dev.myengine.core.CommandId(1), Tick(1), "pulse", towerPosition))
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

    private fun createWorld(registry: ContentRegistry): TileWorld {
        val terrain = registry.tiles.values.associate {
            it.id to TerrainRule(it.id, it.buildable, it.blocksMovement, it.isCore)
        }
        val world = TileWorld.filled(WorldSize(64, 64), terrain, "floor")
        for (x in 0 until 64) {
            world.setTile(TilePosition(x, 0), WorldTile("wall"))
            world.setTile(TilePosition(x, 63), WorldTile("wall"))
        }
        for (y in 0 until 64) {
            world.setTile(TilePosition(0, y), WorldTile("wall"))
            world.setTile(TilePosition(63, y), WorldTile("wall"))
        }
        world.setTile(TilePosition(32, 32), WorldTile("core"))
        world.setTile(TilePosition(5, 5), WorldTile("resource", ResourceNode("bolt", 100)))
        return world
    }
}
