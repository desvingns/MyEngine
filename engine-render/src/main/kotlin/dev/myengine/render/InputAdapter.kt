package dev.myengine.render

import dev.myengine.core.CommandId
import dev.myengine.core.EngineCommand
import dev.myengine.core.Tick

sealed class PlatformInputEvent {
    data class Tap(val point: ScreenPoint) : PlatformInputEvent()
    data class Pan(val deltaTilesX: Float, val deltaTilesY: Float) : PlatformInputEvent()
    data class Pinch(val factor: Float) : PlatformInputEvent()
}

data class InputState(
    val camera: Camera,
    val selectedTowerId: String? = null,
    val selectedTile: dev.myengine.world.TilePosition? = null,
    val nextCommandId: Long = 1,
)

data class InputResult(
    val state: InputState,
    val commands: List<EngineCommand>,
)

class InputAdapter {
    fun handle(event: PlatformInputEvent, state: InputState, scheduledTick: Tick): InputResult =
        when (event) {
            is PlatformInputEvent.Pan -> InputResult(state.copy(camera = state.camera.pan(event.deltaTilesX, event.deltaTilesY)), emptyList())
            is PlatformInputEvent.Pinch -> InputResult(state.copy(camera = state.camera.zoomBy(event.factor)), emptyList())
            is PlatformInputEvent.Tap -> {
                val tile = state.camera.screenToTile(event.point)
                val towerId = state.selectedTowerId
                if (towerId == null) {
                    InputResult(state.copy(selectedTile = tile), emptyList())
                } else {
                    val command = BuildTowerCommand(CommandId(state.nextCommandId), scheduledTick, towerId, tile)
                    InputResult(state.copy(selectedTile = tile, nextCommandId = state.nextCommandId + 1), listOf(command))
                }
            }
        }
}
