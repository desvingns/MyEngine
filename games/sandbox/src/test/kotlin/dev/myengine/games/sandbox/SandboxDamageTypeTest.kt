package dev.myengine.games.sandbox

import dev.myengine.content.ContentRegistry
import dev.myengine.content.DamageTypeContent
import dev.myengine.content.TowerUpgradeTier
import dev.myengine.core.CommandId
import dev.myengine.core.Tick
import dev.myengine.core.command.BuildTowerCommand
import dev.myengine.core.command.TileCoordinate
import dev.myengine.core.command.UpgradeTowerCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class SandboxDamageTypeTest {
    @Test
    fun towerUpgradeInheritsBaseDamageType() {
        val registry = typedRegistry(resistance = 50)
        val tower = registry.requireTower("pulse")
        val tier = tower.upgradeTiers.getValue(TowerUpgradeTier.key("main", 1))
        val runtime = SandboxGame.createRuntime(registry)

        runtime.submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", TileCoordinate(2, 2)))
        runtime.step()
        val towerEntity = runtime.state.entities.byTag("tower").single()
        assertEquals("arcane", towerEntity.attack?.damageTypeId)

        runtime.submit(UpgradeTowerCommand(CommandId(2), Tick(2), towerEntity.id.value, tier.branch, tier.tier))
        runtime.step()

        assertEquals("arcane", runtime.state.entities.require(towerEntity.id).attack?.damageTypeId)
    }

    @Test
    fun typedV14SaveRoundtripPreservesHashAndNextTickReplay() {
        val registry = typedRegistry(resistance = 50)

        fun newRuntime(): SandboxRuntime = SandboxGame.createRuntime(registry, seed = 7L)

        val uninterrupted = newRuntime()
        uninterrupted.submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", TileCoordinate(2, 2)))
        uninterrupted.step(35)

        val paused = newRuntime()
        paused.submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", TileCoordinate(2, 2)))
        paused.step(15)
        val save = SandboxSaveCodec.encode(paused.state, seed = 7L)
        val decoded = SandboxSaveCodec.decode(save, registry)

        assertEquals(19, SandboxSaveCodec.SAVE_VERSION)
        assertEquals(paused.state.stableHash(), decoded.stableHash())
        // The static damage type is derived again from the loaded registry, not added to v15 state.
        assertNull(decoded.entities.byTag("tower").single().attack?.damageTypeId)

        val restored = SandboxRuntime(decoded, seed = 7L)
        restored.step(20)

        assertEquals(uninterrupted.state.stableHash(), restored.state.stableHash())
    }

    @Test
    fun resistReplayIsGoldenStableAndLegacyReplayHashesRemainUnchanged() {
        val first = SandboxGame.runScriptedResistScenario()
        val second = SandboxGame.runScriptedResistScenario()
        val zeroResist = SandboxGame.runScriptedUnresistedScenario()

        assertEquals("3f02607020d48668", first.hash)
        assertEquals(first.hash, second.hash)
        assertNotEquals(zeroResist.hash, first.hash)
        assertEquals("e4892bcc18f9d8dc", SandboxGame.runScriptedScenario().hash)
        assertEquals("a763da4ac32b15b4", SandboxGame.runScriptedKillScenario().hash)
    }

    private fun typedRegistry(resistance: Int): ContentRegistry {
        val base = SandboxGame.loadRegistry()
        val damageTypeId = "arcane"
        return base.copy(
            strings = base.strings + ("damage.arcane" to "Arcane damage"),
            damageTypes = mapOf(damageTypeId to DamageTypeContent(damageTypeId, "damage.arcane")),
            towers = base.towers.mapValues { (_, tower) -> tower.copy(damageTypeId = damageTypeId) },
            enemies = base.enemies.mapValues { (_, enemy) -> enemy.copy(resists = mapOf(damageTypeId to resistance)) },
        )
    }
}
