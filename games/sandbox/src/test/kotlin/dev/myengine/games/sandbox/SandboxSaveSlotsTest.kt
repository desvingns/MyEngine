package dev.myengine.games.sandbox

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SandboxSaveSlotsTest {
    @Test
    fun namedSlotsAreIsolatedAndRoundTripThroughCodecV11() {
        val registry = SandboxGame.loadRegistry()
        val store = SandboxSaveSlotStore(
            Files.createTempDirectory("sandbox-save-slot-store-isolation"),
            timestampMillis = { 1_000L },
        )
        val first = SandboxSession.start(registry, seed = 11L).also { it.step(3) }
        val second = SandboxSession.start(registry, seed = 22L).also { it.step(7) }

        val firstMetadata = store.save("first", first)
        val secondMetadata = store.save("second", second)

        assertEquals(null, firstMetadata.autosaveSequence)
        assertEquals(null, secondMetadata.autosaveSequence)
        assertEquals(listOf("first", "second"), store.listMetadata().map { it.slotName })
        assertNotEquals(
            Files.readString(store.pathFor("first"), StandardCharsets.UTF_8),
            Files.readString(store.pathFor("second"), StandardCharsets.UTF_8),
        )
        assertEquals(3L, store.restore("first", registry).runtime.state.tick.value)
        assertEquals(7L, store.restore("second", registry).runtime.state.tick.value)
        assertEquals(11, SandboxSaveCodec.SAVE_VERSION)
        assertEquals(11, firstMetadata.codecVersion)
        assertTrue(Files.isRegularFile(store.pathFor("first")))
    }

    @Test
    fun autosaveCadenceAndRingRotationAreDeterministic() {
        val registry = SandboxGame.loadRegistry()
        val policy = SandboxAutosavePolicy(
            cadenceTicks = 2L,
            slotNames = listOf("autosave-a", "autosave-b"),
        )
        val firstStore = SandboxSaveSlotStore(
            Files.createTempDirectory("sandbox-save-slot-store-rotation-a"),
            policy,
            timestampMillis = { 1_000L },
        )
        val secondStore = SandboxSaveSlotStore(
            Files.createTempDirectory("sandbox-save-slot-store-rotation-b"),
            policy,
            timestampMillis = { 1_000L },
        )
        val first = SandboxSession.start(registry, seed = 17L)
        val second = SandboxSession.start(registry, seed = 17L)
        val firstWrites = mutableListOf<Pair<String, Long?>>()
        val secondWrites = mutableListOf<Pair<String, Long?>>()

        for (tick in 0L..6L) {
            firstStore.maybeAutosave(first)?.let { firstWrites += it.slotName to it.autosaveSequence }
            secondStore.maybeAutosave(second)?.let { secondWrites += it.slotName to it.autosaveSequence }
            if (tick < 6L) {
                first.step()
                second.step()
            }
        }

        val expectedWrites = listOf<Pair<String, Long?>>(
            "autosave-a" to 0L,
            "autosave-b" to 1L,
            "autosave-a" to 2L,
            "autosave-b" to 3L,
        )
        assertEquals(expectedWrites, firstWrites)
        assertEquals(expectedWrites, secondWrites)
        assertEquals(
            listOf("autosave-a" to 2L, "autosave-b" to 3L),
            firstStore.listMetadata().map { it.slotName to it.autosaveSequence },
        )
        assertEquals(
            firstStore.listMetadata().map { it.slotName to it.autosaveSequence },
            secondStore.listMetadata().map { it.slotName to it.autosaveSequence },
        )
        assertEquals(1_000L, firstStore.readMetadata("autosave-b").timestampMillis)
    }

    @Test
    fun metadataRemainsReadableWhenPayloadCannotBeDecoded() {
        val registry = SandboxGame.loadRegistry()
        val store = SandboxSaveSlotStore(
            Files.createTempDirectory("sandbox-save-slot-store-metadata"),
            timestampMillis = { 2_000L },
        )
        val session = SandboxSession.start(registry).also { it.step(5) }
        val before = store.save("checkpoint", session)
        val path = store.pathFor("checkpoint")
        val original = Files.readString(path, StandardCharsets.UTF_8)
        val corrupted = original.replace(Regex("(?m)^coreHealth=.*$"), "coreHealth=not-a-number")
        assertNotEquals(original, corrupted)
        Files.writeString(path, corrupted, StandardCharsets.UTF_8)

        val after = store.readMetadata("checkpoint")
        assertEquals(before.slotName, after.slotName)
        assertEquals(before.mapId, after.mapId)
        assertEquals(before.wave, after.wave)
        assertEquals(before.contentVersion, after.contentVersion)
        assertEquals(before.timestampMillis, after.timestampMillis)
        assertFailsWith<Exception> { store.restore("checkpoint", registry) }
    }

    @Test
    fun slotMetadataDoesNotChangeCodecRestoreCompatibility() {
        val registry = SandboxGame.loadRegistry()
        val store = SandboxSaveSlotStore(Files.createTempDirectory("sandbox-save-slot-store-codec"))
        val session = SandboxSession.start(registry).also { it.step(4) }
        store.save("checkpoint", session)

        val restored = SandboxSession.restore(
            Files.readString(store.pathFor("checkpoint"), StandardCharsets.UTF_8),
            registry,
        )

        assertEquals(session.stableHash(), restored.stableHash())
        assertEquals(11, SandboxSaveCodec.SAVE_VERSION)
    }

    @Test
    fun newestCorruptAutosaveFallsBackToLastValidAutosave() {
        val registry = SandboxGame.loadRegistry()
        val root = Files.createTempDirectory("sandbox-save-slot-store-fallback")
        val policy = SandboxAutosavePolicy(
            cadenceTicks = 1L,
            slotNames = listOf("autosave-a", "autosave-b", "autosave-c"),
        )
        val store = SandboxSaveSlotStore(root, policy, timestampMillis = { 3_000L })
        val session = SandboxSession.start(registry)

        store.maybeAutosave(session)
        session.step()
        store.maybeAutosave(session)
        session.step()
        store.maybeAutosave(session)

        val newestPath = store.pathFor("autosave-c")
        val corrupted = Files.readString(newestPath, StandardCharsets.UTF_8)
            .replace(Regex("(?m)^coreHealth=.*$"), "coreHealth=not-a-number")
        Files.writeString(newestPath, corrupted, StandardCharsets.UTF_8)
        assertEquals(2L, store.readMetadata("autosave-c").autosaveSequence)

        val restored = store.restore("autosave-c", registry)

        assertEquals(1L, restored.runtime.state.tick.value)
        assertEquals(
            store.restore("autosave-b", registry).stableHash(),
            restored.stableHash(),
        )
    }

    @Test
    fun invalidSlotNamesAndPolicyValuesAreRejected() {
        val registry = SandboxGame.loadRegistry()
        val store = SandboxSaveSlotStore(Files.createTempDirectory("sandbox-save-slot-store-guards"))
        val session = SandboxSession.start(registry)

        assertFailsWith<IllegalArgumentException> { SandboxAutosavePolicy(cadenceTicks = 0L) }
        assertFailsWith<IllegalArgumentException> { SandboxAutosavePolicy(slotNames = emptyList()) }
        assertFailsWith<IllegalArgumentException> {
            SandboxAutosavePolicy(slotNames = listOf("same", "same"))
        }
        assertFailsWith<IllegalArgumentException> {
            SandboxAutosavePolicy(slotNames = listOf("../escape"))
        }

        listOf("", ".", "..", "../escape", "nested/name", " leading", "trailing ", "a\\b", "Upper")
            .forEach { invalidName ->
                assertFailsWith<IllegalArgumentException>("slot name '$invalidName'") {
                    store.save(invalidName, session)
                }
            }
        assertFailsWith<IllegalArgumentException> {
            SandboxSaveSlotStore(
                Files.createTempDirectory("sandbox-save-slot-store-time"),
                timestampMillis = { -1L },
            ).save("valid", session)
        }
    }

    @Test
    fun manualSlotCannotCollideWithAutosaveNamespace() {
        val registry = SandboxGame.loadRegistry()
        val policy = SandboxAutosavePolicy(slotNames = listOf("autosave-a"))
        val store = SandboxSaveSlotStore(Files.createTempDirectory("sandbox-save-slot-store-collision"), policy)

        assertFailsWith<IllegalArgumentException> {
            store.save("autosave-a", SandboxSession.start(registry))
        }
    }

    @Test
    fun futureCodecVersionIsRejectedInsteadOfFallingBack() {
        val registry = SandboxGame.loadRegistry()
        val store = SandboxSaveSlotStore(Files.createTempDirectory("sandbox-save-slot-store-future"))
        val session = SandboxSession.start(registry)
        store.save("checkpoint", session)
        val path = store.pathFor("checkpoint")
        val future = Files.readString(path, StandardCharsets.UTF_8)
            .replace("saveVersion=${SandboxSaveCodec.SAVE_VERSION}", "saveVersion=${SandboxSaveCodec.SAVE_VERSION + 1}")
        Files.writeString(path, future, StandardCharsets.UTF_8)

        assertFailsWith<IllegalArgumentException> { store.restore("checkpoint", registry) }
    }

    @Test
    fun packContentAndMapMismatchesAreRejectedInsteadOfFallingBack() {
        val registry = SandboxGame.loadRegistry()
        val store = SandboxSaveSlotStore(
            Files.createTempDirectory("sandbox-save-slot-store-compatibility"),
            SandboxAutosavePolicy(slotNames = listOf("autosave-a")),
        )
        val session = SandboxSession.start(registry)
        store.save("checkpoint", session)
        store.maybeAutosave(session)
        val path = store.pathFor("checkpoint")
        val original = Files.readString(path, StandardCharsets.UTF_8)

        listOf(
            original.replace("packId=${registry.manifest.id}", "packId=other-pack"),
            original.replace("contentVersion=${registry.manifest.version}", "contentVersion=other-version"),
            original.replace("mapId=${session.runtime.state.mapId}", "mapId=unknown-map"),
        ).forEach { incompatible ->
            Files.writeString(path, incompatible, StandardCharsets.UTF_8)
            assertFailsWith<Exception> { store.restore("checkpoint", registry) }
            Files.writeString(path, original, StandardCharsets.UTF_8)
        }
    }

    @Test
    fun listMetadataRejectsFilenameAndMetadataSlotMismatch() {
        val registry = SandboxGame.loadRegistry()
        val store = SandboxSaveSlotStore(Files.createTempDirectory("sandbox-save-slot-store-filename"))
        store.save("checkpoint", SandboxSession.start(registry))
        val original = store.pathFor("checkpoint")
        val renamed = original.resolveSibling("other.save")
        Files.move(original, renamed)

        assertEquals(emptyList(), store.listMetadata())
    }
}
