package dev.myengine.android

import dev.myengine.core.Tick
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InputTickPolicyTest {
    @Test
    fun schedulesInputForTheNextRepresentableTick() {
        assertEquals(Tick(42), nextInputTickOrNull(Tick(41)))
    }

    @Test
    fun ignoresInputWhenTheClockHasReachedItsTerminalBoundary() {
        assertNull(nextInputTickOrNull(Tick(Long.MAX_VALUE)))
    }

    @Test
    fun ignoresInputWhenTheGameRunIsTerminal() {
        assertNull(nextInputTickOrNull(Tick(41), terminal = true))
    }
}
