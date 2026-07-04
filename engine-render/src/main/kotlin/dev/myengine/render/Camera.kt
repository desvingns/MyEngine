package dev.myengine.render

import dev.myengine.world.TilePosition
import dev.myengine.world.WorldSize

data class ScreenPoint(val x: Float, val y: Float)
data class WorldPoint(val x: Float, val y: Float)

class Camera(
    private val worldSize: WorldSize,
    private val viewportWidth: Float,
    private val viewportHeight: Float,
    val zoom: Float = 1f,
    val center: WorldPoint = WorldPoint(worldSize.width / 2f, worldSize.height / 2f),
) {
    init {
        require(viewportWidth > 0f && viewportHeight > 0f) { "Viewport must be positive." }
        require(zoom in MIN_ZOOM..MAX_ZOOM) { "Zoom is outside supported range." }
    }

    fun pan(deltaTilesX: Float, deltaTilesY: Float): Camera =
        copy(center = WorldPoint(center.x + deltaTilesX, center.y + deltaTilesY)).clamped()

    fun zoomBy(factor: Float): Camera =
        copy(zoom = (zoom * factor).coerceIn(MIN_ZOOM, MAX_ZOOM)).clamped()

    fun worldToScreen(point: WorldPoint): ScreenPoint {
        val scale = tilePixels()
        return ScreenPoint(
            x = viewportWidth / 2f + (point.x - center.x) * scale,
            y = viewportHeight / 2f + (point.y - center.y) * scale,
        )
    }

    fun screenToWorld(point: ScreenPoint): WorldPoint {
        val scale = tilePixels()
        return WorldPoint(
            x = center.x + (point.x - viewportWidth / 2f) / scale,
            y = center.y + (point.y - viewportHeight / 2f) / scale,
        )
    }

    fun screenToTile(point: ScreenPoint): TilePosition {
        val world = screenToWorld(point)
        return TilePosition(world.x.toInt().coerceIn(0, worldSize.width - 1), world.y.toInt().coerceIn(0, worldSize.height - 1))
    }

    private fun clamped(): Camera {
        val clampedCenter = WorldPoint(
            center.x.coerceIn(0f, worldSize.width.toFloat()),
            center.y.coerceIn(0f, worldSize.height.toFloat()),
        )
        return copy(center = clampedCenter)
    }

    private fun copy(
        worldSize: WorldSize = this.worldSize,
        viewportWidth: Float = this.viewportWidth,
        viewportHeight: Float = this.viewportHeight,
        zoom: Float = this.zoom,
        center: WorldPoint = this.center,
    ): Camera = Camera(worldSize, viewportWidth, viewportHeight, zoom, center)

    private fun tilePixels(): Float = BASE_TILE_PIXELS * zoom

    companion object {
        private const val BASE_TILE_PIXELS = 24f
        private const val MIN_ZOOM = 0.5f
        private const val MAX_ZOOM = 4f
    }
}
