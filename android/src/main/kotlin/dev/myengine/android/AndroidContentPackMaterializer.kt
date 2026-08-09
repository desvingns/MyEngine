package dev.myengine.android

import android.content.res.AssetManager
import java.io.File
import java.nio.file.Path

/**
 * Copies the packaged sandbox content tree to app-private storage for the JVM-oriented loader.
 *
 * The content loader deliberately accepts [Path] rather than Android assets, so this adapter keeps
 * the Android boundary explicit: assets are materialized before the sandbox registry is created,
 * and the simulation continues to receive the same validated external content pack.
 */
internal object AndroidContentPackMaterializer {
    private const val CONTENT_DIRECTORY = "content"

    fun materialize(
        assets: AssetManager,
        filesDir: File,
        packAssetRoot: String = "sandbox",
    ): Path {
        require(packAssetRoot.isNotBlank() && !packAssetRoot.contains('/') && !packAssetRoot.contains("..")) {
            "Asset pack root must be a single safe directory name."
        }
        val destination = filesDir.toPath().resolve(CONTENT_DIRECTORY).resolve(packAssetRoot)
        copyTree(assets, packAssetRoot, destination.toFile())
        return destination
    }

    private fun copyTree(assets: AssetManager, assetPath: String, destination: File) {
        val children = assets.list(assetPath).orEmpty().sorted()
        if (children.isEmpty()) {
            destination.parentFile?.mkdirs()
            assets.open(assetPath).use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
            return
        }

        destination.mkdirs()
        children.forEach { child ->
            require(child != "." && child != ".." && !child.contains('/')) {
                "Unsafe asset child '$child'."
            }
            copyTree(assets, "$assetPath/$child", File(destination, child))
        }
    }
}
