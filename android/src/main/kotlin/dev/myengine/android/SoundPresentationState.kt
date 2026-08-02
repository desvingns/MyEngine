package dev.myengine.android

/** Presentation-only audio controls; simulation state and saves do not contain these values. */
data class SoundPresentationState(
    val volume: Float = 1f,
    val muted: Boolean = false,
) {
    init {
        require(volume in 0f..1f) { "Sound volume must be between 0 and 1." }
    }

    fun withVolume(value: Float): SoundPresentationState = copy(volume = value.coerceIn(0f, 1f))

    fun withMuted(value: Boolean): SoundPresentationState = copy(muted = value)
}
