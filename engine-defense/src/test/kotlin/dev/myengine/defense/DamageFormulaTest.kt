package dev.myengine.defense

import kotlin.test.Test
import kotlin.test.assertEquals

class DamageFormulaTest {
    @Test
    fun sevenDamageAtThirtyThreePercentResistanceFloorsToFour() {
        assertEquals(
            4,
            DamageFormula.effectiveDamage(
                baseDamage = 7,
                distance = 0,
                falloffPercent = 0,
                resistPercent = 33,
            ),
        )
    }

    @Test
    fun fiveDamageAtFiftyPercentResistanceFloorsToTwo() {
        assertEquals(
            2,
            DamageFormula.effectiveDamage(
                baseDamage = 5,
                distance = 0,
                falloffPercent = 0,
                resistPercent = 50,
            ),
        )
    }

    @Test
    fun fullResistanceProducesZeroDamage() {
        assertEquals(
            0,
            DamageFormula.effectiveDamage(
                baseDamage = 99,
                distance = 0,
                falloffPercent = 0,
                resistPercent = 100,
            ),
        )
    }

    @Test
    fun maximumIntDamageUsesLongIntermediatesWithoutOverflow() {
        assertEquals(
            Int.MAX_VALUE,
            DamageFormula.effectiveDamage(
                baseDamage = Int.MAX_VALUE,
                distance = 0,
                falloffPercent = 0,
                resistPercent = 0,
            ),
        )
    }

    @Test
    fun splashFalloffAndResistanceUseOneFinalFloor() {
        // 7 * (100 - 33) * (100 - 33) / 10_000 = 3.1429..., so the single final floor is 3.
        // Sequentially flooring falloff first would incorrectly produce 2.
        assertEquals(
            3,
            DamageFormula.effectiveDamage(
                baseDamage = 7,
                distance = 1,
                falloffPercent = 33,
                resistPercent = 33,
            ),
        )
    }
}
