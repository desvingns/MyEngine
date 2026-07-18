package dev.myengine.render

import dev.myengine.core.CommandId
import dev.myengine.core.EngineCommand
import dev.myengine.core.Tick
import dev.myengine.core.command.BuildTowerCommand
import dev.myengine.core.command.TileCoordinate

sealed class PlatformInputEvent {
    data class Tap(val point: ScreenPoint) : PlatformInputEvent()
    data class Pan(val deltaTilesX: Float, val deltaTilesY: Float) : PlatformInputEvent()
    data class Pinch(val factor: Float) : PlatformInputEvent()
}

data class InputState(
    val camera: Camera,
    val selectedTile: dev.myengine.world.TilePosition? = null,
)

data class InputUiState(
    val selectedTowerId: String? = null,
)

data class InputResult(
    val state: InputState,
    val commands: List<EngineCommand>,
)

class InputAdapter {
    fun handle(
        event: PlatformInputEvent,
        state: InputState,
        scheduledTick: Tick,
        uiState: InputUiState = InputUiState(),
        commandId: CommandId? = null,
    ): InputResult =
        when (event) {
            is PlatformInputEvent.Pan -> InputResult(state.copy(camera = state.camera.pan(event.deltaTilesX, event.deltaTilesY)), emptyList())
            is PlatformInputEvent.Pinch -> InputResult(state.copy(camera = state.camera.zoomBy(event.factor)), emptyList())
            is PlatformInputEvent.Tap -> {
                val tile = state.camera.screenToTile(event.point)
                val towerId = uiState.selectedTowerId
                if (towerId == null || commandId == null) {
                    InputResult(state.copy(selectedTile = tile), emptyList())
                } else {
                    val command = BuildTowerCommand(
                        id = commandId,
                        scheduledTick = scheduledTick,
                        towerId = towerId,
                        position = TileCoordinate(tile.x, tile.y),
                    )
                    InputResult(state.copy(selectedTile = tile), listOf(command))
                }
            }
        }
}
