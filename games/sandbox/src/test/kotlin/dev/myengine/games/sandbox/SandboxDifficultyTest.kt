package dev.myengine.games.sandbox

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SandboxDifficultyTest {
    @Test
    fun setupMaterializesDifficultyBeforeTheFirstTick() {
        val easy = SandboxGame.loadRegistry(difficultyId = "easy")
        val normal = SandboxGame.loadRegistry(difficultyId = "normal")
        val hard = SandboxGame.loadRegistry(difficultyId = "hard")

        assertEquals("easy", easy.resolvedDifficultyId)
        assertEquals("normal", normal.resolvedDifficultyId)
        assertEquals("hard", hard.resolvedDifficultyId)
        assertTrue(easy.enemies.getValue("drift").health < normal.enemies.getValue("drift").health)
        assertTrue(normal.enemies.getValue("drift").health < hard.enemies.getValue("drift").health)
        assertTrue(easy.waves.getValue("wave-1").spawns.single().count < hard.waves.getValue("wave-1").spawns.single().count)
        assertEquals(0L, SandboxGame.createRuntime(difficultyId = "hard").state.tick.value)
    }

    @Test
    fun sameSeedAndDifficultyProducesTheSameFinalHash() {
        val first = SandboxGame.runScriptedKillScenario(seed = 7, difficultyId = "hard")
        val second = SandboxGame.runScriptedKillScenario(seed = 7, difficultyId = "hard")
        val easy = SandboxGame.runScriptedKillScenario(seed = 7, difficultyId = "easy")

        assertEquals(first.hash, second.hash)
        assertNotEquals(first.hash, easy.hash)
    }
}
