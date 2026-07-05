package dev.myengine.games.sandbox

import dev.myengine.render.Camera
import dev.myengine.render.PlaceholderRenderSurface
import dev.myengine.world.WorldSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * SG-003 integration guard: projecting an authoritative snapshot through the render surface
 * must not mutate simulation state. We run a few deterministic steps, capture the state's
 * stable hash, project, and assert the hash is unchanged. No literal hash is asserted so the
 * test stays robust to unrelated content/balance changes.
 */
class SandboxRenderNonMutationTest {

    @Test
    fun projectingSnapshotDoesNotMutateAuthoritativeState() {
        val registry = SandboxGame.loadRegistry()
        val runtime = SandboxGame.createRuntime(registry)
        runtime.step(10)

        val snapshot = runtime.snapshot()
        val hashBefore = runtime.state.stableHash()

        val camera = Camera(
            WorldSize(snapshot.worldSize.width, snapshot.worldSize.height),
            viewportWidth = 480f,
            viewportHeight = 480f,
        )
        val frame = PlaceholderRenderSurface().project(snapshot, camera)

        // Sanity: the projection actually did work (world tiles at minimum).
        assertTrue(frame.primitives.isNotEmpty(), "expected a non-empty render frame")

        // The authoritative state hash is unchanged by the render path.
        assertEquals(hashBefore, runtime.state.stableHash())
    }
}
