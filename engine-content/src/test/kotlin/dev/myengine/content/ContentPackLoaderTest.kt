package dev.myengine.content

import java.math.BigDecimal
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
        assertEquals(4, result.registry?.towers?.get("basic")?.upgradeTiers?.get(TowerUpgradeTier.key("main", 1))?.damage)
    }

    @Test
    fun missingTowerTierAndHudLocalizationKeysAreActionable() {
        listOf(
            Triple("tower.basic", "basic", "displayKey"),
            Triple("tower.basic.upgrade.main.1", "basic", "upgrade.main.1.displayKey"),
            Triple("hud.wave", "hud", "hud.wave"),
        ).forEach { (missingKey, expectedId, expectedField) ->
            val root = createPack()
            val strings = root.resolve("strings.properties").toFile().readLines()
                .filterNot { it.startsWith("$missingKey=") }
                .joinToString("\n")
            root.resolve("strings.properties").writeText(strings)

            val result = ContentPackLoader.load(root)

            assertFalse(result.isValid)
            assertTrue(
                result.errors.any { it.file == "strings.properties" && it.id == expectedId && it.field == expectedField },
                result.errors.joinToString("\n"),
            )
        }
    }

    @Test
    fun difficultyDefinitionsResolveWithDeterministicDecimalScaling() {
        val root = createPack()
        root.resolve("enemies.properties").writeText(
            """
            scout.health=10
            scout.speedTilesPerTick=1
            scout.rewardResource=bolt
            scout.rewardAmount=5
            scout.coreDamage=1
            """.trimIndent(),
        )
        root.resolve("waves.properties").writeText(
            """
            wave1.startTick=2
            wave1.spawns=scout:10
            """.trimIndent(),
        )
        root.resolve("difficulties.properties").writeText(
            """
            hard.healthMult=1.3
            hard.countMult=1.15
            hard.rewardMult=0.9
            hard.goldRateMult=0.9
            """.trimIndent(),
        )

        val result = ContentPackLoader.load(root)

        assertTrue(result.isValid, result.errors.joinToString("\n"))
        val registry = result.registry!!
        val difficulty = registry.difficulties.getValue("hard")
        assertEquals(BigDecimal("1.3"), difficulty.healthMult)
        assertEquals(13, registry.resolveDifficulty("hard").enemies.getValue("scout").health)
        assertEquals(11, registry.resolveDifficulty("hard").waves.getValue("wave1").spawns.single().count)
        // 5 * 0.9 * 0.9 = 4.05, rounded once at the final payout boundary.
        assertEquals(4, registry.resolveDifficulty("hard").enemies.getValue("scout").rewardAmount)
    }

    @Test
    fun malformedOrIncompleteDifficultyIsActionable() {
        val root = createPack()
        root.resolve("difficulties.properties").writeText(
            """
            hard.healthMult=not-a-decimal
            hard.countMult=1.15
            hard.rewardMult=0.9
            """.trimIndent(),
        )

        val result = ContentPackLoader.load(root)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.file == "difficulties.properties" && it.field == "healthMult" })

        root.resolve("difficulties.properties").writeText(
            """
            hard.healthMult=1.3
            hard.countMult=1.15
            hard.rewardMult=0.9
            """.trimIndent(),
        )
        val incomplete = ContentPackLoader.load(root)

        assertFalse(incomplete.isValid)
        assertTrue(incomplete.errors.any { it.file == "difficulties.properties" && it.field == "goldRateMult" })
    }

    @Test
    fun missingReferenceIsActionable() {
        val root = createPack()
        root.resolve("towers.properties").writeText(
            """
            basic.displayKey=tower.basic
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

    @Test
    fun missingUpgradeCostReferenceIsActionable() {
        val root = createPack()
        root.resolve("towers.properties").writeText(
            """
            basic.displayKey=tower.basic
            basic.range=3
            basic.damage=1
            basic.cooldownTicks=2
            basic.costResource=bolt
            basic.costAmount=2
            basic.upgrade.main.1.displayKey=tower.basic.upgrade.main.1
            basic.upgrade.main.1.range=4
            basic.upgrade.main.1.damage=4
            basic.upgrade.main.1.cooldownTicks=1
            basic.upgrade.main.1.costResource=missing
            basic.upgrade.main.1.costAmount=2
            """.trimIndent(),
        )

        val result = ContentPackLoader.load(root)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.file == "towers.properties" && it.field == "upgrade.main.1.costResource" })
    }

    @Test
    fun unsafeUpgradeBranchIsActionable() {
        val root = createPack()
        root.resolve("towers.properties").writeText(
            """
            basic.displayKey=tower.basic
            basic.range=3
            basic.damage=1
            basic.cooldownTicks=2
            basic.costResource=bolt
            basic.costAmount=2
            basic.upgrade.bad|branch.1.displayKey=tower.basic.upgrade.bad
            basic.upgrade.bad|branch.1.range=4
            basic.upgrade.bad|branch.1.damage=4
            basic.upgrade.bad|branch.1.cooldownTicks=1
            basic.upgrade.bad|branch.1.costResource=bolt
            basic.upgrade.bad|branch.1.costAmount=2
            """.trimIndent(),
        )

        val result = ContentPackLoader.load(root)

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.file == "towers.properties" && it.field == "upgrade.bad|branch.1.range" })
    }

    @Test
    fun mapsJsonLoadsStructuredMapDefinition() {
        val root = createPack()
        root.resolve("maps.json").writeText(mapJson())

        val result = ContentPackLoader.load(root)

        assertTrue(result.isValid, result.errors.joinToString("\n"))
        val map = result.registry!!.requireMap("test-map")
        assertEquals(5, map.width)
        assertEquals(5, map.height)
        assertEquals(MapCoordinate(2, 2), map.core)
        assertEquals(MapCoordinate(0, 0), map.primarySpawn.position)
        assertEquals("resource", map.terrainMapping.getValue('R').terrainId)
        assertEquals(MapResourceNode("bolt", 9), map.terrainMapping.getValue('R').resourceNode)
        assertEquals(MapWinCondition.FINITE_WAVES, map.terminalRules.winCondition)
        assertEquals(null, map.terminalRules.leakBudget)
    }

    @Test
    fun terminalRulesSupportFiniteAndNoWinModesWithPositiveLeakBudget() {
        val finiteRoot = createPack()
        finiteRoot.resolve("maps.json").writeText(
            mapJson(terminalRules = """{ "winCondition": "finite_waves", "leakBudget": 3 }"""),
        )

        val finite = ContentPackLoader.load(finiteRoot)

        assertTrue(finite.isValid, finite.errors.joinToString("\n"))
        val finiteRules = requireNotNull(finite.registry).requireMap().terminalRules
        assertEquals(MapWinCondition.FINITE_WAVES, finiteRules.winCondition)
        assertEquals(3, finiteRules.leakBudget)

        listOf("no_win", "endless").forEach { mode ->
            val root = createPack()
            root.resolve("maps.json").writeText(mapJson(terminalRules = """{ "winCondition": "$mode" }"""))

            val result = ContentPackLoader.load(root)

            assertTrue(result.isValid, "$mode: ${result.errors.joinToString("\n")}")
            assertEquals(MapWinCondition.NO_WIN, result.registry!!.requireMap().terminalRules.winCondition)
        }
    }

    @Test
    fun zeroWavePacksRequireNoWinOrEndlessTerminalRules() {
        val finiteRoot = createPack()
        finiteRoot.resolve("waves.properties").writeText("")
        finiteRoot.resolve("maps.json").writeText(mapJson(terminalRules = """{ "winCondition": "finite_waves" }"""))

        val finite = ContentPackLoader.load(finiteRoot)

        assertFalse(finite.isValid)
        assertTrue(
            finite.errors.any {
                it.file == "maps.json" &&
                    it.id == "test-map" &&
                    it.field == "terminalRules.winCondition" &&
                    it.message.contains("requires at least one declared wave")
            },
            finite.errors.joinToString("\n"),
        )

        listOf("no_win", "endless").forEach { mode ->
            val root = createPack()
            root.resolve("waves.properties").writeText("")
            root.resolve("maps.json").writeText(mapJson(terminalRules = """{ "winCondition": "$mode" }"""))

            val result = ContentPackLoader.load(root)

            assertTrue(result.isValid, "$mode: ${result.errors.joinToString("\n")}")
            assertTrue(requireNotNull(result.registry).waves.isEmpty())
        }
    }

    @Test
    fun terminalRulesRejectInvalidLeakBudgetsAndMalformedValues() {
        listOf(
            Triple("""{ "leakBudget": 0 }""", "terminalRules.leakBudget", "positive"),
            Triple("""{ "leakBudget": -1 }""", "terminalRules.leakBudget", "positive"),
            Triple("""{ "leakBudget": 1.5 }""", "terminalRules.leakBudget", "positive integer"),
            Triple("[]", "terminalRules", "Expected an object"),
            Triple("""{ "winCondition": "unknown" }""", "terminalRules.winCondition", "finite_waves"),
        ).forEach { (rule, field, message) ->
            assertInvalidMap(
                mapJson(terminalRules = rule),
                field = field,
                messageFragment = message,
            )
        }
    }

    @Test
    fun mapRowWidthFailureIsActionable() {
        assertInvalidMap(
            mapJson(terrainRows = listOf("....", ".....", "..CR.", ".....", ".....")),
            field = "terrainRows[0]",
            messageFragment = "Expected row width 5",
        )
    }

    @Test
    fun mapSpawnBoundsFailureIsActionable() {
        assertInvalidMap(
            mapJson(spawnX = 5),
            field = "spawns.entry",
            messageFragment = "outside 5x5",
        )
    }

    @Test
    fun mapUnknownTileFailureIsActionable() {
        assertInvalidMap(
            mapJson(floorTile = "missing-tile"),
            field = "terrainMapping...tile",
            messageFragment = "Unknown tile 'missing-tile'",
        )
    }

    @Test
    fun mapMustContainExactlyOneCore() {
        assertInvalidMap(
            mapJson(terrainRows = listOf(".....", ".....", "..R..", ".....", ".....")),
            field = "terrainRows",
            messageFragment = "Expected exactly one core tile, found 0",
        )
    }

    @Test
    fun mapSpawnMustHaveWalkablePathToCore() {
        assertInvalidMap(
            mapJson(terrainRows = listOf(".....", "#####", "..CR.", ".....", ".....")),
            field = "spawns.entry",
            messageFragment = "No walkable path from spawn 'entry' to core (2,2)",
        )
    }

    private fun assertInvalidMap(mapJson: String, field: String, messageFragment: String) {
        val root = createPack()
        root.resolve("maps.json").writeText(mapJson)

        val result = ContentPackLoader.load(root)

        assertFalse(result.isValid)
        assertTrue(
            result.errors.any {
                it.file == "maps.json" && it.id == "test-map" && it.field == field && it.message.contains(messageFragment)
            },
            result.errors.joinToString("\n"),
        )
    }

    private fun mapJson(
        terrainRows: List<String> = listOf(".....", ".....", "..CR.", ".....", "....."),
        spawnX: Int = 0,
        spawnY: Int = 0,
        floorTile: String = "floor",
        terminalRules: String? = null,
    ): String {
        val rows = terrainRows.joinToString(",\n") { "        \"$it\"" }
        val terminalRulesField = terminalRules?.let { ",\n                  \"terminalRules\": $it" }.orEmpty()
        return """
            {
              "maps": [
                {
                  "id": "test-map",
                  "width": 5,
                  "height": 5,
                  "terrainRows": [
            $rows
                  ],
                  "terrainMapping": {
                    ".": { "tile": "$floorTile" },
                    "#": { "tile": "wall" },
                    "C": { "tile": "core" },
                    "R": { "tile": "resource", "resource": { "id": "bolt", "amount": 9 } }
                  },
                  "spawns": [
                    { "id": "entry", "x": $spawnX, "y": $spawnY }
                  ],
                  "core": { "x": 2, "y": 2 }$terminalRulesField
                }
              ]
            }
        """.trimIndent()
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
            resource.buildable=false
            resource.blocksMovement=false
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
            basic.upgrade.main.1.displayKey=tower.basic.upgrade.main.1
            basic.upgrade.main.1.range=4
            basic.upgrade.main.1.damage=4
            basic.upgrade.main.1.cooldownTicks=1
            basic.upgrade.main.1.costResource=bolt
            basic.upgrade.main.1.costAmount=2
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
        root.resolve("strings.properties").writeText(
            """
            resource.bolt=Bolt
            tower.basic=Basic
            tower.basic.upgrade.main.1=Improved basic
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
