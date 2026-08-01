package dev.myengine.core

class SeededRandom(seed: Long) {
    private var state: Long = seed

    fun nextLong(): Long {
        state += GOLDEN_GAMMA
        var z = state
        z = (z xor (z ushr 30)) * -4658895280553007687L
        z = (z xor (z ushr 27)) * -7723592293110705685L
        return z xor (z ushr 31)
    }

    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive." }
        return (nextLong().ushr(1) % bound.toLong()).toInt()
    }

    fun nextDouble(): Double = nextLong().ushr(11).toDouble() * DOUBLE_UNIT

    fun fork(streamName: String): SeededRandom {
        val hash = StableHash().add(state).add(streamName).digest().toULong(radix = 16).toLong()
        return SeededRandom(hash)
    }

    fun snapshot(): Long = state

    /** Restores the exact internal cursor captured by [snapshot]. */
    fun restore(snapshot: Long): SeededRandom {
        state = snapshot
        return this
    }

    /** Creates an independent stream positioned at an existing cursor. */
    companion object {
        fun fromSnapshot(snapshot: Long): SeededRandom = SeededRandom(0L).restore(snapshot)

        private const val GOLDEN_GAMMA: Long = -7046029254386353131L
        private const val DOUBLE_UNIT: Double = 1.0 / (1L shl 53).toDouble()
    }
}
