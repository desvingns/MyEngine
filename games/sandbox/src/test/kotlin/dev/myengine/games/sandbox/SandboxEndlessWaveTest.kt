package dev.myengine.games.sandbox

import dev.myengine.content.EndlessWaveComposition
import dev.myengine.content.EndlessWaveContent
import dev.myengine.content.MapTerminalRules
import dev.myengine.content.MapWinCondition
import dev.myengine.content.ContentRegistry
import dev.myengine.content.WaveSpawn
import dev.myengine.core.RunStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SandboxEndlessWaveTest {
    @Test
    fun endlessScheduleUsesSavedRngCursorAndNeverWins() {
        val registry = endlessRegistry()
        val uninterrupted = SandboxGame.createRuntime(registry, seed = 31L)
        uninterrupted.step(2)

        assertEquals(RunStatus.ACTIVE, uninterrupted.state.run.status)
        assertEquals(setOf("endless-wave-1", "endless-wave-2"), uninterrupted.state.defense.spawnedWaveIds)
        assertEquals(2, uninterrupted.state.defense.metrics.enemiesSpawned)

        val paused = SandboxGame.createRuntime(registry, seed = 31L)
        paused.step(1)
        val save = SandboxSaveCodec.encode(paused.state, seed = 31L)
        val restored = SandboxRuntime(SandboxSaveCodec.decode(save, registry), seed = 31L)
        restored.step(1)

        assertEquals(uninterrupted.state.stableHash(), restored.state.stableHash())
        assertEquals(uninterrupted.state.randomCursor, restored.state.randomCursor)
        assertTrue(save.contains("randomCursor="))
        assertEquals(19, SandboxSaveCodec.SAVE_VERSION)
    }

    private fun endlessRegistry(): ContentRegistry {
        val base = SandboxGame.loadRegistry()
        val map = base.requireMap().copy(terminalRules = MapTerminalRules(MapWinCondition.NO_WIN))
        val drift = base.requireEnemy("drift").copy(speedTilesPerTick = 0)
        return base.copy(
            enemies = base.enemies + (drift.id to drift),
            maps = mapOf(map.id to map),
            waves = emptyMap(),
            incidents = emptyMap(),
            endlessWave = EndlessWaveContent(
                startTick = 1,
                intervalTicks = 1,
                compositionCycle = listOf(EndlessWaveComposition(listOf(WaveSpawn("drift", 1)))),
                countGrowthPercent = 100,
                healthGrowthPercent = 100,
                rewardGrowthPercent = 100,
            ),
        )
    }
}
