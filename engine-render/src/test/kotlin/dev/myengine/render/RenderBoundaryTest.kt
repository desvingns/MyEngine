package dev.myengine.render

import dev.myengine.core.CommandId
import dev.myengine.core.Tick
import dev.myengine.core.command.BuildTowerCommand
import dev.myengine.core.command.TileCoordinate
import dev.myengine.core.command.UpgradeTowerCommand
import dev.myengine.world.TilePosition
import dev.myengine.world.WorldSize
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RenderBoundaryTest {
    @Test
    fun cameraRoundtripsScreenAndWorld() {
        val camera = Camera(WorldSize(64, 64), viewportWidth = 240f, viewportHeight = 240f)
        val tile = camera.screenToTile(camera.worldToScreen(WorldPoint(10f, 12f)))

        assertEquals(TilePosition(10, 12), tile)
    }

    @Test
    fun panShiftsCenterByDelta() {
        val camera = Camera(WorldSize(64, 64), viewportWidth = 240f, viewportHeight = 240f)

        val panned = camera.pan(3f, -4f)

        // Start centered at (32, 32); a well-inside pan lands exactly on the delta.
        assertEquals(WorldPoint(35f, 28f), panned.center)
    }

    @Test
    fun panClampsCenterWithinWorldBounds() {
        val camera = Camera(WorldSize(64, 64), viewportWidth = 240f, viewportHeight = 240f)

        // Pan far past both world edges; center must clamp into [0..width]/[0..height].
        val pastMax = camera.pan(1000f, 1000f)
        assertEquals(WorldPoint(64f, 64f), pastMax.center)

        val pastMin = camera.pan(-1000f, -1000f)
        assertEquals(WorldPoint(0f, 0f), pastMin.center)
    }

    @Test
    fun zoomByClampsToSupportedRange() {
        val camera = Camera(WorldSize(64, 64), viewportWidth = 240f, viewportHeight = 240f)

        // Default zoom is 1f. A factor that would drop below MIN_ZOOM (0.5) clamps up.
        assertEquals(0.5f, camera.zoomBy(0.01f).zoom)
        // A factor that would exceed MAX_ZOOM (4) clamps down.
        assertEquals(4f, camera.zoomBy(100f).zoom)
    }

    @Test
    fun zoomChangesProjectionScale() {
        val camera = Camera(WorldSize(64, 64), viewportWidth = 240f, viewportHeight = 240f)
        val zoomedIn = camera.zoomBy(2f)
        val zoomedOut = camera.zoomBy(0.5f)

        // Measure the on-screen distance from viewport centre for a point one tile off-centre.
        // center is (32,32); pick (33,32) so only x moves.
        fun offset(cam: Camera): Float {
            val screen = cam.worldToScreen(WorldPoint(33f, 32f))
            return abs(screen.x - cam.worldToScreen(cam.center).x)
        }

        val baseOffset = offset(camera)
        // BASE_TILE_PIXELS=24: base=24, in=48, out=12. Scale grows with zoom.
        assertTrue(offset(zoomedIn) > baseOffset, "zooming in must magnify the projection")
        assertTrue(offset(zoomedOut) < baseOffset, "zooming out must shrink the projection")
    }

    @Test
    fun tapCreatesBuildCommandWhenTowerSelected() {
        val camera = Camera(WorldSize(64, 64), viewportWidth = 240f, viewportHeight = 240f)
        val state = InputState(camera = camera)
        val uiState = InputUiState(selectedTowerId = "basic")
        val suppliedId = CommandId(42)

        val result = InputAdapter().handle(
            PlatformInputEvent.Tap(ScreenPoint(120f, 120f)),
            state,
            Tick(3),
            uiState,
            suppliedId,
        )

        assertEquals(1, result.commands.size)
        val command = assertIs<BuildTowerCommand>(result.commands.single())
        assertEquals(suppliedId, command.id)
        assertEquals(Tick(3), command.scheduledTick)
        assertEquals("basic", command.towerId)
        assertEquals(TileCoordinate(32, 32), command.position)
        assertEquals(TilePosition(32, 32), result.state.selectedTile)
        assertEquals(camera, result.state.camera)

        // The adapter is a pure translator: repeated handling with the same caller ID
        // must not issue a fresh ID or mutate the supplied one.
        val repeated = InputAdapter().handle(
            PlatformInputEvent.Tap(ScreenPoint(120f, 120f)),
            state,
            Tick(3),
            uiState,
            suppliedId,
        )
        assertEquals(suppliedId, assertIs<BuildTowerCommand>(repeated.commands.single()).id)
    }

    @Test
    fun tapWithoutCallerCommandIdDoesNotIssueACommand() {
        val state = InputState(Camera(WorldSize(64, 64), 240f, 240f))

        val result = InputAdapter().handle(
            PlatformInputEvent.Tap(ScreenPoint(120f, 120f)),
            state,
            Tick(3),
            InputUiState(selectedTowerId = "basic"),
        )

        assertTrue(result.commands.isEmpty())
        assertEquals(TilePosition(32, 32), result.state.selectedTile)
    }

    @Test
    fun upgradeUsesCallerOwnedCommandId() {
        val state = InputState(Camera(WorldSize(64, 64), 240f, 240f))
        val suppliedId = CommandId(77)

        val result = InputAdapter().handle(
            PlatformInputEvent.Upgrade(branch = "main", tier = 2),
            state,
            Tick(9),
            InputUiState(selectedTowerEntityId = 123L),
            suppliedId,
        )

        val command = assertIs<UpgradeTowerCommand>(result.commands.single())
        assertEquals(suppliedId, command.id)
        assertEquals(Tick(9), command.scheduledTick)
        assertEquals(123L, command.towerEntityId)
        assertEquals("main", command.branch)
        assertEquals(2, command.tier)
        assertEquals(state, result.state)
    }
}
