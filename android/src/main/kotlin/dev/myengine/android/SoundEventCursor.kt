package dev.myengine.android

import dev.myengine.core.GameplayEvent

/** Pure snapshot cursor that deduplicates repeated renders of the same tick and event ordinal. */
data class SoundEventCursor(
    val lastTick: Long = -1L,
    val lastOrdinal: Int = -1,
) {
    fun observe(events: List<GameplayEvent>): SoundEventObservation {
        val newlyObserved = events
            .asSequence()
            .filter { event ->
                event.tick.value > lastTick ||
                    (event.tick.value == lastTick && event.ordinal > lastOrdinal)
            }
            .sortedWith(compareBy<GameplayEvent> { it.tick.value }.thenBy { it.ordinal })
            .toList()
        val next = newlyObserved.lastOrNull()?.let { event ->
            SoundEventCursor(event.tick.value, event.ordinal)
        } ?: this
        return SoundEventObservation(next, newlyObserved)
    }
}

data class SoundEventObservation(
    val cursor: SoundEventCursor,
    val events: List<GameplayEvent>,
)
