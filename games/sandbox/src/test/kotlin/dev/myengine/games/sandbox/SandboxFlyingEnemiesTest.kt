package dev.myengine.games.sandbox

import dev.myengine.content.MapContent
import dev.myengine.content.MapCoordinate
import dev.myengine.content.MapSpawn
import dev.myengine.content.MapTerrainSymbol
import dev.myengine.core.MovementMode
import dev.myengine.core.Tick
import dev.myengine.world.TilePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SandboxFlyingEnemiesTest {
    @Test
    fun airEnemyLeaksThroughBlockedGroundRouteAndSaveRoundTripsMovementMode() {
        val registry = flyingRegistry()
        val first = SandboxGame.createInitialState(registry = registry, mapId = "air-test")
        val runtime = SandboxRuntime(first)

        runtime.step(1)
        assertEquals(TilePosition(1, 0), runtime.state.entities.byTag("enemy").single().position!!.tile)
        assertEquals(MovementMode.AIR, runtime.state.entities.byTag("enemy").single().enemy!!.movementMode)

        val saved = SandboxSaveCodec.encode(runtime.state, seed = 7L)
        val restored = SandboxSaveCodec.decode(saved, registry)
        assertEquals(runtime.state.stableHash(), restored.stableHash())
        assertEquals(MovementMode.AIR, restored.entities.byTag("enemy").single().enemy!!.movementMode)

        val restoredRuntime = SandboxRuntime(restored)
        restoredRuntime.step(2)
        assertTrue(restoredRuntime.state.entities.byTag("enemy").isEmpty(), "air enemy must reach the core through the blocker")
        assertEquals(18, restoredRuntime.state.defense.coreHealth)
    }

    @Test
    fun mixedGroundAndAirWaveHasStableReplayAndDifferentRoutes() {
        val registry = flyingRegistry(mixed = true)

        fun run(): SandboxRuntime {
            val runtime = SandboxRuntime(SandboxGame.createInitialState(registry = registry, mapId = "air-test"))
            runtime.step(3)
            return runtime
        }

        val first = run()
        val second = run()
        assertEquals(first.state.stableHash(), second.state.stableHash())
        assertEquals(TilePosition(0, 0), first.state.entities.byTag("enemy").single { it.type == "enemy:ground" }.position!!.tile)
        assertEquals(1, first.state.defense.metrics.enemiesLeaked)
        assertNotEquals(first.state.stableHash(), SandboxGame.createInitialState(registry = registry, mapId = "air-test").stableHash())
    }

    private fun flyingRegistry(mixed: Boolean = false) = SandboxGame.loadRegistry().let { base ->
        val map = MapContent(
            id = "air-test",
            width = 3,
            height = 1,
            terrainRows = listOf("fwc"),
            terrainMapping = mapOf(
                'f' to MapTerrainSymbol("floor"),
                'w' to MapTerrainSymbol("wall"),
                'c' to MapTerrainSymbol("core"),
            ),
            spawns = mapOf("entry" to MapSpawn("entry", MapCoordinate(0, 0))),
            core = MapCoordinate(2, 0),
        )
        val air = base.requireEnemy("drift").copy(id = "air", movementMode = MovementMode.AIR)
        val ground = base.requireEnemy("drift").copy(id = "ground")
        base.copy(
            enemies = mapOf(air.id to air, ground.id to ground),
            waves = mapOf(
                "air-wave" to dev.myengine.content.WaveContent(
                    id = "air-wave",
                    startTick = 1,
                    spawns = if (mixed) {
                        listOf(dev.myengine.content.WaveSpawn(air.id, 1), dev.myengine.content.WaveSpawn(ground.id, 1))
                    } else {
                        listOf(dev.myengine.content.WaveSpawn(air.id, 1))
                    },
                ),
            ),
            maps = mapOf(map.id to map),
        )
    }
}
