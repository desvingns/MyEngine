package dev.myengine.devtools

import dev.myengine.games.sandbox.SandboxGame
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ReplayTrajectoryTest {
    private val fixtureRoot: Path = DevtoolReports.repoRoot()
        .resolve("engine-devtools/src/test/resources/replay-fixtures/dx-003")

    @Test
    fun repeatedTrajectoriesAreByteIdenticalAndHonorFinalHashContracts() {
        listOf("canonical", "kill", "resist").forEach { scenario ->
            val definition = ReplayDefinition(scenario)
            val first = ReplayTrajectoryCapture.capture(definition)
            val second = ReplayTrajectoryCapture.capture(definition)

            assertEquals(first.toJsonLines(), second.toJsonLines(), scenario)
            assertEquals(36, first.records.size, scenario)
            assertEquals(
                Files.readString(DevtoolReports.repoRoot().resolve("games/sandbox/src/test/resources/golden/$scenario.hash")).trim(),
                first.records.last().hash,
                scenario,
            )
        }
    }

    @Test
    fun checkedInFixturesParseAndMatchFreshCaptures() {
        listOf("canonical", "kill", "resist").forEach { scenario ->
            val expected = ReplayTrajectory.read(fixtureRoot.resolve("$scenario.jsonl"))
            val actual = ReplayTrajectoryCapture.capture(
                ReplayDefinition.fromMetadata(expected.metadata, DevtoolReports.repoRoot().resolve("games/sandbox/content/sandbox")),
            )
            val comparison = ReplayTrajectoryComparer.compare(expected, actual)

            assertEquals("match", comparison.status, scenario)
            assertEquals(0, comparison.exitCode, scenario)
        }
    }

    @Test
    fun parserRejectsWrongVersionAndNonConsecutiveRecords() {
        val canonical = Files.readString(fixtureRoot.resolve("canonical.jsonl"))

        assertFailsWith<ReplayTrajectoryFormatException> {
            ReplayTrajectory.parse(canonical.replace("dx-003-trajectory-v1", "dx-003-trajectory-v0"))
        }
        assertFailsWith<ReplayTrajectoryFormatException> {
            ReplayTrajectory.parse(canonical.replaceFirst("\"factory\":\"sandbox\"", "\"factory\":\"sandbox\",\"unexpected\":true"))
        }
        assertFailsWith<ReplayTrajectoryFormatException> {
            ReplayTrajectory.parse(canonical.replaceFirst("\"tick\":1", "\"tick\":2"))
        }
    }

    @Test
    fun parserRejectsMissingAndDuplicateRecords() {
        val lines = Files.readString(fixtureRoot.resolve("canonical.jsonl")).lineSequence().toList()
        val metadata = lines.first()
        val records = lines.drop(1)

        assertFailsWith<ReplayTrajectoryFormatException> {
            ReplayTrajectory.parse(
                listOf(
                    metadata,
                    records.filterNot { it.startsWith("{\"tick\":1,") }.joinToString("\n"),
                ).joinToString("\n"),
            )
        }
        assertFailsWith<ReplayTrajectoryFormatException> {
            ReplayTrajectory.parse(
                listOf(
                    metadata,
                    (records.take(2) + records[1] + records.drop(2)).joinToString("\n"),
                ).joinToString("\n"),
            )
        }
    }

    @Test
    fun comparerReportsDeterministicSemanticDiffAndHashOnlyFallback() {
        val actual = ReplayTrajectoryCapture.capture(ReplayDefinition("canonical"))
        val perturbed = ReplayTrajectory.read(fixtureRoot.resolve("canonical-perturbed.jsonl"))
        val semantic = ReplayTrajectoryComparer.compare(perturbed, actual)

        assertEquals("divergence", semantic.status)
        assertEquals(1, semantic.exitCode)
        assertEquals(5L, semantic.firstDivergentTick)
        assertEquals("0000000000000000", semantic.expectedHash)
        assertEquals(actual.records[5].hash, semantic.actualHash)
        assertEquals(listOf("defense_metrics.core_health"), semantic.changedFields)
        assertEquals(semantic.changedFields, ReplayTrajectoryComparer.compare(perturbed, actual).changedFields)

        val hashOnlyText = Files.readString(fixtureRoot.resolve("canonical.jsonl"))
            .replaceFirst("\"tick\":5, \"hash\":\"da9e2402246fcb48\"", "\"tick\":5, \"hash\":\"0000000000000000\"")
            .replaceFirst("\"hash\":\"da9e2402246fcb48\"}}", "\"hash\":\"0000000000000000\"}}")
        val hashOnly = ReplayTrajectoryComparer.compare(ReplayTrajectory.parse(hashOnlyText), actual)

        assertEquals(listOf("hash_only"), hashOnly.changedFields)

        val twoFieldText = Files.readString(fixtureRoot.resolve("canonical.jsonl"))
            .lineSequence()
            .map { line ->
                if (line.startsWith("{\"tick\":5,")) {
                    line.replace("\"hash\":\"da9e2402246fcb48\"", "\"hash\":\"0000000000000000\"")
                        .replace("\"core_health\":20", "\"core_health\":19")
                        .replace("\"bolt\":4", "\"bolt\":99")
                } else {
                    line
                }
            }
            .joinToString("\n")
        val twoField = ReplayTrajectoryComparer.compare(ReplayTrajectory.parse(twoFieldText), actual)

        assertEquals(
            listOf("defense_metrics.core_health", "inventories.global.bolt"),
            twoField.changedFields,
        )
    }

    @Test
    fun captureUsesTickOneStepsAndStopsAtRequestedTick() {
        val oneTick = ReplayTrajectoryCapture.capture(ReplayDefinition("canonical", requestedTicks = 1))
        val full = ReplayTrajectoryCapture.capture(ReplayDefinition("canonical", requestedTicks = 35))
        val zeroTick = ReplayTrajectoryCapture.capture(ReplayDefinition("canonical", requestedTicks = 0))

        assertEquals(listOf(0L, 1L), oneTick.records.map { it.tick })
        assertEquals(full.records[1], oneTick.records[1])
        assertEquals(listOf(0L), zeroTick.records.map { it.tick })
        assertEquals(SandboxGame.runScriptedScenario().hash, full.records.last().hash)
    }

    @Test
    fun captureStopsWhenStepOneNoLongerAdvancesTerminalScenario() {
        val factory = object : HeadlessScenarioFactory {
            override val id: String = "sandbox"

            override fun create(packRoot: Path, scenarioId: String, seed: Long): HeadlessScenario =
                object : HeadlessScenario {
                    private var tick = 0L

                    override val scenarioId: String = scenarioId
                    override val packId: String? = "terminal-fixture"

                    override fun submitScriptCommand(command: String) = Unit

                    override fun step(ticks: Int) {
                        repeat(ticks) {
                            if (tick < 2L) tick += 1L
                        }
                    }

                    override fun asciiFrame(): String = ""

                    override fun stateDump(): HeadlessStateDump = HeadlessStateDump(
                        tick = tick,
                        entities = emptyList(),
                        inventories = mapOf("global" to emptyMap()),
                        defenseMetrics = HeadlessDefenseDump(20, 0, 0, 0, 0, 0),
                        hash = tick.toString().padStart(16, '0'),
                    )
                }
        }

        val trajectory = ReplayTrajectoryCapture.capture(
            ReplayDefinition("canonical", requestedTicks = 35),
            factory,
        )

        assertEquals(listOf(0L, 1L, 2L), trajectory.records.map { it.tick })
        assertEquals(2L, trajectory.records.last().tick)
    }

    @Test
    fun cliReturnsGateFriendlyCodesForMatchDivergenceAndInvalidArguments() {
        val match = DevtoolReports.runReplayCommand(arrayOf("replay-bisect", fixtureRoot.resolve("canonical.jsonl").toString()))
        val divergence = DevtoolReports.runReplayCommand(arrayOf("replay-bisect", fixtureRoot.resolve("canonical-perturbed.jsonl").toString()))
        val invalid = DevtoolReports.runReplayCommand(arrayOf("replay-bisect"))
        val malformed = DevtoolReports.runReplayCommand(arrayOf("replay-bisect", fixtureRoot.resolve("malformed.jsonl").toString()))
        val trajectory = DevtoolReports.runReplayCommand(arrayOf("replay-inspect", "--trajectory", "canonical", "1"))
        val unknownScenario = DevtoolReports.runReplayCommand(arrayOf("replay-inspect", "--trajectory", "unknown"))

        assertEquals(0, match.exitCode)
        assertTrue(match.output.contains("\"status\":\"match\""), match.output)
        assertEquals(1, divergence.exitCode)
        assertTrue(divergence.output.contains("\"first_divergent_tick\":5"), divergence.output)
        assertEquals(2, invalid.exitCode)
        assertTrue(invalid.output.contains("\"status\":\"invalid\""), invalid.output)
        assertEquals(2, malformed.exitCode)
        assertTrue(malformed.output.contains("\"invalid_fixture\""), malformed.output)
        assertEquals(2, unknownScenario.exitCode)
        assertTrue(unknownScenario.output.contains("\"invalid_arguments_or_runtime\""), unknownScenario.output)
        assertEquals(0, trajectory.exitCode)
        assertEquals(3, trajectory.output.trim().lineSequence().count())
        assertEquals(1L, ReplayTrajectory.parse(trajectory.output).records.last().tick)
    }
}
