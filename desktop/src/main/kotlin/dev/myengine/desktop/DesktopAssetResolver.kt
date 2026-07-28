package dev.myengine.desktop

import dev.myengine.render.RenderAssetRef
import dev.myengine.render.RenderAssetResolver
import java.nio.file.Files
import java.nio.file.Path

/**
 * Desktop-only availability resolver. It validates paths relative to one content-pack root and
 * treats the small text atlas index as one region key per non-comment line.
 */
class DesktopAssetResolver(packRoot: Path) : RenderAssetResolver {
    private val root = packRoot.toAbsolutePath().normalize()
    private val atlasKeys = mutableMapOf<Path, Set<String>>()

    override fun isAvailable(reference: RenderAssetRef): Boolean {
        val path = root.resolve(reference.path).normalize()
        if (!path.startsWith(root) || !Files.isRegularFile(path)) return false
        val key = reference.atlasKey ?: return true
        val keys = atlasKeys.getOrPut(path) {
            runCatching {
                Files.readAllLines(path)
                    .asSequence()
                    .map { it.substringBefore('#').trim() }
                    .filter { it.isNotEmpty() }
                    .map { it.substringBefore('=').trim() }
                    .filter { it.isNotEmpty() }
                    .toSet()
            }.getOrDefault(emptySet())
        }
        return key in keys
    }
}
