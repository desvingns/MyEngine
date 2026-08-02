package dev.myengine.content

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DamageTypeContentLoaderTest {
    @Test
    fun validTypedPackLoadsDamageTypesAndResists() {
        val result = ContentPackLoader.load(typedPack())

        assertTrue(result.isValid, result.errors.joinToString("\n"))
        val registry = result.registry!!
        assertEquals(setOf("arcane"), registry.damageTypes.keys)
        assertEquals("damage.arcane", registry.damageTypes.getValue("arcane").displayKey)
        assertEquals("arcane", registry.requireTower("basic").damageTypeId)
        assertEquals(mapOf("arcane" to 33), registry.requireEnemy("scout").resists)
    }

    @Test
    fun missingTowerDamageTypeIdIsActionableWhenTypedContentIsDeclared() {
        val root = typedPack(
            towers = """
                basic.displayKey=tower.basic
                basic.range=3
                basic.damage=7
                basic.cooldownTicks=2
                basic.costResource=bolt
                basic.costAmount=2
                basic.sellRefundRatio=0.5
                basic.targetingMode=nearest
                basic.damageTypeId=arcane
                support.displayKey=tower.support
                support.range=3
                support.damage=1
                support.cooldownTicks=2
                support.costResource=bolt
                support.costAmount=2
                support.sellRefundRatio=0.5
                support.targetingMode=nearest
            """.trimIndent(),
            stringsExtra = "\ntower.support=Support tower",
        )

        val result = ContentPackLoader.load(root)

        assertFalse(result.isValid)
        assertTrue(
            result.errors.any {
                it.file == "towers.properties" &&
                    it.id == "support" &&
                    it.field == "damageTypeId" &&
                    it.message.contains("damage type")
            },
            result.errors.joinToString("\n"),
        )
    }

    @Test
    fun unknownTowerDamageTypeReferenceIsActionable() {
        val root = typedPack(
            towers = baseTower().replace("basic.damageTypeId=arcane", "basic.damageTypeId=shadow"),
        )

        val result = ContentPackLoader.load(root)

        assertFalse(result.isValid)
        assertTrue(
            result.errors.any {
                it.file == "towers.properties" &&
                    it.id == "basic" &&
                    it.field == "damageTypeId" &&
                    it.message.contains("Unknown damage type 'shadow'")
            },
            result.errors.joinToString("\n"),
        )
    }

    @Test
    fun unknownEnemyResistanceReferenceIsActionable() {
        val root = typedPack(
            enemies = baseEnemy().replace("scout.resist.arcane=33", "scout.resist.shadow=33"),
        )

        val result = ContentPackLoader.load(root)

        assertFalse(result.isValid)
        assertTrue(
            result.errors.any {
                it.file == "enemies.properties" &&
                    it.id == "scout" &&
                    it.field == "resist.shadow" &&
                    it.message.contains("Unknown damage type 'shadow'")
            },
            result.errors.joinToString("\n"),
        )
    }

    @Test
    fun declaredDamageTypeWithoutTowerUsageIsRejected() {
        val root = typedPack(
            towers = baseTower().replace("basic.damageTypeId=arcane", ""),
        )

        val result = ContentPackLoader.load(root)

        assertFalse(result.isValid)
        assertTrue(
            result.errors.any {
                it.file == "damage-types.properties" &&
                    it.id == "arcane" &&
                    it.field == "usage.towers"
            },
            result.errors.joinToString("\n"),
        )
    }

    @Test
    fun declaredDamageTypeWithoutEnemyResistanceUsageIsRejected() {
        val root = typedPack(
            enemies = baseEnemy().replace("scout.resist.arcane=33", ""),
        )

        val result = ContentPackLoader.load(root)

        assertFalse(result.isValid)
        assertTrue(
            result.errors.any {
                it.file == "damage-types.properties" &&
                    it.id == "arcane" &&
                    it.field == "usage.enemyResists"
            },
            result.errors.joinToString("\n"),
        )
    }

    @Test
    fun zeroAndFullResistanceValuesAreValid() {
        listOf(0, 100).forEach { resistance ->
            val result = ContentPackLoader.load(
                typedPack(enemies = baseEnemy().replace("scout.resist.arcane=33", "scout.resist.arcane=$resistance")),
            )

            assertTrue(result.isValid, "resistance=$resistance: ${result.errors.joinToString("\n")}")
            assertEquals(mapOf("arcane" to resistance), result.registry!!.requireEnemy("scout").resists)
        }
    }

    @Test
    fun invalidResistanceValuesHaveActionableDiagnostics() {
        listOf("-1", "101", "not-an-int").forEach { value ->
            val result = ContentPackLoader.load(
                typedPack(enemies = baseEnemy().replace("scout.resist.arcane=33", "scout.resist.arcane=$value")),
            )

            assertFalse(result.isValid, "resistance=$value must be rejected")
            assertTrue(
                result.errors.any {
                    it.file == "enemies.properties" &&
                        it.id == "scout" &&
                        it.field == "resist.arcane" &&
                        it.message.contains("0 to 100")
                },
                "resistance=$value: ${result.errors.joinToString("\n")}",
            )
        }
    }

    @Test
    fun diagnosticsAreDeterministicAcrossRepeatedLoads() {
        val root = typedPack(
            towers = baseTower().replace("basic.damageTypeId=arcane", "basic.damageTypeId=shadow"),
            enemies = baseEnemy().replace("scout.resist.arcane=33", "scout.resist.shadow=101"),
        )

        val first = ContentPackLoader.load(root)
        val second = ContentPackLoader.load(root)

        assertEquals(first.errors, second.errors)
    }

    @Test
    fun legacyPackWithoutDamageTypesRemainsCompatible() {
        val result = ContentPackLoader.load(legacyPack())

        assertTrue(result.isValid, result.errors.joinToString("\n"))
        assertTrue(result.registry!!.damageTypes.isEmpty())
        assertEquals(null, result.registry.requireTower("basic").damageTypeId)
        assertTrue(result.registry.requireEnemy("scout").resists.isEmpty())
    }

    private fun typedPack(
        towers: String = baseTower(),
        enemies: String = baseEnemy(),
        stringsExtra: String = "",
    ): Path = createPack(
        damageTypes = "arcane.displayKey=damage.arcane",
        towers = towers,
        enemies = enemies,
        stringsExtra = "\ndamage.arcane=Arcane damage$stringsExtra",
    )

    private fun legacyPack(): Path = createPack(
        damageTypes = null,
        towers = baseTower().replace("\nbasic.damageTypeId=arcane", ""),
        enemies = baseEnemy().replace("\nscout.resist.arcane=33", ""),
    )

    private fun baseTower(): String = """
        basic.displayKey=tower.basic
        basic.range=3
        basic.damage=7
        basic.cooldownTicks=2
        basic.costResource=bolt
        basic.costAmount=2
        basic.sellRefundRatio=0.5
        basic.targetingMode=nearest
        basic.damageTypeId=arcane
    """.trimIndent()

    private fun baseEnemy(): String = """
        scout.health=10
        scout.speedTilesPerTick=1
        scout.rewardResource=bolt
        scout.rewardAmount=1
        scout.coreDamage=1
        scout.resist.arcane=33
    """.trimIndent()

    private fun createPack(
        damageTypes: String?,
        towers: String,
        enemies: String,
        stringsExtra: String = "",
    ): Path = Files.createTempDirectory("myengine-damage-types-content").also { root ->
        root.resolve("manifest.properties").writeText(
            """
            id=damage-types-test
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
        damageTypes?.let { root.resolve("damage-types.properties").writeText(it) }
        root.resolve("resources.properties").writeText("bolt.displayKey=resource.bolt\n")
        root.resolve("towers.properties").writeText(towers)
        root.resolve("enemies.properties").writeText(enemies)
        root.resolve("recipes.properties").writeText("generator.outputResource=bolt\ngenerator.outputAmount=1\ngenerator.durationTicks=1\n")
        root.resolve("waves.properties").writeText("wave1.startTick=1\nwave1.spawns=scout:1\n")
        root.resolve("incidents.properties").writeText("spark.minThreat=0\nspark.maxThreat=3\nspark.weight=1\n")
        root.resolve("strings.properties").writeText(
            """
            resource.bolt=Bolt
            tower.basic=Basic tower
            hud.resources=Resources
            hud.wave=Wave
            hud.nextWave=Next wave
            hud.coreHealth=Core
            hud.build=Build
            hud.upgrade=Upgrade
            hud.damage=Damage
            hud.kills=Kills
            hud.tier=Tier
            $stringsExtra
            """.trimIndent(),
        )
    }
}
