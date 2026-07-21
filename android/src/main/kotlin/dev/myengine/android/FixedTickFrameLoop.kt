package dev.myengine.android

import dev.myengine.core.TickRate
import dev.myengine.core.TickScheduler

/**
 * Presentation-side Choreographer timing policy. It deliberately owns no simulation state.
 *
 * The first frame after [start] returns zero ticks, so pause/resume never converts background
 * time into a simulation catch-up. The Android shell uses 20 Hz (50 ms) for this first playable
 * surface; deterministic replay remains defined by discrete ticks and ordered commands.
 */
class FixedTickFrameLoop(private val tickRate: TickRate = TickRate(TICKS_PER_SECOND)) {
    private var scheduler = TickScheduler(tickRate)
    private var lastFrameNanos: Long? = null
    var presentationSpeed: PresentationSpeed = PresentationSpeed.ONE_X

    fun start() {
        scheduler = TickScheduler(tickRate)
        lastFrameNanos = null
    }

    fun stop() {
        lastFrameNanos = null
    }

    fun advance(frameTimeNanos: Long): Int {
        val previous = lastFrameNanos
        lastFrameNanos = frameTimeNanos
        if (previous == null) return 0
        val elapsedNanos = (frameTimeNanos - previous).coerceIn(0L, MAX_ELAPSED_NANOS)
        if (presentationSpeed == PresentationSpeed.PAUSED) return 0
        val dueTicks = scheduler.advance(elapsedNanos.toDouble() / NANOS_PER_SECOND)
        return (dueTicks.toLong() * presentationSpeed.multiplier)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    internal companion object {
        const val TICKS_PER_SECOND = 20
        private const val NANOS_PER_SECOND = 1_000_000_000.0
        private const val MAX_ELAPSED_NANOS = 250_000_000L
    }
}
