package dev.myengine.core

import java.util.Collections

/** Lifecycle state of one deterministic simulation run. */
enum class RunStatus {
    ACTIVE,
    WON,
    LOST,
}

/** Stable terminal causes currently supported by the defense sandbox. */
enum class TerminalReason {
    ALL_WAVES_CLEARED,
    CORE_HEALTH_EXHAUSTED,
    LEAK_BUDGET_EXHAUSTED,
}

/**
 * Immutable presentation-safe totals captured when a run ends, or projected from an active run.
 * Resource entries are always created in stable key order by the owning simulation.
 */
class RunSummary(
    val waves: Int = 0,
    val kills: Int = 0,
    val leaks: Int = 0,
    resources: Map<String, Int> = emptyMap(),
    val ticks: Tick = Tick(0),
) {
    /** Sorted defensive copy; snapshot consumers cannot mutate simulation-owned resource totals. */
    val resources: Map<String, Int> = Collections.unmodifiableMap(resources.toSortedMap())

    init {
        require(waves >= 0) { "Wave count must be non-negative." }
        require(kills >= 0) { "Kill count must be non-negative." }
        require(leaks >= 0) { "Leak count must be non-negative." }
        require(resources.values.all { it >= 0 }) { "Resource totals must be non-negative." }
    }

    fun appendHash(hash: StableHash) {
        hash.add(waves).add(kills).add(leaks).add(ticks.value)
        resources.toSortedMap().forEach { (id, amount) -> hash.add(id).add(amount) }
    }

    fun copy(
        waves: Int = this.waves,
        kills: Int = this.kills,
        leaks: Int = this.leaks,
        resources: Map<String, Int> = this.resources,
        ticks: Tick = this.ticks,
    ): RunSummary = RunSummary(waves, kills, leaks, resources, ticks)

    operator fun component1(): Int = waves
    operator fun component2(): Int = kills
    operator fun component3(): Int = leaks
    operator fun component4(): Map<String, Int> = resources
    operator fun component5(): Tick = ticks

    override fun equals(other: Any?): Boolean = other is RunSummary &&
        waves == other.waves &&
        kills == other.kills &&
        leaks == other.leaks &&
        resources == other.resources &&
        ticks == other.ticks

    override fun hashCode(): Int {
        var result = waves
        result = 31 * result + kills
        result = 31 * result + leaks
        result = 31 * result + resources.hashCode()
        result = 31 * result + ticks.hashCode()
        return result
    }

    override fun toString(): String =
        "RunSummary(waves=$waves, kills=$kills, leaks=$leaks, resources=$resources, ticks=$ticks)"
}

/**
 * Authoritative terminal state. Active runs intentionally have no frozen [summary]; callers can
 * project a current [RunSummary] until the deterministic terminal boundary captures one.
 */
data class RunState(
    val status: RunStatus = RunStatus.ACTIVE,
    val terminalReason: TerminalReason? = null,
    val terminalTick: Tick? = null,
    val summary: RunSummary? = null,
) {
    init {
        val isActive = status == RunStatus.ACTIVE
        require(isActive == (terminalReason == null)) { "Active state and terminal reason must agree." }
        require(isActive == (terminalTick == null)) { "Active state and terminal tick must agree." }
        require(isActive == (summary == null)) { "Active state and terminal summary must agree." }
    }

    val isTerminal: Boolean
        get() = status != RunStatus.ACTIVE

    fun appendHash(hash: StableHash) {
        hash.add(status.name)
        hash.add(terminalReason?.name ?: "")
        hash.add(terminalTick?.value ?: -1L)
        summary?.appendHash(hash)
    }
}
