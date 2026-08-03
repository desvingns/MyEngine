package dev.myengine.games.sandbox

import dev.myengine.core.CommandId
import dev.myengine.core.EngineCommand
import dev.myengine.core.RunStatus
import dev.myengine.core.Tick
import dev.myengine.core.command.BuildTowerCommand
import dev.myengine.core.command.CallWaveEarlyCommand
import dev.myengine.core.command.SellTowerCommand
import dev.myengine.core.command.TileCoordinate
import dev.myengine.core.command.UpgradeTowerCommand
import dev.myengine.world.TilePosition
import java.io.StringReader
import java.io.StringWriter
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * SG-004 (Android lifecycle save smoke), pure-JVM device-independent surface.
 *
 * The Android Bundle glue in MyEngineActivity (onCreate/onSaveInstanceState) needs an
 * emulator and is out of scope here. What we actually smoke-test is the Android-free
 * [SandboxSession] holder the lifecycle delegates to: that a save-at-pause then
 * restore-and-resume is behaviorally indistinguishable from an uninterrupted run.
 *
 * Save format v15 persists `state`, active status effects, effective enemy components, tower upgrade branch/tier/targeting-mode markers, per-tower metrics, the data-defined map identity,
 * content version, and the runtime's pending
 * (not-yet-drained) [dev.myengine.core.CommandQueue] contents, so [SandboxSession.save] is sound at ANY tick.
 * Tests below cover both shapes: saves taken at a quiescent tick where the submitted build
 * command has already been drained, and saves taken while a future-tick command is still
 * queued — the latter is the regression coverage for the bug this save-format bump fixes.
 * No literal hash or save-byte count is asserted.
 */
class SandboxSessionLifecycleTest {

    private fun buildPulseAt(tick: Long, position: TilePosition) =
        BuildTowerCommand(CommandId(1), Tick(tick), "pulse", TileCoordinate(position.x, position.y))

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
        val v6Save = SandboxSession.start(registry).also { it.step(5) }.save()

        // Simulate a pre-map v1 save. Legacy formats have no map/content metadata and must choose
        // the only map provided by the loaded pack.
        val v1Save = legacySave(v6Save, version = 1, dropPendingCommands = true)

        val decodedState = SandboxSaveCodec.decode(v1Save, registry)
        assertEquals(Tick(5), decodedState.tick)
        assertEquals(registry.requireMap().id, decodedState.mapId)
        assertEquals(emptyList(), SandboxSaveCodec.decodePendingCommands(v1Save))

        val restored = SandboxSession.restore(v1Save, registry)
        assertEquals(emptyList(), restored.runtime.pendingCommands())
    }

    @Test
    fun v1ThroughV7SavesMigrateWithoutTargetingMode() {
        val registry = SandboxGame.loadRegistry()
        assertEquals(1, registry.maps.size, "legacy migration is only valid while the content pack has one map")
        val v9Save = SandboxSession.start(registry).also { it.step(5) }.save()

        (1..7).forEach { version ->
            val legacy = legacySave(v9Save, version, dropPendingCommands = version == 1)

            val decoded = SandboxSaveCodec.decode(legacy, registry)

            assertEquals(Tick(5), decoded.tick, "v$version tick")
            assertEquals(registry.requireMap().id, decoded.mapId, "v$version map")
            assertEquals(RunStatus.ACTIVE, decoded.run.status, "v$version must migrate without terminal state")
            if (version < 6) assertEquals(emptyMap(), decoded.defense.towerMetrics, "v$version tower metrics")
        }
    }

    @Test
    fun v9SavePersistsMapContentVersionTowerMetricsAndEffectsField() {
        val registry = SandboxGame.loadRegistry()
        val map = registry.requireMap("sandbox-canonical")
        val session = SandboxSession.start(registry, mapId = map.id)
        session.submit(buildPulseAt(1, TilePosition(2, 2)))
        session.step(12)
        val expectedTowerMetrics = session.runtime.state.defense.towerMetrics
        val save = session.save()

        assertEquals(18, SandboxSaveCodec.SAVE_VERSION)
        assertEquals(map.id, saveProperty(save, "mapId"))
        assertEquals(registry.manifest.version, saveProperty(save, "contentVersion"))
        assertEquals(map.id, SandboxSaveCodec.decode(save, registry).mapId)
        assertTrue(expectedTowerMetrics.isNotEmpty())
        assertEquals(expectedTowerMetrics, SandboxSaveCodec.decode(save, registry).defense.towerMetrics)

        val unknownMap = save.replace("mapId=${map.id}", "mapId=unknown-map")
        assertFailsWith<IllegalStateException> { SandboxSaveCodec.decode(unknownMap, registry) }

        val wrongContentVersion = save.replace(
            "contentVersion=${registry.manifest.version}",
            "contentVersion=999.0.0",
        )
        assertFailsWith<IllegalArgumentException> { SandboxSaveCodec.decode(wrongContentVersion, registry) }
    }

    /**
     * B4. Pending commands round-trip with their exact original identity (id/scheduledTick), not
     * reallocated ids or reordered ticks -- submitted out of order, saved before either drains.
     */
    @Test
    fun pendingCommandsPreserveOriginalIdsAndTicksThroughRoundtrip() {
        val registry = SandboxGame.loadRegistry()
        val first = BuildTowerCommand(CommandId(9), Tick(30), "pulse", TileCoordinate(2, 2))
        val second = BuildTowerCommand(CommandId(4), Tick(28), "pulse", TileCoordinate(3, 3))

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

    @Test
    fun pendingCommandActorIdsAndStablePayloadsRoundtripExactly() {
        val registry = SandboxGame.loadRegistry()
        val commands: List<EngineCommand> = listOf(
            BuildTowerCommand(
                id = CommandId(12),
                scheduledTick = Tick(30),
                towerId = "pulse",
                position = TileCoordinate(4, 5),
                actorId = 101L,
            ),
            UpgradeTowerCommand(
                id = CommandId(13),
                scheduledTick = Tick(31),
                towerEntityId = 123L,
                branch = "main",
                tier = 2,
                actorId = 202L,
            ),
            SellTowerCommand(
                id = CommandId(14),
                scheduledTick = Tick(32),
                towerEntityId = 123L,
                actorId = 303L,
            ),
            CallWaveEarlyCommand(
                id = CommandId(15),
                scheduledTick = Tick(33),
                actorId = 404L,
            ),
        )

        val save = SandboxSaveCodec.encode(
            state = SandboxGame.createInitialState(registry),
            seed = 7,
            pendingCommands = commands,
        )
        val restored = SandboxSaveCodec.decodePendingCommands(save)

        assertEquals(commands.size, restored.size)
        assertEquals(commands.map { it.type }, restored.map { it.type })
        assertEquals(commands.map { it.id }, restored.map { it.id })
        assertEquals(commands.map { it.scheduledTick }, restored.map { it.scheduledTick })
        assertEquals(commands.map { it.actorId }, restored.map { it.actorId })
        assertEquals(commands.map { it.stablePayload() }, restored.map { it.stablePayload() })

        val restoredBuild = assertIs<BuildTowerCommand>(restored[0])
        assertEquals(101L, restoredBuild.actorId)
        assertEquals("pulse:4:5", restoredBuild.stablePayload())
        val restoredUpgrade = assertIs<UpgradeTowerCommand>(restored[1])
        assertEquals(202L, restoredUpgrade.actorId)
        assertEquals("123:main:2", restoredUpgrade.stablePayload())
        val restoredSell = assertIs<SellTowerCommand>(restored[2])
        assertEquals(CommandId(14), restoredSell.id)
        assertEquals(Tick(32), restoredSell.scheduledTick)
        assertEquals(303L, restoredSell.actorId)
        assertEquals("123", restoredSell.stablePayload())
        val restoredEarlyCall = assertIs<CallWaveEarlyCommand>(restored[3])
        assertEquals(CommandId(15), restoredEarlyCall.id)
        assertEquals(Tick(33), restoredEarlyCall.scheduledTick)
        assertEquals(404L, restoredEarlyCall.actorId)
        assertEquals("", restoredEarlyCall.stablePayload())
    }

    @Test
    fun legacyV6EnemyRouteIsDiscardedAndGoalFieldMigrationIsReplayStable() {
        val registry = SandboxGame.loadRegistry()
        val original = SandboxSession.start(registry).also { it.step(10) }
        val pathFreeSave = original.save()
        val legacyV6Fixture = withLegacyEnemyRoute(legacySave(pathFreeSave, version = 6, dropPendingCommands = false))
        val originalEnemyPosition = original.runtime.state.entities.byTag("enemy").first().position!!.tile

        // This is an actual v6-shaped fixture with the now-obsolete per-enemy route populated.
        // Its route is presentation-era data only: migration keeps the saved position but erases
        // path/pathIndex so the restored GoalField is the sole authoritative movement source.
        val decoded = SandboxSaveCodec.decode(legacyV6Fixture, registry)
        val migratedEnemy = decoded.entities.byTag("enemy").first()
        assertEquals(originalEnemyPosition, migratedEnemy.position!!.tile)
        assertEquals(emptyList(), migratedEnemy.movement!!.path)
        assertEquals(0, migratedEnemy.movement!!.pathIndex)
        assertEquals(
            original.stableHash(),
            decoded.stableHash(),
            "migration canonicalizes legacy routes out of the stable-hash state",
        )

        val baseline = SandboxSession.restore(pathFreeSave, registry)
        val migrated = SandboxSession.restore(legacyV6Fixture, registry)
        migrated.step()
        assertEquals(TilePosition(3, 1), migrated.runtime.state.entities.byTag("enemy").first().position!!.tile)

        baseline.step(20)
        migrated.step(19)
        assertEquals(baseline.stableHash(), migrated.stableHash())
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
        assertEquals(18, SandboxSaveCodec.SAVE_VERSION)
        SandboxSaveCodec.decode(valid, registry)

        val future = valid.replace("saveVersion=${SandboxSaveCodec.SAVE_VERSION}", "saveVersion=${SandboxSaveCodec.SAVE_VERSION + 1}")
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

        val garbled = valid.replace("saveVersion=${SandboxSaveCodec.SAVE_VERSION}", "saveVersion=x")
        assertNotEquals(valid, garbled)

        assertFailsWith<IllegalArgumentException> { SandboxSaveCodec.decode(garbled, registry) }
    }

    @Test
    fun decodePendingCommandsRejectsMalformedFutureAndOutOfRangeVersionsLikeDecode() {
        val registry = SandboxGame.loadRegistry()
        val valid = SandboxSession.start(registry).also { it.step(5) }.save()
        val unsupportedSaves = listOf(
            valid.replace("saveVersion=${SandboxSaveCodec.SAVE_VERSION}", "saveVersion=${SandboxSaveCodec.SAVE_VERSION + 1}"),
            valid.replace("saveVersion=${SandboxSaveCodec.SAVE_VERSION}", "saveVersion=x"),
            valid.replace("saveVersion=${SandboxSaveCodec.SAVE_VERSION}", "saveVersion=0"),
        )

        unsupportedSaves.forEach { save ->
            assertFailsWith<IllegalArgumentException> { SandboxSaveCodec.decode(save, registry) }
            assertFailsWith<IllegalArgumentException> { SandboxSaveCodec.decodePendingCommands(save) }
        }
    }

    private fun legacySave(text: String, version: Int, dropPendingCommands: Boolean): String = text.lines()
        .filterNot {
            (version < 4 && (
                it.startsWith("mapId=") ||
                    it.startsWith("contentVersion=") ||
                    it.startsWith("packId=")
                )) ||
                (version < 5 && (
                    it.startsWith("runStatus=") ||
                        it.startsWith("terminalReason=") ||
                        it.startsWith("terminalTick=") ||
                        it.startsWith("runSummary=") ||
                        it.startsWith("runResources=")
                    )) ||
                (version < 6 && it.startsWith("towerMetrics=")) ||
                (dropPendingCommands && it.startsWith("pendingCommands="))
        }
        .joinToString("\n") {
            when {
                it.startsWith("saveVersion=") -> "saveVersion=$version"
                version < 7 && it.startsWith("entities=") -> it.substringBefore("=") + "=" + it.substringAfter("=")
                    .split(';').joinToString(";") { encoded -> encoded.split('|').take(15).joinToString("|") }
                else -> it
            }
        }

    private fun saveProperty(text: String, key: String): String? =
        Properties().also { it.load(StringReader(text)) }.getProperty(key)

    private fun withLegacyEnemyRoute(save: String): String {
        val props = Properties().also { it.load(StringReader(save)) }
        require(props.getProperty("saveVersion") == "6") { "fixture must remain a v6 save" }
        props["entities"] = props.getProperty("entities").split(';').joinToString(";") { encoded ->
            val parts = encoded.split('|').toMutableList()
            if (parts[1].startsWith("enemy:")) {
                parts[11] = "1:1/2:1/3:1"
                parts[12] = "2"
            }
            parts.joinToString("|")
        }
        return StringWriter().also { props.store(it, "Legacy v6 path fixture") }.toString()
    }

    /** Reads the `seed` value out of a save's property text without decoding full state. */
    private fun seedProperty(text: String): String =
        requireNotNull(saveProperty(text, "seed"))
}
