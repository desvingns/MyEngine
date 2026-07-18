package dev.myengine.games.sandbox

import dev.myengine.content.TowerUpgradeTier
import dev.myengine.core.CommandId
import dev.myengine.core.Tick
import dev.myengine.core.command.BuildTowerCommand
import dev.myengine.core.command.TileCoordinate
import dev.myengine.core.command.UpgradeTowerCommand
import dev.myengine.entities.Entity
import dev.myengine.logistics.Inventory
import java.io.StringReader
import java.io.StringWriter
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Acceptance coverage for MTD-003: content-defined tower upgrades mutate attack stats,
 * spend resources deterministically, reject without spending, and survive save/restore.
 */
class SandboxTowerUpgradeTest {
    @Test
    fun upgradeTierMutatesAttackStatsAndSpendsContentDefinedCost() {
        val registry = SandboxGame.loadRegistry()
        val tower = registry.requireTower("pulse")
        val tier = tower.upgradeTiers.getValue(TowerUpgradeTier.key("main", 1))
        val runtime = SandboxGame.createRuntime(registry)
        val towerEntity = placePulseTower(runtime)
        val balanceAfterBuild = runtime.state.inventory.amount(tier.costResource)

        runtime.submit(UpgradeTowerCommand(CommandId(2), Tick(2), towerEntity.id.value, tier.branch, tier.tier))
        runtime.step(1)

        val upgraded = runtime.state.entities.require(towerEntity.id)
        assertEquals(tier.range, upgraded.attack?.range)
        assertEquals(tier.damage, upgraded.attack?.damage)
        assertEquals(tier.cooldownTicks, upgraded.attack?.cooldownTicks)
        assertEquals(tier.branch, upgraded.tower?.upgradeBranch)
        assertEquals(tier.tier, upgraded.tower?.upgradeTier)
        assertEquals(balanceAfterBuild - tier.costAmount, runtime.state.inventory.amount(tier.costResource))
        assertEquals("upgraded:${towerEntity.id.value}:${tier.branch}:${tier.tier}", runtime.state.lastCommandOrError)
    }

    @Test
    fun unaffordableUpgradeIsRejectedWithoutChangingBalanceOrStats() {
        val registry = SandboxGame.loadRegistry()
        val tower = registry.requireTower("pulse")
        val tier = tower.upgradeTiers.getValue(TowerUpgradeTier.key("main", 1))
        val runtime = SandboxGame.createRuntime(registry)
        val towerEntity = placePulseTower(runtime)
        val unaffordableBalance = tier.costAmount - 1
        runtime.state.inventory = Inventory(mapOf(tier.costResource to unaffordableBalance))

        runtime.submit(UpgradeTowerCommand(CommandId(2), Tick(2), towerEntity.id.value, tier.branch, tier.tier))
        runtime.step(1)

        val unchanged = runtime.state.entities.require(towerEntity.id)
        assertTrue(tier.costAmount > 0, "test setup requires a positive upgrade cost")
        assertEquals(unaffordableBalance, runtime.state.inventory.amount(tier.costResource))
        assertTrue(runtime.state.inventory.amount(tier.costResource) >= 0)
        assertEquals(tower.range, unchanged.attack?.range)
        assertEquals(tower.damage, unchanged.attack?.damage)
        assertEquals(tower.cooldownTicks, unchanged.attack?.cooldownTicks)
        assertEquals(null, unchanged.tower?.upgradeBranch)
        assertEquals(0, unchanged.tower?.upgradeTier)
        assertEquals("missing_resource:${tier.costResource}", runtime.state.lastCommandOrError)
    }

    @Test
    fun unknownUpgradeTierIsRejectedWithoutSpending() {
        val registry = SandboxGame.loadRegistry()
        val runtime = SandboxGame.createRuntime(registry)
        val towerEntity = placePulseTower(runtime)
        val balanceAfterBuild = runtime.state.inventory.amount("bolt")

        runtime.submit(UpgradeTowerCommand(CommandId(2), Tick(2), towerEntity.id.value, "missing", 1))
        runtime.step(1)

        val unchanged = runtime.state.entities.require(towerEntity.id)
        assertEquals(balanceAfterBuild, runtime.state.inventory.amount("bolt"))
        assertEquals(null, unchanged.tower?.upgradeBranch)
        assertEquals("unknown_upgrade:pulse:missing:1", runtime.state.lastCommandOrError)
    }

    @Test
    fun repeatedSameTierUpgradeIsRejectedWithoutSpendingAgain() {
        val registry = SandboxGame.loadRegistry()
        val tower = registry.requireTower("pulse")
        val tier = tower.upgradeTiers.getValue(TowerUpgradeTier.key("main", 1))
        val runtime = SandboxGame.createRuntime(registry)
        val towerEntity = placePulseTower(runtime)
        runtime.submit(UpgradeTowerCommand(CommandId(2), Tick(2), towerEntity.id.value, tier.branch, tier.tier))
        runtime.step(1)
        val balanceAfterFirstUpgrade = runtime.state.inventory.amount(tier.costResource)
        runtime.state.producers = emptyList()

        runtime.submit(UpgradeTowerCommand(CommandId(3), Tick(3), towerEntity.id.value, tier.branch, tier.tier))
        runtime.step(1)

        val unchanged = runtime.state.entities.require(towerEntity.id)
        assertEquals(balanceAfterFirstUpgrade, runtime.state.inventory.amount(tier.costResource))
        assertEquals(tier.branch, unchanged.tower?.upgradeBranch)
        assertEquals(tier.tier, unchanged.tower?.upgradeTier)
        assertEquals("invalid_upgrade_transition:main:1->main:1", runtime.state.lastCommandOrError)
    }

    @Test
    fun upgradedTowerSaveLoadRoundtripPreservesBranchTierStatsAndHash() {
        val registry = SandboxGame.loadRegistry()
        val tower = registry.requireTower("pulse")
        val tier = tower.upgradeTiers.getValue(TowerUpgradeTier.key("main", 1))
        val runtime = SandboxGame.createRuntime(registry)
        val towerEntity = placePulseTower(runtime)
        runtime.submit(UpgradeTowerCommand(CommandId(2), Tick(2), towerEntity.id.value, tier.branch, tier.tier))
        runtime.step(1)

        val save = SandboxSaveCodec.encode(runtime.state, seed = 7)
        val loaded = SandboxSaveCodec.decode(save, registry)
        val upgraded = loaded.entities.require(towerEntity.id)

        assertEquals(runtime.state.stableHash(), loaded.stableHash())
        assertEquals(tier.branch, upgraded.tower?.upgradeBranch)
        assertEquals(tier.tier, upgraded.tower?.upgradeTier)
        assertEquals(tier.range, upgraded.attack?.range)
        assertEquals(tier.damage, upgraded.attack?.damage)
        assertEquals(tier.cooldownTicks, upgraded.attack?.cooldownTicks)
    }

    @Test
    fun v2SaveWithoutUpgradeFieldsDecodesAsNoUpgradeButKeepsSavedAttackStats() {
        val registry = SandboxGame.loadRegistry()
        val tower = registry.requireTower("pulse")
        val tier = tower.upgradeTiers.getValue(TowerUpgradeTier.key("main", 1))
        val runtime = SandboxGame.createRuntime(registry)
        val towerEntity = placePulseTower(runtime)
        runtime.submit(UpgradeTowerCommand(CommandId(2), Tick(2), towerEntity.id.value, tier.branch, tier.tier))
        runtime.step(1)

        val v2Save = stripUpgradeColumnsAndSetVersion(SandboxSaveCodec.encode(runtime.state, seed = 7), version = 2)
        val loaded = SandboxSaveCodec.decode(v2Save, registry)
        val upgradedStatsWithoutTierMarker = loaded.entities.require(towerEntity.id)

        assertEquals(null, upgradedStatsWithoutTierMarker.tower?.upgradeBranch)
        assertEquals(0, upgradedStatsWithoutTierMarker.tower?.upgradeTier)
        assertEquals(tier.range, upgradedStatsWithoutTierMarker.attack?.range)
        assertEquals(tier.damage, upgradedStatsWithoutTierMarker.attack?.damage)
        assertEquals(tier.cooldownTicks, upgradedStatsWithoutTierMarker.attack?.cooldownTicks)
        assertEquals(tower.id, upgradedStatsWithoutTierMarker.tower?.towerId)
    }

    @Test
    fun pendingUpgradeCommandRoundtripsThroughLifecycleSave() {
        val registry = SandboxGame.loadRegistry()
        val tier = registry.requireTower("pulse").upgradeTiers.getValue(TowerUpgradeTier.key("main", 1))
        val position = TileCoordinate(2, 2)

        val uninterrupted = SandboxSession.start(registry).also { session ->
            session.submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", position))
            session.step(1)
            val towerEntityId = session.runtime.state.entities.byTag("tower").single().id.value
            session.submit(UpgradeTowerCommand(CommandId(2), Tick(25), towerEntityId, tier.branch, tier.tier))
            session.step(39)
        }.stableHash()

        val paused = SandboxSession.start(registry)
        paused.submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", position))
        paused.step(1)
        val towerEntityId = paused.runtime.state.entities.byTag("tower").single().id.value
        paused.submit(UpgradeTowerCommand(CommandId(2), Tick(25), towerEntityId, tier.branch, tier.tier))
        paused.step(19)

        val restored = SandboxSession.restore(paused.save(), registry)
        assertIs<UpgradeTowerCommand>(restored.runtime.pendingCommands().single())

        restored.step(20)
        assertEquals(uninterrupted, restored.stableHash())
    }

    private fun placePulseTower(runtime: SandboxRuntime): Entity {
        runtime.submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", TileCoordinate(2, 2)))
        runtime.step(1)
        return runtime.state.entities.byTag("tower").single()
    }

    private fun stripUpgradeColumnsAndSetVersion(text: String, version: Int): String {
        val props = Properties().also { it.load(StringReader(text)) }
        props["saveVersion"] = version.toString()
        props["entities"] = props.getProperty("entities", "")
            .split(';')
            .filter { it.isNotBlank() }
            .joinToString(";") { entityRow ->
                entityRow.split('|').take(13).joinToString("|")
            }
        return StringWriter().also { props.store(it, "MyEngine sandbox save") }.toString()
    }
}
