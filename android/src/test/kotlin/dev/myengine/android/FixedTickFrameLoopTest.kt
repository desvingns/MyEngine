package dev.myengine.android

import kotlin.test.Test
import kotlin.test.assertEquals

class FixedTickFrameLoopTest {
    @Test
    fun `accumulates choreographer frames into fixed twenty hertz ticks`() {
        val loop = FixedTickFrameLoop()
        loop.start()

        assertEquals(0, loop.advance(0L))
        assertEquals(0, loop.advance(25_000_000L))
        assertEquals(1, loop.advance(50_000_000L))
        assertEquals(2, loop.advance(175_000_000L))
    }

    @Test
    fun `resume discards paused wall clock time`() {
        val loop = FixedTickFrameLoop()
        loop.start()
        loop.advance(0L)
        assertEquals(1, loop.advance(50_000_000L))

        loop.stop()
        loop.start()

        assertEquals(0, loop.advance(10_000_000_000L))
        assertEquals(1, loop.advance(10_050_000_000L))
    }

    @Test
    fun `long frames are bounded before reaching the scheduler`() {
        val loop = FixedTickFrameLoop()
        loop.start()

        loop.advance(0L)

        assertEquals(5, loop.advance(5_000_000_000L))
    }
}
