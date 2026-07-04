package dev.myengine.android

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import dev.myengine.games.sandbox.SandboxGame

class MyEngineActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scenario = runCatching { SandboxGame.runScriptedScenario() }
        setContentView(
            TextView(this).apply {
                text = scenario.fold(
                    onSuccess = { "${SandboxGame.banner()}\nTick ${it.snapshot.debug.tick.value}\nHash ${it.hash}" },
                    onFailure = { "${SandboxGame.banner()}\nStartup error: ${it.message}" },
                )
                textSize = 18f
            },
        )
    }
}
