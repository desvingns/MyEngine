package dev.myengine.games.sandbox

import dev.myengine.content.MetaProgressionContent
import dev.myengine.content.MetaUnlockableContent
import dev.myengine.content.TechUnlockType
import dev.myengine.core.CommandId
import dev.myengine.core.RunState
import dev.myengine.core.RunStatus
import dev.myengine.core.RunSummary
import dev.myengine.core.TerminalReason
import dev.myengine.core.Tick
import dev.myengine.core.command.BuildTowerCommand
import dev.myengine.core.command.TileCoordinate
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SandboxMetaProgressionTest {
    @Test
    fun profileCreditsTerminalRunOnceUnlocksContentAndRoundTrips() {
        val registry = registryWithMeta()
        val terminalRun = RunState(
            status = RunStatus.WON,
            terminalReason = TerminalReason.ALL_WAVES_CLEARED,
            terminalTick = Tick(12),
            summary = RunSummary(resources = mapOf("bolt" to 4)),
        )
        val store = MetaProgressionStore()

        assertEquals(4, store.recordCompletedRun("run-1", terminalRun, registry))
        assertEquals(0, store.recordCompletedRun("run-1", terminalRun, registry))
        assertEquals(4, store.profile.metaCurrency)
        assertTrue(store.unlock("pulse-profile", registry))
        assertFalse(store.unlock("pulse-profile", registry))

        val encoded = store.save()
        assertEquals(encoded, MetaProgressionCodec.encode(store.profile))
        assertEquals(store.profile, MetaProgressionStore.restore(encoded).profile)
    }

    @Test
    fun profileCodecIsStableAndRejectsUnsupportedVersions() {
        val profile = MetaProgressionProfile(
            metaCurrency = 9,
            unlockedIds = setOf("pulse-profile", "another-unlock"),
            creditedRunIds = setOf("run-b", "run-a"),
        )

        val first = MetaProgressionCodec.encode(profile)
        assertEquals(first, MetaProgressionCodec.encode(profile))
        assertTrue(first.indexOf("profileVersion=1") < first.indexOf("metaCurrency=9"))
        assertFailsWith<IllegalArgumentException> {
            MetaProgressionCodec.decode(first.replace("profileVersion=1", "profileVersion=2"))
        }
    }

    @Test
    fun metaUnlockGatesTowerAndPersistsInRunSave() {
        val registry = registryWithMeta()
        val command = BuildTowerCommand(CommandId(1), Tick(1), "pulse", TileCoordinate(30, 32))

        val locked = SandboxGame.createRuntime(registry)
        locked.submit(command)
        locked.step()
        assertEquals("locked_tower:pulse", locked.state.lastCommandOrError)
        assertTrue(locked.state.entities.byTag("tower").isEmpty())
        assertEquals(6, locked.state.inventory.amount("bolt"))

        val unlocked = SandboxGame.createRuntime(registry, metaUnlockIds = setOf("pulse-profile"))
        unlocked.submit(command)
        unlocked.step()
        assertEquals(1, unlocked.state.entities.byTag("tower").size)
        assertEquals(3, unlocked.state.inventory.amount("bolt"))
        assertTrue(locked.state.stableHash() != unlocked.state.stableHash())

        val save = SandboxSaveCodec.encode(unlocked.state, seed = 7)
        val restored = SandboxSaveCodec.decode(save, registry)
        assertEquals(setOf("pulse-profile"), restored.metaUnlockIds)
        assertEquals(unlocked.state.stableHash(), restored.stableHash())
    }

    @Test
    fun activeRunCannotCreditMetaCurrency() {
        val registry = registryWithMeta()
        val active = RunState()
        assertFailsWith<IllegalArgumentException> {
            MetaProgressionStore().recordCompletedRun("run-1", active, registry)
        }
    }

    @Test
    fun corruptedMetaUnlockSaveRejectsEmptyAndDuplicateTokens() {
        val registry = registryWithMeta()
        val state = SandboxGame.createInitialState(registry, metaUnlockIds = setOf("pulse-profile"))
        val save = SandboxSaveCodec.encode(state, seed = 7)
        val token = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("pulse-profile".toByteArray(Charsets.UTF_8))

        assertFailsWith<IllegalArgumentException> {
            SandboxSaveCodec.decode(save.replace(Regex("metaUnlockIds=.*"), "metaUnlockIds=$token,,"), registry)
        }
        assertFailsWith<IllegalArgumentException> {
            SandboxSaveCodec.decode(save.replace(Regex("metaUnlockIds=.*"), "metaUnlockIds=$token,$token"), registry)
        }
    }

    private fun registryWithMeta() = SandboxGame.loadRegistry().copy(
        metaProgression = MetaProgressionContent(
            currencyResourceId = "bolt",
            unlockables = mapOf(
                "pulse-profile" to MetaUnlockableContent(
                    id = "pulse-profile",
                    type = TechUnlockType.TOWER,
                    targetId = "pulse",
                ),
            ),
        ),
    )
}
