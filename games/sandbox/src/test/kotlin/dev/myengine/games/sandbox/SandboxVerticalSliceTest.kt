package dev.myengine.games.sandbox

import dev.myengine.core.CommandId
import dev.myengine.core.Tick
import dev.myengine.core.command.BuildTowerCommand
import dev.myengine.core.command.TileCoordinate
import dev.myengine.world.TilePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SandboxVerticalSliceTest {
    @Test
    fun scriptedScenarioIsDeterministic() {
        val first = SandboxGame.runScriptedScenario()
        val second = SandboxGame.runScriptedScenario()

        assertEquals(first.hash, second.hash)
        assertEquals(ReplayGoldenHashes.canonical, first.hash)
        assertTrue(first.snapshot.entities.isNotEmpty())
    }

    @Test
    fun canonicalMapContentMaterializesWithoutChangingReplayHash() {
        val registry = SandboxGame.loadRegistry()
        val map = registry.requireMap("sandbox-canonical")

        val runtime = SandboxGame.createRuntime(registry, mapId = map.id)

        assertEquals(map.id, runtime.state.mapId)
        assertEquals(map.width, runtime.state.world.size.width)
        assertEquals(map.height, runtime.state.world.size.height)
        val coreTile = runtime.state.world.tileAt(TilePosition(map.core.x, map.core.y))
        assertEquals("core", coreTile.tile.terrainId)
        assertTrue(coreTile.terrain.isCore)
        val resourceTile = runtime.state.world.tileAt(TilePosition(5, 5)).tile
        assertEquals("resource", resourceTile.terrainId)
        assertEquals("bolt", resourceTile.resourceNode?.resourceId)
        assertEquals(100, resourceTile.resourceNode?.amount)
        assertEquals(ReplayGoldenHashes.canonical, SandboxGame.runScriptedScenario(mapId = map.id).hash)
    }

    @Test
    fun changedBuildCommandChangesReplayHash() {
        val registry = SandboxGame.loadRegistry()
        fun runAt(position: TilePosition): String {
            val runtime = SandboxGame.createRuntime(registry)
            runtime.submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", TileCoordinate(position.x, position.y)))
            runtime.step(20)
            return runtime.state.stableHash()
        }

        assertNotEquals(runAt(TilePosition(30, 32)), runAt(TilePosition(31, 32)))
    }

    @Test
    fun saveLoadRoundtripPreservesHash() {
        val registry = SandboxGame.loadRegistry()
        val runtime = SandboxGame.createRuntime(registry)
        runtime.submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", TileCoordinate(30, 32)))
        runtime.step(5)

        val save = SandboxSaveCodec.encode(runtime.state, seed = 7)
        val loaded = SandboxSaveCodec.decode(save, registry)

        assertEquals(runtime.state.stableHash(), loaded.stableHash())
    }

    @Test
    fun killProducingScenarioIsReplayStable() {
        val registry = SandboxGame.loadRegistry()
        fun run(): Pair<String, Int> {
            val runtime = SandboxGame.createRuntime(registry)
            // Tower adjacent to the enemy spawn (1,1) -> guaranteed kills within step(35),
            // so the reward-deposit path is actually exercised (unlike the (30,32) scenario).
            runtime.submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", TileCoordinate(2, 2)))
            runtime.step(35)
            return runtime.state.stableHash() to runtime.state.defense.metrics.enemiesKilled
        }

        val (firstHash, firstKills) = run()
        val (secondHash, _) = run()

        // Fail loudly if the scenario ever stops exercising the reward-deposit path.
        assertTrue(firstKills > 0, "kill-producing scenario killed no enemies")
        // The reward-deposit path (deposit into inventory -> stableHash) is replay-stable.
        assertEquals(firstHash, secondHash)
    }

    @Test
    fun killScenarioApiKillsDeterministicallyAndDiffersFromCanonical() {
        val first = SandboxGame.runScriptedKillScenario()
        val second = SandboxGame.runScriptedKillScenario()
        val canonical = SandboxGame.runScriptedScenario()

        // The kill-bearing gate must actually exercise the reward path...
        assertTrue(first.metrics.enemiesKilled > 0, "kill scenario killed no enemies")
        assertTrue(first.metrics.towerShots > 0, "kill scenario never fired")
        // ...be replay-stable...
        assertEquals(first.hash, second.hash)
        assertEquals(ReplayGoldenHashes.kill, first.hash)
        // ...and carry its own hash distinct from the (no-kill) canonical baseline.
        assertNotEquals(canonical.hash, first.hash)
        assertEquals(0, canonical.metrics.enemiesKilled)
    }

    @Test
    fun saveLoadRoundtripPreservesHashWithRewardDeposit() {
        val registry = SandboxGame.loadRegistry()

        // Content-derived reward resource (single enemy type in sandbox waves); no literal id.
        val enemyId = registry.waves.values
            .flatMap { it.spawns }
            .map { it.enemyId }
            .distinct()
            .single()
        val rewardResource = registry.enemies[enemyId]!!.rewardResource

        val runtime = SandboxGame.createRuntime(registry)
        runtime.submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", TileCoordinate(2, 2)))
        runtime.step(35)

        // Precondition: kills happened, so the reward-inflated inventory is non-empty.
        assertTrue(runtime.state.defense.metrics.enemiesKilled > 0, "no kills; deposit not exercised")
        val preSaveRewardBalance = runtime.state.inventory.amount(rewardResource)
        assertTrue(preSaveRewardBalance > 0, "reward balance must be non-empty before save")

        val save = SandboxSaveCodec.encode(runtime.state, seed = 7)
        val loaded = SandboxSaveCodec.decode(save, registry)

        // The full-state hash (which includes inventory) survives encode/decode.
        assertEquals(runtime.state.stableHash(), loaded.stableHash())
        // And the deposited reward specifically survives the toSortedMap encode/decode.
        assertEquals(preSaveRewardBalance, loaded.inventory.amount(rewardResource))
    }
}
