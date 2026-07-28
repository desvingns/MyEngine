package dev.myengine.render

/** Platform-owned availability check for an opaque render asset reference. */
fun interface RenderAssetResolver {
    fun isAvailable(reference: RenderAssetRef): Boolean
}
