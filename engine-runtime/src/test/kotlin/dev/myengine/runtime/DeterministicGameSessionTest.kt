package dev.myengine.runtime

import dev.myengine.core.CommandId
import dev.myengine.core.EngineCommand
import dev.myengine.core.TextCommand
import dev.myengine.core.Tick
import dev.myengine.core.TickRate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalGameRuntimeApi::class)
class DeterministicGameSessionTest {
    @Test
    fun commandSubmissionUsesStableQueueOrderAndProducesTypedSnapshot() {
        val session = startedSession()
        session.submit(TextCommand(CommandId(9), Tick(1), "append", "B"))
        session.submit(TextCommand(CommandId(2), Tick(1), "append", "A"))

        val stepped = assertIs<SessionStepResult.Advanced>(session.step())
        val snapshot = session.snapshot()

        assertEquals(1, stepped.advancedTicks)
        assertEquals(Tick(1), stepped.tick)
        assertEquals(Tick(1), snapshot.tick)
        assertEquals("AB", snapshot.value.trace)
        assertEquals("1:AB", session.stableHash())
    }

    @Test
    fun snapshotDoesNotImplicitlyCalculateReplayHash() {
        val session = FakeSession(FakeDescriptor(), seed = 7)

        val snapshot = session.snapshot()

        assertEquals(Tick(0), snapshot.tick)
        assertEquals(FakeSnapshot(Tick(0), ""), snapshot.value)
        assertEquals(0, session.stableHashCalls)
        session.stableHash()
        assertEquals(1, session.stableHashCalls)
    }

    @Test
    fun duplicateCallerOwnedIdsAreAcceptedAndUseTheCompleteStableComparator() {
        val descriptor = FakeDescriptor()
        val session = startedSession(descriptor)

        val secondByPayload = session.submit(TextCommand(CommandId(5), Tick(1), "append", "B"))
        val firstByPayload = session.submit(TextCommand(CommandId(5), Tick(1), "append", "A"))
        session.step()

        assertEquals(CommandIdPolicy.CALLER_OWNED, descriptor.identity.commandIdPolicy)
        assertIs<SessionSubmitResult.Accepted>(secondByPayload)
        assertIs<SessionSubmitResult.Accepted>(firstByPayload)
        assertEquals("AB", session.snapshot().value.trace)
    }

    @Test
    fun commandsScheduledAtOrBeforeCurrentTickCatchUpOnTheNextTick() {
        val session = FakeSession(FakeDescriptor(), seed = 7, initialTick = Tick(3))
        session.submit(TextCommand(CommandId(2), Tick(3), "append", "C"))
        session.submit(TextCommand(CommandId(1), Tick(0), "append", "P"))

        val stepped = assertIs<SessionStepResult.Advanced>(session.step())

        assertEquals(Tick(4), stepped.tick)
        assertEquals("PC", session.snapshot().value.trace)
        assertEquals(emptyList(), session.pendingForTest())
    }

    @Test
    fun explicitInputBoundaryAppliesOnlyDueCommandsWithoutAdvancingTime() {
        val session = FakeSession(FakeDescriptor(), seed = 7, initialTick = Tick(3))
        session.submit(TextCommand(CommandId(5), Tick(4), "append", "future"))
        session.submit(TextCommand(CommandId(3), Tick(3), "append", "B"))
        session.submit(TextCommand(CommandId(2), Tick(3), "append", "A"))
        session.submit(TextCommand(CommandId(1), Tick(0), "append", "past"))

        session.applyInputBoundaryForTest()
        session.applyInputBoundaryForTest()

        assertEquals(Tick(3), session.currentTick)
        assertEquals("pastAB", session.snapshot().value.trace)
        assertEquals(listOf(CommandId(5)), session.pendingForTest().map { it.id })
        session.step()
        assertEquals(Tick(4), session.currentTick)
        assertEquals("pastABfuture", session.snapshot().value.trace)
    }

    @Test
    fun explicitInputBoundaryPreservesSaveRestoreWithFutureCommands() {
        val descriptor = FakeDescriptor()
        val session = FakeSession(descriptor, seed = 7)
        session.submit(TextCommand(CommandId(1), Tick(0), "append", "immediate"))
        session.submit(TextCommand(CommandId(2), Tick(3), "append", "future"))
        session.applyInputBoundaryForTest()
        val restored = restoredSession(descriptor, assertIs<SessionSaveResult.Saved>(session.save()).save)

        assertEquals(session.snapshot(), restored.snapshot())
        repeat(4) {
            session.step()
            restored.step()
            assertEquals(session.snapshot(), restored.snapshot())
            assertEquals(session.stableHash(), restored.stableHash())
        }
    }

    @Test
    fun explicitInputBoundaryNeverConsumesCommandsAfterTerminal() {
        val command = TextCommand(CommandId(1), Tick(1), "append", "not-applied")
        val session = FakeSession(FakeDescriptor(), 7, initialTick = Tick(1),
            pendingCommands = listOf(command), terminalAtTick = 1)

        session.applyInputBoundaryForTest()

        assertEquals(Tick(1), session.currentTick)
        assertEquals("", session.snapshot().value.trace)
        assertEquals(listOf(command), session.pendingForTest())
    }

    @Test
    fun orderedBatchKeepsAndAppliesStructurallyIdenticalDuplicateCommands() {
        val session = FakeSession(FakeDescriptor(), seed = 7)
        val duplicate = TextCommand(CommandId(5), Tick(1), "append", "X")

        val submitted = session.submitAll(listOf(duplicate, duplicate.copy()))
        session.step()

        assertEquals(2, submitted.size)
        assertTrue(submitted.all { it is SessionSubmitResult.Accepted })
        assertEquals("XX", session.snapshot().value.trace)
        assertEquals(emptyList(), session.pendingForTest())
    }

    @Test
    fun runtimeIdentityIsStructuralWhileSystemOrderIsDefensivelyCopied() {
        val mutableOrder = mutableListOf("commands", "fake-system")
        val identity = fakeIdentity(mutableOrder)
        val equalIdentity = fakeIdentity(listOf("commands", "fake-system"))

        mutableOrder += "late-mutation"

        assertEquals(equalIdentity, identity)
        assertEquals(equalIdentity.hashCode(), identity.hashCode())
        assertEquals(listOf("commands", "fake-system"), identity.stableSystemOrder)
        assertTrue("id=fake" in identity.toString())
        assertFailsWith<UnsupportedOperationException> {
            @Suppress("UNCHECKED_CAST")
            (identity.stableSystemOrder as MutableList<String>) += "illegal-mutation"
        }
    }

    @Test
    fun stepRejectsNonPositiveAndOverBoundCountsWithoutMutation() {
        val descriptor = FakeDescriptor(maxTicksPerStep = 3)
        val session = startedSession(descriptor)

        val zero = assertIs<SessionStepResult.Rejected>(session.step(0))
        val overBound = assertIs<SessionStepResult.Rejected>(session.step(4))

        assertEquals(SessionStepResult.Reason.INVALID_TICK_COUNT, zero.reason)
        assertEquals(1..3, overBound.allowedTicks)
        assertEquals(Tick(0), session.currentTick)
    }

    @Test
    fun terminalSessionStopsStepEarlyAndRejectsLaterSubmission() {
        val session = FakeSession(FakeDescriptor(), seed = 7, terminalAtTick = 2)

        val stepped = assertIs<SessionStepResult.Advanced>(session.step(5))
        val rejected = assertIs<SessionSubmitResult.Rejected>(
            session.submit(TextCommand(CommandId(10), Tick(3), "append", "late")),
        )

        assertEquals(2, stepped.advancedTicks)
        assertEquals(Tick(2), stepped.tick)
        assertTrue(stepped.terminal)
        assertEquals(SessionSubmitResult.Reason.TERMINAL_SESSION, rejected.reason)
        assertEquals(emptyList(), session.pendingForTest())
    }

    @Test
    fun maximumRepresentableTickIsAGenericTerminalBoundary() {
        val session = FakeSession(
            descriptor = FakeDescriptor(),
            seed = 7,
            initialTick = Tick(Long.MAX_VALUE - 1),
        )
        val finalTickCommand = session.submit(
            TextCommand(CommandId(9), Tick(Long.MAX_VALUE), "append", "final"),
        )

        val stepped = assertIs<SessionStepResult.Advanced>(session.step(5))
        val submitted = session.submit(TextCommand(CommandId(10), Tick(Long.MAX_VALUE), "append", "late"))
        val repeated = assertIs<SessionStepResult.Advanced>(session.step())

        assertIs<SessionSubmitResult.Accepted>(finalTickCommand)
        assertEquals(1, stepped.advancedTicks)
        assertEquals(Tick(Long.MAX_VALUE), stepped.tick)
        assertEquals("final", session.snapshot().value.trace)
        assertTrue(stepped.terminal)
        assertTrue(session.isTerminal)
        assertIs<SessionSubmitResult.Rejected>(submitted)
        assertEquals(0, repeated.advancedTicks)
        assertEquals(Tick(Long.MAX_VALUE), repeated.tick)
        assertTrue(repeated.terminal)
    }

    @Test
    fun saveReturnsTypedFailureWhenConcreteCodecThrows() {
        val session = FakeSession(FakeDescriptor(), seed = 7, failSave = true)

        val failed = assertIs<SessionSaveResult.Failed>(session.save())

        assertEquals("fake codec failure", failed.reason)
    }

    @Test
    fun saveRestorePreservesPendingCommandsAndDeterministicContinuity() {
        val descriptor = FakeDescriptor()
        val uninterrupted = startedSession(descriptor)
        val paused = startedSession(descriptor)
        val future = TextCommand(CommandId(4), Tick(3), "append", "X", actorId = 10)
        uninterrupted.submit(future)
        paused.submit(future)

        uninterrupted.step(5)
        paused.step(2)
        val save = assertIs<SessionSaveResult.Saved>(paused.save()).save
        val restored = restoredSession(descriptor, save)
        restored.step(3)

        assertEquals(uninterrupted.snapshot(), restored.snapshot())
        assertEquals(uninterrupted.stableHash(), restored.stableHash())
        assertEquals(descriptor.identity.id, save.runtimeId)
        assertEquals(descriptor.identity.contentPack, save.contentPack)
        assertEquals(descriptor.identity.saveSchema.currentVersion, save.schemaVersion)
    }

    @Test
    fun incompatibleEnvelopeReturnsTypedFailureWithoutSession() {
        val descriptor = FakeDescriptor()
        val save = assertIs<SessionSaveResult.Saved>(startedSession(descriptor).save()).save

        val wrongContent = descriptor.restore(
            save.copy(contentPack = save.contentPack.copy(version = "other")),
        )
        val futureSchema = descriptor.restore(save.copy(schemaVersion = 2))

        assertEquals(
            SaveIncompatibility.CONTENT_PACK_VERSION,
            assertIs<SessionRestoreResult.Incompatible>(wrongContent).kind,
        )
        assertEquals(
            SaveIncompatibility.SAVE_SCHEMA_VERSION,
            assertIs<SessionRestoreResult.Incompatible>(futureSchema).kind,
        )
    }

    @Test
    fun malformedPayloadWithCompatibleEnvelopeReturnsInvalidSave() {
        val descriptor = FakeDescriptor()
        val compatible = assertIs<SessionSaveResult.Saved>(startedSession(descriptor).save()).save

        val invalid = assertIs<SessionRestoreResult.InvalidSave>(
            descriptor.restore(compatible.copy(payload = "not-a-fake-save")),
        )

        assertEquals("Invalid fake save.", invalid.reason)
    }

    @Test
    fun publicRuntimeSurfaceDoesNotReferenceAndroidOrSandboxTypes() {
        val signatures = listOf(
            GameRuntimeDescriptor::class.java,
            GameSession::class.java,
            DeterministicGameSession::class.java,
        ).flatMap { type -> type.declaredMethods.map { it.toGenericString() } }

        assertTrue(signatures.none { "android." in it })
        assertTrue(signatures.none { "games.sandbox" in it })
    }

    private fun startedSession(descriptor: FakeDescriptor = FakeDescriptor()): GameSession<FakeSnapshot> =
        when (val result = descriptor.start(seed = 7)) {
            is SessionStartResult.Started -> result.session
            is SessionStartResult.Failed -> error(result.reason)
        }

    private fun restoredSession(
        descriptor: FakeDescriptor,
        save: VersionedGameSave,
    ): GameSession<FakeSnapshot> = when (val result = descriptor.restore(save)) {
        is SessionRestoreResult.Restored -> result.session
        is SessionRestoreResult.Incompatible -> error("Unexpected incompatibility: ${result.kind}")
        is SessionRestoreResult.InvalidSave -> error(result.reason)
    }

    private data class FakeSnapshot(val tick: Tick, val trace: String)

    private fun fakeIdentity(stableSystemOrder: List<String>): GameRuntimeIdentity = GameRuntimeIdentity(
        id = "fake",
        tickRate = TickRate(20),
        contentPack = ContentPackIdentity("fake-content", "1"),
        saveSchema = SaveSchemaIdentity("fake-save", 1, 1),
        stableSystemOrder = stableSystemOrder,
        commandIdPolicy = CommandIdPolicy.CALLER_OWNED,
        maxTicksPerStep = 100,
    )

    private class FakeDescriptor(
        maxTicksPerStep: Int = 100,
    ) : GameRuntimeDescriptor<FakeSnapshot> {
        override val identity = GameRuntimeIdentity(
            id = "fake",
            tickRate = TickRate(20),
            contentPack = ContentPackIdentity("fake-content", "1"),
            saveSchema = SaveSchemaIdentity("fake-save", 1, 1),
            stableSystemOrder = listOf("commands", "fake-system"),
            commandIdPolicy = CommandIdPolicy.CALLER_OWNED,
            maxTicksPerStep = maxTicksPerStep,
        )

        override fun start(seed: Long): SessionStartResult<FakeSnapshot> =
            SessionStartResult.Started(FakeSession(this, seed))

        override fun restore(save: VersionedGameSave): SessionRestoreResult<FakeSnapshot> {
            validateSaveCompatibility(identity, save)?.let { return it }
            return try {
                val fields = save.payload.split('#', limit = 4)
                require(fields.size == 4) { "Invalid fake save." }
                val tick = Tick(fields[0].toLong())
                val trace = fields[1]
                val seed = fields[2].toLong()
                val commands = fields[3].split(';').filter { it.isNotBlank() }.map { encoded ->
                    val parts = encoded.split(',', limit = 5)
                    TextCommand(
                        id = CommandId(parts[0].toLong()),
                        scheduledTick = Tick(parts[1].toLong()),
                        type = parts[2],
                        payload = parts[4],
                        actorId = parts[3].toLongOrNull(),
                    )
                }
                SessionRestoreResult.Restored(FakeSession(this, seed, tick, trace, commands))
            } catch (failure: RuntimeException) {
                SessionRestoreResult.InvalidSave(failure.message ?: "Invalid fake save.")
            }
        }
    }

    private class FakeSession(
        descriptor: FakeDescriptor,
        seed: Long,
        initialTick: Tick = Tick(0),
        initialTrace: String = "",
        pendingCommands: List<EngineCommand> = emptyList(),
        private val terminalAtTick: Long? = null,
        private val failSave: Boolean = false,
    ) : DeterministicGameSession<FakeSnapshot>(descriptor, seed, pendingCommands) {
        private var tick = initialTick
        private var trace = initialTrace
        var stableHashCalls: Int = 0
            private set

        override val authoritativeTick: Tick get() = tick
        override val terminal: Boolean get() = terminalAtTick?.let { tick.value >= it } ?: false

        fun pendingForTest(): List<EngineCommand> = pendingCommandSnapshot()

        fun applyInputBoundaryForTest() {
            drainCommandsAtCurrentTick().forEach { trace += it.stablePayload() }
        }

        override fun advanceOneTick(tick: Tick, commands: List<EngineCommand>) {
            this.tick = tick
            commands.forEach { trace += it.stablePayload() }
        }

        override fun projectSnapshot(): FakeSnapshot = FakeSnapshot(tick, trace)

        override fun stableHashValue(): String {
            stableHashCalls += 1
            return "${tick.value}:$trace"
        }

        override fun encodeSavePayload(pendingCommands: List<EngineCommand>): String {
            if (failSave) error("fake codec failure")
            return buildString {
                append(tick.value).append('#').append(trace).append('#').append(seed).append('#')
                append(
                    pendingCommands.joinToString(";") { command ->
                        listOf(
                            command.id.value,
                            command.scheduledTick.value,
                            command.type,
                            command.actorId ?: "",
                            command.stablePayload(),
                        ).joinToString(",")
                    },
                )
            }
        }
    }
}
