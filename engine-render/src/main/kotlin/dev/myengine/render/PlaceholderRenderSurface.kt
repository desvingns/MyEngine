package dev.myengine.render

import dev.myengine.core.Tick
import dev.myengine.world.TilePosition

/**
 * Generic classes of placeholder draw primitives. A concrete Android/desktop
 * surface can later map each kind to a real drawable without the render module
 * knowing anything about game content.
 */
enum class RenderKind {
    TILE_FLOOR,
    TILE_WALL,
    TILE_RESOURCE,
    CORE,
    TOWER,
    ENEMY,
}

/**
 * A single, fully-projected draw instruction: what to draw (kind), where it lives
 * in tile space, where it lands on screen (through the camera) and optional health.
 */
data class RenderPrimitive(
    val kind: RenderKind,
    val tile: TilePosition,
    val screen: ScreenPoint,
    val health: Int? = null,
    val towerTier: Int? = null,
)

/**
 * A deterministic, ordered frame of primitives derived purely from an
 * [EngineSnapshot] projected through a [Camera].
 */
data class RenderFrame(
    val primitives: List<RenderPrimitive>,
    val path: List<ScreenPoint>,
    val coreHealth: Int,
    val tick: Tick,
)

/**
 * Pure projection from a simulation snapshot (through a camera) into a stable,
 * ordered list of placeholder draw primitives.
 *
 * This is render-only: it never mutates the snapshot, the camera, or any input,
 * and returns fresh data on every call. Ordering is deterministic so frames are
 * comparable across runs: tiles first (in snapshot order), then entities sorted
 * by id.
 */
class PlaceholderRenderSurface {
    fun project(snapshot: EngineSnapshot, camera: Camera): RenderFrame {
        val primitives = ArrayList<RenderPrimitive>(snapshot.tiles.size + snapshot.entities.size)

        for (tile in snapshot.tiles) {
            val kind = terrainKind(tile.terrainId) ?: continue
            primitives += RenderPrimitive(
                kind = kind,
                tile = tile.position,
                screen = tileCenterScreen(tile.position, camera),
            )
        }

        for (entity in snapshot.entities.sortedBy { it.id }) {
            val kind = entityKind(entity.type) ?: continue
            primitives += RenderPrimitive(
                kind = kind,
                tile = entity.position,
                screen = tileCenterScreen(entity.position, camera),
                health = if (kind == RenderKind.ENEMY) entity.health else null,
                towerTier = if (kind == RenderKind.TOWER) entity.towerTier else null,
            )
        }

        return RenderFrame(
            primitives = primitives,
            path = snapshot.path.map { tileCenterScreen(it, camera) },
            coreHealth = snapshot.coreHealth,
            tick = snapshot.debug.tick,
        )
    }

    private fun tileCenterScreen(tile: TilePosition, camera: Camera): ScreenPoint =
        camera.worldToScreen(WorldPoint(tile.x + 0.5f, tile.y + 0.5f))

    private fun terrainKind(terrainId: String): RenderKind? = when (terrainId) {
        "floor" -> RenderKind.TILE_FLOOR
        "wall" -> RenderKind.TILE_WALL
        "resource" -> RenderKind.TILE_RESOURCE
        "core" -> RenderKind.CORE
        else -> null
    }

    private fun entityKind(type: String): RenderKind? = when {
        type.startsWith("tower") -> RenderKind.TOWER
        type.startsWith("enemy") -> RenderKind.ENEMY
        else -> null
    }
}
