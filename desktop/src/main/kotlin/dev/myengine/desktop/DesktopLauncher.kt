package dev.myengine.desktop

import dev.myengine.games.sandbox.SandboxGame
import dev.myengine.render.AsciiRenderer
import dev.myengine.render.Camera
import dev.myengine.render.PlaceholderRenderSurface
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

data class DesktopLaunchOptions(
    val packRoot: Path = SandboxGame.contentRoot(),
    val seed: Long = DesktopContentHotReloadSession.DEFAULT_SEED,
    val watch: Boolean = false,
) {
    companion object {
        fun parse(args: Array<String>): DesktopLaunchOptions {
            var packRoot: Path? = null
            var seed = DesktopContentHotReloadSession.DEFAULT_SEED
            var watch = false
            for (arg in args) {
                when {
                    arg == "--watch" -> watch = true
                    arg.startsWith("--pack=") -> packRoot = Paths.get(arg.substringAfter('='))
                    arg.startsWith("--seed=") -> seed = arg.substringAfter('=').toLongOrNull()
                        ?: error("Invalid --seed value: ${arg.substringAfter('=')}")
                    else -> error("Unknown desktop launcher argument: $arg")
                }
            }
            return DesktopLaunchOptions(packRoot = packRoot ?: SandboxGame.contentRoot(), seed = seed, watch = watch)
        }
    }
}

fun main(args: Array<String>) {
    val options = DesktopLaunchOptions.parse(args)
    val session = DesktopContentHotReloadSession(options.packRoot, options.seed)

    println(SandboxGame.banner())
    printOutcome(session.start(), options.packRoot)

    if (!options.watch) return
    if (!Files.isDirectory(options.packRoot)) {
        println("watch_error=Content pack directory does not exist: ${options.packRoot}")
        return
    }
    println("watching=${options.packRoot.toAbsolutePath().normalize()}")
    DesktopContentPackWatcher(options.packRoot).use { watcher ->
        watcher.runUntilInterrupted {
            printOutcome(session.reload(), options.packRoot)
        }
    }
}

private fun printOutcome(outcome: DesktopReloadOutcome, packRoot: Path) {
    println(
        "status=${outcome.status.id} seed=${outcome.seed} reload_ms=${outcome.elapsedMillis} " +
            "errors=${outcome.errors.size}",
    )
    if (outcome.errors.isNotEmpty()) {
        outcome.errors.forEach { println("error=$it") }
        outcome.lastGoodHash?.let { println("last_good_hash=$it") }
        return
    }

    val result = outcome.scenario ?: return
    println("hash=${result.hash}")
    println(AsciiRenderer().render(result.snapshot))

    // Debug-only placeholder render smoke: project the snapshot through the pure render surface,
    // rasterize the RenderFrame with the AWT harness rasterizer, and dump a PNG under the build dir.
    val worldSize = result.snapshot.worldSize
    val viewportWidth = worldSize.width * VIEWPORT_TILE_PIXELS
    val viewportHeight = worldSize.height * VIEWPORT_TILE_PIXELS
    val camera = Camera(worldSize, viewportWidth.toFloat(), viewportHeight.toFloat())
    val frame = PlaceholderRenderSurface().project(result.snapshot, camera)
    val rasterizer = FrameRasterizer(DesktopAssetResolver(packRoot))
    val image = rasterizer.rasterize(frame, viewportWidth, viewportHeight)
    // Resolves against the desktop module working dir under `desktop:run` -> desktop/build/render-smoke.png.
    val pngPath = Paths.get("build", "render-smoke.png").toAbsolutePath()
    rasterizer.writePng(image, pngPath)
    println("png=$pngPath")
}

private const val VIEWPORT_TILE_PIXELS = 24
