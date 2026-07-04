package dev.myengine.content

import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContentPackLoaderTest {
    @Test
    fun validPackLoadsRegistry() {
        val root = createPack()

        val result = ContentPackLoader.load(root)

        assertTrue(result.isValid, result.errors.joinToString("\n"))
        assertEquals("test-pack", result.registry?.manifest?.id)
        assertEquals("bolt", result.registry?.towers?.get("basic")?.costResource)
    }

    @Test
    fun missingReferenceIsActionable() {
        val root = createPack()
        root.resolve("towers.properties").writeText(
            """
            basic.range=3
            basic.damage=1
            basic.cooldownTicks=2
            basic.costResource=missing
            basic.costAmount=2
            """.trimIndent(),
        )

        val result = ContentPackLoader.load(root)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.file == "towers.properties" && it.field == "costResource" })
    }

    private fun createPack() = Files.createTempDirectory("myengine-content-test").also { root ->
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
        root.resolve("tiles.properties").writeText(
            """
            floor.buildable=true
            floor.blocksMovement=false
            wall.buildable=false
            wall.blocksMovement=true
            core.buildable=false
            core.blocksMovement=false
            core.isCore=true
            """.trimIndent(),
        )
        root.resolve("resources.properties").writeText("bolt.displayKey=resource.bolt\n")
        root.resolve("towers.properties").writeText(
            """
            basic.range=3
            basic.damage=1
            basic.cooldownTicks=2
            basic.costResource=bolt
            basic.costAmount=2
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
        root.resolve("waves.properties").writeText(
            """
            wave1.startTick=2
            wave1.spawns=scout:2
            """.trimIndent(),
        )
        root.resolve("incidents.properties").writeText(
            """
            spark.minThreat=0
            spark.maxThreat=5
            spark.weight=1
            """.trimIndent(),
        )
        root.resolve("strings.properties").writeText("resource.bolt=Bolt\n")
    }
}
