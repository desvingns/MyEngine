package dev.myengine.world

object WorldModuleContract {
    const val MODULE_NAME: String = "engine-world"

    fun responsibility(): String = "Authoritative tile/world state with no render or Android dependency."
}
