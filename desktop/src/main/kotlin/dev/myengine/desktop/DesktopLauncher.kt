package dev.myengine.desktop

import dev.myengine.games.sandbox.SandboxGame
import dev.myengine.render.AsciiRenderer

fun main() {
    println(SandboxGame.banner())
    val result = SandboxGame.runScriptedScenario()
    println("hash=${result.hash}")
    println(AsciiRenderer().render(result.snapshot))
}
