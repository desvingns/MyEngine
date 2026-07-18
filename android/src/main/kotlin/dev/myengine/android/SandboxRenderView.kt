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
) : SurfaceView(context), SurfaceHolder.Callback {
    private val renderer = PlaceholderRenderSurface()
    private val inputAdapter = InputAdapter()
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
    }

    private var inputState: InputState? = null
    private val uiState = InputUiState(selectedTowerId = DEFAULT_TOWER_ID)
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
                    dispatchInput(PlatformInputEvent.Tap(ScreenPoint(event.x, event.y)))
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
        val commandId = if (event is PlatformInputEvent.Tap) commandIdProvider() else null
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

    private fun drawFrame(canvas: Canvas, frame: RenderFrame, camera: Camera) {
        canvas.drawColor(androidColor(RenderPalette.background))
        drawTiles(canvas, frame, camera)
        drawPath(canvas, frame)
        drawEntities(canvas, frame, camera)
        drawOverlay(canvas, frame)
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
        drawPrimitives(canvas, frame.primitives.filter { it.kind.isTile() }, camera)
    }

    private fun drawEntities(canvas: Canvas, frame: RenderFrame, camera: Camera) {
        drawPrimitives(canvas, frame.primitives.filterNot { it.kind.isTile() }, camera)
    }

    private fun drawPrimitives(
        canvas: Canvas,
        primitives: List<dev.myengine.render.RenderPrimitive>,
        camera: Camera,
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
                    "T${primitive.towerTier ?: 0}",
                    primitive.screen.x - halfTile / 2f,
                    primitive.screen.y,
                    textPaint,
                )
            }
        }
    }

    private fun drawOverlay(canvas: Canvas, frame: RenderFrame) {
        textPaint.color = androidColor(RenderPalette.coreHealthText)
        canvas.drawText("tick ${frame.tick.value}  core ${frame.coreHealth}", 16f, 36f, textPaint)
    }

    private fun tilePixels(camera: Camera): Float =
        camera.worldToScreen(WorldPoint(1f, 0f)).x - camera.worldToScreen(WorldPoint(0f, 0f)).x

    private fun androidColor(color: dev.myengine.render.Rgb): Int =
        0xff000000.toInt() or color.toRgbInt()

    private fun RenderKind.isTile(): Boolean = this in setOf(
        RenderKind.TILE_FLOOR,
        RenderKind.TILE_WALL,
        RenderKind.TILE_RESOURCE,
        RenderKind.CORE,
    )

    private companion object {
        const val DEFAULT_TOWER_ID = "pulse"
    }
}
