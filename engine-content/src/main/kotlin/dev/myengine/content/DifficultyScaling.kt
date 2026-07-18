package dev.myengine.content

import java.math.BigDecimal
import java.math.RoundingMode

/** Deterministic decimal scaling used while materializing a difficulty-specific registry. */
internal object DifficultyScaling {
    fun scalePopulation(base: Int, multiplier: BigDecimal): Int =
        BigDecimal.valueOf(base.toLong())
            .multiply(multiplier)
            .setScale(0, RoundingMode.FLOOR)
            .intValueExact()
            .coerceAtLeast(1)

    /** Applies rewardMult, then goldRateMult, and rounds only the final payout half-up. */
    fun scalePayout(base: Int, difficulty: DifficultyContent): Int =
        BigDecimal.valueOf(base.toLong())
            .multiply(difficulty.rewardMult)
            .multiply(difficulty.goldRateMult)
            .setScale(0, RoundingMode.HALF_UP)
            .intValueExact()
}
