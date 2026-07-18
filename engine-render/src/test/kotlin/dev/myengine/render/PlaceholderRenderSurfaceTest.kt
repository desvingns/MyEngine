package dev.myengine.render

import dev.myengine.core.Tick
import dev.myengine.world.TilePosition
import dev.myengine.world.WorldSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * SG-003: pure projection of an [EngineSnapshot] into placeholder draw primitives.
 * Snapshots are hand-built; no game module is imported. All assertions are deterministic.
 */
class PlaceholderRenderSurfaceTest {

    private val camera = Camera(WorldSize(64, 64), viewportWidth = 240f, viewportHeight = 240f)
    private val surface = PlaceholderRenderSurface()

    private fun overlay(tick: Long = 7) = DebugOverlay(
        tick = Tick(tick),
        entityCount = 0,
        wave = null,
        selectedTile = null,
        lastCommandOrError = null,
    )

    private fun tile(x: Int, y: Int, terrainId: String) =
        RenderTile(TilePosition(x, y), terrainId, buildable = true)

    private fun expectedScreen(tile: TilePosition) =
        camera.worldToScreen(WorldPoint(tile.x + 0.5f, tile.y + 0.5f))

    @Test
    fun projectsTilesCoreTowersAndEnemies() {
        val snapshot = EngineSnapshot(
            worldSize = WorldSize(64, 64),
            tiles = listOf(
                tile(1, 1, "floor"),
                tile(2, 1, "wall"),
                tile(3, 1, "resource"),
                tile(4, 1, "core"),
            ),
            entities = listOf(
                RenderEntity(id = 10, type = "tower:pulse", position = TilePosition(5, 5)),
                RenderEntity(id = 11, type = "enemy:grunt", position = TilePosition(6, 6), health = 5),
            ),
            coreHealth = 20,
            debug = overlay(),
        )

        val frame = surface.project(snapshot, camera)
        val kinds = frame.primitives.map { it.kind }

        assertTrue(RenderKind.TILE_FLOOR in kinds)
        assertTrue(RenderKind.TILE_WALL in kinds)
        assertTrue(RenderKind.TILE_RESOURCE in kinds)
        assertTrue(RenderKind.CORE in kinds)
        assertTrue(RenderKind.TOWER in kinds)
        assertTrue(RenderKind.ENEMY in kinds)

        // ENEMY carries health, TOWER does not.
        val enemy = frame.primitives.single { it.kind == RenderKind.ENEMY }
        assertEquals(5, enemy.health)
        val tower = frame.primitives.single { it.kind == RenderKind.TOWER }
        assertNull(tower.health)

        // Frame metadata is derived straight from the snapshot.
        assertEquals(20, frame.coreHealth)
        assertEquals(Tick(7), frame.tick)

        // Every primitive projects its own tile centre through the camera.
        frame.primitives.forEach {
            assertEquals(expectedScreen(it.tile), it.screen)
        }
    }

    @Test
    fun projectsPathInSnapshotOrderAndTowerTierThroughTheSuppliedCamera() {
        val pathTiles = listOf(
            TilePosition(1, 1),
            TilePosition(2, 1),
            TilePosition(2, 2),
        )
        val snapshot = EngineSnapshot(
            worldSize = WorldSize(64, 64),
            tiles = listOf(tile(1, 1, "floor")),
            entities = listOf(
                RenderEntity(id = 1, type = "tower:pulse", position = TilePosition(5, 5), towerTier = 2),
                RenderEntity(id = 2, type = "enemy:grunt", position = TilePosition(6, 6), health = 5, towerTier = 9),
            ),
            path = pathTiles,
            coreHealth = 20,
            debug = overlay(),
        )

        val pathCamera = Camera(WorldSize(64, 64), viewportWidth = 240f, viewportHeight = 240f)
            .pan(3f, -4f)
            .zoomBy(1.5f)
        val frame = surface.project(snapshot, pathCamera)

        // Path points, like primitives, are projected at tile centres and keep their snapshot order.
        val expectedPath = pathTiles.map {
            pathCamera.worldToScreen(WorldPoint(it.x + 0.5f, it.y + 0.5f))
        }
        assertEquals(expectedPath, frame.path)

        // Tier is presentation data for towers only; it must not leak from other entity kinds.
        val tower = frame.primitives.single { it.kind == RenderKind.TOWER }
        assertEquals(2, tower.towerTier)
        val enemy = frame.primitives.single { it.kind == RenderKind.ENEMY }
        assertNull(enemy.towerTier)
    }

    @Test
    fun tilesPrecedeEntitiesAndEntitiesAreSortedByIdAscending() {
        val snapshot = EngineSnapshot(
            worldSize = WorldSize(64, 64),
            tiles = listOf(tile(1, 1, "floor"), tile(2, 1, "wall")),
            // Deliberately out of id order to prove the sort.
            entities = listOf(
                RenderEntity(id = 30, type = "tower:a", position = TilePosition(3, 3)),
                RenderEntity(id = 10, type = "enemy:b", position = TilePosition(4, 4), health = 1),
                RenderEntity(id = 20, type = "tower:c", position = TilePosition(5, 5)),
            ),
            coreHealth = 20,
            debug = overlay(),
        )

        val frame = surface.project(snapshot, camera)

        val tileCount = snapshot.tiles.size
        // All tile primitives come first.
        assertTrue(frame.primitives.take(tileCount).all { it.kind.isTile() })
        assertTrue(frame.primitives.drop(tileCount).none { it.kind.isTile() })

        // Entities appear in ascending id order. Map each entity primitive back to its id
        // via position (positions are unique here).
        val idByPosition = snapshot.entities.associate { it.position to it.id }
        val entityIds = frame.primitives.drop(tileCount).map { idByPosition.getValue(it.tile) }
        assertEquals(listOf(10L, 20L, 30L), entityIds)
    }

    @Test
    fun unknownTerrainAndEntityTypesAreSkippedWithoutCrashing() {
        val snapshot = EngineSnapshot(
            worldSize = WorldSize(64, 64),
            tiles = listOf(
                tile(1, 1, "floor"),
                tile(2, 1, "swamp"), // unknown terrain -> omitted
            ),
            entities = listOf(
                RenderEntity(id = 1, type = "tower:t", position = TilePosition(3, 3)),
                RenderEntity(id = 2, type = "prop:1", position = TilePosition(4, 4)), // unknown -> omitted
            ),
            coreHealth = 20,
            debug = overlay(),
        )

        val frame = surface.project(snapshot, camera)

        // Only the recognised floor tile and tower entity survive.
        assertEquals(2, frame.primitives.size)
        assertEquals(1, frame.primitives.count { it.kind == RenderKind.TILE_FLOOR })
        assertEquals(1, frame.primitives.count { it.kind == RenderKind.TOWER })
    }

    @Test
    fun projectionIsPureAndDoesNotMutateSnapshot() {
        val snapshot = EngineSnapshot(
            worldSize = WorldSize(64, 64),
            tiles = listOf(tile(1, 1, "floor"), tile(4, 1, "core")),
            entities = listOf(
                RenderEntity(id = 2, type = "enemy:g", position = TilePosition(6, 6), health = 5),
                RenderEntity(id = 1, type = "tower:p", position = TilePosition(5, 5)),
            ),
            path = listOf(TilePosition(1, 1), TilePosition(2, 1), TilePosition(2, 2)),
            coreHealth = 20,
            debug = overlay(),
        )
        val before = snapshot.copy()

        val first = surface.project(snapshot, camera)
        val second = surface.project(snapshot, camera)

        // Same inputs -> equal frames (data-class equality) on every call.
        assertEquals(first, second)
        // Source snapshot is untouched by projection.
        assertEquals(before, snapshot)
    }

    private fun RenderKind.isTile(): Boolean = this in setOf(
        RenderKind.TILE_FLOOR,
        RenderKind.TILE_WALL,
        RenderKind.TILE_RESOURCE,
        RenderKind.CORE,
    )
}
