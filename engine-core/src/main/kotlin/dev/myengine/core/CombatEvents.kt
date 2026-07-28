package dev.myengine.core

/**
 * Immutable presentation data emitted when a tower begins a shot during one simulation tick.
 * It is intentionally not part of authoritative saved state: consumers may animate it, but
 * simulation never reads it back.
 */
data class ShotEvent(
    val sourceEntityId: Long,
    val targetEntityId: Long,
    val tick: Tick,
)

/** Immutable presentation data emitted for each enemy that receives non-zero tower damage. */
data class HitEvent(
    val sourceEntityId: Long,
    val targetEntityId: Long,
    val tick: Tick,
)

/** Read-only combat events from exactly one simulation tick. */
data class CombatEvents(
    val shots: List<ShotEvent> = emptyList(),
    val hits: List<HitEvent> = emptyList(),
) {
    companion object {
        val EMPTY: CombatEvents = CombatEvents()
    }
}
