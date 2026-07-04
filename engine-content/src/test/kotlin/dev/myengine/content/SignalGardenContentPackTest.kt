package dev.myengine.content

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Loads the ACTUAL on-disk Signal Garden content pack (not a temp fixture) and
 * asserts it validates and resolves its key cross-references. Resolves the pack
 * purely as a filesystem path so engine-content stays free of any game module deps.
 */
class SignalGardenContentPackTest {
    @Test
    fun signalGardenPackLoadsAndResolves() {
        val root = signalGardenRoot()

        val result = ContentPackLoader.load(root)

        assertTrue(result.isValid, result.errors.joinToString("\n"))
        val registry = result.registry
        assertNotNull(registry)
        assertEquals("signal-garden", registry.manifest.id)

        assertTrue(registry.resources.containsKey("charge"), "resource 'charge' should be present")
        assertEquals("charge", registry.towers["pulse-emitter"]?.costResource)
        assertEquals("charge", registry.enemies["drift-spark"]?.rewardResource)
    }

    private fun signalGardenRoot(): Path {
        val relative = Paths.get("games", "signal-garden", "content", "signal-garden")
        val cwd = Paths.get("").toAbsolutePath()
        val candidate = generateSequence(cwd) { it.parent }
            .take(6)
            .map { it.resolve(relative) }
            .firstOrNull(Files::exists)
        return requireNotNull(candidate) {
            "Could not locate '$relative' walking up from '$cwd'."
        }
    }
}
