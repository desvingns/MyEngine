package dev.myengine.android

import android.app.Activity
import android.os.Bundle
import android.view.Choreographer
import android.widget.TextView
import dev.myengine.core.CommandId
import dev.myengine.core.EngineCommand
import dev.myengine.games.sandbox.SandboxGame
import dev.myengine.games.sandbox.SandboxSession
import dev.myengine.render.EngineSnapshot

class MyEngineActivity : Activity() {
    private var session: SandboxSession? = null
    private var renderView: SandboxRenderView? = null
    private var latestSnapshot: EngineSnapshot? = null
    private var pausedSave: String? = null
    private var nextCommandId: Long = FIRST_COMMAND_ID
    private val fixedTickLoop = FixedTickFrameLoop()
    private var presentationSpeed = PresentationSpeed.ONE_X
    private var loopRunning = false
    private val frameCallback = Choreographer.FrameCallback(::onFrame)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nextCommandId = savedInstanceState?.getLong(NEXT_COMMAND_ID_KEY, FIRST_COMMAND_ID) ?: FIRST_COMMAND_ID
        presentationSpeed = PresentationSpeed.fromMultiplier(
            savedInstanceState?.getInt(SPEED_KEY, PresentationSpeed.ONE_X.multiplier)
                ?: PresentationSpeed.ONE_X.multiplier,
        )
        fixedTickLoop.presentationSpeed = presentationSpeed
        val started = runCatching {
            val saved = savedInstanceState?.getString(SAVE_KEY)
            if (saved != null) SandboxSession.restore(saved) else SandboxSession.start()
        }
        started.onSuccess {
            session = it
            latestSnapshot = it.runtime.snapshot()
            val view = SandboxRenderView(
                context = this,
                latestSnapshot = { latestSnapshot },
                commandIdProvider = ::issueCommandId,
                onCommand = ::submitFromInput,
                presentationSpeed = { presentationSpeed },
                onPresentationSpeedChange = ::setPresentationSpeed,
            )
            renderView = view
            setContentView(view)
        }.onFailure {
            setContentView(
                TextView(this).apply {
                    text = "${SandboxGame.banner()}\nStartup error: ${it.message}"
                    textSize = 18f
                },
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(NEXT_COMMAND_ID_KEY, nextCommandId)
        outState.putInt(SPEED_KEY, presentationSpeed.multiplier)
        (pausedSave ?: session?.save())?.let { outState.putString(SAVE_KEY, it) }
    }

    override fun onResume() {
        super.onResume()
        startLoop()
    }

    override fun onPause() {
        stopLoop()
        // Save after all frame callbacks are removed: the state and pending command queue now
        // describe exactly the next frame that a recreated activity restores.
        pausedSave = session?.save()
        super.onPause()
    }

    /** Input may enqueue commands only; the frame loop drains them on the next fixed tick. */
    private fun submitFromInput(command: EngineCommand) {
        session?.submit(command)
    }

    private fun setPresentationSpeed(speed: PresentationSpeed) {
        presentationSpeed = speed
        fixedTickLoop.presentationSpeed = speed
        renderView?.renderLatestFrame()
    }

    private fun startLoop() {
        if (loopRunning || session == null) return
        loopRunning = true
        fixedTickLoop.start()
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun stopLoop() {
        loopRunning = false
        fixedTickLoop.stop()
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    private fun onFrame(frameTimeNanos: Long) {
        if (!loopRunning) return
        val activeSession = session
        if (activeSession != null) {
            val ticks = fixedTickLoop.advance(frameTimeNanos)
            if (ticks > 0) activeSession.step(ticks)
            latestSnapshot = activeSession.runtime.snapshot()
            renderView?.renderLatestFrame()
        }
        if (loopRunning) Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    /** Caller-owned command allocator: it stays in lifecycle UI state, outside render and simulation. */
    private fun issueCommandId(): CommandId = CommandId(nextCommandId++)

    private companion object {
        private const val SAVE_KEY = "me_sandbox_save"
        private const val NEXT_COMMAND_ID_KEY = "me_sandbox_next_command_id"
        private const val SPEED_KEY = "me_sandbox_presentation_speed"
        private const val FIRST_COMMAND_ID = 1L
    }
}
