package dev.myengine.games.sandbox

import dev.myengine.content.ResourceContent
import dev.myengine.content.TowerUpgradeTier
import dev.myengine.core.CommandId
import dev.myengine.core.Tick
import dev.myengine.core.command.BuildTowerCommand
import dev.myengine.core.command.SellTowerCommand
import dev.myengine.core.command.TileCoordinate
import dev.myengine.core.command.UpgradeTowerCommand
import dev.myengine.defense.TowerDefenseMetrics
import dev.myengine.logistics.Inventory
import dev.myengine.world.TilePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SandboxTowerSellTest {
    @Test
    fun sellingBaseTowerRoundsRefundDownClearsMetricsAndPermitsReuse() {
        val runtime = SandboxGame.createRuntime()
        runtime.state.producers = emptyList()
        runtime.state.inventory = Inventory(mapOf("bolt" to 10))
        val position = TilePosition(2, 2)

        runtime.submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", TileCoordinate(position.x, position.y)))
        runtime.step()
        val tower = runtime.state.entities.byTag("tower").single()
        runtime.state.defense = runtime.state.defense.copy(
            towerMetrics = mapOf(tower.id.value to TowerDefenseMetrics(actualDamage = 7, kills = 1)),
        )

        runtime.submit(SellTowerCommand(CommandId(2), Tick(2), tower.id.value))
        runtime.step()

        assertEquals("sold:${tower.id.value}", runtime.state.lastCommandOrError)
        assertEquals(8, runtime.state.inventory.amount("bolt"), "3 * 0.5 must round down to one refund")
        assertEquals(0, runtime.state.entities.byTag("tower").size)
        assertFalse(runtime.state.defense.towerMetrics.containsKey(tower.id.value))
        assertTrue(runtime.state.world.canBuild(position), "selling must free the occupied tile")

        runtime.submit(BuildTowerCommand(CommandId(3), Tick(3), "pulse", TileCoordinate(position.x, position.y)))
        runtime.step()
        assertEquals("placed:2", runtime.state.lastCommandOrError)
        assertEquals(2L, runtime.state.entities.byTag("tower").single().id.value)
    }

    @Test
    fun sellingUpgradedTowerRefundsEachSpentResourceWithIndependentRounding() {
        val base = SandboxGame.loadRegistry()
        val tier = base.requireTower("pulse").upgradeTiers.getValue(TowerUpgradeTier.key("main", 1))
        val registry = base.copy(
            resources = base.resources + ("crystal" to ResourceContent("crystal", "resource.crystal")),
            towers = base.towers + (
                "pulse" to base.requireTower("pulse").copy(
                    upgradeTiers = mapOf(tier.key to tier.copy(costResource = "crystal", costAmount = 3)),
                )
                ),
        )
        val runtime = SandboxGame.createRuntime(registry)
        runtime.state.producers = emptyList()
        runtime.state.inventory = Inventory(mapOf("bolt" to 10, "crystal" to 10))

        runtime.submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", TileCoordinate(2, 2)))
        runtime.step()
        val towerId = runtime.state.entities.byTag("tower").single().id.value
        runtime.submit(UpgradeTowerCommand(CommandId(2), Tick(2), towerId, tier.branch, tier.tier))
        runtime.step()
        runtime.submit(SellTowerCommand(CommandId(3), Tick(3), towerId))
        runtime.step()

        assertEquals("sold:$towerId", runtime.state.lastCommandOrError)
        assertEquals(8, runtime.state.inventory.amount("bolt"))
        assertEquals(8, runtime.state.inventory.amount("crystal"))
    }

    @Test
    fun capacityRejectedSellLeavesTowerOccupancyMetricsAndInventoryUntouched() {
        val runtime = SandboxGame.createRuntime()
        runtime.state.producers = emptyList()
        val position = TilePosition(2, 2)
        runtime.submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", TileCoordinate(position.x, position.y)))
        runtime.step()
        val tower = runtime.state.entities.byTag("tower").single()
        runtime.state.defense = runtime.state.defense.copy(
            towerMetrics = mapOf(tower.id.value to TowerDefenseMetrics(actualDamage = 3, kills = 1)),
        )
        runtime.state.inventory = Inventory(mapOf("bolt" to 1), capacity = 1)
        val inventoryBefore = runtime.state.inventory

        runtime.submit(SellTowerCommand(CommandId(2), Tick(2), tower.id.value))
        runtime.step()

        assertEquals("refund_capacity:bolt", runtime.state.lastCommandOrError)
        assertEquals(inventoryBefore, runtime.state.inventory)
        assertNotNull(runtime.state.entities.get(tower.id))
        assertFalse(runtime.state.world.canBuild(position))
        assertEquals(TowerDefenseMetrics(actualDamage = 3, kills = 1), runtime.state.defense.towerMetrics[tower.id.value])
    }

    @Test
    fun sellingRestoresGoalFieldBeforeSameTickEnemyMovementAndIsReplayStable() {
        fun run(): Pair<TilePosition, String> {
            val runtime = SandboxGame.createRuntime()
            runtime.step(11)
            runtime.submit(BuildTowerCommand(CommandId(1), Tick(12), "pulse", TileCoordinate(4, 1)))
            runtime.step()
            val towerId = runtime.state.entities.byTag("tower").single().id.value
            runtime.submit(SellTowerCommand(CommandId(2), Tick(13), towerId))
            runtime.step()
            return runtime.state.entities.byTag("enemy").map { it.position!!.tile }.distinct().single() to runtime.state.stableHash()
        }

        val first = run()
        assertEquals(first, run())
        assertEquals(TilePosition(4, 2), first.first, "same-tick sell must use the rebuilt unblocked field")
    }

    @Test
    fun pendingSellSurvivesLifecycleSaveWithOriginalIdTickAndActor() {
        val session = SandboxSession.start()
        val sell = SellTowerCommand(
            id = CommandId(47),
            scheduledTick = Tick(25),
            towerEntityId = 123L,
            actorId = 909L,
        )
        session.submit(sell)
        session.step(10)

        val restored = SandboxSession.restore(session.save())

        assertEquals(listOf(sell), restored.runtime.pendingCommands())
    }
}
