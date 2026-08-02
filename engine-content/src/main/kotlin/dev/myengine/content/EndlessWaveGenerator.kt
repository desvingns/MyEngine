package dev.myengine.content

import dev.myengine.core.SeededRandom
import java.math.BigDecimal
import java.math.RoundingMode

/** Pure deterministic materialization of one pack-defined endless wave. */
object EndlessWaveGenerator {
    fun generate(
        config: EndlessWaveContent,
        waveNumber: Int,
        random: SeededRandom,
    ): WaveContent {
        require(waveNumber > 0) { "Endless wave number must be positive." }
        val cycleOffset = random.nextInt(config.compositionCycle.size)
        val cycleIndex = ((waveNumber - 1) % config.compositionCycle.size + cycleOffset) % config.compositionCycle.size
        val growthSteps = waveNumber - 1
        val composition = config.compositionCycle[cycleIndex]
        return WaveContent(
            id = idFor(waveNumber),
            startTick = startTickFor(config, waveNumber),
            spawns = composition.spawns.map { spawn ->
                spawn.copy(count = scaleCount(spawn.count, config.countGrowthPercent, growthSteps))
            },
            spawnSelection = config.spawnSelection,
            healthScalePercent = scalePercent(config.healthGrowthPercent, growthSteps),
            rewardScalePercent = scalePercent(config.rewardGrowthPercent, growthSteps),
            modifiers = emptyList(),
        )
    }

    fun idFor(waveNumber: Int): String {
        require(waveNumber > 0) { "Endless wave number must be positive." }
        return "endless-wave-$waveNumber"
    }

    fun startTickFor(config: EndlessWaveContent, waveNumber: Int): Long {
        require(waveNumber > 0) { "Endless wave number must be positive." }
        val offset = saturatingMultiply(config.intervalTicks, (waveNumber - 1).toLong())
        return if (config.startTick > Long.MAX_VALUE - offset) Long.MAX_VALUE else config.startTick + offset
    }

    private fun scaleCount(base: Int, growthPercent: Int, steps: Int): Int {
        var value = base.toLong()
        repeat(steps) {
            value = saturatingMultiplyDiv100(value, growthPercent.toLong())
                .coerceAtLeast(1L)
                .coerceAtMost(Int.MAX_VALUE.toLong())
        }
        return value.toInt()
    }

    private fun scalePercent(growthPercent: Int, steps: Int): Long {
        var value = 100L
        repeat(steps) {
            value = saturatingMultiplyDiv100(value, growthPercent.toLong()).coerceAtLeast(1L)
        }
        return value
    }

    private fun saturatingMultiply(value: Long, multiplier: Long): Long = when {
        value == 0L || multiplier == 0L -> 0L
        value > Long.MAX_VALUE / multiplier -> Long.MAX_VALUE
        else -> value * multiplier
    }

    private fun saturatingMultiplyDiv100(value: Long, multiplier: Long): Long =
        BigDecimal.valueOf(value)
            .multiply(BigDecimal.valueOf(multiplier))
            .divide(BigDecimal.valueOf(100L), 0, RoundingMode.FLOOR)
            .min(BigDecimal.valueOf(Long.MAX_VALUE))
            .longValueExact()
}
