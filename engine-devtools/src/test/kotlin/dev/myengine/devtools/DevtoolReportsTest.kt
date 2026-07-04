package dev.myengine.devtools

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
    fun contentReportListsSandboxIds() {
        val report = DevtoolReports.contentReport()

        assertTrue(report.valid, report.errors.joinToString("\n"))
        assertTrue(report.ids.getValue("towers").contains("pulse"))
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
}
