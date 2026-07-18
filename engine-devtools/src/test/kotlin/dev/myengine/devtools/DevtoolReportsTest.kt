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
    fun replayInspectReportsBothScenarios() {
        val json = DevtoolReports.replayInspect()

        assertTrue(json.contains("\"scenario\":\"canonical\""), json)
        assertTrue(json.contains("\"scenario\":\"kill\""), json)
        assertTrue(json.contains("\"enemies_killed\""), json)
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
        assertTrue(report.deltas.any { it.metric == "reward_total" }, json)
        assertTrue(json.contains("\"baseline_root\""), json)
        assertTrue(json.contains("\"changed_root\""), json)
        assertTrue(json.contains("\"thresholds\""), json)
        assertTrue(json.contains("\"warnings\""), json)
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
