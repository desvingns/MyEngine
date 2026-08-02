package dev.myengine.defense

/**
 * Deterministic integer damage calculation shared by direct and splash hits.
 *
 * All scaling is multiplied before the single final floor division. The Long intermediates are
 * sufficient for the Int content ranges: base damage and both percentage factors remain bounded
 * before the final division by 10,000.
 */
object DamageFormula {
    fun effectiveDamage(
        baseDamage: Int,
        distance: Int,
        falloffPercent: Int,
        resistPercent: Int,
    ): Int {
        require(baseDamage >= 0) { "Base damage cannot be negative." }
        require(distance >= 0) { "Damage distance cannot be negative." }
        require(falloffPercent >= 0) { "Damage falloff cannot be negative." }
        require(resistPercent in 0..100) { "Resistance must be between 0 and 100 percent." }
        val remainingPercent = (100L - distance.toLong() * falloffPercent.toLong()).coerceAtLeast(0L)
        return (
            baseDamage.toLong() * remainingPercent * (100L - resistPercent.toLong()) / 10_000L
            ).toInt()
    }
}
