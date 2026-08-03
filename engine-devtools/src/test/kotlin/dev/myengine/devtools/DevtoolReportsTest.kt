package dev.myengine.devtools

import dev.myengine.games.sandbox.SandboxGame
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DevtoolReportsTest {
    @Test
    fun scenarioReportIsMachineReadable() {
        val json = DevtoolReports.runSandboxScenario().toJson()

        assertTrue(json.contains("\"final_hash\""))
        assertTrue(json.contains("\"sim_ms\""))
        assertTrue(json.contains("\"scenario\":\"canonical\""))
    }

    @Test
    fun canonicalScenarioKillsNothing() {
        // Documents the gap that motivated the second scenario: the canonical (30,32) tower never
        // reaches the enemy corridor within 35 ticks, so it never exercises the reward path.
        val report = DevtoolReports.runSandboxScenario()

        assertEquals(0, report.enemiesKilled)
        assertEquals(0, report.towerShots)
    }

    @Test
    fun killScenarioExercisesKillAndRewardPath() {
        // The default gate now includes a scenario that actually kills, so kills+shots are gated.
        val report = DevtoolReports.runSandboxKillScenario()

        assertTrue(report.enemiesKilled > 0, "kill scenario must kill enemies")
        assertTrue(report.towerShots > 0, "kill scenario must fire the tower")
    }

    @Test
    fun scenarioSuiteReportsBothScenarios() {
        val json = DevtoolReports.runScenarioSuite()

        assertTrue(json.contains("\"scenario\":\"canonical\""), json)
        assertTrue(json.contains("\"scenario\":\"kill\""), json)
    }

    @Test
    fun endlessScalingReportIsDeterministicAndMachineReadable() {
        val root = Files.createTempDirectory("myengine-endless-scaling")
        copyFlatPack(SandboxGame.contentRoot(), root)
        Files.copy(SandboxGame.contentRoot().resolve("maps.json"), root.resolve("maps.json"), REPLACE_EXISTING)
        Files.writeString(
            root.resolve("maps.json"),
            Files.readString(root.resolve("maps.json")).replace("finite_waves", "no_win"),
        )
        Files.writeString(
            root.resolve("endless.properties"),
            """
            startTick=3
            intervalTicks=4
            compositionCycle=drift:2
            countGrowthPercent=125
            healthGrowthPercent=110
            rewardGrowthPercent=105
            """.trimIndent(),
        )

        val first = DevtoolReports.endlessWaveScalingReport(root, waveCount = 3, seed = 11L)
        val second = DevtoolReports.endlessWaveScalingReport(root, waveCount = 3, seed = 11L)
        val parsed = Json.parseToJsonElement(first.toJson()).jsonObject

        assertTrue(first.valid, first.errors.joinToString("\n"))
        assertEquals(first, second)
        assertEquals("sandbox", first.packId)
        assertEquals(3, first.rows.size)
        assertEquals(3, parsed.getValue("wave_count").jsonPrimitive.content.toInt())
        assertEquals(3, parsed.getValue("rows").jsonArray.size)
        assertTrue(first.rows.zipWithNext().all { (left, right) -> right.startTick > left.startTick })
        assertTrue(first.rows.zipWithNext().all { (left, right) -> right.totalHealth >= left.totalHealth })
    }

    @Test
    fun replayInspectReportsBothScenarios() {
        val json = DevtoolReports.replayInspect()

        assertTrue(json.contains("\"scenario\":\"canonical\""), json)
        assertTrue(json.contains("\"scenario\":\"kill\""), json)
        assertTrue(json.contains("\"enemies_killed\""), json)
    }

    @Test
    fun proceduralMapReportIsDeterministicAndIncludesAsciiAndSeed() {
        val first = DevtoolReports.proceduralMapReport(seed = 41L)
        val second = DevtoolReports.proceduralMapReport(seed = 41L)
        val parsed = Json.parseToJsonElement(first.toJson()).jsonObject

        assertEquals(first, second)
        assertEquals(41L, first.seed)
        assertEquals("sandbox-canonical-generated", first.mapId)
        assertTrue(first.ascii.contains('C'))
        assertTrue(first.ascii.contains('#'))
        assertEquals(41L, parsed.getValue("seed").jsonPrimitive.content.toLong())
        assertEquals(first.hash, parsed.getValue("hash").jsonPrimitive.content)
    }

    @Test
    fun proceduralMapCommandPrintsOneJsonObject() {
        val text = captureStdout { main(arrayOf("procedural-map", "43")) }.trim()
        val parsed = Json.parseToJsonElement(text).jsonObject

        assertTrue(text.startsWith("{") && text.endsWith("}"), text)
        assertEquals(43L, parsed.getValue("seed").jsonPrimitive.content.toLong())
        assertTrue(parsed.getValue("ascii").jsonPrimitive.content.contains('C'))
    }

    @Test
    fun goalFieldBenchmarkReportsCanonical64x64StableJsonShape() {
        val json = DevtoolReports.goalFieldRebuildBenchmark().toJson()
        val parsed = Json.parseToJsonElement(json).jsonObject

        assertEquals(setOf("width", "height", "reachable_tiles", "rebuild_ns"), parsed.keys)
        assertEquals(64, parsed.getValue("width").jsonPrimitive.content.toInt())
        assertEquals(64, parsed.getValue("height").jsonPrimitive.content.toInt())
        assertTrue(parsed.getValue("reachable_tiles").jsonPrimitive.content.toInt() >= 0)
        assertTrue(parsed.getValue("rebuild_ns").jsonPrimitive.content.toLong() >= 0L)
    }

    @Test
    fun spatialIndexBenchmarkReportsOneKContractAsStructuredJson() {
        val report = DevtoolReports.spatialIndexBenchmark(enemyCount = 1_000, towerCount = 4)
        val parsed = Json.parseToJsonElement(report.toJson()).jsonObject

        assertEquals(
            setOf(
                "scenario",
                "enemy_count",
                "concurrent_enemies",
                "tower_count",
                "ticks",
                "query_count",
                "tower_shots",
                "alive_enemies_after",
                "elapsed_ns",
                "sim_ms",
            ),
            parsed.keys,
        )
        assertEquals("spatial-index-1k", parsed.getValue("scenario").jsonPrimitive.content)
        assertEquals(1_000, parsed.getValue("enemy_count").jsonPrimitive.content.toInt())
        assertEquals(1_000, parsed.getValue("concurrent_enemies").jsonPrimitive.content.toInt())
        assertEquals(4, parsed.getValue("tower_count").jsonPrimitive.content.toInt())
        assertEquals(1, parsed.getValue("ticks").jsonPrimitive.content.toInt())
        assertEquals(4, parsed.getValue("query_count").jsonPrimitive.content.toInt())
        assertTrue(parsed.getValue("tower_shots").jsonPrimitive.content.toInt() in 0..4)
        assertTrue(parsed.getValue("alive_enemies_after").jsonPrimitive.content.toInt() in 0..1_000)
        assertTrue(parsed.getValue("elapsed_ns").jsonPrimitive.content.toLong() >= 0L)
        assertTrue(parsed.getValue("sim_ms").jsonPrimitive.content.toDouble() >= 0.0)
    }

    @Test
    fun beltTransportBenchmarkReportsOneHundredBeltContract() {
        val report = DevtoolReports.beltTransportBenchmark()

        assertEquals(100, report.beltCount)
        assertEquals(1_000, report.ticks)
        assertTrue(report.deliveredItems > 0)
        assertTrue(report.toJson().contains("\"belt_count\":100"))
    }

    @Test
    fun contentReportListsSandboxIds() {
        val report = DevtoolReports.contentReport()

        assertTrue(report.valid, report.errors.joinToString("\n"))
        assertTrue(report.ids.getValue("towers").contains("pulse"))
        assertTrue(report.ids.getValue("maps").contains("sandbox-canonical"))
    }

    @Test
    fun contentReportAllCoversEveryGamePack() {
        val report = DevtoolReports.contentReportAll()

        assertTrue(
            report.results.size >= 2,
            "expected sandbox + signal-garden packs, got ${report.results.map { it.root }}",
        )
        assertTrue(
            report.results.any { it.root.contains("signal-garden") },
            "default gate must include signal-garden: ${report.results.map { it.root }}",
        )
        assertTrue(
            report.valid,
            report.results.filterNot { it.report.valid }
                .joinToString("\n") { "${it.root}: ${it.report.errors}" },
        )
    }

    @Test
    fun balanceDeltaReportComparesBaselineAndChangedContent() {
        val report = DevtoolReports.balanceDeltaReport(
            baselineRoot = SandboxGame.contentRoot(),
            changedRoot = DevtoolReports.repoRoot().resolve("games/signal-garden/content/signal-garden"),
        )
        val json = report.toJson()

        assertTrue(report.valid, report.errors.joinToString("\n"))
        assertEquals("sandbox", report.baseline?.packId)
        assertEquals("signal-garden", report.changed?.packId)
        assertTrue(report.deltas.any { it.metric == "enemy_health_total" }, json)
        assertTrue(report.deltas.any { it.metric == "core_damage_potential" }, json)
        assertTrue(report.deltas.any { it.metric == "structure_attack_types" }, json)
        assertTrue(report.deltas.any { it.metric == "structure_damage_potential" }, json)
        assertTrue(report.deltas.any { it.metric == "reward_total" }, json)
        assertTrue(json.contains("\"baseline_root\""), json)
        assertTrue(json.contains("\"changed_root\""), json)
        assertTrue(json.contains("\"thresholds\""), json)
        assertTrue(json.contains("\"warnings\""), json)
    }

    @Test
    fun balanceReportExposesBossCountsAndEffectiveScaledRewards() {
        val changedRoot = Files.createTempDirectory("myengine-balance-boss")
        copyFlatPack(SandboxGame.contentRoot(), changedRoot)
        Files.writeString(
            changedRoot.resolve("enemies.properties"),
            Files.readString(changedRoot.resolve("enemies.properties")) +
                "\ndrift.isBoss=true\ndrift.healthScalePercent=200\n" +
                "drift.rewardScalePercent=150\n",
        )
        Files.writeString(
            changedRoot.resolve("waves.properties"),
            Files.readString(changedRoot.resolve("waves.properties")) +
                "\nwave-1.modifier.0.healthPercent=150\n" +
                "wave-1.modifier.0.speedPercent=100\nwave-1.modifier.0.count=1\n",
        )

        val report = DevtoolReports.balanceDeltaReport(SandboxGame.contentRoot(), changedRoot)
        val changed = requireNotNull(report.changed)

        assertTrue(report.valid, report.toJson())
        assertEquals(1, changed.bossEnemyTypes)
        assertTrue(changed.bossWaveEnemies > 0)
        assertTrue(changed.enemyHealthTotal > report.baseline!!.enemyHealthTotal)
        assertTrue(changed.rewardTotal > report.baseline.rewardTotal)
        assertTrue(report.deltas.any { it.metric == "boss_enemy_types" })
    }

    @Test
    fun balanceReportExposesContentDefinedStructureAttackPotential() {
        val changedRoot = Files.createTempDirectory("myengine-balance-structure")
        copyFlatPack(SandboxGame.contentRoot(), changedRoot)
        Files.writeString(
            changedRoot.resolve("enemies.properties"),
            Files.readString(changedRoot.resolve("enemies.properties")) +
                "\ndrift.attacksStructures=true\n",
        )

        val report = DevtoolReports.balanceDeltaReport(SandboxGame.contentRoot(), changedRoot)

        assertTrue(report.valid, report.toJson())
        assertEquals(1, report.changed!!.structureAttackTypes)
        assertTrue(report.changed!!.structureDamagePotential > 0, report.toJson())
        assertTrue(report.deltas.any { it.metric == "structure_damage_potential" }, report.toJson())
        assertTrue(report.toJson().contains("structure_damage_potential"), report.toJson())
    }

    @Test
    fun balanceDeltaReportJsonParsesAsStructuredObject() {
        val json = DevtoolReports.balanceDeltaReport(
            baselineRoot = SandboxGame.contentRoot(),
            changedRoot = DevtoolReports.repoRoot().resolve("games/signal-garden/content/signal-garden"),
        ).toJson()
        val parsed = Json.parseToJsonElement(json).jsonObject

        assertTrue(parsed.getValue("valid").jsonPrimitive.boolean)
        assertEquals("sandbox", parsed.getValue("baseline").jsonObject.getValue("pack_id").jsonPrimitive.content)
        assertEquals("signal-garden", parsed.getValue("changed").jsonObject.getValue("pack_id").jsonPrimitive.content)
        assertTrue(parsed.getValue("thresholds").jsonObject.containsKey("large_percent_delta"))
        assertTrue(parsed.getValue("deltas").jsonArray.isNotEmpty())
        assertTrue(parsed.getValue("warnings").jsonArray.isNotEmpty())
        assertTrue(parsed.getValue("errors").jsonArray.isEmpty())
    }

    @Test
    fun balanceReportCommandPrintsOneJsonObject() {
        val text = captureStdout { main(arrayOf("balance-report")) }.trim()
        val parsed = Json.parseToJsonElement(text).jsonObject

        assertTrue(text.startsWith("{") && text.endsWith("}"), text)
        assertTrue(parsed.getValue("valid").jsonPrimitive.boolean)
        assertEquals("sandbox", parsed.getValue("baseline").jsonObject.getValue("pack_id").jsonPrimitive.content)
        assertEquals("signal-garden", parsed.getValue("changed").jsonObject.getValue("pack_id").jsonPrimitive.content)
    }

    @Test
    fun balanceDeltaReportNoOpCopyHasNoWarnings() {
        val changedRoot = Files.createTempDirectory("myengine-balance-noop")
        copyFlatPack(SandboxGame.contentRoot(), changedRoot)

        val report = DevtoolReports.balanceDeltaReport(
            baselineRoot = SandboxGame.contentRoot(),
            changedRoot = changedRoot,
        )

        assertTrue(report.valid, report.errors.joinToString("\n"))
        assertTrue(report.warnings.isEmpty(), report.toJson())
        assertTrue(report.deltas.none { it.flagged }, report.toJson())
    }

    @Test
    fun balanceDeltaReportFlagsLargeEnemyCoreAndResourceDeltas() {
        val changedRoot = Files.createTempDirectory("myengine-balance-delta")
        copyFlatPack(SandboxGame.contentRoot(), changedRoot)
        Files.writeString(
            changedRoot.resolve("enemies.properties"),
            """
            drift.health=20
            drift.speedTilesPerTick=1
            drift.rewardResource=bolt
            drift.rewardAmount=10
            drift.coreDamage=10
            """.trimIndent() + "\n",
        )

        val report = DevtoolReports.balanceDeltaReport(
            baselineRoot = SandboxGame.contentRoot(),
            changedRoot = changedRoot,
        )
        val warnedCategories = report.warnings.map { it.category }.toSet()
        val warnedMetrics = report.warnings.map { it.metric }.toSet()

        assertTrue(report.valid, report.errors.joinToString("\n"))
        assertTrue("enemy" in warnedCategories, report.toJson())
        assertTrue("core" in warnedCategories, report.toJson())
        assertTrue("resource" in warnedCategories, report.toJson())
        assertTrue("enemy_health_total" in warnedMetrics, report.toJson())
        assertTrue("core_damage_potential" in warnedMetrics, report.toJson())
        assertTrue("reward_total" in warnedMetrics, report.toJson())
    }

    @Test
    fun balanceDeltaReportSummarizesIntegerEffectiveSplashAoeAndExposesItsJsonDeltas() {
        val changedRoot = Files.createTempDirectory("myengine-balance-splash")
        copyFlatPack(SandboxGame.contentRoot(), changedRoot)
        Files.writeString(
            changedRoot.resolve("towers.properties"),
            Files.readString(changedRoot.resolve("towers.properties")) +
                "\npulse.splashRadius=2\npulse.falloff=50\n",
        )

        val report = DevtoolReports.balanceDeltaReport(
            baselineRoot = SandboxGame.contentRoot(),
            changedRoot = changedRoot,
        )
        val changed = requireNotNull(report.changed)
        val deltas = report.deltas.associateBy { it.metric }
        val json = Json.parseToJsonElement(report.toJson()).jsonObject
        val changedJson = json.getValue("changed").jsonObject
        val deltaJson = json.getValue("deltas").jsonArray.associateBy { it.jsonObject.getValue("metric").jsonPrimitive.content }

        assertTrue(report.valid, report.errors.joinToString("\n"))
        assertEquals(1, changed.splashTowerTypes)
        assertEquals(2, changed.splashRadiusTotal)
        assertEquals(50, changed.splashFalloffPercentTotal)
        // damage=2, falloff=50: distance 0 -> 1 tile, distance 1 -> 4 tiles, distance 2 -> 0 damage.
        assertEquals(5, changed.splashEffectiveAoeTiles)
        assertEquals("1", changedJson.getValue("splash_tower_types").jsonPrimitive.content)
        assertEquals("2", changedJson.getValue("splash_radius_total").jsonPrimitive.content)
        assertEquals("50", changedJson.getValue("splash_falloff_percent_total").jsonPrimitive.content)
        assertEquals("5", changedJson.getValue("splash_effective_aoe_tiles").jsonPrimitive.content)
        assertEquals(1.0, deltas.getValue("splash_tower_types").delta)
        assertEquals(2.0, deltas.getValue("splash_radius_total").delta)
        assertEquals(50.0, deltas.getValue("splash_falloff_percent_total").delta)
        assertEquals(5.0, deltas.getValue("splash_effective_aoe_tiles").delta)
        assertTrue(!deltas.getValue("splash_tower_types").flagged)
        assertTrue(!deltas.getValue("splash_radius_total").flagged)
        assertTrue(deltas.getValue("splash_falloff_percent_total").flagged)
        assertTrue(deltas.getValue("splash_effective_aoe_tiles").flagged)
        assertTrue(deltaJson.getValue("splash_falloff_percent_total").jsonObject.getValue("flagged").jsonPrimitive.boolean)
        assertTrue(deltaJson.getValue("splash_effective_aoe_tiles").jsonObject.getValue("flagged").jsonPrimitive.boolean)
    }

    @Test
    fun balanceDeltaReportInvalidChangedPackReturnsErrors() {
        val changedRoot = Files.createTempDirectory("myengine-balance-invalid")
        copyFlatPack(SandboxGame.contentRoot(), changedRoot)
        Files.delete(changedRoot.resolve("enemies.properties"))

        val report = DevtoolReports.balanceDeltaReport(
            baselineRoot = SandboxGame.contentRoot(),
            changedRoot = changedRoot,
        )

        assertTrue(!report.valid, report.toJson())
        assertTrue(report.errors.any { it.startsWith("changed:") }, report.toJson())
        assertTrue(report.deltas.isEmpty(), report.toJson())
    }

    private fun copyFlatPack(source: java.nio.file.Path, target: java.nio.file.Path) {
        Files.createDirectories(target)
        listOf(
            "manifest.properties",
            "tiles.properties",
            "resources.properties",
            "recipes.properties",
            "towers.properties",
            "enemies.properties",
            "waves.properties",
            "incidents.properties",
            "strings.properties",
        ).forEach { file ->
            Files.copy(source.resolve(file), target.resolve(file), REPLACE_EXISTING)
        }
        source.resolve("buildings.properties").takeIf(Files::exists)?.let { file ->
            Files.copy(file, target.resolve(file.fileName.toString()), REPLACE_EXISTING)
        }
        source.resolve("visuals").takeIf(Files::exists)?.let { visuals ->
            Files.walk(visuals).use { files ->
                files.filter(Files::isRegularFile).forEach { file ->
                    val destination = target.resolve(source.relativize(file).toString())
                    destination.parent?.let(Files::createDirectories)
                    Files.copy(file, destination, REPLACE_EXISTING)
                }
            }
        }
    }

    private fun captureStdout(block: () -> Unit): String {
        val originalOut = System.out
        val out = ByteArrayOutputStream()
        System.setOut(PrintStream(out))
        return try {
            block()
            out.toString()
        } finally {
            System.setOut(originalOut)
        }
    }
}
