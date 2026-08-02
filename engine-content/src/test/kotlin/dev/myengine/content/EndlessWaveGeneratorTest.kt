package dev.myengine.content

import dev.myengine.core.SeededRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class EndlessWaveGeneratorTest {
    private val config = EndlessWaveContent(
        startTick = 10,
        intervalTicks = 5,
        compositionCycle = listOf(
            EndlessWaveComposition(listOf(WaveSpawn("scout", 2))),
            EndlessWaveComposition(listOf(WaveSpawn("scout", 1), WaveSpawn("brute", 1))),
        ),
        countGrowthPercent = 200,
        healthGrowthPercent = 150,
        rewardGrowthPercent = 125,
    )

    @Test
    fun sameSeedAndCursorReproduceWaveAndRngCursor() {
        val firstRandom = SeededRandom(17L)
        val secondRandom = SeededRandom(17L)

        val first = EndlessWaveGenerator.generate(config, waveNumber = 3, random = firstRandom)
        val second = EndlessWaveGenerator.generate(config, waveNumber = 3, random = secondRandom)

        assertEquals(first, second)
        assertEquals(firstRandom.snapshot(), secondRandom.snapshot())
        assertNotEquals(17L, firstRandom.snapshot(), "generation must consume the supplied RNG stream")
    }

    @Test
    fun growthIsContentDefinedAndStartTicksAreDeterministic() {
        val growthConfig = config.copy(compositionCycle = listOf(config.compositionCycle.first()))
        val wave = EndlessWaveGenerator.generate(growthConfig, waveNumber = 2, random = SeededRandom(17L))

        assertEquals(15L, wave.startTick)
        assertEquals(4, wave.spawns.sumOf { it.count })
        assertEquals(150L, wave.healthScalePercent)
        assertEquals(125L, wave.rewardScalePercent)
    }

    @Test
    fun zeroStartTickDoesNotOverflowToLongMax() {
        val zeroStart = config.copy(startTick = 0)

        assertEquals(0L, EndlessWaveGenerator.startTickFor(zeroStart, 1))
        assertEquals(5L, EndlessWaveGenerator.startTickFor(zeroStart, 2))
    }
}
