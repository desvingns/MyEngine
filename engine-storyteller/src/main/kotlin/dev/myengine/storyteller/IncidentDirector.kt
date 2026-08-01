package dev.myengine.storyteller

import dev.myengine.content.IncidentContent
import dev.myengine.content.IncidentEffectDescriptor
import dev.myengine.core.SeededRandom

data class IncidentExecution(
    val tick: Long,
    val incidentId: String,
    val threat: Int,
    val effects: List<IncidentEffectDescriptor>,
)

/** Serializable storyteller state; maps and history are always exposed in deterministic order. */
data class IncidentDirectorState(
    val cooldownUntil: Map<String, Long> = emptyMap(),
    val lastSelectionTick: Long? = null,
    val lastSelectionId: String? = null,
    val executions: List<IncidentExecution> = emptyList(),
) {
    fun canonical(): IncidentDirectorState = copy(
        cooldownUntil = cooldownUntil.toSortedMap(),
        executions = executions.toList(),
    )
}

data class IncidentSelection(
    val incidentId: String,
    val threat: Int,
    val tick: Long = -1,
    val effects: List<IncidentEffectDescriptor> = emptyList(),
)

/**
 * Deterministic, stateful incident selector. It only chooses content; a game owns execution of
 * the returned typed effects. Legacy callers can still use the two-argument overload, while the
 * simulation path uses one persistent random stream and the stateful tick-aware overload.
 */
class IncidentDirector(
    incidents: Collection<IncidentContent>,
    private val random: SeededRandom = SeededRandom(0L),
    initialState: IncidentDirectorState = IncidentDirectorState(),
) {
    private val incidentsById: List<IncidentContent> = incidents.sortedBy { it.id }
    private var state: IncidentDirectorState = initialState.canonical()

    fun state(): IncidentDirectorState = state.canonical()

    fun restore(restored: IncidentDirectorState): IncidentDirector {
        state = restored.canonical()
        return this
    }

    fun select(tick: Long, threatBudget: Int): IncidentSelection? {
        require(tick >= 0) { "Incident tick must be non-negative." }
        if (state.lastSelectionTick == tick) return null

        val eligible = incidentsById.filter { incident ->
            tick inCadenceOf incident &&
                threatBudget in incident.pacingMinThreat..incident.pacingMaxThreat &&
                tick >= (state.cooldownUntil[incident.id] ?: Long.MIN_VALUE)
        }
        if (eligible.isEmpty()) return null

        val totalWeight = eligible.fold(0L) { total, incident -> total + incident.weight }
        require(totalWeight in 1..Int.MAX_VALUE.toLong()) { "Incident weight total is out of range." }
        var cursor = random.nextInt(totalWeight.toInt())
        val selected = eligible.first { incident ->
            cursor -= incident.weight
            cursor < 0
        }
        val selection = IncidentSelection(
            incidentId = selected.id,
            threat = threatBudget,
            tick = tick,
            effects = selected.effects.toList(),
        )
        val nextCooldowns = state.cooldownUntil.toMutableMap().apply {
            if (selected.cooldownTicks > 0) put(selected.id, tick + selected.cooldownTicks.toLong())
            else remove(selected.id)
        }
        state = state.copy(
            cooldownUntil = nextCooldowns.toSortedMap(),
            lastSelectionTick = tick,
            lastSelectionId = selected.id,
            executions = state.executions + IncidentExecution(tick, selected.id, threatBudget, selected.effects.toList()),
        ).canonical()
        return selection
    }

    /** Compatibility overload retained for the pre-ENG-016 unit contract. */
    fun select(threatBudget: Int, random: SeededRandom): IncidentSelection? {
        val eligible = incidentsById
            .filter { threatBudget in it.minThreat..it.maxThreat }
        if (eligible.isEmpty()) return null
        val totalWeight = eligible.sumOf { it.weight }
        var cursor = random.nextInt(totalWeight)
        val selected = eligible.first { incident ->
            cursor -= incident.weight
            cursor < 0
        }
        return IncidentSelection(selected.id, threatBudget, effects = selected.effects.toList())
    }

    private infix fun Long.inCadenceOf(incident: IncidentContent): Boolean {
        if (incident.cadenceIntervalTicks <= 0 || this < incident.cadenceStartTick) return false
        val cadenceEndTick = incident.cadenceEndTick
        if (cadenceEndTick != null && this > cadenceEndTick) return false
        return (this - incident.cadenceStartTick) % incident.cadenceIntervalTicks == 0L
    }
}
