package dev.myengine.content

import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Eng025FlyingContentTest {
    @Test
    fun parsesAirMovementAndTargetCapabilityFlags() {
        val root = packRoot()
        root.resolve("enemies.properties").toFile().appendText("\nfliers.movementMode=air\n")
        root.resolve("towers.properties").toFile().appendText(
            "\nground-only.canTargetAir=false\nground-only.canTargetGround=true\n",
        )

        val result = ContentPackLoader.load(root)

        assertTrue(result.isValid, result.errors.joinToString("\n"))
        assertEquals(dev.myengine.core.MovementMode.AIR, result.registry!!.requireEnemy("fliers").movementMode)
        assertFalse(result.registry.requireTower("ground-only").canTargetAir)
        assertTrue(result.registry.requireTower("ground-only").canTargetGround)
        assertTrue(result.registry.requireTower("basic").canTargetAir)
        assertTrue(result.registry.requireTower("basic").canTargetGround)
    }

    @Test
    fun rejectsUnknownMovementModeAndTowerWithNoTargetCapability() {
        val root = packRoot()
        root.resolve("enemies.properties").toFile().appendText("\nfliers.movementMode=teleport\n")
        root.resolve("towers.properties").toFile().appendText(
            "\nnone.canTargetAir=false\nnone.canTargetGround=false\n",
        )

        val result = ContentPackLoader.load(root)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.file == "enemies.properties" && it.field == "movementMode" })
        assertTrue(result.errors.any { it.file == "towers.properties" && it.field == "canTargetAir" })
    }

    private fun packRoot() = Files.createTempDirectory("myengine-eng025-content").also { root ->
        root.resolve("manifest.properties").writeText(
            """
            id=eng025-content
            version=0.1
            schemaVersion=1
            engineMin=0.0.1
            engineMax=0.1.x
            locales=en
            dependencies=
            """.trimIndent(),
        )
        root.resolve("tiles.properties").writeText("floor.buildable=true\nfloor.blocksMovement=false\n")
        root.resolve("resources.properties").writeText("bolt.displayKey=resource.bolt\n")
        root.resolve("towers.properties").writeText(
            """
            basic.displayKey=tower.basic
            basic.range=2
            basic.damage=2
            basic.cooldownTicks=1
            basic.costResource=bolt
            basic.costAmount=1
            basic.sellRefundRatio=0.5
            ground-only.displayKey=tower.ground-only
            ground-only.range=2
            ground-only.damage=2
            ground-only.cooldownTicks=1
            ground-only.costResource=bolt
            ground-only.costAmount=1
            ground-only.sellRefundRatio=0.5
            """.trimIndent(),
        )
        root.resolve("enemies.properties").writeText(
            """
            scout.health=2
            scout.speedTilesPerTick=1
            scout.rewardResource=bolt
            scout.rewardAmount=1
            scout.coreDamage=1
            fliers.health=2
            fliers.speedTilesPerTick=1
            fliers.rewardResource=bolt
            fliers.rewardAmount=1
            fliers.coreDamage=1
            """.trimIndent(),
        )
        root.resolve("recipes.properties").writeText("generator.outputResource=bolt\ngenerator.outputAmount=1\ngenerator.durationTicks=1\n")
        root.resolve("waves.properties").writeText("w1.startTick=1\nw1.spawns=scout:1\n")
        root.resolve("strings.properties").writeText(
            """
            resource.bolt=Bolt
            tower.basic=Basic
            tower.ground-only=Ground only
            hud.resources=Resources
            hud.wave=Wave
            hud.nextWave=Next wave
            hud.coreHealth=Core
            hud.build=Build
            hud.upgrade=Upgrade
            hud.damage=Damage
            hud.kills=Kills
            hud.tier=Tier
            """.trimIndent(),
        )
    }
}
