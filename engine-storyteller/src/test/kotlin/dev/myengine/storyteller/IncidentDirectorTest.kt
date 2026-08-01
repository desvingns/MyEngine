package dev.myengine.storyteller

import dev.myengine.content.IncidentContent
import dev.myengine.core.SeededRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class IncidentDirectorTest {
    @Test
    fun selectionIsDeterministic() {
        val director = IncidentDirector(
            listOf(
                IncidentContent("spark", 0, 5, 1),
                IncidentContent("surge", 0, 5, 2),
            ),
        )

        val first = List(5) { director.select(3, SeededRandom(9)) }
        val second = List(5) { director.select(3, SeededRandom(9)) }

        assertEquals(first, second)
    }

    @Test
    fun noEligibleIncidentReturnsNull() {
        val director = IncidentDirector(listOf(IncidentContent("spark", 0, 5, 1)))

        assertNull(director.select(9, SeededRandom(1)))
    }

    @Test
    fun statefulSelectionHonorsCadencePacingAndCooldown() {
        val director = IncidentDirector(
            incidents = listOf(
                IncidentContent(
                    id = "pulse",
                    minThreat = 0,
                    maxThreat = 10,
                    weight = 1,
                    cadenceStartTick = 2,
                    cadenceIntervalTicks = 2,
                    cadenceEndTick = 8,
                    cooldownTicks = 3,
                ),
            ),
            random = SeededRandom(4),
        )

        assertNull(director.select(1, 3))
        assertNotNull(director.select(2, 3))
        assertNull(director.select(2, 3))
        assertNull(director.select(4, 3), "cooldown blocks the next cadence window")
        assertNotNull(director.select(6, 3))
        assertEquals(listOf(2L, 6L), director.state().executions.map { it.tick })
    }

    @Test
    fun statefulSelectionReturnsEmptyWhenNoCadenceOrCandidates() {
        val director = IncidentDirector(
            listOf(IncidentContent("legacy", 0, 5, 1)),
            SeededRandom(1),
        )

        assertNull(director.select(1, 3))
        assertNull(director.select(1, 99))
        assertEquals(0, director.state().executions.size)
    }
}
