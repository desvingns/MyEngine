package dev.myengine.render

import dev.myengine.core.Tick
import dev.myengine.world.TilePosition
import dev.myengine.world.WorldSize
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
    fun tapCreatesBuildCommandWhenTowerSelected() {
        val camera = Camera(WorldSize(64, 64), viewportWidth = 240f, viewportHeight = 240f)
        val state = InputState(camera = camera, selectedTowerId = "basic")

        val result = InputAdapter().handle(PlatformInputEvent.Tap(ScreenPoint(120f, 120f)), state, Tick(3))

        assertEquals(1, result.commands.size)
        assertIs<BuildTowerCommand>(result.commands.single())
        assertTrue(result.state.nextCommandId > state.nextCommandId)
    }
}
