package dev.myengine.android

/** Valid presentation pacing modes for the Android shell. */
enum class PresentationSpeed(
    val multiplier: Int,
    val label: String,
) {
    PAUSED(0, "0x"),
    ONE_X(1, "1x"),
    TWO_X(2, "2x"),
    FOUR_X(4, "4x"),
    ;

    companion object {
        fun fromMultiplier(value: Int): PresentationSpeed =
            entries.firstOrNull { it.multiplier == value } ?: ONE_X
    }
}
