package dev.myengine.games.sandbox

import java.io.StringReader
import java.io.StringWriter
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals

/** Pins the version-neutral raw fixture used by scripts/perf/RuntimeSessionBenchmark.java. */
class SandboxRuntimeBenchmarkContractTest {
    @Test
    fun benchmarkFixturesRunBothAcceptedScenariosFromAnActivePendingCommand() {
        val registry = SandboxGame.loadRegistry()
        val emptySave = SandboxSession.start(registry, seed = 7).save()
        val scenarios = listOf(
            Triple("pulse:30:32", "12a65fd2b87593cf", 0),
            Triple("pulse:2:2", "bb37eefc1903cc77", 2),
        )
        scenarios.forEach { (payload, expectedHash, expectedKills) ->
            val properties = Properties().apply {
                load(StringReader(emptySave))
                setProperty("pendingCommands", "build_tower|1|1||$payload")
            }
            val text = StringWriter().also { properties.store(it, "ENG-036 benchmark fixture") }.toString()
            val session = SandboxSession.restore(text, registry)

            assertEquals(0L, session.runtime.state.tick.value)
            assertEquals(1, session.runtime.pendingCommands().size)
            session.step(35)

            assertEquals(35L, session.runtime.state.tick.value)
            assertEquals(expectedHash, session.stableHash())
            assertEquals(expectedKills, session.runtime.state.defense.metrics.enemiesKilled)
        }
    }
}
