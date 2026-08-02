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
fun EnemyContent.effectiveStats(waveModifier: WaveModifier? = null): EffectiveEnemyStats {
    val health = scalePopulation(health, healthScalePercent)
    val speed = scalePopulation(speedTilesPerTick, speedScalePercent)
    val reward = scaleReward(rewardAmount, rewardScalePercent)
    return EffectiveEnemyStats(
        health = waveModifier?.let { scalePopulation(health, it.healthPercent) } ?: health,
        speedTilesPerTick = waveModifier?.let { scalePopulation(speed, it.speedPercent) } ?: speed,
        rewardAmount = reward,
    )
}

private fun scalePopulation(base: Int, percent: Int): Int =
    (base.toLong() * percent.toLong() / 100L)
        .coerceAtLeast(1L)
        .coerceAtMost(Int.MAX_VALUE.toLong())
        .toInt()

private fun scaleReward(base: Int, percent: Int): Int =
    BigDecimal.valueOf(base.toLong())
        .multiply(BigDecimal.valueOf(percent.toLong()))
        .divide(BigDecimal.valueOf(100L), 0, RoundingMode.HALF_UP)
        .min(BigDecimal.valueOf(Int.MAX_VALUE.toLong()))
        .intValueExact()
