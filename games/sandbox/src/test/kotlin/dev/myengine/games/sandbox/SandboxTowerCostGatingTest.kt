package dev.myengine.games.sandbox

import dev.myengine.core.CommandId
import dev.myengine.core.Tick
import dev.myengine.logistics.Inventory
import dev.myengine.render.BuildTowerCommand
import dev.myengine.world.TilePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Acceptance coverage for MTD-002: tower placement cost is content-driven, affordability-gated,
 * deterministic, and never lets the player resource balance go negative.
 */
class SandboxTowerCostGatingTest {
    @Test
    fun affordablePlacementSpendsContentDefinedTowerCost() {
        val registry = SandboxGame.loadRegistry()
        val tower = registry.requireTower("pulse")
        val runtime = SandboxGame.createRuntime(registry)
        val startingBalance = runtime.state.inventory.amount(tower.costResource)

        runtime.submit(BuildTowerCommand(CommandId(1), Tick(1), tower.id, TilePosition(2, 2)))
        runtime.step(1)

        assertTrue(startingBalance >= tower.costAmount, "test setup must afford the tower")
        assertEquals(startingBalance - tower.costAmount, runtime.state.inventory.amount(tower.costResource))
        assertTrue(runtime.state.lastCommandOrError?.startsWith("placed:") == true)
    }

    @Test
    fun unaffordablePlacementIsRejectedWithoutChangingBalance() {
        val registry = SandboxGame.loadRegistry()
        val tower = registry.requireTower("pulse")
        val runtime = SandboxGame.createRuntime(registry)
        val unaffordableBalance = tower.costAmount - 1
        runtime.state.inventory = Inventory(mapOf(tower.costResource to unaffordableBalance))

        runtime.submit(BuildTowerCommand(CommandId(1), Tick(1), tower.id, TilePosition(2, 2)))
        runtime.step(1)

        assertTrue(tower.costAmount > 0, "test setup requires a positive tower cost")
        assertEquals(unaffordableBalance, runtime.state.inventory.amount(tower.costResource))
        assertTrue(runtime.state.inventory.amount(tower.costResource) >= 0)
        assertEquals("missing_resource:${tower.costResource}", runtime.state.lastCommandOrError)
        assertEquals(0, runtime.state.entities.all().count { it.tower != null })
    }

    @Test
    fun rejectedPlacementDoesNotSpendAffordableBalance() {
        val registry = SandboxGame.loadRegistry()
        val tower = registry.requireTower("pulse")
        val runtime = SandboxGame.createRuntime(registry)
        val startingBalance = runtime.state.inventory.amount(tower.costResource)

        runtime.submit(BuildTowerCommand(CommandId(1), Tick(1), tower.id, TilePosition(0, 0)))
        runtime.step(1)

        assertEquals(startingBalance, runtime.state.inventory.amount(tower.costResource))
        assertEquals("tile_not_buildable", runtime.state.lastCommandOrError)
        assertEquals(0, runtime.state.entities.all().count { it.tower != null })
    }

    @Test
    fun unaffordablePlacementReplayHashIsStable() {
        val registry = SandboxGame.loadRegistry()
        val tower = registry.requireTower("pulse")

        fun run(): String {
            val runtime = SandboxGame.createRuntime(registry)
            runtime.state.inventory = Inventory(mapOf(tower.costResource to tower.costAmount - 1))
            runtime.submit(BuildTowerCommand(CommandId(1), Tick(1), tower.id, TilePosition(2, 2)))
            runtime.step(3)
            return runtime.state.stableHash()
        }

        assertEquals(run(), run())
    }
}
