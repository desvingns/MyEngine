package dev.myengine.content

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NeedContentLoaderTest {
    @Test
    fun sandboxPackLoadsHungerAndRestPolicies() {
        val result = ContentPackLoader.load(currentPackRoot("games/sandbox/content/sandbox"))

        assertTrue(result.isValid, result.errors.joinToString("\n"))
        val registry = result.registry!!
        assertEquals(1, registry.requireNeed("hunger").decayPerTick)
        assertEquals(25, registry.requireNeed("hunger").threshold)
        assertEquals("eat", registry.requireNeed("hunger").jobType)
        assertEquals("sleep", registry.requireNeed("rest").jobType)
    }

    @Test
    fun needContentRejectsInvalidThresholdAndDecay() {
        assertFailsWith<IllegalArgumentException> {
            NeedContent("hunger", decayPerTick = 0, threshold = 25, recoveryAmount = 10, jobType = "eat", priority = 1)
        }
        assertFailsWith<IllegalArgumentException> {
            NeedContent("hunger", decayPerTick = 1, threshold = 101, recoveryAmount = 10, jobType = "eat", priority = 1)
        }
    }

    private fun currentPackRoot(relativePath: String): Path {
        val cwd = Paths.get("").toAbsolutePath()
        return generateSequence(cwd) { it.parent }
            .map { it.resolve(relativePath) }
            .firstOrNull(Files::isDirectory)
            ?: error("Could not locate content pack '$relativePath' from $cwd")
    }
}
