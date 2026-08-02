package dev.myengine.content

import java.math.BigDecimal
import dev.myengine.core.command.TargetingMode
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertIs

class ContentPackLoaderTest {
    @Test
    fun validPackLoadsRegistry() {
        val root = createPack()

        val result = ContentPackLoader.load(root)

        assertTrue(result.isValid, result.errors.joinToString("\n"))
        assertEquals("test-pack", result.registry?.manifest?.id)
        assertEquals("bolt", result.registry?.towers?.get("basic")?.costResource)
        assertEquals(BigDecimal("0.5"), result.registry?.towers?.get("basic")?.sellRefundRatio)
        assertEquals(4, result.registry?.towers?.get("basic")?.upgradeTiers?.get(TowerUpgradeTier.key("main", 1))?.damage)
        assertTrue(result.registry?.buildings?.isEmpty() == true)
    }

    @Test
    fun validSoundMappingsLoadWithNormalizedEventIdsAndRealPackFiles() {
        val root = createPack()
        Files.createDirectories(root.resolve("sounds"))
        root.resolve("sounds/shot.wav").writeText("shot fixture")
        root.resolve("sounds/wave.wav").writeText("wave fixture")
        root.resolve("sounds.properties").writeText(
            "shot=sounds/shot.wav\nwave_start=sounds/wave.wav\n",
        )

        val result = ContentPackLoader.load(root)

        assertTrue(result.isValid, result.errors.joinToString("\n"))
        assertEquals(
            mapOf(
                dev.myengine.core.GameplayEventType.SHOT to SoundRef("sounds/shot.wav"),
                dev.myengine.core.GameplayEventType.WAVE_START to SoundRef("sounds/wave.wav"),
            ),
            result.registry!!.sounds,
        )
    }

    @Test
    fun soundMappingsRejectUnknownEventIdMissingFileAndPathEscape() {
        val unknown = createPack()
        unknown.resolve("sounds.properties").writeText("laser=sounds/laser.wav\n")
        val unknownResult = ContentPackLoader.load(unknown)
        assertFalse(unknownResult.isValid)
        assertTrue(unknownResult.errors.any { it.file == "sounds.properties" && it.field == "eventId" && it.id == "laser" })

        val missing = createPack()
        missing.resolve("sounds.properties").writeText("shot=sounds/missing.wav\n")
        val missingResult = ContentPackLoader.load(missing)
        assertFalse(missingResult.isValid)
        assertTrue(
            missingResult.errors.any {
                it.file == "sounds.properties" && it.field == "path" &&
                    it.message.contains("does not exist")
            },
            missingResult.errors.joinToString("\n"),
        )

        val escaped = createPack()
        escaped.resolve("sounds.properties").writeText("shot=../outside.wav\n")
        val escapedResult = ContentPackLoader.load(escaped)
        assertFalse(escapedResult.isValid)
        assertTrue(
            escapedResult.errors.any {
                it.file == "sounds.properties" && it.field == "path" &&
                    it.message.contains("escapes the content-pack root")
            },
            escapedResult.errors.joinToString("\n"),
        )
    }

    @Test
    fun currentContentPacksRemainValidWithOptionalSoundMappings() {
        val roots = listOf(
            currentPackRoot("games/sandbox/content/sandbox"),
            currentPackRoot("games/signal-garden/content/signal-garden"),
        )

        roots.forEach { root ->
            val result = ContentPackLoader.load(root)
            assertTrue(result.isValid, "$root:\n${result.errors.joinToString("\n")}")
        }
    }

    @Test
    fun statusEffectsAreDataDefinedAndReferencesAreValidated() {
        val root = createPack()
        root.resolve("effects.properties").writeText(
            """
            slow.kind=slow
            slow.magnitude=40
            slow.durationTicks=3
            slow.stackingRule=refresh
            burn.kind=dot
            burn.magnitude=2
            burn.durationTicks=2
            burn.stackingRule=stack
            """.trimIndent(),
        )
        root.resolve("towers.properties").writeText(
            root.resolve("towers.properties").toFile().readText() + "\nbasic.effectId=slow\n",
        )

        val result = ContentPackLoader.load(root)

        assertTrue(result.isValid, result.errors.joinToString("\n"))
        assertEquals(StatusEffectKind.SLOW, result.registry!!.effects.getValue("slow").kind)
        assertEquals(StatusEffectStackingRule.STACK, result.registry.effects.getValue("burn").stackingRule)
        assertEquals("slow", result.registry.towers.getValue("basic").effectId)

        val unknownReference = createPack()
        unknownReference.resolve("towers.properties").writeText(
            unknownReference.resolve("towers.properties").toFile().readText() + "\nbasic.effectId=missing\n",
        )
        val unknownResult = ContentPackLoader.load(unknownReference)
        assertFalse(unknownResult.isValid)
        assertTrue(unknownResult.errors.any { it.field == "effectId" && it.message.contains("Unknown status effect") })

        val invalidDefinition = createPack()
        invalidDefinition.resolve("effects.properties").writeText(
            """
            slow.kind=slow
            slow.magnitude=101
            slow.durationTicks=3
            slow.stackingRule=ignore
            """.trimIndent(),
        )
        val invalidResult = ContentPackLoader.load(invalidDefinition)
        assertFalse(invalidResult.isValid)
        assertTrue(invalidResult.errors.any { it.field == "magnitude" })
    }

    @Test
    fun incidentsAreOptionalAndTypedEffectsValidateReferencesAndValues() {
        val noIncidents = createPack()
        Files.delete(noIncidents.resolve("incidents.properties"))
        val noIncidentResult = ContentPackLoader.load(noIncidents)
        assertTrue(noIncidentResult.isValid, noIncidentResult.errors.joinToString("\n"))
        assertTrue(noIncidentResult.registry!!.incidents.isEmpty())

        val valid = createPack()
        valid.resolve("incidents.properties").writeText(
            "spark.minThreat=0\nspark.maxThreat=5\nspark.weight=1\n" +
                "spark.cadenceStartTick=2\nspark.cadenceIntervalTicks=3\nspark.cooldownTicks=4\n" +
                "spark.effects=spawn_wave:wave1,resource_event:bolt:2,modifier:storm:3:5\n",
        )
        val validResult = ContentPackLoader.load(valid)
        assertTrue(validResult.isValid, validResult.errors.joinToString("\n"))
        val effects = validResult.registry!!.incidents.getValue("spark").effects
        assertIs<IncidentEffectDescriptor.SpawnWave>(effects[0])
        assertIs<IncidentEffectDescriptor.ResourceEvent>(effects[1])
        assertIs<IncidentEffectDescriptor.Modifier>(effects[2])

        val invalidRef = createPack()
        invalidRef.resolve("incidents.properties").writeText(
            "spark.minThreat=0\nspark.maxThreat=5\nspark.weight=1\n" +
                "spark.cadenceIntervalTicks=1\nspark.effects=spawn_wave:missing,resource_event:missing:0\n",
        )
        val invalidResult = ContentPackLoader.load(invalidRef)
        assertFalse(invalidResult.isValid)
        assertTrue(invalidResult.errors.any { it.message.contains("Unknown wave") })
        assertTrue(invalidResult.errors.any { it.message.contains("positive integer") })
    }

    @Test
    fun crossFieldIncidentValidationUsesDiagnosticsInsteadOfConstructorRequirements() {
        val invalidCases = listOf(
            "maxThreat" to "spark.minThreat=5\nspark.maxThreat=4\nspark.weight=1\n",
            "pacingMaxThreat" to "spark.minThreat=0\nspark.maxThreat=5\nspark.weight=1\n" +
                "spark.pacingMinThreat=5\nspark.pacingMaxThreat=4\n",
            "cadenceEndTick" to "spark.minThreat=0\nspark.maxThreat=5\nspark.weight=1\n" +
                "spark.cadenceStartTick=5\nspark.cadenceEndTick=4\n",
        )

        invalidCases.forEach { (field, content) ->
            val root = createPack()
            root.resolve("incidents.properties").writeText(content)

            val result = ContentPackLoader.load(root)

            assertFalse(result.isValid, "Expected invalid incident field $field to be diagnosed.")
            assertTrue(result.registry == null)
            assertTrue(result.errors.any { it.id == "spark" && it.field == field })
        }
    }

    @Test
    fun visualReferencesLoadForTilesTowersTiersEnemiesAndBuildings() {
        val root = createPack()
        Files.createDirectories(root.resolve("visuals"))
        root.resolve("visuals/placeholder.atlas").writeText(
            """
            tile.floor
            tower.basic
            tower.basic.main.1
            enemy.scout
            building.marker
            """.trimIndent(),
        )
        root.resolve("tiles.properties").writeText(
            root.resolve("tiles.properties").toFile().readText() +
                "\nfloor.atlasPath=visuals/placeholder.atlas\nfloor.atlasKey=tile.floor\n",
        )
        root.resolve("towers.properties").writeText(
            root.resolve("towers.properties").toFile().readText() +
                "\nbasic.atlasPath=visuals/placeholder.atlas\nbasic.atlasKey=tower.basic\n" +
                "basic.upgrade.main.1.atlasPath=visuals/placeholder.atlas\n" +
                "basic.upgrade.main.1.atlasKey=tower.basic.main.1\n",
        )
        root.resolve("enemies.properties").writeText(
            root.resolve("enemies.properties").toFile().readText() +
                "\nscout.atlasPath=visuals/placeholder.atlas\nscout.atlasKey=enemy.scout\n",
        )
        root.resolve("buildings.properties").writeText(
            "marker.atlasPath=visuals/placeholder.atlas\nmarker.atlasKey=building.marker\n",
        )

        val result = ContentPackLoader.load(root)

        assertTrue(result.isValid, result.errors.joinToString("\n"))
        assertEquals(VisualAssetRef("visuals/placeholder.atlas", "tile.floor"), result.registry!!.requireTile("floor").assetRef)
        assertEquals(VisualAssetRef("visuals/placeholder.atlas", "tower.basic"), result.registry.requireTower("basic").assetRef)
        assertEquals(
            VisualAssetRef("visuals/placeholder.atlas", "tower.basic.main.1"),
            result.registry.requireTower("basic").upgradeTiers.getValue(TowerUpgradeTier.key("main", 1)).assetRef,
        )
        assertEquals(VisualAssetRef("visuals/placeholder.atlas", "enemy.scout"), result.registry.requireEnemy("scout").assetRef)
        assertEquals(VisualAssetRef("visuals/placeholder.atlas", "building.marker"), result.registry.requireBuilding("marker").assetRef)
    }

    @Test
    fun validSpritePathReferencesLoadForTilesTowersTiersEnemiesAndBuildings() {
        val root = createPack()
        Files.createDirectories(root.resolve("visuals"))
        root.resolve("visuals/placeholder.sprite").writeText("original placeholder")
        root.resolve("tiles.properties").writeText(
            root.resolve("tiles.properties").toFile().readText() +
                "\nfloor.spritePath=visuals/placeholder.sprite\n",
        )
        root.resolve("towers.properties").writeText(
            root.resolve("towers.properties").toFile().readText() +
                "\nbasic.spritePath=visuals/placeholder.sprite\n" +
                "basic.upgrade.main.1.spritePath=visuals/placeholder.sprite\n",
        )
        root.resolve("enemies.properties").writeText(
            root.resolve("enemies.properties").toFile().readText() +
                "\nscout.spritePath=visuals/placeholder.sprite\n",
        )
        root.resolve("buildings.properties").writeText(
            "marker.spritePath=visuals/placeholder.sprite\n",
        )

        val result = ContentPackLoader.load(root)

        assertTrue(result.isValid, result.errors.joinToString("\n"))
        val expected = VisualAssetRef("visuals/placeholder.sprite")
        assertEquals(expected, result.registry!!.requireTile("floor").assetRef)
        assertEquals(expected, result.registry.requireTower("basic").assetRef)
        assertEquals(
            expected,
            result.registry.requireTower("basic").upgradeTiers.getValue(TowerUpgradeTier.key("main", 1)).assetRef,
        )
        assertEquals(expected, result.registry.requireEnemy("scout").assetRef)
        assertEquals(expected, result.registry.requireBuilding("marker").assetRef)
    }

    @Test
    fun visualReferenceFailuresIdentifyPackPathAndAtlasKey() {
        val missingFile = createPack()
        missingFile.resolve("tiles.properties").writeText(
            missingFile.resolve("tiles.properties").toFile().readText() +
                "\nfloor.spritePath=visuals/missing.sprite\n",
        )
        val missingFileResult = ContentPackLoader.load(missingFile)
        assertTrue(
            missingFileResult.errors.any {
                it.file == "tiles.properties" && it.id == "floor" && it.field == "spritePath" &&
                    it.message.contains("Pack 'test-pack'") && it.message.contains("visuals/missing.sprite")
            },
            missingFileResult.errors.joinToString("\n"),
        )

        val missingKey = createPack()
        Files.createDirectories(missingKey.resolve("visuals"))
        missingKey.resolve("visuals/placeholder.atlas").writeText("tile.floor\n")
        missingKey.resolve("tiles.properties").writeText(
            missingKey.resolve("tiles.properties").toFile().readText() +
                "\nfloor.atlasPath=visuals/placeholder.atlas\nfloor.atlasKey=tile.missing\n",
        )
        val missingKeyResult = ContentPackLoader.load(missingKey)
        assertTrue(
            missingKeyResult.errors.any {
                it.file == "tiles.properties" && it.id == "floor" && it.field == "atlasKey" &&
                    it.message.contains("visuals/placeholder.atlas") && it.message.contains("tile.missing")
            },
            missingKeyResult.errors.joinToString("\n"),
        )
    }

    @Test
    fun validWaveEarlyCallBonusLoadsAsContent() {
        val root = createPack()
        root.resolve("waves.properties").writeText(
            root.resolve("waves.properties").toFile().readText() +
                "\nwave1.earlyCallBonusResourceId=bolt\nwave1.earlyCallBonusAmount=3\n",
        )

        val result = ContentPackLoader.load(root)

        assertTrue(result.isValid, result.errors.joinToString("\n"))
        assertEquals(
            WaveEarlyCallBonus(resourceId = "bolt", amount = 3),
            result.registry!!.waves.getValue("wave1").earlyCallBonus,
        )
    }

    @Test
    fun waveEarlyCallBonusRequiresBothPairedFields() {
        listOf(
            "wave1.earlyCallBonusResourceId=bolt" to "earlyCallBonusAmount",
            "wave1.earlyCallBonusAmount=3" to "earlyCallBonusResourceId",
        ).forEach { (field, missingField) ->
            val root = createPack()
            root.resolve("waves.properties").writeText(
                root.resolve("waves.properties").toFile().readText() + "\n$field\n",
            )

            val result = ContentPackLoader.load(root)

            assertFalse(result.isValid, "missing $missingField must be rejected")
            assertTrue(
                result.errors.any {
                    it.file == "waves.properties" && it.id == "wave1" && it.field == missingField
                },
                result.errors.joinToString("\n"),
            )
        }
    }

    @Test
    fun waveEarlyCallBonusAmountMustBePositive() {
        listOf("0", "-1").forEach { amount ->
            val root = createPack()
            root.resolve("waves.properties").writeText(
                root.resolve("waves.properties").toFile().readText() +
                    "\nwave1.earlyCallBonusResourceId=bolt\nwave1.earlyCallBonusAmount=$amount\n",
            )

            val result = ContentPackLoader.load(root)

            assertFalse(result.isValid, "$amount must be rejected")
            assertTrue(
                result.errors.any {
                    it.file == "waves.properties" && it.id == "wave1" && it.field == "earlyCallBonusAmount"
                },
                result.errors.joinToString("\n"),
            )
        }
    }

    @Test
    fun waveEarlyCallBonusRejectsUnknownResource() {
        val root = createPack()
        root.resolve("waves.properties").writeText(
            root.resolve("waves.properties").toFile().readText() +
                "\nwave1.earlyCallBonusResourceId=unknown\nwave1.earlyCallBonusAmount=3\n",
        )

        val result = ContentPackLoader.load(root)

        assertFalse(result.isValid)
        assertTrue(
            result.errors.any {
                it.file == "waves.properties" && it.id == "wave1" && it.field == "earlyCallBonusResourceId" &&
                    it.message.contains("Unknown resource")
            },
            result.errors.joinToString("\n"),
        )
    }

    @Test
    fun targetingModeDefaultsToNearestWhenAbsentButRejectsInvalidValues() {
        val legacy = createPack()
        legacy.resolve("towers.properties").writeText(
            legacy.resolve("towers.properties").toFile().readText()
                .lines().filterNot { it.startsWith("basic.targetingMode=") }.joinToString("\n"),
        )

        val legacyResult = ContentPackLoader.load(legacy)

        assertTrue(legacyResult.isValid, legacyResult.errors.joinToString("\n"))
        assertEquals(TargetingMode.NEAREST, legacyResult.registry!!.requireTower("basic").targetingMode)

        listOf("invalid", "").forEach { invalid ->
            val root = createPack()
            root.resolve("towers.properties").writeText(
                root.resolve("towers.properties").toFile().readText()
                    .replace("basic.targetingMode=nearest", "basic.targetingMode=$invalid"),
            )

            val result = ContentPackLoader.load(root)

            assertFalse(result.isValid, "$invalid must be rejected")
            assertTrue(
                result.errors.any { it.file == "towers.properties" && it.id == "basic" && it.field == "targetingMode" },
                result.errors.joinToString("\n"),
            )
        }
    }

    @Test
    fun splashFieldsMustBePairedAndWithinIntegerRanges() {
        val valid = createPack()
        valid.resolve("towers.properties").writeText(
            valid.resolve("towers.properties").toFile().readText() +
                "\nbasic.splashRadius=2\nbasic.falloff=50\n",
        )

        val validResult = ContentPackLoader.load(valid)

        assertTrue(validResult.isValid, validResult.errors.joinToString("\n"))
        assertEquals(2, validResult.registry!!.requireTower("basic").splashRadius)
        assertEquals(50, validResult.registry.requireTower("basic").falloffPercent)

        listOf(
            "basic.splashRadius=0" to "splashRadius",
            "basic.splashRadius=-1" to "splashRadius",
            "basic.splashRadius=not-an-int" to "splashRadius",
            "basic.falloff=25" to "falloff",
            "basic.splashRadius=1\nbasic.falloff=-1" to "falloff",
            "basic.splashRadius=1\nbasic.falloff=101" to "falloff",
            "basic.splashRadius=1\nbasic.falloff=half" to "falloff",
        ).forEach { (fields, field) ->
            val root = createPack()
            root.resolve("towers.properties").writeText(
                root.resolve("towers.properties").toFile().readText() + "\n$fields\n",
            )

            val result = ContentPackLoader.load(root)

            assertFalse(result.isValid, "$fields must be rejected")
            assertTrue(
                result.errors.any { it.file == "towers.properties" && it.id == "basic" && it.field == field },
                result.errors.joinToString("\n"),
            )
        }
    }

    @Test
    fun sellRefundRatioIsRequiredAndLimitedToInclusiveUnitInterval() {
        listOf("0", "1").forEach { ratio ->
            val root = createPack()
            root.resolve("towers.properties").writeText(
                root.resolve("towers.properties").toFile().readText().replace("basic.sellRefundRatio=0.5", "basic.sellRefundRatio=$ratio"),
            )

            val result = ContentPackLoader.load(root)

            assertTrue(result.isValid, "$ratio: ${result.errors.joinToString("\\n")}")
            assertEquals(BigDecimal(ratio), result.registry!!.requireTower("basic").sellRefundRatio)
        }

        listOf(null, "not-a-decimal", "-0.01", "1.01").forEach { ratio ->
            val root = createPack()
            val towerFile = root.resolve("towers.properties")
            towerFile.writeText(
                towerFile.toFile().readText().let { text ->
                    if (ratio == null) text.lines().filterNot { it.startsWith("basic.sellRefundRatio=") }.joinToString("\n")
                    else text.replace("basic.sellRefundRatio=0.5", "basic.sellRefundRatio=$ratio")
                },
            )

            val result = ContentPackLoader.load(root)

            assertFalse(result.isValid, "$ratio must be rejected")
            assertTrue(
                result.errors.any { it.file == "towers.properties" && it.id == "basic" && it.field == "sellRefundRatio" },
                result.errors.joinToString("\\n"),
            )
        }
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
            basic.sellRefundRatio=0.5
            basic.targetingMode=nearest
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
            basic.sellRefundRatio=0.5
            basic.targetingMode=nearest
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
            basic.sellRefundRatio=0.5
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
            basic.sellRefundRatio=0.5
            basic.targetingMode=nearest
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

    private fun currentPackRoot(relativePath: String): Path {
        val cwd = Paths.get("").toAbsolutePath()
        return generateSequence(cwd) { it.parent }
            .map { it.resolve(relativePath) }
            .firstOrNull { Files.isDirectory(it) }
            ?: error("Could not locate current content pack '$relativePath' from $cwd")
    }
}
