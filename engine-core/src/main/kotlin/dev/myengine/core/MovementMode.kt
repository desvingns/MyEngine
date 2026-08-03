package dev.myengine.core

/** Deterministic movement policy for content-defined moving entities. */
enum class MovementMode(val id: String) {
    GROUND("ground"),
    AIR("air"),
    ;

    companion object {
        fun fromId(id: String): MovementMode? = entries.firstOrNull { it.id == id.trim().lowercase() }
    }
}
