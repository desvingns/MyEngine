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
import kotlin.test.assertNull

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
    fun waveSpawnSelectionSupportsDefaultAllAndNamedIdsPreservingAuthoredOrder() {
        val twoSpawnMap = listOf(
            "entry" to MapCoordinate(0, 0),
            "west" to MapCoordinate(4, 4),
        )

        val defaultRoot = createPack()
        defaultRoot.resolve("maps.json").writeText(mapJson(spawns = twoSpawnMap))
        val defaultResult = ContentPackLoader.load(defaultRoot)

        assertTrue(defaultResult.isValid, defaultResult.errors.joinToString("\n"))
        assertNull(defaultResult.registry!!.waves.getValue("wave1").spawnSelection)

        val allRoot = createPack()
        allRoot.resolve("maps.json").writeText(mapJson(spawns = twoSpawnMap))
        allRoot.resolve("waves.properties").writeText(
            allRoot.resolve("waves.properties").toFile().readText() +
                "\nwave1.spawnSelection=all\n",
        )
        val allResult = ContentPackLoader.load(allRoot)

        assertTrue(allResult.isValid, allResult.errors.joinToString("\n"))
        assertNull(allResult.registry!!.waves.getValue("wave1").spawnSelection)

        val namedRoot = createPack()
        namedRoot.resolve("maps.json").writeText(mapJson(spawns = twoSpawnMap))
        namedRoot.resolve("waves.properties").writeText(
            namedRoot.resolve("waves.properties").toFile().readText() +
                "\nwave1.spawnSelection=entry|west\n",
        )
        val namedResult = ContentPackLoader.load(namedRoot)

        assertTrue(namedResult.isValid, namedResult.errors.joinToString("\n"))
        assertEquals(
            listOf("entry", "west"),
            namedResult.registry!!.waves.getValue("wave1").spawnSelection,
        )
    }

    @Test
    fun waveSpawnSelectionRejectsBlankDuplicateMixedAndUnknownIds() {
        val invalidSelections = listOf(
            "wave1.spawnSelection=" to "cannot be blank",
            "wave1.spawnSelection=entry|" to "cannot be blank",
            "wave1.spawnSelection=entry|west|entry" to "duplicate ids",
            "wave1.spawnSelection=all|entry" to "either 'all' or a named spawn id list",
            "wave1.spawnSelection=missing" to "Unknown spawn 'missing'",
        )

        invalidSelections.forEach { (field, message) ->
            val root = createPack()
            root.resolve("maps.json").writeText(
                mapJson(
                    spawns = listOf(
                        "entry" to MapCoordinate(0, 0),
                        "west" to MapCoordinate(4, 4),
                    ),
                ),
            )
            root.resolve("waves.properties").writeText(
                root.resolve("waves.properties").toFile().readText() + "\n$field\n",
            )

            val result = ContentPackLoader.load(root)

            assertFalse(result.isValid, "Expected invalid selection '$field' to be rejected.")
            assertTrue(
                result.errors.any {
                    it.file == "waves.properties" && it.id == "wave1" &&
                        it.field == "spawnSelection" && it.message.contains(message)
                },
                "$field:\n${result.errors.joinToString("\n")}",
            )
        }
    }

    @Test
    fun mapSpawnIdsRejectExactAllAndPipeDelimiterButKeepMixedCaseNames() {
        val reserved = createPack()
        reserved.resolve("maps.json").writeText(
            mapJson(spawns = listOf("all" to MapCoordinate(0, 0))),
        )

        val reservedResult = ContentPackLoader.load(reserved)

        assertFalse(reservedResult.isValid)
        assertTrue(
            reservedResult.errors.any {
                it.file == "maps.json" && it.id == "test-map" &&
                    it.field == "spawns[0].id" && it.message.contains("reserved")
            },
            reservedResult.errors.joinToString("\n"),
        )

        val delimiter = createPack()
        delimiter.resolve("maps.json").writeText(
            mapJson(spawns = listOf("north|west" to MapCoordinate(0, 0))),
        )

        val delimiterResult = ContentPackLoader.load(delimiter)

        assertFalse(delimiterResult.isValid)
        assertTrue(
            delimiterResult.errors.any {
                it.file == "maps.json" && it.id == "test-map" &&
                    it.field == "spawns[0].id" && it.message.contains("delimiter")
            },
            delimiterResult.errors.joinToString("\n"),
        )

        val mixedCase = createPack()
        mixedCase.resolve("maps.json").writeText(
            mapJson(spawns = listOf("All" to MapCoordinate(0, 0))),
        )
        mixedCase.resolve("waves.properties").writeText(
            mixedCase.resolve("waves.properties").toFile().readText() +
                "\nwave1.spawnSelection=All\n",
        )

        val mixedCaseResult = ContentPackLoader.load(mixedCase)

        assertTrue(mixedCaseResult.isValid, mixedCaseResult.errors.joinToString("\n"))
        val mixedCaseRegistry = mixedCaseResult.registry!!
        assertEquals(
            listOf("All"),
            mixedCaseRegistry.waves.getValue("wave1").spawnSelection,
        )
        assertTrue(mixedCaseRegistry.maps.getValue("test-map").spawns.containsKey("All"))
    }

    @Test
    fun eliteBossScalingAndIndexedWaveModifiersAreDataDefined() {
        val root = createPack()
        root.resolve("enemies.properties").writeText(
            root.resolve("enemies.properties").toFile().readText() +
                "\nscout.isBoss=true\nscout.healthScalePercent=150\n" +
                "scout.speedScalePercent=200\nscout.rewardScalePercent=125\n",
        )
        root.resolve("waves.properties").writeText(
            root.resolve("waves.properties").toFile().readText() +
                "\nwave1.modifier.0.healthPercent=200\nwave1.modifier.0.speedPercent=50\n" +
                "wave1.modifier.0.count=1\n",
        )

        val result = ContentPackLoader.load(root)

        assertTrue(result.isValid, result.errors.joinToString("\n"))
        val enemy = result.registry!!.requireEnemy("scout")
        assertTrue(enemy.isBoss)
        assertEquals(150, enemy.healthScalePercent)
        assertEquals(200, enemy.speedScalePercent)
        assertEquals(125, enemy.rewardScalePercent)
        assertEquals(listOf(WaveModifier(200, 50, 1)), result.registry.waves.getValue("wave1").modifiers)
    }

    @Test
    fun eliteBossAndWaveModifierValidationRejectInvalidValues() {
        val bothRanks = createPack()
        bothRanks.resolve("enemies.properties").writeText(
            bothRanks.resolve("enemies.properties").toFile().readText() +
                "\nscout.isElite=true\nscout.isBoss=true\n",
        )
        val bothRanksResult = ContentPackLoader.load(bothRanks)
        assertFalse(bothRanksResult.isValid)
        assertTrue(bothRanksResult.errors.any { it.id == "scout" && it.field == "isBoss" })

        val invalidModifier = createPack()
        invalidModifier.resolve("waves.properties").writeText(
            invalidModifier.resolve("waves.properties").toFile().readText() +
                "\nwave1.modifier.1.healthPercent=0\nwave1.modifier.1.speedPercent=101\n" +
                "wave1.modifier.1.count=1\n",
        )
        val invalidModifierResult = ContentPackLoader.load(invalidModifier)
        assertFalse(invalidModifierResult.isValid)
        assertTrue(invalidModifierResult.errors.any { it.field == "modifier" })
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
            "marker.displayKey=building.marker\n" +
                "marker.costResource=bolt\nmarker.costAmount=2\nmarker.maxHealth=20\n" +
                "marker.footprintWidth=1\nmarker.footprintHeight=1\nmarker.sellRefundRatio=0.5\n" +
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
            "marker.displayKey=building.marker\n" +
                "marker.costResource=bolt\nmarker.costAmount=2\nmarker.maxHealth=20\n" +
                "marker.footprintWidth=1\nmarker.footprintHeight=1\nmarker.sellRefundRatio=0.5\n" +
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
    fun buildingWallContractLoadsAndValidatesLocalizationAndReferences() {
        val valid = createPack()
        valid.resolve("buildings.properties").writeText(
            "wall.displayKey=building.wall\n" +
                "wall.costResource=bolt\nwall.costAmount=3\nwall.maxHealth=20\n" +
                "wall.footprintWidth=1\nwall.footprintHeight=1\nwall.sellRefundRatio=0.5\n",
        )
        valid.resolve("strings.properties").writeText(
            valid.resolve("strings.properties").toFile().readText() + "\nbuilding.wall=Wall\n",
        )

        val loaded = ContentPackLoader.load(valid)

        assertTrue(loaded.isValid, loaded.errors.joinToString("\n"))
        assertEquals(
            BuildingContent(
                id = "wall",
                costResource = "bolt",
                costAmount = 3,
                maxHealth = 20,
                footprintWidth = 1,
                footprintHeight = 1,
                sellRefundRatio = BigDecimal("0.5"),
                displayKey = "building.wall",
            ),
            loaded.registry!!.requireBuilding("wall"),
        )

        val missingLocalization = createPack()
        missingLocalization.resolve("buildings.properties").writeText(
            "wall.displayKey=building.wall\n" +
                "wall.costResource=bolt\nwall.costAmount=3\nwall.maxHealth=20\n" +
                "wall.footprintWidth=1\nwall.footprintHeight=1\nwall.sellRefundRatio=0.5\n",
        )
        val missingResult = ContentPackLoader.load(missingLocalization)
        assertFalse(missingResult.isValid)
        assertTrue(
            missingResult.errors.any {
                it.file == "strings.properties" && it.id == "wall" &&
                    it.field == "displayKey" && it.message.contains("building.wall")
            },
            missingResult.errors.joinToString("\n"),
        )

        val invalidDefinition = createPack()
        invalidDefinition.resolve("buildings.properties").writeText(
            "wall.displayKey=building.wall\n" +
                "wall.costResource=missing\nwall.costAmount=0\nwall.maxHealth=0\n" +
                "wall.footprintWidth=2\nwall.footprintHeight=1\nwall.sellRefundRatio=1.1\n",
        )
        invalidDefinition.resolve("strings.properties").writeText(
            invalidDefinition.resolve("strings.properties").toFile().readText() + "\nbuilding.wall=Wall\n",
        )
        val invalidResult = ContentPackLoader.load(invalidDefinition)
        assertFalse(invalidResult.isValid)
        assertTrue(invalidResult.errors.any { it.id == "wall" && it.field == "footprint" })
    }

    @Test
    fun zeroCostBuildingReturnsStructuredValidationError() {
        val root = createPack()
        root.resolve("buildings.properties").writeText(
            "wall.displayKey=building.wall\n" +
                "wall.costResource=bolt\nwall.costAmount=0\nwall.maxHealth=20\n" +
                "wall.footprintWidth=1\nwall.footprintHeight=1\nwall.sellRefundRatio=0.5\n",
        )
        root.resolve("strings.properties").writeText(
            root.resolve("strings.properties").toFile().readText() + "\nbuilding.wall=Wall\n",
        )

        val result = try {
            ContentPackLoader.load(root)
        } catch (error: Throwable) {
            throw AssertionError("Zero-cost building must return structured validation errors, not throw.", error)
        }

        assertFalse(result.isValid)
        assertTrue(
            result.errors.any {
                it.file == "buildings.properties" && it.id == "wall" &&
                    it.field == "costAmount"
            },
            result.errors.joinToString("\n"),
        )
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
    fun endlessPropertiesLoadContentDefinedScheduleAndRequireNoWin() {
        val root = createPack()
        root.resolve("waves.properties").writeText("")
        root.resolve("maps.json").writeText(mapJson(terminalRules = """{ "winCondition": "no_win" }"""))
        root.resolve("endless.properties").writeText(
            """
            startTick=3
            intervalTicks=4
            compositionCycle=scout:2;scout:1
            countGrowthPercent=125
            healthGrowthPercent=110
            rewardGrowthPercent=105
            """.trimIndent(),
        )

        val result = ContentPackLoader.load(root)

        assertTrue(result.isValid, result.errors.joinToString("\n"))
        val endless = result.registry!!.endlessWave
        assertEquals(3L, endless?.startTick)
        assertEquals(4L, endless?.intervalTicks)
        assertEquals(2, endless?.compositionCycle?.size)
        assertEquals(125, endless?.countGrowthPercent)
    }

    @Test
    fun endlessPropertiesRejectFiniteWinCondition() {
        val root = createPack()
        root.resolve("waves.properties").writeText("")
        root.resolve("maps.json").writeText(mapJson())
        root.resolve("endless.properties").writeText(
            """
            startTick=3
            intervalTicks=4
            compositionCycle=scout:2
            countGrowthPercent=125
            healthGrowthPercent=110
            rewardGrowthPercent=105
            """.trimIndent(),
        )

        val result = ContentPackLoader.load(root)

        assertFalse(result.isValid)
        assertTrue(
            result.errors.any {
                it.file == "maps.json" && it.field == "terminalRules.winCondition" &&
                    it.message.contains("requires no_win")
            },
            result.errors.joinToString("\n"),
        )
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
        spawns: List<Pair<String, MapCoordinate>> = listOf("entry" to MapCoordinate(spawnX, spawnY)),
    ): String {
        val rows = terrainRows.joinToString(",\n") { "        \"$it\"" }
        val spawnEntries = spawns.joinToString(",\n") { (id, position) ->
            "                    { \"id\": \"$id\", \"x\": ${position.x}, \"y\": ${position.y} }"
        }
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
            $spawnEntries
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
            building.marker=Marker
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
