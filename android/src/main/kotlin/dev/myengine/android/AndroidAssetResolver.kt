package dev.myengine.android

import android.content.res.AssetManager
import dev.myengine.render.RenderAssetRef
import dev.myengine.render.RenderAssetResolver

/** Android-only resolver for the packaged content tree; missing refs remain palette-fallback safe. */
class AndroidAssetResolver(
    private val assets: AssetManager,
    private val rootPrefix: String = "",
) : RenderAssetResolver {
    private val atlasKeys = mutableMapOf<String, Set<String>>()

    override fun isAvailable(reference: RenderAssetRef): Boolean {
        val assetPath = listOf(rootPrefix.trim('/'), reference.path.trim('/'))
            .filter { it.isNotEmpty() }
            .joinToString("/")
        if (assetPath.contains("..")) return false
        val key = reference.atlasKey ?: return assets.openOrNull(assetPath) { true } == true
        val keys = atlasKeys.getOrPut(assetPath) {
            assets.openOrNull(assetPath) { stream ->
                stream.bufferedReader().useLines { lines ->
                    lines.map { it.substringBefore('#').trim() }
                        .filter { it.isNotEmpty() }
                        .map { it.substringBefore('=').trim() }
                        .filter { it.isNotEmpty() }
                        .toSet()
                }
            } ?: emptySet()
        }
        return key in keys
    }

    private fun <T> AssetManager.openOrNull(path: String, block: (java.io.InputStream) -> T): T? =
        runCatching { open(path).use(block) }.getOrNull()
}
