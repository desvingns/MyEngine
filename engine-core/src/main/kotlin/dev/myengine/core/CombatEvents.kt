package dev.myengine.core

/** Stable ids used by transient presentation events and optional content sound mappings. */
enum class GameplayEventType(val id: String) {
    SHOT("shot"),
    HIT("hit"),
    DEATH("death"),
    WAVE_START("wave-start"),
    BUILD("build"),
    SELL("sell"),
    ;

    companion object {
        fun fromId(value: String): GameplayEventType? {
            val normalized = value.trim().lowercase().replace('_', '-')
            return entries.firstOrNull { it.id == normalized }
        }
    }
}

/** Immutable transient gameplay presentation event from one completed simulation tick. */
data class GameplayEvent(
    val tick: Tick,
    val type: GameplayEventType,
    val ordinal: Int = 0,
    val sourceEntityId: Long? = null,
    val targetEntityId: Long? = null,
    val contentId: String? = null,
) {
    init {
        require(ordinal >= 0) { "Gameplay event ordinal must be non-negative." }
    }

    /** Alias for consumers that describe the stable per-tick ordinal as a sequence. */
    val sequence: Int get() = ordinal
}

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
    /** Ordered immutable feed containing all transient gameplay events for this tick. */
    val gameplayEvents: List<GameplayEvent> = emptyList(),
) {
    /** Short alias for presentation consumers. */
    val events: List<GameplayEvent> get() = gameplayEvents

    companion object {
        val EMPTY: CombatEvents = CombatEvents()
    }
}
