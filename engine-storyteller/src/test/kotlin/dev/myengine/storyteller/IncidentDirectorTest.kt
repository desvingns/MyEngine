package dev.myengine.storyteller

import dev.myengine.content.IncidentContent
import dev.myengine.core.SeededRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
}
