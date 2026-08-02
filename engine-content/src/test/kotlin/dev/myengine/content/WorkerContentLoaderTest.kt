package dev.myengine.content

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WorkerContentLoaderTest {
    @Test
    fun sandboxPackLoadsWorkerSpeedAndCapacity() {
        val result = ContentPackLoader.load(currentPackRoot("games/sandbox/content/sandbox"))

        assertTrue(result.isValid, result.errors.joinToString("\n"))
        assertEquals(2, result.registry!!.requireWorker("hauler").speedTilesPerTick)
        assertEquals(4, result.registry!!.requireWorker("hauler").capacity)
    }

    @Test
    fun workerContentRejectsNonPositiveCapabilities() {
        assertFailsWith<IllegalArgumentException> { WorkerContent("hauler", speedTilesPerTick = 0, capacity = 1) }
        assertFailsWith<IllegalArgumentException> { WorkerContent("hauler", speedTilesPerTick = 1, capacity = 0) }
    }

    private fun currentPackRoot(relativePath: String): Path {
        val cwd = Paths.get("").toAbsolutePath()
        return generateSequence(cwd) { it.parent }
            .map { it.resolve(relativePath) }
            .firstOrNull { Files.isDirectory(it) }
            ?: error("Could not locate content pack '$relativePath' from $cwd")
    }
}
