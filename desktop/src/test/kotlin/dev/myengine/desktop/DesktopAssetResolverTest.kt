package dev.myengine.desktop

import dev.myengine.core.Tick
import dev.myengine.render.RenderAssetRef
import dev.myengine.render.RenderFrame
import dev.myengine.render.RenderKind
import dev.myengine.render.RenderPalette
import dev.myengine.render.RenderPrimitive
import dev.myengine.render.ScreenPoint
import dev.myengine.world.TilePosition
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopAssetResolverTest {
    @Test
    fun resolvesSpriteFilesAndAtlasKeysInsidePackRoot() {
        val root = Files.createTempDirectory("myengine-desktop-assets")
        Files.createDirectories(root.resolve("visuals"))
        root.resolve("visuals/tile.sprite").writeText("original placeholder")
        root.resolve("visuals/placeholder.atlas").writeText("tile.floor\n")
        val resolver = DesktopAssetResolver(root)

        assertTrue(resolver.isAvailable(RenderAssetRef("visuals/tile.sprite")))
        assertTrue(resolver.isAvailable(RenderAssetRef("visuals/placeholder.atlas", "tile.floor")))
        assertFalse(resolver.isAvailable(RenderAssetRef("visuals/placeholder.atlas", "tile.missing")))
        assertFalse(resolver.isAvailable(RenderAssetRef("../outside.sprite")))
    }

    @Test
    fun rasterizerMarksResolvedAssetsAndKeepsOmittedOrMissingRefsOnDeterministicPaletteFallback() {
        val root = Files.createTempDirectory("myengine-desktop-raster-assets")
        Files.createDirectories(root.resolve("visuals"))
        root.resolve("visuals/tile.sprite").writeText("original placeholder")
        val resolver = DesktopAssetResolver(root)
        val frame = RenderFrame(
            primitives = listOf(
                RenderPrimitive(
                    kind = RenderKind.TILE_FLOOR,
                    tile = TilePosition(0, 0),
                    screen = ScreenPoint(80f, 80f),
                    assetRef = RenderAssetRef("visuals/tile.sprite"),
                ),
                RenderPrimitive(
                    kind = RenderKind.TILE_WALL,
                    tile = TilePosition(1, 0),
                    screen = ScreenPoint(120f, 80f),
                ),
                RenderPrimitive(
                    kind = RenderKind.TILE_RESOURCE,
                    tile = TilePosition(2, 0),
                    screen = ScreenPoint(160f, 80f),
                    assetRef = RenderAssetRef("visuals/missing.sprite"),
                ),
            ),
            path = emptyList(),
            coreHealth = 10,
            tick = Tick(0),
        )

        val rasterizer = FrameRasterizer(resolver)
        val first = rasterizer.rasterize(frame, 240, 160)
        val second = rasterizer.rasterize(frame, 240, 160)

        assertEquals(
            RenderPalette.assetMarker.toRgbInt(),
            first.getRGB(80, 80) and 0xFFFFFF,
            "an available asset must be visible to the desktop consumer",
        )
        assertEquals(
            RenderPalette.color(RenderKind.TILE_WALL).toRgbInt(),
            first.getRGB(120, 80) and 0xFFFFFF,
            "an omitted ref must use the palette fallback",
        )
        assertEquals(
            RenderPalette.color(RenderKind.TILE_RESOURCE).toRgbInt(),
            first.getRGB(160, 80) and 0xFFFFFF,
            "an unavailable declared ref must use the palette fallback",
        )
        assertTrue(
            first.getRGB(0, 0, 240, 160, null, 0, 240)
                .contentEquals(second.getRGB(0, 0, 240, 160, null, 0, 240)),
            "asset resolution and palette fallback must be pixel-deterministic",
        )
    }
}
