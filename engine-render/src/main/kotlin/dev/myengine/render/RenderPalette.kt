package dev.myengine.render

/**
 * A pure RGB color triple (each channel 0..255). Kept AWT/Android-free on purpose:
 * this type compiles into the Android artifact (via `:games:sandbox` -> `:engine-render`),
 * so it must not reference `java.awt.Color` or any Android graphics type. Concrete
 * launchers translate [Rgb] into their platform color at draw time.
 */
data class Rgb(val r: Int, val g: Int, val b: Int) {
    init {
        require(r in 0..255 && g in 0..255 && b in 0..255) { "Rgb channels must be in 0..255." }
    }

    /** Packed 0xRRGGBB, convenient for `BufferedImage.setRGB`/`getRGB` in a JVM harness. */
    fun toRgbInt(): Int = (r shl 16) or (g shl 8) or b
}

/**
 * The durable, game-agnostic mapping from a [RenderKind] to a concrete placeholder color,
 * plus the auxiliary colors a launcher needs (background, core-health text, enemy health pip).
 *
 * This is the shared source of truth SG-003 follow-up note #2 asked for: any future launcher
 * (desktop harness today, Android shell later) should reuse this mapping instead of inventing
 * its own kind->color table, so placeholder frames look the same everywhere and pixel-smoke
 * assertions stay stable.
 *
 * Colors are chosen to be clearly distinct from each other AND from [background] so that a
 * sampled pixel at a cell center unambiguously identifies its kind.
 */
object RenderPalette {
    /** Neutral dark fill behind every primitive; distinct from all six kind colors. */
    val background: Rgb = Rgb(16, 16, 24)

    /** Color used for the `core <n>` health readout text. */
    val coreHealthText: Rgb = Rgb(240, 240, 240)

    /** Marker color for an enemy health pip (drawn only when a primitive carries health). */
    val enemyPip: Rgb = Rgb(255, 96, 160)

    private val floor: Rgb = Rgb(64, 64, 64)      // dark gray
    private val wall: Rgb = Rgb(128, 128, 128)    // mid gray
    private val resource: Rgb = Rgb(240, 192, 32) // amber / yellow
    private val core: Rgb = Rgb(32, 224, 224)     // cyan
    private val tower: Rgb = Rgb(48, 96, 240)     // blue
    private val enemy: Rgb = Rgb(224, 48, 48)     // red

    /** One distinct color per render kind. Total function over all six kinds. */
    fun color(kind: RenderKind): Rgb = when (kind) {
        RenderKind.TILE_FLOOR -> floor
        RenderKind.TILE_WALL -> wall
        RenderKind.TILE_RESOURCE -> resource
        RenderKind.CORE -> core
        RenderKind.TOWER -> tower
        RenderKind.ENEMY -> enemy
    }
}
