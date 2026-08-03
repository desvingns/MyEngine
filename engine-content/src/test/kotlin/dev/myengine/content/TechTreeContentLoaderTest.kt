package dev.myengine.content

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TechTreeContentLoaderTest {
    @Test
    fun validOptionalTechTreeLoadsSortedNodesAndTypedUnlocks() {
        val root = createPack().also {
            it.resolve("tech-tree.json").writeText(
                """
                {
                  "nodes": [
                    {"id":"advanced","cost":{"resource":"bolt","amount":3},"prerequisites":["basic"],"unlocks":[{"type":"recipe","id":"generator"}]},
                    {"id":"basic","cost":{"resource":"bolt","amount":2},"unlocks":[{"type":"tower","id":"basic"}]}
                  ]
                }
                """.trimIndent(),
            )
        }

        val result = ContentPackLoader.load(root)

        assertTrue(result.isValid, result.errors.joinToString("\n"))
        val registry = result.registry!!
        assertEquals(listOf("advanced", "basic"), registry.techNodes.keys.toList())
        assertEquals(listOf("basic"), registry.techNodes.getValue("advanced").prerequisites)
        assertEquals(TechUnlockType.RECIPE, registry.techNodes.getValue("advanced").unlocks.single().type)
        assertEquals("generator", registry.techTree.getValue("advanced").unlocks.single().id)
    }

    @Test
    fun missingResourceNodeAndUnlockTargetsAreRejected() {
        val root = createPack().also {
            it.resolve("tech-tree.json").writeText(
                """
                {"nodes":[
                  {"id":"basic","cost":{"resource":"missing-resource","amount":1},"prerequisites":["missing-node"],"unlocks":[
                    {"type":"tower","id":"missing-tower"},
                    {"type":"building","id":"missing-building"},
                    {"type":"recipe","id":"missing-recipe"}
                  ]}
                ]}
                """.trimIndent(),
            )
        }

        val result = ContentPackLoader.load(root)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.message == "Unknown resource 'missing-resource'." })
        assertTrue(result.errors.any { it.message == "Unknown prerequisite 'missing-node'." })
        assertTrue(result.errors.any { it.message == "Unknown tower 'missing-tower'." })
        assertTrue(result.errors.any { it.message == "Unknown building 'missing-building'." })
        assertTrue(result.errors.any { it.message == "Unknown recipe 'missing-recipe'." })
    }

    @Test
    fun duplicateNodePrerequisiteAndUnlockIdsAreRejected() {
        val root = createPack().also {
            it.resolve("tech-tree.json").writeText(
                """
                {"nodes":[
                  {"id":"basic","cost":{"resource":"bolt","amount":1},"prerequisites":["basic","basic"],"unlocks":[
                    {"type":"tower","id":"basic"},
                    {"type":"tower","id":"basic"}
                  ]},
                  {"id":"basic","cost":{"resource":"bolt","amount":2},"unlocks":[{"type":"tower","id":"basic"}]}
                ]}
                """.trimIndent(),
            )
        }

        val result = ContentPackLoader.load(root)
        val messages = result.errors.map { it.message }

        assertFalse(result.isValid)
        assertTrue(messages.any { it == "Duplicate tech node id." })
        assertTrue(messages.any { it == "Duplicate prerequisite 'basic'." })
        assertTrue(messages.any { it == "Duplicate unlock reference 'tower:basic'." })
        assertTrue(messages.any { it.contains("already declared by") })
    }

    @Test
    fun selfAndMutualPrerequisiteCyclesAreRejected() {
        val self = createPack().also {
            it.resolve("tech-tree.json").writeText(
                """{"nodes":[{"id":"self","cost":{"resource":"bolt","amount":1},"prerequisites":["self"]}]}""",
            )
        }
        val mutual = createPack().also {
            it.resolve("tech-tree.json").writeText(
                """{"nodes":[
                  {"id":"a","cost":{"resource":"bolt","amount":1},"prerequisites":["b"]},
                  {"id":"b","cost":{"resource":"bolt","amount":1},"prerequisites":["a"]}
                ]}""",
            )
        }

        val selfResult = ContentPackLoader.load(self)
        val mutualResult = ContentPackLoader.load(mutual)

        assertTrue(selfResult.errors.any { it.message == "Prerequisite cycle: self -> self." })
        assertTrue(mutualResult.errors.any { it.message == "Prerequisite cycle: a -> b -> a." })
    }

    @Test
    fun invalidDiagnosticsAreDeterministicAcrossLoads() {
        val root = createPack().also {
            it.resolve("tech-tree.json").writeText(
                """{"nodes":[
                  {"id":"b","cost":{"resource":"missing","amount":1},"prerequisites":["a"]},
                  {"id":"a","cost":{"resource":"missing","amount":1},"prerequisites":["b"]}
                ]}""",
            )
        }

        val first = ContentPackLoader.load(root)
        val second = ContentPackLoader.load(root)

        assertFalse(first.isValid)
        assertEquals(first.errors, second.errors)
    }

    @Test
    fun legacyPackWithoutTechTreeRemainsCompatible() {
        val result = ContentPackLoader.load(createPack())

        assertTrue(result.isValid, result.errors.joinToString("\n"))
        assertTrue(result.registry!!.techNodes.isEmpty())
    }

    private fun createPack(): Path = Files.createTempDirectory("myengine-tech-tree-test").also { root ->
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
            """.trimIndent(),
        )
        root.resolve("resources.properties").writeText("bolt.displayKey=resource.bolt\n")
        root.resolve("towers.properties").writeText(
            """
            basic.displayKey=tower.basic
            basic.range=3
            basic.damage=1
            basic.cooldownTicks=2
            basic.costResource=bolt
            basic.costAmount=2
            basic.sellRefundRatio=0.5
            basic.targetingMode=nearest
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
            tower.basic=Basic
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
