package dev.myengine.games.sandbox

import dev.myengine.core.CommandId
import dev.myengine.core.Tick
import dev.myengine.render.BuildTowerCommand
import dev.myengine.world.TilePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end coverage for SG-002 (reward deposit hook): a tower kill must deposit the
 * enemy's content-derived reward into the sandbox inventory.
 *
 * All expectations are read from content (registry enemy definitions), never hardcoded,
 * so the test proves resource conservation rather than a magic number.
 *
 * The sandbox also runs a bolt producer, so we cannot attribute an absolute balance to the
 * reward hook. Instead we run two otherwise-identical, deterministic scenarios that differ
 * ONLY in whether the tower can reach enemies; the production output is common to both, so
 * the balance delta between them isolates exactly the kill rewards.
 */
class SandboxRewardDepositTest {
    @Test
    fun towerKillsDepositContentDerivedRewardIntoInventory() {
        val registry = SandboxGame.loadRegistry()

        // The sandbox waves spawn a single enemy type; read its reward id/amount from content.
        val enemyId = registry.waves.values
            .flatMap { it.spawns }
            .map { it.enemyId }
            .distinct()
            .single()
        val enemy = registry.enemies[enemyId]!!
        val rewardResource = enemy.rewardResource
        val rewardAmount = enemy.rewardAmount

        // Scenario A: tower near the enemy spawn. Enemies spawn at (1,1), which is Manhattan
        // distance 2 <= pulse range 5 from (2,2), so wave-1 (tick 10) enemies come into range
        // within a couple of ticks -> at least one kill within step(35). (2,2) is buildable
        // floor (walls are only the x=0/x=63/y=0/y=63 border; core is (32,32); resource (5,5)).
        val killing = SandboxGame.createRuntime(registry)
        killing.submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", TilePosition(2, 2)))
        killing.step(35)

        // Scenario B: tower far from the (1,1)->(32,32) enemy corridor. Every enemy-occupied
        // tile lies within the ~[1..32]x[1..32] box, so distance from (40,40) is >= 16 > range
        // 5 -> guaranteed 0 kills. Same build spend, same deterministic production, so the
        // balance delta isolates exactly the kill rewards. (40,40) is buildable floor.
        val idle = SandboxGame.createRuntime(registry)
        idle.submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", TilePosition(40, 40)))
        idle.step(35)

        val enemiesKilled = killing.state.defense.metrics.enemiesKilled
        val idleKills = idle.state.defense.metrics.enemiesKilled

        // Guard: the killing scenario must actually exercise the hook, and the control must not.
        assertTrue(enemiesKilled > 0, "killing scenario produced no kills; nothing to deposit")
        assertEquals(0, idleKills, "control scenario unexpectedly killed enemies")

        val killingBalance = killing.state.inventory.amount(rewardResource)
        val idleBalance = idle.state.inventory.amount(rewardResource)

        // Conservation: the only difference between the two runs is the kills, so the balance
        // delta equals exactly enemiesKilled * rewardAmount (content-derived).
        assertEquals(
            enemiesKilled * rewardAmount,
            killingBalance - idleBalance,
            "reward deposit delta must equal content-derived kill rewards",
        )

        // And the kill scenario strictly increased the reward balance relative to the control.
        assertTrue(killingBalance > idleBalance, "reward deposit did not increase balance")
    }
}
