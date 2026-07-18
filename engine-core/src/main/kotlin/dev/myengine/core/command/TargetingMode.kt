package dev.myengine.core.command

/**
 * Content-defined priority used by a tower when more than one reachable enemy is in range.
 * [id] is stable in content and command/save payloads; enum names are deliberately not serialized.
 */
enum class TargetingMode(val id: String) {
    FIRST("first"),
    LAST("last"),
    NEAREST("nearest"),
    STRONGEST("strongest"),
    WEAKEST("weakest");

    companion object {
        fun fromId(id: String): TargetingMode? = entries.firstOrNull { it.id == id }
    }
}
