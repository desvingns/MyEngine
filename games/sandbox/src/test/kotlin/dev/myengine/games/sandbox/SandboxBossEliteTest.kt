package dev.myengine.games.sandbox

import dev.myengine.entities.EntityId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SandboxBossEliteTest {
    @Test
    fun bossSnapshotAndSaveRoundtripPreserveEffectiveSpawnState() {
        val base = SandboxGame.loadRegistry()
        val boss = base.requireEnemy("drift").copy(
            isBoss = true,
            healthScalePercent = 150,
            rewardScalePercent = 150,
        )
        val registry = base.copy(enemies = mapOf(boss.id to boss))
        val runtime = SandboxGame.createRuntime(registry, seed = 7L)

        runtime.step(10)

        val snapshotBoss = runtime.snapshot().entities.first { it.type == "enemy:drift" }
        assertTrue(snapshotBoss.isBoss)
        val entity = runtime.state.entities.require(EntityId(snapshotBoss.id))
        assertTrue(entity.enemy!!.isBoss)
        assertTrue(entity.health!!.max > boss.health)

        val save = SandboxSaveCodec.encode(runtime.state, seed = 7L)
        val restored = SandboxRuntime(SandboxSaveCodec.decode(save, registry), seed = 7L)
        assertEquals(runtime.state.stableHash(), restored.state.stableHash())

        runtime.step(3)
        restored.step(3)
        assertEquals(runtime.state.stableHash(), restored.state.stableHash())
    }

    @Test
    fun bossReplayIsDeterministic() {
        val base = SandboxGame.loadRegistry()
        val registry = base.copy(
            enemies = mapOf(base.requireEnemy("drift").id to base.requireEnemy("drift").copy(isBoss = true)),
        )

        fun run(): String = SandboxGame.createRuntime(registry, seed = 19L).also { it.step(18) }.state.stableHash()

        assertEquals(run(), run())
    }
}
