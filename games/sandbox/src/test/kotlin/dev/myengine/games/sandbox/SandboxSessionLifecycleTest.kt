package dev.myengine.games.sandbox

import dev.myengine.core.CommandId
import dev.myengine.core.Tick
import dev.myengine.render.BuildTowerCommand
import dev.myengine.world.TilePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

/**
 * SG-004 (Android lifecycle save smoke), pure-JVM device-independent surface.
 *
 * The Android Bundle glue in MyEngineActivity (onCreate/onSaveInstanceState) needs an
 * emulator and is out of scope here. What we actually smoke-test is the Android-free
 * [SandboxSession] holder the lifecycle delegates to: that a save-at-pause then
 * restore-and-resume is behaviorally indistinguishable from an uninterrupted run.
 *
 * Save format v2 persists both `state` and the runtime's pending (not-yet-drained)
 * [dev.myengine.core.CommandQueue] contents, so [SandboxSession.save] is sound at ANY tick.
 * Tests below cover both shapes: saves taken at a quiescent tick where the submitted build
 * command has already been drained, and saves taken while a future-tick command is still
 * queued — the latter is the regression coverage for the bug this save-format bump fixes.
 * No literal hash or save-byte count is asserted.
 */
class SandboxSessionLifecycleTest {

    private fun buildPulseAt(tick: Long, position: TilePosition) =
        BuildTowerCommand(CommandId(1), Tick(tick), "pulse", position)

    /** A. Save at a quiescent tick, restore, and the reconstructed state hashes identically. */
    @Test
    fun saveRestoreRoundtripPreservesState() {
        val session = SandboxSession.start()
        // No commands submitted, so tick 35 is trivially quiescent.
        session.step(35)
        val expected = session.stableHash()

        val text = session.save()
        val restored = SandboxSession.restore(text)

        assertEquals(expected, restored.stableHash())
    }

    /**
     * B. THE key smoke: lifecycle pause (save) then resume (restore + continue) yields the
     * exact same final state as an uninterrupted run of identical commands over identical ticks.
     */
    @Test
    fun pauseThenResumeMatchesUninterruptedRun() {
        val registry = SandboxGame.loadRegistry()
        // (2,2) sits next to the enemy spawn (1,1): the command applies (and drains) at tick 1,
        // long before either the pause point (tick 20) or the shared final tick (tick 40).
        val position = TilePosition(2, 2)

        // Uninterrupted: build at tick 1, run straight to tick 40.
        val a = SandboxSession.start(registry)
        a.submit(buildPulseAt(1, position))
        a.step(40)
        val uninterrupted = a.stableHash()

        // Paused: build at tick 1, run to tick 20 (quiescent), save, restore, resume to tick 40.
        val b = SandboxSession.start(registry)
        b.submit(buildPulseAt(1, position))
        b.step(20)
        val save = b.save()
        val c = SandboxSession.restore(save, registry)
        c.step(20)
        val resumed = c.stableHash()

        assertEquals(uninterrupted, resumed)
    }

    /**
     * B2. THE key regression test for the save-format v2 fix: a save taken WHILE a future-tick
     * command is still pending (not yet drained) must round-trip that command through
     * restore, so the resumed run matches an uninterrupted run bit-for-bit. Before this fix,
     * a save at a non-quiescent tick would silently drop the pending command.
     */
    @Test
    fun pauseWithPendingFutureCommandThenResumeMatchesUninterruptedRun() {
        val registry = SandboxGame.loadRegistry()
        // (2,2) sits next to the enemy spawn (1,1): reachable within the run, but scheduled for
        // tick 25 so it is still queued (undrained) at the tick-20 pause point below.
        val position = TilePosition(2, 2)

        // Uninterrupted: build scheduled for tick 25, run straight to tick 40.
        val a = SandboxSession.start(registry)
        a.submit(buildPulseAt(25, position))
        a.step(40)
        val uninterrupted = a.stableHash()

        // Paused: build scheduled for tick 25, run only to tick 20 -- the command is still
        // pending/undrained at this point -- save, restore, then resume to tick 40.
        val b = SandboxSession.start(registry)
        b.submit(buildPulseAt(25, position))
        b.step(20)
        val save = b.save()
        val c = SandboxSession.restore(save, registry)

        // Proves the command round-tripped into the new runtime's queue, not merely that the
        // final hash happens to match by coincidence.
        assertEquals(1, c.runtime.pendingCommands().size)

        c.step(20)
        val resumed = c.stableHash()

        assertEquals(uninterrupted, resumed)
    }

    /**
     * B3. A v1-shaped save (no `pendingCommands` property, `saveVersion=1`) still decodes and
     * restores cleanly, with an empty pending-command queue -- matching pre-fix v1 behavior.
     * This is the migration/back-compat counterpart to B2.
     */
    @Test
    fun v1SaveWithoutPendingCommandsMigratesToEmptyQueue() {
        val registry = SandboxGame.loadRegistry()
        val v2Save = SandboxSession.start(registry).also { it.step(5) }.save()

        // Simulate a v1 save: strip the `pendingCommands` property entirely and roll the
        // version back, rather than duplicating the codec's own encoding internals.
        val v1Save = v2Save.lines()
            .filterNot { it.startsWith("pendingCommands=") }
            .joinToString("\n") { if (it.startsWith("saveVersion=")) "saveVersion=1" else it }

        val decodedState = SandboxSaveCodec.decode(v1Save, registry)
        assertEquals(Tick(5), decodedState.tick)
        assertEquals(emptyList(), SandboxSaveCodec.decodePendingCommands(v1Save))

        val restored = SandboxSession.restore(v1Save, registry)
        assertEquals(emptyList(), restored.runtime.pendingCommands())
    }

    /**
     * B4. Pending commands round-trip with their exact original identity (id/scheduledTick), not
     * reallocated ids or reordered ticks -- submitted out of order, saved before either drains.
     */
    @Test
    fun pendingCommandsPreserveOriginalIdsAndTicksThroughRoundtrip() {
        val registry = SandboxGame.loadRegistry()
        val first = BuildTowerCommand(CommandId(9), Tick(30), "pulse", TilePosition(2, 2))
        val second = BuildTowerCommand(CommandId(4), Tick(28), "pulse", TilePosition(3, 3))

        val session = SandboxSession.start(registry)
        // Submitted out of id/tick order on purpose: the queue is order-preserving on submit,
        // not sorted, so this also guards against an accidental sort creeping into the roundtrip.
        session.submit(first)
        session.submit(second)
        session.step(20)

        val restored = SandboxSession.restore(session.save(), registry)

        val expected = listOf(first, second).map { it.id to it.scheduledTick }
        val actual = restored.runtime.pendingCommands().map { it.id to it.scheduledTick }
        assertEquals(expected, actual)
    }

    /** C. The seed roundtrips through save/restore, and re-saving reproduces the same seed line. */
    @Test
    fun seedRoundtripsThroughSaveRestore() {
        val session = SandboxSession.start()
        session.step(35)

        val restored = SandboxSession.restore(session.save())

        assertEquals(session.seed, restored.seed)
        assertEquals(SandboxSession.DEFAULT_SEED, restored.seed)

        // Re-saving the restored session reproduces the same `seed` property in the text.
        assertEquals(seedProperty(session.save()), seedProperty(restored.save()))
        assertEquals(session.seed.toString(), seedProperty(restored.save()))
    }

    /**
     * D. Restore yields an independent runtime: advancing the restored session must not mutate
     * the original session's state (they are separate runtimes over separate state).
     */
    @Test
    fun restoreYieldsIndependentRuntime() {
        val session = SandboxSession.start()
        session.step(35)
        val originalHash = session.stableHash()

        val restored = SandboxSession.restore(session.save())
        // Advancing the restored copy must diverge without touching the original.
        restored.step(10)

        assertEquals(originalHash, session.stableHash())
        assertNotEquals(originalHash, restored.stableHash())
    }

    /**
     * E. Future-version rejection (constitution invariant: saves are versioned-from-v1 with
     * future-version failure handling). A save whose `saveVersion` is bumped past the codec's
     * supported version must be refused on decode rather than silently mis-parsed.
     * [SandboxSaveCodec.decode] guards with `require(...)`, which throws IllegalArgumentException.
     */
    @Test
    fun futureSaveVersionIsRejected() {
        val registry = SandboxGame.loadRegistry()
        val session = SandboxSession.start(registry)
        session.step(5)

        val valid = session.save()
        // Sanity: the valid save carries the codec's current version and decodes cleanly.
        assertEquals(SandboxSaveCodec.SAVE_VERSION, 2)
        SandboxSaveCodec.decode(valid, registry)

        val future = valid.replace("saveVersion=2", "saveVersion=3")
        // Guard against a silent no-op swap if the encoded key/format ever changes.
        assertNotEquals(valid, future)

        assertFailsWith<IllegalArgumentException> { SandboxSaveCodec.decode(future, registry) }
        // The lifecycle entry point delegates to decode, so it must reject too.
        assertFailsWith<IllegalArgumentException> { SandboxSession.restore(future, registry) }
    }

    /**
     * F. A non-numeric `saveVersion` (`toIntOrNull() == null`) also fails the version guard,
     * so a corrupt/garbled version field cannot slip past into a mis-decoded state.
     */
    @Test
    fun nonNumericSaveVersionIsRejected() {
        val registry = SandboxGame.loadRegistry()
        val valid = SandboxSession.start(registry).also { it.step(5) }.save()

        val garbled = valid.replace("saveVersion=2", "saveVersion=x")
        assertNotEquals(valid, garbled)

        assertFailsWith<IllegalArgumentException> { SandboxSaveCodec.decode(garbled, registry) }
    }

    /** Reads the `seed` value out of a save's property text without decoding full state. */
    private fun seedProperty(text: String): String =
        java.util.Properties().also { it.load(java.io.StringReader(text)) }.getProperty("seed")
}
