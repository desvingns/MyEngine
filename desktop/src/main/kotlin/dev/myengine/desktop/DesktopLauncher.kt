package dev.myengine.desktop

import dev.myengine.games.sandbox.SandboxGame
import dev.myengine.render.AsciiRenderer
import dev.myengine.render.Camera
import dev.myengine.render.PlaceholderRenderSurface
import java.nio.file.Paths

fun main() {
    println(SandboxGame.banner())
    val result = SandboxGame.runScriptedScenario()
    println("hash=${result.hash}")
    println(AsciiRenderer().render(result.snapshot))

    // Debug-only placeholder render smoke: project the snapshot through the pure render surface,
    // rasterize the RenderFrame with the AWT harness rasterizer, and dump a PNG under the build dir.
    val worldSize = result.snapshot.worldSize
    val viewportWidth = worldSize.width * VIEWPORT_TILE_PIXELS
    val viewportHeight = worldSize.height * VIEWPORT_TILE_PIXELS
    val camera = Camera(worldSize, viewportWidth.toFloat(), viewportHeight.toFloat())
    val frame = PlaceholderRenderSurface().project(result.snapshot, camera)
    val rasterizer = FrameRasterizer(DesktopAssetResolver(SandboxGame.contentRoot()))
    val image = rasterizer.rasterize(frame, viewportWidth, viewportHeight)
    // Resolves against the desktop module working dir under `desktop:run` -> desktop/build/render-smoke.png.
    val pngPath = Paths.get("build", "render-smoke.png").toAbsolutePath()
    rasterizer.writePng(image, pngPath)
    println("png=$pngPath")
}

private const val VIEWPORT_TILE_PIXELS = 24
