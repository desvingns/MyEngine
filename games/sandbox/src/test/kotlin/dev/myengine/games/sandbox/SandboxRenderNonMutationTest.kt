package dev.myengine.games.sandbox

import dev.myengine.core.CommandId
import dev.myengine.core.Tick
import dev.myengine.core.command.BuildTowerCommand
import dev.myengine.core.command.TileCoordinate
import dev.myengine.render.Camera
import dev.myengine.render.PlaceholderRenderSurface
import dev.myengine.render.RenderAssetRef
import dev.myengine.render.RenderKind
import dev.myengine.world.WorldSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * SG-003 integration guard: projecting an authoritative snapshot through the render surface
 * must not mutate simulation state. We run a few deterministic steps, capture the state's
 * stable hash, project, and assert the hash is unchanged. No literal hash is asserted so the
 * test stays robust to unrelated content/balance changes.
 */
class SandboxRenderNonMutationTest {

    @Test
    fun projectingSnapshotDoesNotMutateAuthoritativeState() {
        val registry = SandboxGame.loadRegistry()
        val runtime = SandboxGame.createRuntime(registry)
        runtime.step(10)

        val snapshot = runtime.snapshot()
        val hashBefore = runtime.state.stableHash()

        val camera = Camera(
            WorldSize(snapshot.worldSize.width, snapshot.worldSize.height),
            viewportWidth = 480f,
            viewportHeight = 480f,
        )
        val frame = PlaceholderRenderSurface().project(snapshot, camera)

        // Sanity: the projection actually did work (world tiles at minimum).
        assertTrue(frame.primitives.isNotEmpty(), "expected a non-empty render frame")
        assertTrue(snapshot.tiles.any { it.assetRef != null }, "expected sandbox content refs to cross the snapshot boundary")
        assertTrue(frame.primitives.any { it.assetRef != null }, "expected sandbox content refs to cross the render boundary")

        // The authoritative state hash is unchanged by the render path.
        assertEquals(hashBefore, runtime.state.stableHash())
    }

    @Test
    fun preservesOpaqueContentRefsFromRegistryThroughSnapshotIntoRenderFrame() {
        val registry = SandboxGame.loadRegistry()
        val runtime = SandboxGame.createRuntime(registry)
        runtime.submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", TileCoordinate(30, 32)))
        runtime.step(10)

        val snapshot = runtime.snapshot()
        val frame = PlaceholderRenderSurface().project(
            snapshot,
            Camera(
                WorldSize(snapshot.worldSize.width, snapshot.worldSize.height),
                viewportWidth = 480f,
                viewportHeight = 480f,
            ),
        )
        val expectedFloor = registry.requireTile("floor").assetRef!!.let { RenderAssetRef(it.path, it.atlasKey) }
        val expectedTower = registry.requireTower("pulse").assetRef!!.let { RenderAssetRef(it.path, it.atlasKey) }
        val expectedEnemy = registry.requireEnemy("drift").assetRef!!.let { RenderAssetRef(it.path, it.atlasKey) }

        val floorTile = snapshot.tiles.first { it.terrainId == "floor" }
        assertEquals(expectedFloor, floorTile.assetRef)
        assertEquals(
            expectedFloor,
            frame.primitives.first { it.tile == floorTile.position }.assetRef,
        )

        val towerEntity = snapshot.entities.first { it.type == "tower:pulse" }
        assertEquals(expectedTower, towerEntity.assetRef)
        assertEquals(expectedTower, frame.primitives.first { it.kind == RenderKind.TOWER }.assetRef)

        val enemyEntity = snapshot.entities.first { it.type == "enemy:drift" }
        assertEquals(expectedEnemy, enemyEntity.assetRef)
        assertEquals(expectedEnemy, frame.primitives.first { it.kind == RenderKind.ENEMY }.assetRef)
    }

    @Test
    fun latestTickCombatEventsAreImmutableAndDoNotMutateEarlierSnapshots() {
        val registry = SandboxGame.loadRegistry()
        val runtime = SandboxGame.createRuntime(registry)
        runtime.submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", TileCoordinate(2, 2)))
        runtime.step(10)

        val snapshotAtShot = runtime.snapshot()
        val towerId = snapshotAtShot.entities.single { it.type == "tower:pulse" }.id
        val shot = snapshotAtShot.combatEvents.shots.single()
        val hit = snapshotAtShot.combatEvents.hits.single()

        assertEquals(Tick(10), shot.tick)
        assertEquals(towerId, shot.sourceEntityId)
        assertEquals(shot.sourceEntityId, hit.sourceEntityId)
        assertEquals(shot.targetEntityId, hit.targetEntityId)
        assertEquals(Tick(10), hit.tick)
        assertFailsWith<UnsupportedOperationException> {
            @Suppress("UNCHECKED_CAST")
            (snapshotAtShot.combatEvents.shots as MutableList<Any?>).clear()
        }

        runtime.step(1)
        assertTrue(runtime.snapshot().combatEvents.shots.isEmpty(), "events must be replaced for the next tick")
        assertEquals(listOf(shot), snapshotAtShot.combatEvents.shots, "an earlier snapshot must stay unchanged")
    }
}
