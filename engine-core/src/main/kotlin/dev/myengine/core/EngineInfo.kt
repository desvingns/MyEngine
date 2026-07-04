package dev.myengine.core

object EngineInfo {
    const val NAME: String = "MyEngine"
    const val SCAFFOLD_PHASE: Int = 14

    fun banner(): String = "$NAME scaffold phase $SCAFFOLD_PHASE"
}
