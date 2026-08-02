package dev.myengine.android

import dev.myengine.core.CommandId
import dev.myengine.core.Tick
import dev.myengine.core.command.BuildTowerCommand
import dev.myengine.core.command.TileCoordinate
import dev.myengine.games.sandbox.SandboxGame
import dev.myengine.games.sandbox.SandboxSaveCodec
import dev.myengine.games.sandbox.SandboxSession
import java.io.StringReader
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

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

    @Test
    fun `all presentation speeds scale one due tick and paused returns no ticks`() {
        val ticksBySpeed = PresentationSpeed.entries.associateWith { speed ->
            FixedTickFrameLoop().also {
                it.presentationSpeed = speed
                it.start()
                assertEquals(0, it.advance(0L))
            }.advance(50_000_000L)
        }

        assertEquals(0, ticksBySpeed.getValue(PresentationSpeed.PAUSED))
        assertEquals(1, ticksBySpeed.getValue(PresentationSpeed.ONE_X))
        assertEquals(2, ticksBySpeed.getValue(PresentationSpeed.TWO_X))
        assertEquals(4, ticksBySpeed.getValue(PresentationSpeed.FOUR_X))
    }

    @Test
    fun `entering and leaving pause discards wall clock time without resetting simulation pacing`() {
        val loop = FixedTickFrameLoop()
        loop.start()
        assertEquals(0, loop.advance(0L))
        assertEquals(1, loop.advance(50_000_000L))

        loop.presentationSpeed = PresentationSpeed.PAUSED
        assertEquals(0, loop.advance(10_000_000_000L))

        loop.presentationSpeed = PresentationSpeed.ONE_X
        assertEquals(1, loop.advance(10_050_000_000L))
    }

    @Test
    fun `long timestamp and four times multiplier remain bounded`() {
        val loop = FixedTickFrameLoop().also {
            it.presentationSpeed = PresentationSpeed.FOUR_X
            it.start()
        }

        loop.advance(0L)

        // The elapsed-time cap is applied before the multiplier, so a huge frame timestamp
        // cannot overflow the due-tick or returned Int calculation.
        assertEquals(20, loop.advance(Long.MAX_VALUE))
    }

    @Test
    fun `invalid presentation speed values fall back to one times`() {
        assertEquals(PresentationSpeed.ONE_X, PresentationSpeed.fromMultiplier(-1))
        assertEquals(PresentationSpeed.ONE_X, PresentationSpeed.fromMultiplier(3))
        assertEquals(PresentationSpeed.ONE_X, PresentationSpeed.fromMultiplier(Int.MAX_VALUE))
    }

    @Test
    fun `one times and four times preserve the same per tick hash trajectory`() {
        val registry = SandboxGame.loadRegistry()
        val commands = scriptedCommands()

        val oneX = runPresentationTrajectory(
            speed = PresentationSpeed.ONE_X,
            registry = registry,
            commands = commands,
        )
        val fourX = runPresentationTrajectory(
            speed = PresentationSpeed.FOUR_X,
            registry = registry,
            commands = commands,
        )

        assertEquals(40, oneX.size)
        assertEquals(oneX, fourX)
        assertTrue(oneX.zipWithNext().any { (previous, current) -> previous != current })
        assertNotEquals(oneX.first(), oneX.last())
    }

    @Test
    fun `presentation speed is absent from sandbox save payload and version`() {
        val registry = SandboxGame.loadRegistry()
        val commands = scriptedCommands()
        val oneX = runPresentation(PresentationSpeed.ONE_X, registry, commands)
        val fourX = runPresentation(PresentationSpeed.FOUR_X, registry, commands)

        assertEquals(oneX.session.stableHash(), fourX.session.stableHash())
        assertEquals(
            saveProperties(oneX.session.save()),
            saveProperties(fourX.session.save()),
            "presentation speed must not alter the authoritative save payload",
        )
        assertEquals(15, SandboxSaveCodec.SAVE_VERSION)
        assertFalse(
            saveProperties(oneX.session.save()).stringPropertyNames().any {
                it.contains("speed", ignoreCase = true)
            },
        )
    }

    private fun scriptedCommands() = listOf(
        BuildTowerCommand(
            id = CommandId(1),
            scheduledTick = Tick(1),
            towerId = "pulse",
            position = TileCoordinate(2, 2),
        ),
    )

    private fun runPresentationTrajectory(
        speed: PresentationSpeed,
        registry: dev.myengine.content.ContentRegistry,
        commands: List<BuildTowerCommand>,
    ): List<String> = runPresentation(speed, registry, commands).trajectory

    private fun runPresentation(
        speed: PresentationSpeed,
        registry: dev.myengine.content.ContentRegistry,
        commands: List<BuildTowerCommand>,
    ): PresentationRun {
        val session = SandboxSession.start(registry = registry, seed = 123L)
        commands.forEach(session::submit)

        val loop = FixedTickFrameLoop().also {
            it.presentationSpeed = speed
            it.start()
        }
        var frameTimeNanos = 0L
        loop.advance(frameTimeNanos)
        val hashes = mutableListOf<String>()
        while (hashes.size < 40) {
            frameTimeNanos += 50_000_000L
            val dueTicks = loop.advance(frameTimeNanos)
            repeat(dueTicks) {
                session.step()
                hashes += session.stableHash()
            }
        }
        return PresentationRun(session, hashes)
    }

    private data class PresentationRun(
        val session: SandboxSession,
        val trajectory: List<String>,
    )

    private fun saveProperties(text: String): Properties = Properties().also {
        it.load(StringReader(text))
    }
}
