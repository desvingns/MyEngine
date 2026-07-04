package dev.myengine.core

@JvmInline
value class Tick(val value: Long) : Comparable<Tick> {
    init {
        require(value >= 0) { "Tick must be non-negative." }
    }

    fun next(): Tick = Tick(value + 1)

    override fun compareTo(other: Tick): Int = value.compareTo(other.value)
}

data class TickRate(val ticksPerSecond: Int) {
    init {
        require(ticksPerSecond > 0) { "ticksPerSecond must be positive." }
    }

    val secondsPerTick: Double = 1.0 / ticksPerSecond.toDouble()
}

class TickScheduler(private val rate: TickRate) {
    private var carrySeconds: Double = 0.0

    fun advance(elapsedSeconds: Double): Int {
        require(elapsedSeconds >= 0.0) { "elapsedSeconds must be non-negative." }
        carrySeconds += elapsedSeconds
        val ticks = (carrySeconds / rate.secondsPerTick).toInt()
        if (ticks > 0) {
            carrySeconds -= ticks.toDouble() * rate.secondsPerTick
        }
        return ticks
    }
}
