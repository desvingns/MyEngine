package dev.myengine.desktop

import dev.myengine.render.RenderFrame
import dev.myengine.render.RenderPalette
import dev.myengine.render.Rgb
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

/**
 * Deterministic, headless rasterizer for a [RenderFrame]. Desktop-harness ONLY: it uses
 * `java.awt`/`javax.imageio`, which are unavailable on Android, so this class must never be
 * pulled into the Android artifact. It lives in the pure-JVM `desktop` application module.
 *
 * It is a pure render consumer: it reads a [RenderFrame] (already projected from an
 * `EngineSnapshot` through a `Camera` by `PlaceholderRenderSurface`) plus the shared
 * [RenderPalette], and produces a `BufferedImage`. It never mutates simulation state.
 *
 * Determinism: no randomness, `TYPE_INT_RGB`, antialiasing OFF for both shapes and text, and a
 * fixed [CELL_SIZE] cell centered on each primitive's screen point. That makes the color sampled
 * at a primitive's rounded screen point exactly `RenderPalette.color(kind)`.
 */
class FrameRasterizer {

    /**
     * Draw [frame] into a fresh [width] x [height] `BufferedImage`.
     *
     * - Fills the whole image with [RenderPalette.background].
     * - For each primitive (in frame order) draws a filled [CELL_SIZE]px square centered on its
     *   screen point, colored via [RenderPalette.color]; drawing is clipped to image bounds so
     *   off-screen primitives never crash.
     * - For each primitive that carries health draws a small [RenderPalette.enemyPip] marker in the
     *   cell's top-left corner. Primitives with null health (tiles/towers/core) get no pip.
     * - Draws a `core <n>` readout from [RenderFrame.coreHealth] at a fixed top-left position.
     */
    fun rasterize(frame: RenderFrame, width: Int, height: Int): BufferedImage {
        require(width > 0 && height > 0) { "Image dimensions must be positive." }
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF)

            g.color = awt(RenderPalette.background)
            g.fillRect(0, 0, width, height)

            for (primitive in frame.primitives) {
                val cx = Math.round(primitive.screen.x)
                val cy = Math.round(primitive.screen.y)
                val left = cx - CELL_SIZE / 2
                val top = cy - CELL_SIZE / 2

                g.color = awt(RenderPalette.color(primitive.kind))
                g.fillRect(left, top, CELL_SIZE, CELL_SIZE)

                if (primitive.health != null) {
                    g.color = awt(RenderPalette.enemyPip)
                    g.fillRect(left, top, PIP_SIZE, PIP_SIZE)
                }
            }

            g.color = awt(RenderPalette.coreHealthText)
            g.drawString("core ${frame.coreHealth}", CORE_TEXT_X, CORE_TEXT_Y)
        } finally {
            g.dispose()
        }
        return image
    }

    /** Write [image] to [path] as a PNG, creating parent directories as needed. */
    fun writePng(image: BufferedImage, path: Path) {
        path.parent?.let { Files.createDirectories(it) }
        Files.newOutputStream(path).use { ImageIO.write(image, "png", it) }
    }

    private fun awt(rgb: Rgb): Color = Color(rgb.r, rgb.g, rgb.b)

    companion object {
        /** Side length in pixels of the filled quad drawn per primitive, centered on its screen point. */
        const val CELL_SIZE: Int = 20

        /** Side length in pixels of the enemy health pip drawn in a cell's top-left corner. */
        const val PIP_SIZE: Int = 4

        /** Fixed baseline position of the `core <n>` readout string. */
        const val CORE_TEXT_X: Int = 4
        const val CORE_TEXT_Y: Int = 14
    }
}
