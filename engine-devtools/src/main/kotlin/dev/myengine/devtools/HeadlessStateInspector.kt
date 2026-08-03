package dev.myengine.devtools

import java.nio.file.Path
import java.util.ServiceLoader

/**
 * Game-owned headless scenario surface consumed by the generic state inspector.
 *
 * A scenario owns command parsing and authoritative state access. The inspector only coordinates
 * the optional script, advances ticks, and serializes the stable projection below.
 */
interface HeadlessScenario {
    val scenarioId: String
    val packId: String?

    fun submitScriptCommand(command: String)

    fun step(ticks: Int)

    fun asciiFrame(): String

    fun stateDump(): HeadlessStateDump
}

/** Provider SPI: a game module can register one or more scenario adapters without changing the inspector. */
interface HeadlessScenarioFactory {
    val id: String

    fun create(packRoot: Path, scenarioId: String, seed: Long): HeadlessScenario
}

data class HeadlessEntityDump(
    val id: Long,
    val type: String,
    val x: Int?,
    val y: Int?,
    val health: Int?,
    val maxHealth: Int?,
    val inventory: Map<String, Int> = emptyMap(),
) {
    fun toJson(): String = buildJson(
        "id" to id,
        "type" to type,
        "position" to RawJson(buildJson("x" to x, "y" to y)),
        "health" to health,
        "max_health" to maxHealth,
        "inventory" to RawJson(sortedIntMapJson(inventory)),
    )
}

data class HeadlessTowerMetricDump(
    val actualDamage: Long,
    val kills: Int,
) {
    fun toJson(): String = buildJson(
        "actual_damage" to actualDamage,
        "kills" to kills,
    )
}

data class HeadlessDefenseDump(
    val coreHealth: Int,
    val enemiesSpawned: Int,
    val enemiesKilled: Int,
    val enemiesLeaked: Int,
    val coreDamage: Int,
    val towerShots: Int,
    val towerMetrics: Map<Long, HeadlessTowerMetricDump> = emptyMap(),
) {
    fun toJson(): String {
        val towers = towerMetrics.toSortedMap().entries.joinToString(",") { (id, metric) ->
            "\"$id\":${metric.toJson()}"
        }
        return buildJson(
            "core_health" to coreHealth,
            "enemies_spawned" to enemiesSpawned,
            "enemies_killed" to enemiesKilled,
            "enemies_leaked" to enemiesLeaked,
            "core_damage" to coreDamage,
            "tower_shots" to towerShots,
            "tower_metrics" to RawJson("{$towers}"),
        )
    }
}

data class HeadlessStateDump(
    val tick: Long,
    val entities: List<HeadlessEntityDump>,
    val inventories: Map<String, Map<String, Int>>,
    val defenseMetrics: HeadlessDefenseDump,
    val hash: String,
) {
    fun toJson(): String {
        val entityJson = entities.sortedBy { it.id }.joinToString(",") { it.toJson() }
        val inventoryJson = inventories.toSortedMap().entries.joinToString(",") { (owner, resources) ->
            "\"${escape(owner)}\":${sortedIntMapJson(resources)}"
        }
        return buildJson(
            "tick" to tick,
            "entities" to RawJson("[$entityJson]"),
            "inventories" to RawJson("{$inventoryJson}"),
            "defense_metrics" to RawJson(defenseMetrics.toJson()),
            "hash" to hash,
        )
    }
}

data class HeadlessInspectionReport(
    val factoryId: String,
    val scenarioId: String,
    val packId: String?,
    val requestedTicks: Int,
    val asciiFrame: String,
    val state: HeadlessStateDump,
) {
    fun toJson(): String = buildJson(
        "factory" to factoryId,
        "scenario" to scenarioId,
        "pack_id" to packId,
        "requested_ticks" to requestedTicks,
        "ascii_frame" to asciiFrame,
        "state" to RawJson(state.toJson()),
    )
}

/** Coordinates any registered game adapter and emits a deterministic inspection report. */
class HeadlessStateInspector(
    factories: List<HeadlessScenarioFactory> = discoverFactories(),
) {
    private val factoriesById = factories
        .sortedBy { it.id }
        .associateBy { it.id }

    fun inspect(
        factoryId: String,
        scenarioId: String,
        packRoot: Path,
        ticks: Int,
        commandScript: List<String> = emptyList(),
        seed: Long = 7L,
    ): HeadlessInspectionReport {
        require(ticks >= 0) { "Headless inspection ticks must be non-negative." }
        val factory = factoriesById[factoryId]
            ?: error("Unknown headless scenario factory '$factoryId'. Available: ${factoriesById.keys.sorted().joinToString(", ")}.")
        val scenario = factory.create(packRoot, scenarioId, seed)
        commandScript
            .asSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .forEach(scenario::submitScriptCommand)
        scenario.step(ticks)
        return HeadlessInspectionReport(
            factoryId = factory.id,
            scenarioId = scenario.scenarioId,
            packId = scenario.packId,
            requestedTicks = ticks,
            asciiFrame = scenario.asciiFrame(),
            state = scenario.stateDump(),
        )
    }

    companion object {
        fun discoverFactories(): List<HeadlessScenarioFactory> =
            ServiceLoader.load(HeadlessScenarioFactory::class.java)
                .toList()
                .sortedBy { it.id }
    }
}

private fun sortedIntMapJson(values: Map<String, Int>): String =
    values.toSortedMap().entries.joinToString(",", prefix = "{", postfix = "}") { (key, value) ->
        "\"${escape(key)}\":$value"
    }
