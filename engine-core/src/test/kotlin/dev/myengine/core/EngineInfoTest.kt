package dev.myengine.core

import kotlin.test.Test
import kotlin.test.assertEquals

class EngineInfoTest {
    @Test
    fun bannerNamesTheScaffold() {
        assertEquals("MyEngine scaffold phase 14", EngineInfo.banner())
    }
}
