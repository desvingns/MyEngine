package dev.myengine.content

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MetaProgressionContentLoaderTest {
    @Test
    fun validOptionalMetaProgressionLoadsSortedUnlockables() {
        val root = createPack().also {
            it.resolve("meta-progression.json").writeText(
                """
                {
                  "currencyResource":"bolt",
                  "unlockables":[
                    {"id":"tower-pulse","type":"tower","target":"pulse"},
                    {"id":"recipe-generator","type":"recipe","target":"generator"}
                  ]
                }
                """.trimIndent(),
            )
        }

        val result = ContentPackLoader.load(root)

        assertTrue(result.isValid, result.errors.joinToString("\n"))
        val progression = result.registry!!.metaProgression!!
        assertEquals("bolt", progression.currencyResourceId)
        assertEquals(listOf("recipe-generator", "tower-pulse"), progression.unlockables.keys.toList())
        assertEquals(TechUnlockType.TOWER, progression.unlockables.getValue("tower-pulse").type)
        assertEquals("pulse", progression.unlockables.getValue("tower-pulse").targetId)
    }

    @Test
    fun unknownReferencesAndDuplicateTargetsAreRejected() {
        val root = createPack().also {
            it.resolve("meta-progression.json").writeText(
                """
                {
                  "currencyResource":"missing",
                  "unlockables":[
                    {"id":"first","type":"tower","target":"missing-tower"},
                    {"id":"second","type":"tower","target":"missing-tower"}
                  ]
                }
                """.trimIndent(),
            )
        }

        val result = ContentPackLoader.load(root)
        val messages = result.errors.map { it.message }

        assertFalse(result.isValid)
        assertTrue(messages.contains("Unknown resource 'missing'."))
        assertTrue(messages.contains("Unknown tower 'missing-tower'."))
        assertTrue(messages.any { it.contains("Duplicate meta unlock target 'tower:missing-tower'") })
    }

    @Test
    fun legacyPackWithoutMetaProgressionRemainsCompatible() {
        val result = ContentPackLoader.load(createPack())

        assertTrue(result.isValid, result.errors.joinToString("\n"))
        assertEquals(null, result.registry!!.metaProgression)
    }

    private fun createPack(): Path = Files.createTempDirectory("myengine-meta-progression-test").also { root ->
        root.resolve("manifest.properties").writeText(
            """
            id=test-pack
            version=0.1.0
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
            pulse.displayKey=tower.pulse
            pulse.range=3
            pulse.damage=1
            pulse.cooldownTicks=2
            pulse.costResource=bolt
            pulse.costAmount=2
            pulse.sellRefundRatio=0.5
            pulse.targetingMode=nearest
            """.trimIndent(),
        )
        root.resolve("enemies.properties").writeText(
            """
            scout.health=3
            scout.speedTilesPerTick=1
            scout.rewardResource=bolt
            scout.rewardAmount=1
            scout.coreDamage=1
            """.trimIndent(),
        )
        root.resolve("recipes.properties").writeText(
            """
            generator.outputResource=bolt
            generator.outputAmount=1
            generator.durationTicks=3
            """.trimIndent(),
        )
        root.resolve("waves.properties").writeText("wave1.startTick=2\nwave1.spawns=scout:1\n")
        root.resolve("incidents.properties").writeText("spark.minThreat=0\nspark.maxThreat=1\nspark.weight=1\n")
        root.resolve("strings.properties").writeText(
            """
            resource.bolt=Bolt
            tower.pulse=Pulse
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
