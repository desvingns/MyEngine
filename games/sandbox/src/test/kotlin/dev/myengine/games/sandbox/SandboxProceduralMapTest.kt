package dev.myengine.games.sandbox

import java.io.StringReader
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SandboxProceduralMapTest {
    @Test
    fun sameSeedProducesSameGeneratedRuntimeHash() {
        val first = SandboxGame.createProceduralRuntime(seed = 41L)
        val second = SandboxGame.createProceduralRuntime(seed = 41L)

        assertEquals(first.state.mapId, second.state.mapId)
        assertEquals(first.state.stableHash(), second.state.stableHash())
        assertEquals(first.state.world.toSaveLines(), second.state.world.toSaveLines())
    }

    @Test
    fun proceduralSessionSaveEmbedsMapSeedAndRestoresGeneratedMap() {
        val session = SandboxSession.startProcedural(seed = 53L, wallDensityPercent = 80, maxAttempts = 3)
        val save = session.save()
        val properties = Properties().also { it.load(StringReader(save)) }
        val generatedRegistry = SandboxGame.loadRegistry().copy(
            maps = SandboxGame.loadRegistry().maps +
                (session.runtime.state.mapId to SandboxGame.generateProceduralMap(seed = 53L, wallDensityPercent = 80, maxAttempts = 3).map),
        )

        assertEquals("53", properties.getProperty("seed"))
        assertEquals(session.runtime.state.mapId, properties.getProperty("mapId"))

        val restored = SandboxSession.restore(save, generatedRegistry)
        assertEquals(session.stableHash(), restored.stableHash())
        assertEquals(session.runtime.state.mapId, restored.runtime.state.mapId)
    }

    @Test
    fun generatedMapCanAdvanceWithoutBreakingWorldOrCoreIdentity() {
        val runtime = SandboxGame.createProceduralRuntime(seed = 67L)
        val map = runtime.state.registry.requireMap(runtime.state.mapId)

        runtime.step(2)

        assertTrue(runtime.state.world.corePositions().isNotEmpty())
        assertEquals(map.core.x to map.core.y, runtime.state.world.corePositions().single().x to runtime.state.world.corePositions().single().y)
    }
}
