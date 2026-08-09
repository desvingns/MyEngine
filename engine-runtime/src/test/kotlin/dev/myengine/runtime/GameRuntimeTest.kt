package dev.myengine.runtime

import dev.myengine.core.TickRate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GameRuntimeTest {
    private val descriptor = GameRuntimeDescriptor(
        id = "fake-game",
        tickRate = TickRate(20),
        contentPackId = "fake-pack",
        contentPackVersion = "1.0.0",
        saveSchemaId = "fake-save",
        saveSchemaVersion = 1,
    )

    @Test
    fun sessionOwnsInsertionQueueAndDispatchesStableReadyOrder() {
        val backend = FakeBackend()
        val session = session(backend)
        val late = FakeCommand(id = 2, scheduledTick = 2, payload = "late")
        val early = FakeCommand(id = 1, scheduledTick = 1, payload = "early")

        assertTrue(session.submit(late).accepted)
        assertTrue(session.submit(early).accepted)
        assertEquals(listOf(late, early), session.pendingCommands())

        session.step()
        assertEquals(listOf("early"), backend.appliedPayloads)
        assertEquals(listOf(late), session.pendingCommands())

        session.step()
        assertEquals(listOf("early", "late"), backend.appliedPayloads)
        assertTrue(session.pendingCommands().isEmpty())
    }

    @Test
    fun saveReceivesOpaqueConcretePayloadAndPendingCommands() {
        val backend = FakeBackend()
        val future = FakeCommand(id = 3, scheduledTick = 8, payload = "future")
        val session = session(backend)

        session.submit(future)
        assertEquals(listOf(future), session.save())
        session.step(2)
        assertEquals(listOf(future), session.save())
    }

    @Test
    fun rejectedSubmissionNeverEntersQueueAndRestoreCanRejectExplicitly() {
        val backend = FakeBackend(terminal = true)
        val session = session(backend)
        val command = FakeCommand(id = 1, scheduledTick = 1, payload = "blocked")

        val result = session.submit(command)
        assertFalse(result.accepted)
        assertEquals("run_terminal", result.reason)
        assertTrue(session.pendingCommands().isEmpty())

        val rejected = FakeFactory(descriptor).restore("future-schema")
        assertIs<RestoreResult.Rejected>(rejected)
    }

    @Test
    fun stepIsPositiveAndBounded() {
        val session = session(FakeBackend())

        assertFailsWith<IllegalArgumentException> { session.step(0) }
        assertFailsWith<IllegalArgumentException> { session.step(QueuedGameSession.MAX_TICKS_PER_STEP + 1) }
    }

    private fun session(backend: FakeBackend): QueuedGameSession<FakeCommand, List<String>, List<FakeCommand>> =
        QueuedGameSession(
            descriptor = descriptor,
            ordering = FakeOrdering,
            backend = backend,
            saveFactory = { it },
        )

    private data class FakeCommand(
        val id: Long,
        val scheduledTick: Long,
        val payload: String,
    )

    private class FakeBackend(
        private var terminal: Boolean = false,
    ) : GameSessionBackend<FakeCommand, List<String>> {
        override var currentTick: Long = 0
            private set
        val appliedPayloads = mutableListOf<String>()

        override fun submit(command: FakeCommand): CommandSubmission =
            if (terminal) CommandSubmission.rejected("run_terminal") else CommandSubmission.accepted()

        override fun canStep(): Boolean = !terminal

        override fun step(commands: List<FakeCommand>) {
            currentTick += 1
            appliedPayloads += commands.map { it.payload }
        }

        override fun snapshot(): List<String> = appliedPayloads.toList()
    }

    private object FakeOrdering : CommandOrdering<FakeCommand> {
        override fun scheduledTick(command: FakeCommand): Long = command.scheduledTick

        override fun compare(left: FakeCommand, right: FakeCommand): Int =
            compareValuesBy(left, right, FakeCommand::id, FakeCommand::payload)
    }

    private class FakeFactory(
        override val descriptor: GameRuntimeDescriptor,
    ) : GameRuntimeFactory<FakeCommand, List<String>, String> {
        override fun start(seed: Long): GameSession<FakeCommand, List<String>, String> =
            QueuedGameSession(descriptor, FakeOrdering, FakeBackend(), saveFactory = { it.joinToString() })

        override fun restore(save: String): RestoreResult<GameSession<FakeCommand, List<String>, String>> =
            RestoreResult.Rejected("unsupported_save:$save")
    }
}
