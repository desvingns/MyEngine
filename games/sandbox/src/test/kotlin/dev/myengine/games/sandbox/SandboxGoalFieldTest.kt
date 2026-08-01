package dev.myengine.games.sandbox

import dev.myengine.content.MapContent
import dev.myengine.content.MapCoordinate
import dev.myengine.content.MapSpawn
import dev.myengine.content.MapTerrainSymbol
import dev.myengine.core.CommandId
import dev.myengine.core.Tick
import dev.myengine.core.command.BuildTowerCommand
import dev.myengine.core.command.TileCoordinate
import dev.myengine.world.TilePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SandboxGoalFieldTest {
    @Test
    fun successfulPlacementReroutesExistingEnemiesInTheSameTick() {
        val runtime = SandboxGame.createRuntime()
        runtime.step(11)
        val before = runtime.state.entities.byTag("enemy").map { it.position!!.tile }.distinct()
        val expectedTowerId = runtime.state.entities.all().maxOf { it.id.value } + 1
        assertEquals(listOf(TilePosition(3, 1)), before)

        // With the initial y-then-x field, (4,1) is the next step. Blocking it on tick 12
        // must rebuild before this tick's enemy update, forcing the enemies down immediately.
        runtime.submit(BuildTowerCommand(CommandId(1), Tick(12), "pulse", TileCoordinate(4, 1)))
        runtime.step()

        assertEquals("placed:$expectedTowerId", runtime.state.lastCommandOrError)
        assertEquals(listOf(TilePosition(3, 2)), runtime.state.entities.byTag("enemy").map { it.position!!.tile }.distinct())
    }

    @Test
    fun midRerouteSaveRestoreReachesTheSameStableHash() {
        val uninterrupted = SandboxSession.start()
        uninterrupted.step(11)
        uninterrupted.submit(BuildTowerCommand(CommandId(1), Tick(12), "pulse", TileCoordinate(4, 1)))
        uninterrupted.step()

        val restored = SandboxSession.restore(uninterrupted.save())
        uninterrupted.step(20)
        restored.step(20)

        assertEquals(uninterrupted.stableHash(), restored.stableHash())
    }

    @Test
    fun midRunMazeReplayPinsGoldenHash() {
        fun run(): String {
            val runtime = SandboxGame.createRuntime()
            runtime.step(11)
            runtime.submit(BuildTowerCommand(CommandId(1), Tick(12), "pulse", TileCoordinate(4, 1)))
            runtime.step(24)
            return runtime.state.stableHash()
        }

        val first = run()
        assertEquals(first, run())
        assertEquals("fdf6a084891f61af", first)
    }

    @Test
    fun liveEnemyPlacementRejectsWithoutSpendingOrMutatingPlacementState() {
        val runtime = SandboxGame.createRuntime()
        runtime.step(10)
        runtime.state.producers = emptyList()
        val candidate = TilePosition(2, 1)
        val inventoryBefore = runtime.state.inventory
        val enemyIdsBefore = runtime.state.entities.byTag("enemy").map { it.id.value }
        val nextIdBefore = runtime.state.entities.nextIdSnapshot()

        runtime.submit(BuildTowerCommand(CommandId(1), Tick(11), "pulse", TileCoordinate(candidate.x, candidate.y)))
        runtime.step()

        assertEquals("occupied_by_enemy", runtime.state.lastCommandOrError)
        assertEquals(inventoryBefore, runtime.state.inventory)
        assertTrue(runtime.state.world.canBuild(candidate), "live enemy rejection must not occupy the candidate tile")
        assertEquals(enemyIdsBefore, runtime.state.entities.byTag("enemy").map { it.id.value })
        assertEquals(nextIdBefore, runtime.state.entities.nextIdSnapshot())
    }

    @Test
    fun sealedSpawnCommandDoesNotSpendResourcesOrConsumeEntityId() {
        val base = SandboxGame.loadRegistry()
        val map = MapContent(
            id = "sealed-spawn-test",
            width = 5,
            height = 3,
            terrainRows = listOf("....C", "##.##", "....#"),
            terrainMapping = mapOf(
                '.' to MapTerrainSymbol("floor"),
                '#' to MapTerrainSymbol("wall"),
                'C' to MapTerrainSymbol("core"),
            ),
            // `primary` retains the top-row route; `secondary` alone depends on (2,1).
            spawns = mapOf(
                "primary" to MapSpawn("primary", MapCoordinate(0, 0)),
                "secondary" to MapSpawn("secondary", MapCoordinate(0, 2)),
            ),
            core = MapCoordinate(4, 0),
        )
        val registry = base.copy(maps = mapOf(map.id to map), waves = emptyMap())
        val state = SandboxGame.createInitialState(registry, mapId = map.id).also { it.producers = emptyList() }
        val runtime = SandboxRuntime(state)
        val beforeBolt = state.inventory.amount("bolt")

        runtime.submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", TileCoordinate(2, 1)))
        runtime.step()

        assertEquals("blocks_spawn_path", state.lastCommandOrError)
        assertEquals(beforeBolt, state.inventory.amount("bolt"))
        assertTrue(state.world.canBuild(TilePosition(2, 1)))
        assertEquals(0, state.entities.count())

        runtime.submit(BuildTowerCommand(CommandId(2), Tick(2), "pulse", TileCoordinate(3, 2)))
        runtime.step()

        assertEquals(1L, state.entities.byTag("tower").single().id.value)
    }
}
