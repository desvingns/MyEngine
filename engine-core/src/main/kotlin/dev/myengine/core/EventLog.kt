package dev.myengine.core

enum class EventScope {
    AUTHORITATIVE,
    PRESENTATION,
}

data class SimulationEvent(
    val tick: Tick,
    val type: String,
    val scope: EventScope,
    val payload: String,
)

class EventLog {
    private val events = mutableListOf<SimulationEvent>()

    fun record(event: SimulationEvent) {
        events += event
    }

    fun all(): List<SimulationEvent> = events.toList()

    fun clearPresentation() {
        events.removeAll { it.scope == EventScope.PRESENTATION }
    }
}
