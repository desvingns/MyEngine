package dev.myengine.desktop

import dev.myengine.content.ContentLoadResult
import dev.myengine.content.ContentPackLoader
import dev.myengine.content.ContentValidationError
import dev.myengine.games.sandbox.SandboxGame
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DesktopContentHotReloadTest {
    private val registry = SandboxGame.loadRegistry()

    @Test
    fun validReloadRestartsDeterministicallyWithTheSameSeed() {
        val seeds = mutableListOf<Long>()
        val session = DesktopContentHotReloadSession(
            packRoot = Files.createTempDirectory("myengine-hot-reload"),
            seed = 431L,
            loader = { ContentPackLoader.load(SandboxGame.contentRoot()) },
            scenarioRunner = { loadedRegistry, seed ->
                seeds += seed
                SandboxGame.runScriptedScenario(loadedRegistry, seed)
            },
        )

        val initial = session.start()
        val reloaded = session.reload()

        assertEquals(DesktopReloadStatus.STARTED, initial.status)
        assertEquals(DesktopReloadStatus.RELOADED, reloaded.status)
        assertEquals(listOf(431L, 431L), seeds)
        assertEquals(initial.scenario?.hash, reloaded.scenario?.hash)
        assertTrue(reloaded.elapsedMillis < 2_000L, "sample pack reload must stay below 2 seconds")
    }

    @Test
    fun invalidReloadReportsErrorsAndPreservesLastGoodScenario() {
        var valid = true
        val session = DesktopContentHotReloadSession(
            packRoot = Files.createTempDirectory("myengine-hot-reload-invalid"),
            loader = {
                if (valid) ContentLoadResult(registry, emptyList())
                else ContentLoadResult(null, listOf(ContentValidationError("towers.properties", "pulse", "damage", "bad value")))
            },
        )

        val initial = session.start()
        valid = false
        val rejected = session.reload()

        assertEquals(DesktopReloadStatus.STARTED, initial.status)
        assertEquals(DesktopReloadStatus.REJECTED, rejected.status)
        assertEquals(1, rejected.errors.size)
        assertEquals(initial.scenario?.hash, rejected.lastGoodHash)
        assertEquals(initial.scenario?.hash, assertNotNull(session.currentScenario).hash)
    }

    @Test
    fun loaderExceptionsAreSurfaceableInsteadOfCrashingLauncher() {
        val session = DesktopContentHotReloadSession(
            packRoot = Files.createTempDirectory("myengine-hot-reload-exception"),
            loader = { error("pack is being written") },
        )

        val outcome = session.start()

        assertEquals(DesktopReloadStatus.REJECTED, outcome.status)
        assertEquals("load", outcome.errors.single().field)
        assertTrue(outcome.lastGoodHash == null)
    }

    @Test
    fun watcherCoalescesAFileSaveIntoOneChangeCallback() {
        val root = Files.createTempDirectory("myengine-hot-reload-watch")
        val callbacks = AtomicInteger()
        val watcher = DesktopContentPackWatcher(root, debounceMillis = 10L)
        val thread = Thread {
            watcher.runUntilInterrupted { callbacks.incrementAndGet() }
        }
        thread.start()

        try {
            root.resolve("manifest.properties").writeText("id=watch\n")
            root.resolve("manifest.properties").writeText("id=watch\nversion=1\n")

            var attempts = 0
            while (callbacks.get() == 0 && attempts++ < 200) Thread.sleep(10L)
        } finally {
            thread.interrupt()
            thread.join(2_000L)
        }

        assertEquals(1, callbacks.get())
        assertTrue(!thread.isAlive, "watcher must stop after interruption")
    }
}
