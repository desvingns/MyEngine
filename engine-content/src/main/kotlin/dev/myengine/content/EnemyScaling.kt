package dev.myengine.content

import java.math.BigDecimal
import java.math.RoundingMode

/** Effective immutable stats used by a spawned enemy entity. */
data class EffectiveEnemyStats(
    val health: Int,
    val speedTilesPerTick: Int,
    val rewardAmount: Int,
)

/**
 * Applies content scaling first and an optional wave modifier second. Population values use
 * integer floor semantics with a minimum of one; rewards use deterministic half-up rounding.
 */
fun EnemyContent.effectiveStats(
    waveModifier: WaveModifier? = null,
    waveHealthPercent: Long = 100L,
    waveRewardPercent: Long = 100L,
): EffectiveEnemyStats {
    val health = scalePopulation(health, healthScalePercent)
    val speed = scalePopulation(speedTilesPerTick, speedScalePercent)
    val reward = scaleReward(rewardAmount, rewardScalePercent)
    return EffectiveEnemyStats(
        health = scalePopulation(
            waveModifier?.let { scalePopulation(health, it.healthPercent) } ?: health,
            waveHealthPercent,
        ),
        speedTilesPerTick = waveModifier?.let { scalePopulation(speed, it.speedPercent) } ?: speed,
        rewardAmount = scaleReward(
            reward,
            waveModifier?.rewardPercent?.toLong()?.takeIf { it != 100L } ?: waveRewardPercent,
        ),
    )
}

private fun scalePopulation(base: Int, percent: Int): Int = scalePopulation(base, percent.toLong())

private fun scalePopulation(base: Int, percent: Long): Int =
    saturatingMultiplyDiv100(base.toLong(), percent)
        .coerceAtLeast(1L)
        .coerceAtMost(Int.MAX_VALUE.toLong())
        .toInt()

private fun scaleReward(base: Int, percent: Int): Int = scaleReward(base, percent.toLong())

private fun scaleReward(base: Int, percent: Long): Int =
    BigDecimal.valueOf(base.toLong())
        .multiply(BigDecimal.valueOf(percent))
        .divide(BigDecimal.valueOf(100L), 0, RoundingMode.HALF_UP)
        .min(BigDecimal.valueOf(Int.MAX_VALUE.toLong()))
        .intValueExact()

private fun saturatingMultiplyDiv100(value: Long, multiplier: Long): Long {
    if (value <= 0L || multiplier <= 0L) return 0L
    return BigDecimal.valueOf(value)
        .multiply(BigDecimal.valueOf(multiplier))
        .divide(BigDecimal.valueOf(100L), 0, RoundingMode.FLOOR)
        .min(BigDecimal.valueOf(Long.MAX_VALUE))
        .longValueExact()
}
