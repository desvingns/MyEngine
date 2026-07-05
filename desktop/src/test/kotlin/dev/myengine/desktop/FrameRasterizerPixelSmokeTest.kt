package dev.myengine.desktop

import dev.myengine.core.Tick
import dev.myengine.render.Camera
import dev.myengine.render.DebugOverlay
import dev.myengine.render.EngineSnapshot
import dev.myengine.render.PlaceholderRenderSurface
import dev.myengine.render.RenderEntity
import dev.myengine.render.RenderFrame
import dev.myengine.render.RenderKind
import dev.myengine.render.RenderPalette
import dev.myengine.render.RenderPrimitive
import dev.myengine.render.RenderTile
import dev.myengine.world.TilePosition
import dev.myengine.world.WorldSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * SG-003 follow-up: deterministic, headless pixel-smoke for [FrameRasterizer].
 *
 * This satisfies the `docs/contracts/render.md` "Screenshot or pixel-smoke" gate that plain unit
 * tests did not meet: every one of the six [RenderKind]s is projected through a real [Camera] by
 * [PlaceholderRenderSurface], rasterized into a `BufferedImage`, and its cell-center pixel is
 * asserted to equal the shared [RenderPalette] color for that kind. It also proves the enemy pip
 * is drawn only when a primitive carries health, that the core readout paints something, and that
 * rasterization is bit-for-bit deterministic.
 *
 * Pure AWT + engine-render types only: no libGDX, no GL, no display, no golden PNG.
 */
class FrameRasterizerPixelSmokeTest {

    // Small world so the default camera center is a whole tile; large viewport so every projected
    // center lands well inside the image with room for a CELL_SIZE cell around it.
    private val worldSize = WorldSize(16, 16)
    private val width = 480
    private val height = 480

    private val surface = PlaceholderRenderSurface()

    private fun camera(): Camera =
        Camera(worldSize, viewportWidth = width.toFloat(), viewportHeight = height.toFloat())

    private fun overlay() = DebugOverlay(
        tick = Tick(3),
        entityCount = 2,
        wave = null,
        selectedTile = null,
        lastCommandOrError = null,
    )

    /**
     * Snapshot covering ALL SIX kinds. Tiles/entities sit on distinct tiles spaced >= 2 apart on
     * each axis. At the default scale (BASE_TILE_PIXELS = 24 px/tile, zoom 1) neighbouring centers
     * are >= 48 px apart, comfortably more than CELL_SIZE (20), so no two cells overlap and each
     * primitive's rounded screen center is unambiguous.
     */
    private fun sixKindSnapshot() = EngineSnapshot(
        worldSize = worldSize,
        tiles = listOf(
            RenderTile(TilePosition(4, 4), "floor", buildable = true),
            RenderTile(TilePosition(6, 4), "wall", buildable = false),
            RenderTile(TilePosition(8, 4), "resource", buildable = false),
            RenderTile(TilePosition(10, 4), "core", buildable = false),
        ),
        entities = listOf(
            // health = null -> no pip (TOWER color fills its whole top-left corner).
            RenderEntity(id = 1, type = "tower:pulse", position = TilePosition(4, 8), health = null),
            // non-null health -> pip drawn in the cell's top-left corner.
            RenderEntity(id = 2, type = "enemy:grunt", position = TilePosition(6, 8), health = 7),
        ),
        coreHealth = 42,
        debug = overlay(),
    )

    private fun RenderFrame.primitiveOf(kind: RenderKind): RenderPrimitive =
        primitives.single { it.kind == kind }

    /** getRGB returns ARGB (alpha 0xFF for TYPE_INT_RGB); compare only the low 24 bits. */
    private fun rgb24(argb: Int): Int = argb and 0xFFFFFF

    @Test
    fun eachKindCellCenterPaintsItsPaletteColor() {
        val camera = camera()
        val frame = surface.project(sixKindSnapshot(), camera)
        val image = FrameRasterizer().rasterize(frame, width, height)

        // Sanity: all six kinds are present in the projected frame.
        val kinds = frame.primitives.map { it.kind }.toSet()
        assertEquals(RenderKind.entries.toSet(), kinds, "frame must cover every RenderKind")

        for (kind in RenderKind.entries) {
            val primitive = frame.primitiveOf(kind)
            val px = Math.round(primitive.screen.x)
            val py = Math.round(primitive.screen.y)
            assertTrue(px in 0 until width && py in 0 until height, "$kind center must be inside image")

            val sampled = rgb24(image.getRGB(px, py))
            val expected = RenderPalette.color(kind).toRgbInt()
            assertEquals(expected, sampled, "cell center for $kind must be its palette color")
        }
    }

    @Test
    fun enemyDrawsPipButTowerDoesNot() {
        val camera = camera()
        val frame = surface.project(sixKindSnapshot(), camera)
        val image = FrameRasterizer().rasterize(frame, width, height)

        val pip = RenderPalette.enemyPip.toRgbInt()

        val enemy = frame.primitiveOf(RenderKind.ENEMY)
        val enemyLeft = Math.round(enemy.screen.x) - FrameRasterizer.CELL_SIZE / 2
        val enemyTop = Math.round(enemy.screen.y) - FrameRasterizer.CELL_SIZE / 2
        // The enemy cell's top-left pip region contains the pip color (health != null).
        assertTrue(
            regionContainsColor(image, enemyLeft, enemyTop, FrameRasterizer.PIP_SIZE, pip),
            "enemy cell top-left must contain the enemy pip color",
        )

        val tower = frame.primitiveOf(RenderKind.TOWER)
        val towerLeft = Math.round(tower.screen.x) - FrameRasterizer.CELL_SIZE / 2
        val towerTop = Math.round(tower.screen.y) - FrameRasterizer.CELL_SIZE / 2
        val towerColor = RenderPalette.color(RenderKind.TOWER).toRgbInt()
        // The tower cell's top-left is the tower color, NOT the pip: no pip when health == null.
        assertEquals(
            towerColor,
            rgb24(image.getRGB(towerLeft, towerTop)),
            "tower cell top-left must be the tower color (no pip)",
        )
        assertTrue(
            !regionContainsColor(image, towerLeft, towerTop, FrameRasterizer.PIP_SIZE, pip),
            "tower cell top-left must not contain any pip color",
        )
    }

    @Test
    fun coreHealthTextRegionPaintsSomething() {
        val camera = camera()
        val frame = surface.project(sixKindSnapshot(), camera)
        val image = FrameRasterizer().rasterize(frame, width, height)

        val background = RenderPalette.background.toRgbInt()
        // drawString rendered glyphs somewhere in a bounded box near the fixed text baseline. We do
        // NOT assert exact glyph colors (AWT font rendering varies by JVM); only that at least one
        // pixel differs from the background, i.e. text was painted.
        val boxLeft = FrameRasterizer.CORE_TEXT_X
        val boxTop = FrameRasterizer.CORE_TEXT_Y - 12 // baseline is at the bottom of the glyphs
        var painted = false
        outer@ for (dy in 0 until 16) {
            for (dx in 0 until 80) {
                val x = boxLeft + dx
                val y = boxTop + dy
                if (x !in 0 until width || y !in 0 until height) continue
                if (rgb24(image.getRGB(x, y)) != background) {
                    painted = true
                    break@outer
                }
            }
        }
        assertTrue(painted, "core-health text region must contain non-background pixels")
    }

    @Test
    fun rasterizationIsPixelDeterministic() {
        val frame = surface.project(sixKindSnapshot(), camera())
        val rasterizer = FrameRasterizer()

        val first = rasterizer.rasterize(frame, width, height)
        val second = rasterizer.rasterize(frame, width, height)

        val a = first.getRGB(0, 0, width, height, null, 0, width)
        val b = second.getRGB(0, 0, width, height, null, 0, width)
        assertTrue(a.contentEquals(b), "rasterizing the same frame twice must be pixel-identical")

        // Guard against an all-background frame masking a real regression: a known cell center
        // (the CORE tile) must have been painted with its non-background palette color.
        val core = frame.primitiveOf(RenderKind.CORE)
        val idx = Math.round(core.screen.y) * width + Math.round(core.screen.x)
        assertNotEquals(RenderPalette.background.toRgbInt(), rgb24(a[idx]))
    }

    private fun regionContainsColor(
        image: java.awt.image.BufferedImage,
        left: Int,
        top: Int,
        size: Int,
        expectedRgb24: Int,
    ): Boolean {
        for (dy in 0 until size) {
            for (dx in 0 until size) {
                val x = left + dx
                val y = top + dy
                if (x !in 0 until image.width || y !in 0 until image.height) continue
                if (rgb24(image.getRGB(x, y)) == expectedRgb24) return true
            }
        }
        return false
    }
}
