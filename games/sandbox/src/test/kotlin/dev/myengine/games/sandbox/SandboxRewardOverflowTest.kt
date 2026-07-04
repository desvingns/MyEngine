package dev.myengine.games.sandbox

import dev.myengine.defense.DefenseRuntime
import dev.myengine.logistics.Inventory
import dev.myengine.world.TilePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Coverage for the SG-002 verifier note: when a kill reward cannot fit (`inventory.canAdd == false`,
 * only reachable once a capacity is set), the deposit does NOT silently vanish — the reward is left
 * out of the (capacity-bound) inventory AND surfaced as telemetry via `lastCommandOrError`.
 *
 * Decision recorded here: a full inventory is a legitimate game state, so the drop is non-fatal
 * (no exception), but it must be observable rather than silent.
 */
class SandboxRewardOverflowTest {
    @Test
    fun depositRewardsAddsWhenUncapped() {
        val result = depositRewards(Inventory(mapOf("bolt" to 1)), mapOf("spark" to 3, "bolt" to 2))

        assertEquals(mapOf("bolt" to 3, "spark" to 3), result.inventory.resources)
        assertTrue(result.dropped.isEmpty())
    }

    @Test
    fun depositRewardsDropsAndReportsWhenAtCapacity() {
        // Inventory already at capacity: no reward can fit, so all are reported as dropped and the
        // inventory is returned unchanged (never force-added past capacity).
        val full = Inventory(mapOf("bolt" to 4), capacity = 4)

        val result = depositRewards(full, mapOf("spark" to 2))

        assertEquals(full, result.inventory)
        assertEquals(mapOf("spark" to 2), result.dropped)
    }

    @Test
    fun stepSurfacesTelemetryWhenRewardIsDropped() {
        val registry = SandboxGame.loadRegistry()
        val base = SandboxGame.createInitialState(registry)

        // Pre-place the killing tower directly (no build command, so nothing spends inventory), then
        // cap the inventory exactly at its current total and drop producers. Nothing ever frees a
        // slot, so every kill reward overflows -> deposit drops it and step records telemetry.
        DefenseRuntime().placeTower("pulse", TilePosition(2, 2), registry, base.world, base.entities)
        val total = base.inventory.resources.values.sum()
        val capped = base.copy(
            producers = emptyList(),
            inventory = Inventory(base.inventory.resources, capacity = total),
        )
        val runtime = SandboxRuntime(capped)

        runtime.step(35)

        assertTrue(runtime.state.defense.metrics.enemiesKilled > 0, "no kills; overflow branch not reached")
        // The reward did not sneak into the full inventory...
        assertEquals(total, runtime.state.inventory.resources.values.sum())
        // ...and the drop is observable, not silent.
        assertTrue(
            runtime.state.lastCommandOrError?.startsWith("reward_dropped:") == true,
            "expected reward_dropped telemetry, got '${runtime.state.lastCommandOrError}'",
        )
    }
}
