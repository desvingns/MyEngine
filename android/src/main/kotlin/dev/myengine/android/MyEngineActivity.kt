package dev.myengine.android

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import dev.myengine.games.sandbox.SandboxGame
import dev.myengine.games.sandbox.SandboxSession

class MyEngineActivity : Activity() {
    private var session: SandboxSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val started = runCatching {
            val saved = savedInstanceState?.takeIf { DEBUG_SAVE }?.getString(SAVE_KEY)
            if (saved != null) SandboxSession.restore(saved) else SandboxSession.start()
        }
        started.onSuccess { session = it }
        setContentView(
            TextView(this).apply {
                text = started.fold(
                    onSuccess = {
                        val snapshot = it.runtime.snapshot()
                        "${SandboxGame.banner()}\nTick ${snapshot.debug.tick.value}\nHash ${it.stableHash()}"
                    },
                    onFailure = { "${SandboxGame.banner()}\nStartup error: ${it.message}" },
                )
                textSize = 18f
            },
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (DEBUG_SAVE) {
            // Debug lifecycle save trigger. Sound at any tick — pending commands round-trip
            // through the save; see SandboxSession's KDoc.
            session?.let { outState.putString(SAVE_KEY, it.save()) }
        }
    }

    private companion object {
        // Debug-only lifecycle save/restore, gated to debug builds so it cannot ship enabled
        // in a release build.
        private val DEBUG_SAVE = BuildConfig.DEBUG
        private const val SAVE_KEY = "me_sandbox_save"
    }
}
