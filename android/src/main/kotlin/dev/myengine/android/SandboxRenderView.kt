package dev.myengine.android

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.SurfaceHolder
import android.view.SurfaceView
import dev.myengine.core.CommandId
import dev.myengine.core.EngineCommand
import dev.myengine.render.Camera
import dev.myengine.render.EngineSnapshot
import dev.myengine.render.InputAdapter
import dev.myengine.render.InputState
import dev.myengine.render.InputUiState
import dev.myengine.render.PlaceholderRenderSurface
import dev.myengine.render.PlatformInputEvent
import dev.myengine.render.RenderFrame
import dev.myengine.render.RenderKind
import dev.myengine.render.RenderPalette
import dev.myengine.render.ScreenPoint
import dev.myengine.render.WorldPoint

internal data class HudBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    fun contains(point: ScreenPoint): Boolean = point.x in left..right && point.y in top..bottom
}

internal data class SandboxHudLayout(
    val padding: Float,
    val buildPanel: HudBounds,
    val buildHeader: HudBounds,
    val buildRows: List<HudBounds>,
    val selectedPanel: HudBounds?,
    val selectedHeader: HudBounds?,
    val selectedInfoRows: List<HudBounds>,
    val upgradeRows: List<HudBounds>,
    val speedControls: List<HudBounds>,
    val resourcesBaseline: Float,
)

/** Pure pixel layout derived from dp policy; drawing and hit testing share this result. */
internal object SandboxHudLayoutModel {
    fun calculate(
        viewWidth: Float,
        viewHeight: Float,
        density: Float,
        buildRowCount: Int,
        upgradeRowCount: Int,
        hasSelection: Boolean,
    ): SandboxHudLayout {
        require(viewWidth >= 0f && viewHeight >= 0f)
        require(density > 0f)
        require(buildRowCount >= 0 && upgradeRowCount >= 0)

        val minimumSpeedWidth = MIN_TOUCH_TARGET_DP * density
        val padding = minOf(
            8f * density,
            ((viewWidth - minimumSpeedWidth) / 2f).coerceAtLeast(0f),
        )
        val gap = 8f * density
        val overlayHeight = 48f * density
        val headerHeight = 32f * density
        val rowHeight = 48f * density
        val speedTop = overlayHeight + gap
        val contentWidth = (viewWidth - padding * 2f).coerceAtLeast(0f)
        val speedGap = 4f * density
        val speedColumns = (1..PresentationSpeed.entries.size)
            .lastOrNull { columns ->
                columns * MIN_TOUCH_TARGET_DP * density + (columns - 1) * speedGap <= contentWidth
            }
            ?: 1
        val speedWidth = ((contentWidth - speedGap * (speedColumns - 1)) / speedColumns).coerceAtLeast(0f)
        val speedControls = List(PresentationSpeed.entries.size) { index ->
            val row = index / speedColumns
            val column = index % speedColumns
            val left = padding + (speedWidth + speedGap) * column
            val top = speedTop + (rowHeight + speedGap) * row
            HudBounds(left, top, left + speedWidth, top + rowHeight)
        }
        val speedGridBottom = speedControls.maxOfOrNull { it.bottom } ?: speedTop
        val sideBySide = hasSelection && contentWidth >= (280f * density * 2f + gap)
        val buildWidth = if (sideBySide) (contentWidth - gap) / 2f else contentWidth
        val buildTop = speedGridBottom + gap
        val buildHeader = HudBounds(padding, buildTop, padding + buildWidth, buildTop + headerHeight)
        val buildRows = rows(buildHeader.bottom, padding, buildWidth, rowHeight, buildRowCount)
        val buildBottom = buildRows.lastOrNull()?.bottom ?: buildHeader.bottom
        val buildPanel = HudBounds(padding, buildTop, padding + buildWidth, buildBottom)

        val selectedWidth = if (sideBySide) contentWidth - buildWidth - gap else contentWidth
        val selectedLeft = if (sideBySide) buildPanel.right + gap else padding
        val selectedTop = if (sideBySide) buildTop else buildPanel.bottom + gap
        val selectedHeader = if (hasSelection) {
            HudBounds(selectedLeft, selectedTop, selectedLeft + selectedWidth, selectedTop + headerHeight)
        } else {
            null
        }
        val infoRows = selectedHeader?.let { rows(it.bottom, selectedLeft, selectedWidth, rowHeight, 3) }.orEmpty()
        val upgradeRows = rows(
            infoRows.lastOrNull()?.bottom ?: selectedHeader?.bottom ?: selectedTop,
            selectedLeft,
            selectedWidth,
            rowHeight,
            if (hasSelection) upgradeRowCount else 0,
        )
        val selectedBottom = upgradeRows.lastOrNull()?.bottom
            ?: infoRows.lastOrNull()?.bottom
            ?: selectedHeader?.bottom
        val selectedPanel = if (selectedHeader != null && selectedBottom != null) {
            HudBounds(selectedLeft, selectedTop, selectedLeft + selectedWidth, selectedBottom)
        } else {
            null
        }
        return SandboxHudLayout(
            padding = padding,
            buildPanel = buildPanel,
            buildHeader = buildHeader,
            buildRows = buildRows,
            selectedPanel = selectedPanel,
            selectedHeader = selectedHeader,
            selectedInfoRows = infoRows,
            upgradeRows = upgradeRows,
            speedControls = speedControls,
            resourcesBaseline = (viewHeight - padding).coerceAtLeast(0f),
        )
    }

    private fun rows(top: Float, left: Float, width: Float, height: Float, count: Int): List<HudBounds> =
        List(count) { index ->
            val rowTop = top + height * index
            HudBounds(left, rowTop, left + width, rowTop + height)
        }

    private const val MIN_TOUCH_TARGET_DP = 48f
}

/**
 * Android-only [SurfaceView] consumer of immutable snapshots supplied by the activity loop.
 *
 * Presentation state (camera and selection) stays here. Touches become commands through
 * [InputAdapter]; this view cannot step or mutate the authoritative simulation.
 */
class SandboxRenderView(
    context: Context,
    private val latestSnapshot: () -> EngineSnapshot?,
    private val commandIdProvider: () -> CommandId,
    private val onCommand: (EngineCommand) -> Unit,
    private val presentationSpeed: () -> PresentationSpeed = { PresentationSpeed.ONE_X },
    private val onPresentationSpeedChange: (PresentationSpeed) -> Unit = {},
) : SurfaceView(context), SurfaceHolder.Callback {
    private val density = context.resources.displayMetrics.density
    private val scaledDensity = density * context.resources.configuration.fontScale
    private val renderer = PlaceholderRenderSurface()
    private val inputAdapter = InputAdapter()
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH_DP * density
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TEXT_SIZE_SP * scaledDensity
    }

    private var inputState: InputState? = null
    private var uiState = InputUiState()
    private var lastPoint: ScreenPoint? = null
    private var dragged = false
    private var scaled = false

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaled = true
            dispatchInput(PlatformInputEvent.Pinch(detector.scaleFactor))
            return true
        }
    })

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        updateInputState(width, height)
        renderLatestFrame()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        updateInputState(width, height)
        renderLatestFrame()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) = Unit

    /** Draws the most recently published immutable simulation snapshot, if the surface is ready. */
    fun renderLatestFrame() {
        val state = inputState ?: run {
            updateInputState(width, height)
            inputState
        } ?: return
        val snapshot = latestSnapshot() ?: return
        if (!holder.surface.isValid) return

        val canvas = runCatching { holder.lockCanvas() }.getOrNull() ?: return
        try {
            val frame = renderer.project(snapshot, state.camera)
            drawFrame(canvas, frame, state.camera)
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastPoint = ScreenPoint(event.x, event.y)
                dragged = false
                scaled = false
            }

            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress && event.pointerCount == 1) {
                    val previous = lastPoint
                    if (previous != null) {
                        val dx = event.x - previous.x
                        val dy = event.y - previous.y
                        if (dx != 0f || dy != 0f) {
                            dragged = true
                            val camera = inputState?.camera ?: return true
                            val pixelsPerTile = tilePixels(camera)
                            dispatchInput(PlatformInputEvent.Pan(-dx / pixelsPerTile, -dy / pixelsPerTile))
                        }
                    }
                    lastPoint = ScreenPoint(event.x, event.y)
                }
            }

            MotionEvent.ACTION_UP -> {
                if (!dragged && !scaled) {
                    handleTap(ScreenPoint(event.x, event.y))
                }
                lastPoint = null
            }

            MotionEvent.ACTION_CANCEL -> lastPoint = null
        }
        return true
    }

    private fun updateInputState(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val snapshot = latestSnapshot() ?: return
        val previous = inputState
        val camera = Camera(
            worldSize = snapshot.worldSize,
            viewportWidth = width.toFloat(),
            viewportHeight = height.toFloat(),
            zoom = previous?.camera?.zoom ?: 1f,
            center = previous?.camera?.center
                ?: WorldPoint(snapshot.worldSize.width / 2f, snapshot.worldSize.height / 2f),
        )
        inputState = InputState(camera = camera, selectedTile = previous?.selectedTile)
    }

    private fun dispatchInput(event: PlatformInputEvent) {
        val current = inputState ?: return
        val snapshot = latestSnapshot() ?: return
        val commandId = if (
            event is PlatformInputEvent.Upgrade ||
            (event is PlatformInputEvent.Tap && uiState.selectedTowerId != null)
        ) commandIdProvider() else null
        val result = inputAdapter.handle(
            event = event,
            state = current,
            scheduledTick = snapshot.debug.tick.next(),
            uiState = uiState,
            commandId = commandId,
        )
        inputState = result.state
        result.commands.forEach(onCommand)
        renderLatestFrame()
    }

    private fun handleTap(point: ScreenPoint) {
        val current = inputState ?: return
        val snapshot = latestSnapshot() ?: return
        val frame = renderer.project(snapshot, current.camera)
        val selectedInfo = uiState.selectedTowerEntityId?.let { id -> frame.hud.towers.firstOrNull { it.entityId == id } }
        val layout = hudLayout(frame, selectedInfo?.availableUpgrades?.size ?: 0, selectedInfo != null)
        val speedIndex = layout.speedControls.indexOfFirst { it.contains(point) }.takeIf { it >= 0 }
        if (speedIndex != null) {
            onPresentationSpeedChange(PresentationSpeed.entries[speedIndex])
            return
        }
        // Upgrade wins first by policy; calculated panels are disjoint even in narrow portrait.
        val upgradeIndex = layout.upgradeRows.indexOfFirst { it.contains(point) }.takeIf { it >= 0 }
        if (selectedInfo != null && upgradeIndex != null) {
            val upgrade = selectedInfo.availableUpgrades[upgradeIndex]
            dispatchInput(PlatformInputEvent.Upgrade(upgrade.branch, upgrade.tier))
            return
        }

        val buildIndex = layout.buildRows.indexOfFirst { it.contains(point) }.takeIf { it >= 0 }
        if (buildIndex != null) {
            uiState = InputUiState(selectedTowerId = frame.hud.buildTowers[buildIndex].towerId)
            renderLatestFrame()
            return
        }

        // Panel chrome consumes taps so it never leaks through into the world/build surface.
        if (layout.buildPanel.contains(point) || layout.selectedPanel?.contains(point) == true) return

        val halfTile = tilePixels(current.camera) / 2f
        val tower = frame.primitives.firstOrNull { primitive ->
            primitive.kind == RenderKind.TOWER &&
                point.x in (primitive.screen.x - halfTile)..(primitive.screen.x + halfTile) &&
                point.y in (primitive.screen.y - halfTile)..(primitive.screen.y + halfTile)
        }
        if (tower != null) {
            uiState = InputUiState(selectedTowerEntityId = tower.entityId)
            renderLatestFrame()
            return
        }
        dispatchInput(PlatformInputEvent.Tap(point))
    }

    private fun hudLayout(frame: RenderFrame, upgradeCount: Int, hasSelection: Boolean): SandboxHudLayout =
        SandboxHudLayoutModel.calculate(
            viewWidth = width.toFloat(),
            viewHeight = height.toFloat(),
            density = density,
            buildRowCount = frame.hud.buildTowers.size,
            upgradeRowCount = upgradeCount,
            hasSelection = hasSelection,
        )

    private fun drawFrame(canvas: Canvas, frame: RenderFrame, camera: Camera) {
        canvas.drawColor(androidColor(RenderPalette.background))
        drawTiles(canvas, frame, camera)
        drawPath(canvas, frame)
        drawEntities(canvas, frame, camera)
        drawOverlay(canvas, frame)
        drawHud(canvas, frame)
    }

    private fun drawPath(canvas: Canvas, frame: RenderFrame) {
        if (frame.path.size < 2) return
        strokePaint.color = androidColor(RenderPalette.enemyPip)
        val path = Path().apply {
            moveTo(frame.path.first().x, frame.path.first().y)
            frame.path.drop(1).forEach { lineTo(it.x, it.y) }
        }
        canvas.drawPath(path, strokePaint)
    }

    private fun drawTiles(canvas: Canvas, frame: RenderFrame, camera: Camera) {
        drawPrimitives(canvas, frame.primitives.filter { it.kind.isTile() }, camera, frame.hud.labels.tier)
    }

    private fun drawEntities(canvas: Canvas, frame: RenderFrame, camera: Camera) {
        drawPrimitives(canvas, frame.primitives.filterNot { it.kind.isTile() }, camera, frame.hud.labels.tier)
    }

    private fun drawPrimitives(
        canvas: Canvas,
        primitives: List<dev.myengine.render.RenderPrimitive>,
        camera: Camera,
        tierLabel: String,
    ) {
        val halfTile = tilePixels(camera) / 2f
        primitives.forEach { primitive ->
            fillPaint.color = androidColor(RenderPalette.color(primitive.kind))
            canvas.drawRect(
                primitive.screen.x - halfTile,
                primitive.screen.y - halfTile,
                primitive.screen.x + halfTile,
                primitive.screen.y + halfTile,
                fillPaint,
            )
            if (primitive.kind == RenderKind.TOWER) {
                textPaint.color = androidColor(RenderPalette.coreHealthText)
                canvas.drawText(
                    "$tierLabel ${primitive.towerTier ?: 0}",
                    primitive.screen.x - halfTile / 2f,
                    primitive.screen.y,
                    textPaint,
                )
            }
        }
    }

    private fun drawOverlay(canvas: Canvas, frame: RenderFrame) {
        textPaint.color = androidColor(RenderPalette.coreHealthText)
        val hud = frame.hud
        val nextWave = hud.nextWaveInTicks?.let { "  ${hud.labels.nextWave} $it" }.orEmpty()
        canvas.drawText(
            "${hud.labels.wave} ${hud.wave}/${hud.totalWaves}$nextWave  ${hud.labels.coreHealth} ${hud.coreHealth}",
            dp(8f),
            dp(32f),
            textPaint,
        )
    }

    private fun drawHud(canvas: Canvas, frame: RenderFrame) {
        val hud = frame.hud
        textPaint.color = androidColor(RenderPalette.coreHealthText)
        val selected = uiState.selectedTowerEntityId?.let { id -> hud.towers.firstOrNull { it.entityId == id } }
        val layout = hudLayout(frame, selected?.availableUpgrades?.size ?: 0, selected != null)
        val currentSpeed = presentationSpeed()
        PresentationSpeed.entries.zip(layout.speedControls).forEach { (speed, bounds) ->
            if (speed == currentSpeed) {
                fillPaint.color = androidColor(RenderPalette.enemyPip)
                canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, fillPaint)
            }
            drawTextIn(canvas, speed.label, bounds)
        }
        drawTextIn(canvas, hud.labels.build, layout.buildHeader)
        hud.buildTowers.forEachIndexed { index, tower ->
            val bounds = layout.buildRows[index]
            val selected = tower.towerId == uiState.selectedTowerId
            if (selected) {
                fillPaint.color = androidColor(RenderPalette.enemyPip)
                canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, fillPaint)
            }
            drawTextIn(canvas, "${tower.label}  ${tower.cost.amount} ${tower.cost.label}", bounds)
        }

        val resourceText = hud.resources.joinToString("  ") { "${it.label} ${it.amount}" }
        canvas.drawText("${hud.labels.resources}: $resourceText", layout.padding, layout.resourcesBaseline, textPaint)

        if (selected == null) return
        drawTextIn(canvas, selected.label, requireNotNull(layout.selectedHeader))
        val infoText = listOf(
            "${hud.labels.tier} ${selected.tier}",
            "${hud.labels.damage} ${selected.damage}/${selected.actualDamage}",
            "${hud.labels.kills} ${selected.kills}",
        )
        infoText.zip(layout.selectedInfoRows).forEach { (text, bounds) -> drawTextIn(canvas, text, bounds) }
        selected.availableUpgrades.zip(layout.upgradeRows).forEach { (upgrade, bounds) ->
            drawTextIn(
                canvas,
                "${hud.labels.upgrade} ${upgrade.label}  ${upgrade.cost.amount} ${upgrade.cost.label}",
                bounds,
            )
        }
    }

    private fun drawTextIn(canvas: Canvas, text: String, bounds: HudBounds) {
        val baseline = bounds.top + (bounds.bottom - bounds.top + textPaint.textSize * 0.7f) / 2f
        canvas.drawText(text, bounds.left + dp(4f), baseline, textPaint)
    }

    private fun tilePixels(camera: Camera): Float =
        camera.worldToScreen(WorldPoint(1f, 0f)).x - camera.worldToScreen(WorldPoint(0f, 0f)).x

    private fun dp(value: Float): Float = value * density

    private fun androidColor(color: dev.myengine.render.Rgb): Int =
        0xff000000.toInt() or color.toRgbInt()

    private fun RenderKind.isTile(): Boolean = this in setOf(
        RenderKind.TILE_FLOOR,
        RenderKind.TILE_WALL,
        RenderKind.TILE_RESOURCE,
        RenderKind.CORE,
    )

    private companion object {
        const val STROKE_WIDTH_DP = 2f
        const val TEXT_SIZE_SP = 18f
    }
}
