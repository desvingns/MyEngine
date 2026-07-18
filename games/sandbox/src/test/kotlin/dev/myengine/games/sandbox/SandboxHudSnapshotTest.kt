package dev.myengine.games.sandbox

import dev.myengine.core.CommandId
import dev.myengine.core.Tick
import dev.myengine.core.command.BuildTowerCommand
import dev.myengine.core.command.TileCoordinate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SandboxHudSnapshotTest {
    @Test
    fun fixedTickHeadlessHudIsContentDerivedOrderedAndDeterministic() {
        fun run() = SandboxGame.createRuntime().also { runtime ->
            val initial = runtime.snapshot().hud
            assertEquals(listOf("bolt"), initial.resources.map { it.resourceId })
            assertEquals(6, initial.resources.single().amount)
            assertEquals("Bolt", initial.resources.single().label)
            assertEquals(0, initial.wave)
            assertEquals(2, initial.totalWaves)
            assertEquals(10L, initial.nextWaveInTicks)
            assertEquals(20, initial.coreHealth)
            assertEquals(listOf("pulse"), initial.buildTowers.map { it.towerId })
            assertEquals("Pulse tower", initial.buildTowers.single().label)
            assertEquals("Focused pulse", initial.buildTowers.single().tiers.single().label)
            assertTrue(initial.towers.isEmpty())

            runtime.submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", TileCoordinate(2, 2)))
            runtime.step(13)
        }.snapshot().hud

        val first = run()
        val second = run()
        assertEquals(first, second)
        assertEquals(1, first.wave)
        assertEquals(12L, first.nextWaveInTicks)
        assertEquals(first.resources.sortedBy { it.resourceId }, first.resources)
        assertEquals(first.buildTowers.sortedBy { it.towerId }, first.buildTowers)
        assertEquals(first.towers.sortedBy { it.entityId }, first.towers)
        val tower = first.towers.single()
        assertEquals(4L, tower.actualDamage)
        assertEquals(1, tower.kills)
        assertEquals(listOf(1), tower.availableUpgrades.map { it.tier })
    }
}
