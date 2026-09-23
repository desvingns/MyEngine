package dev.myengine.games.sandbox

import dev.myengine.core.CommandId
import dev.myengine.core.Tick
import dev.myengine.core.command.BuildTowerCommand
import dev.myengine.core.command.TileCoordinate
import dev.myengine.runtime.ContentPackIdentity
import dev.myengine.runtime.ExperimentalGameRuntimeApi
import dev.myengine.runtime.SaveIncompatibility
import dev.myengine.runtime.SessionRestoreResult
import dev.myengine.runtime.SessionSaveResult
import dev.myengine.runtime.SessionStartResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalGameRuntimeApi::class)
class SandboxRuntimeContractTest {
    @Test
    fun legacyDescriptorConstructorAndAliasesRemainSourceCompatible() {
        val descriptor = SandboxDescriptor(id = "legacy-sandbox", engineName = "Legacy Engine")
        val (id, engineName) = descriptor
        val copied = descriptor.copy(id = "copied-sandbox")

        assertEquals("legacy-sandbox", descriptor.id)
        assertEquals("legacy-sandbox", descriptor.identity.id)
        assertEquals("Legacy Engine", descriptor.engineName)
        assertEquals("legacy-sandbox", id)
        assertEquals("Legacy Engine", engineName)
        assertEquals(SandboxDescriptor("copied-sandbox", "Legacy Engine"), copied)
        assertEquals("SandboxDescriptor(id=legacy-sandbox, engineName=Legacy Engine)", descriptor.toString())
    }

    @Test
    fun descriptorPublishesValidatedRuntimeContentAndSaveIdentity() {
        val registry = SandboxGame.loadRegistry()
        val descriptor = SandboxGame.descriptor(registry)

        assertEquals(SandboxDescriptor.RUNTIME_ID, descriptor.identity.id)
        assertEquals(SandboxDescriptor.TICKS_PER_SECOND, descriptor.identity.tickRate.ticksPerSecond)
        assertEquals(ContentPackIdentity(registry.manifest.id, registry.manifest.version), descriptor.identity.contentPack)
        assertEquals(1, descriptor.identity.saveSchema.oldestSupportedVersion)
        assertEquals(SandboxSaveCodec.SAVE_VERSION, descriptor.identity.saveSchema.currentVersion)
        assertEquals(SandboxDescriptor.SYSTEM_ORDER, descriptor.identity.stableSystemOrder)
    }

    @Test
    fun typedStartSaveRestoreKeepsSandboxPayloadAndContinuity() {
        val descriptor = SandboxGame.descriptor()
        val started = assertIs<SessionStartResult.Started<*>>(descriptor.start(seed = 29)).session
        started.step(10)
        val saved = assertIs<SessionSaveResult.Saved>(started.save()).save
        val restored = assertIs<SessionRestoreResult.Restored<*>>(descriptor.restore(saved)).session

        assertEquals("29", saveProperty(saved.payload, "seed"))
        assertEquals(SandboxSaveCodec.SAVE_VERSION.toString(), saveProperty(saved.payload, "saveVersion"))
        assertEquals(started.snapshot(), restored.snapshot())
    }

    @Test
    fun typedRestoreRetainsPendingQueueAndEverySubsequentTickHash() {
        val descriptor = SandboxGame.descriptor()
        val uninterrupted = assertIs<SessionStartResult.Started<*>>(descriptor.start(seed = 29)).session
        uninterrupted.submit(BuildTowerCommand(CommandId(1), Tick(25), "pulse", TileCoordinate(2, 2)))
        uninterrupted.step(20)
        val saved = assertIs<SessionSaveResult.Saved>(uninterrupted.save()).save
        val restored = assertIs<SessionRestoreResult.Restored<*>>(descriptor.restore(saved)).session

        repeat(20) {
            uninterrupted.step()
            restored.step()
            assertEquals(uninterrupted.currentTick, restored.currentTick)
            assertEquals(uninterrupted.stableHash(), restored.stableHash())
            assertEquals(uninterrupted.snapshot(), restored.snapshot())
        }
    }

    @Test
    fun rawMetadataAndPayloadDisagreementsReturnTypedFailures() {
        val descriptor = SandboxGame.descriptor()
        val valid = SandboxSession.start().save()
        val saved = assertIs<SessionSaveResult.Saved>(
            assertIs<SessionStartResult.Started<*>>(descriptor.start(seed = 7)).session.save(),
        ).save

        listOf("", "saveVersion=0", "saveVersion=-1", "saveVersion=invalid", "saveVersion=7\npackId=\n").forEach {
            assertIs<SessionRestoreResult.InvalidSave>(descriptor.restoreText(it))
        }
        assertIs<SessionRestoreResult.InvalidSave>(
            descriptor.restore(saved.copy(payload = valid.replace("saveVersion=7", "saveVersion=6"))),
        )
    }

    @Test
    fun descriptorReportsContentAndFutureSchemaMismatchesWithoutPartialRuntime() {
        val descriptor = SandboxGame.descriptor()
        val started = assertIs<SessionStartResult.Started<*>>(descriptor.start(seed = 7)).session
        val saved = assertIs<SessionSaveResult.Saved>(started.save()).save

        val wrongRuntime = descriptor.restore(saved.copy(runtimeId = "other-runtime"))
        val wrongContentId = descriptor.restore(
            saved.copy(contentPack = saved.contentPack.copy(id = "other-content")),
        )
        val wrongContent = descriptor.restore(
            saved.copy(contentPack = saved.contentPack.copy(version = "incompatible")),
        )
        val wrongSchema = descriptor.restore(saved.copy(schemaId = "other-schema"))
        val future = descriptor.restore(
            saved.copy(schemaVersion = SandboxSaveCodec.SAVE_VERSION + 1),
        )

        assertEquals(
            SaveIncompatibility.RUNTIME_ID,
            assertIs<SessionRestoreResult.Incompatible>(wrongRuntime).kind,
        )
        assertEquals(
            SaveIncompatibility.CONTENT_PACK_ID,
            assertIs<SessionRestoreResult.Incompatible>(wrongContentId).kind,
        )
        assertEquals(
            SaveIncompatibility.CONTENT_PACK_VERSION,
            assertIs<SessionRestoreResult.Incompatible>(wrongContent).kind,
        )
        assertEquals(
            SaveIncompatibility.SAVE_SCHEMA_ID,
            assertIs<SessionRestoreResult.Incompatible>(wrongSchema).kind,
        )
        assertEquals(
            SaveIncompatibility.SAVE_SCHEMA_VERSION,
            assertIs<SessionRestoreResult.Incompatible>(future).kind,
        )
    }

    @Test
    fun rawLegacyFacadeRoutesFutureVersionThroughTypedCompatibilityFailure() {
        val descriptor = SandboxGame.descriptor()
        val valid = SandboxSession.start().save()
        val future = valid.replace(
            "saveVersion=${SandboxSaveCodec.SAVE_VERSION}",
            "saveVersion=${SandboxSaveCodec.SAVE_VERSION + 1}",
        )

        val rejected = assertIs<SessionRestoreResult.Incompatible>(descriptor.restoreText(future))

        assertEquals(SaveIncompatibility.SAVE_SCHEMA_VERSION, rejected.kind)
    }

    private fun saveProperty(text: String, key: String): String? =
        java.util.Properties().also { it.load(java.io.StringReader(text)) }.getProperty(key)
}
