package dev.myengine.games.sandbox

import dev.myengine.content.ContentRegistry
import dev.myengine.content.MapTerminalRules
import dev.myengine.content.MapWinCondition
import dev.myengine.content.WaveContent
import dev.myengine.content.WaveSpawn
import dev.myengine.core.CommandId
import dev.myengine.core.RunStatus
import dev.myengine.core.TerminalReason
import dev.myengine.core.Tick
import dev.myengine.core.command.BuildTowerCommand
import dev.myengine.core.command.TileCoordinate
import dev.myengine.defense.DefenseState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Acceptance coverage for ENG-014 terminal rules, immutable snapshots, and completed-run saves. */
class SandboxTerminalRunTest {
    @Test
    fun coreHealthLossIsDeterministicAndCapturesStableTerminalSummary() {
        val first = runNaturalLoss(coreHealth = 1, leakBudget = null)
        val second = runNaturalLoss(coreHealth = 1, leakBudget = null)

        assertEquals(RunStatus.LOST, first.state.run.status)
        assertEquals(TerminalReason.CORE_HEALTH_EXHAUSTED, first.state.run.terminalReason)
        assertEquals(first.state.run.terminalTick, first.state.run.summary!!.ticks)
        assertEquals(first.state.run, second.state.run)
        assertEquals(first.state.stableHash(), second.state.stableHash())
    }

    @Test
    fun leakBudgetLossIsDeterministicBeforeCoreHealthExhaustion() {
        val first = runNaturalLoss(coreHealth = 100, leakBudget = 1)
        val second = runNaturalLoss(coreHealth = 100, leakBudget = 1)

        assertEquals(RunStatus.LOST, first.state.run.status)
        assertEquals(TerminalReason.LEAK_BUDGET_EXHAUSTED, first.state.run.terminalReason)
        assertEquals(1, first.state.run.summary!!.leaks)
        assertEquals(first.state.run, second.state.run)
        assertEquals(first.state.stableHash(), second.state.stableHash())
    }

    @Test
    fun finiteWavesWinOnlyAfterFinalWaveSpawnsAndLiveEnemiesClear() {
        val runtime = SandboxGame.createRuntime(singleWaveRegistry())
        runtime.submit(buildPulseAt(tick = 2, id = 1, x = 2, y = 2))

        runtime.step()

        assertEquals(RunStatus.ACTIVE, runtime.state.run.status)
        assertTrue(runtime.state.defense.spawnedWaveIds.contains("final-wave"))
        assertTrue(runtime.state.entities.byTag("enemy").isNotEmpty())

        runtime.step()

        assertEquals(RunStatus.WON, runtime.state.run.status)
        assertEquals(TerminalReason.ALL_WAVES_CLEARED, runtime.state.run.terminalReason)
        assertEquals(Tick(2), runtime.state.run.terminalTick)
        assertEquals(1, runtime.state.run.summary!!.waves)
        assertEquals(1, runtime.state.run.summary!!.kills)
        assertEquals(0, runtime.state.entities.byTag("enemy").size)
    }

    @Test
    fun noWinRulesNeverWinAfterConfiguredWavesAreCleared() {
        val runtime = SandboxGame.createRuntime(singleWaveRegistry(winCondition = MapWinCondition.NO_WIN))
        runtime.submit(buildPulseAt(tick = 2, id = 1, x = 2, y = 2))

        runtime.step(5)

        assertEquals(RunStatus.ACTIVE, runtime.state.run.status)
        assertEquals(null, runtime.state.run.terminalReason)
        assertTrue(runtime.state.entities.byTag("enemy").isEmpty())
    }

    @Test
    fun zeroStartTickWaveSpawnsOnFirstRuntimeStep() {
        val runtime = SandboxGame.createRuntime(
            singleWaveRegistry(winCondition = MapWinCondition.NO_WIN, waveStartTick = 0),
        )

        runtime.step()

        assertEquals(Tick(1), runtime.state.tick)
        assertEquals(setOf("final-wave"), runtime.state.defense.spawnedWaveIds)
        assertEquals(1, runtime.state.defense.metrics.enemiesSpawned)
        assertEquals(1, runtime.state.entities.byTag("enemy").size)
    }

    @Test
    fun terminalRunRejectsSubmittedAndQueuedCommandsWithoutMutation() {
        val runtime = terminalWinningRuntime()
        val beforeHash = runtime.state.stableHash()
        val beforeSummary = runtime.state.run.summary
        val beforeLastCommand = runtime.state.lastCommandOrError
        val queued = buildPulseAt(tick = 3, id = 2, x = 3, y = 3)

        assertEquals(listOf(queued), runtime.pendingCommands())
        assertFalse(runtime.submit(buildPulseAt(tick = 3, id = 3, x = 4, y = 4)))

        runtime.step(10)

        assertEquals(beforeHash, runtime.state.stableHash())
        assertEquals(beforeSummary, runtime.state.run.summary)
        assertEquals(beforeLastCommand, runtime.state.lastCommandOrError)
        assertEquals(listOf(queued), runtime.pendingCommands())
    }

    @Test
    fun terminalSaveRestoresPreTerminalFutureCommandWithoutExecutingOrAcceptingNewCommands() {
        val registry = singleWaveRegistry()
        val runtime = terminalWinningRuntime(registry)
        val pendingBeforeSave = runtime.pendingCommands()
        val terminalHash = runtime.state.stableHash()
        val terminalSummary = runtime.state.run.summary

        val restored = SandboxSession.restore(SandboxSession(runtime, seed = 41).save(), registry)

        assertEquals(pendingBeforeSave, restored.runtime.pendingCommands())
        assertFalse(restored.runtime.submit(buildPulseAt(tick = 4, id = 3, x = 4, y = 4)))

        restored.step(10)

        assertEquals(terminalHash, restored.stableHash())
        assertEquals(terminalSummary, restored.runtime.state.run.summary)
        assertEquals(pendingBeforeSave, restored.runtime.pendingCommands())
    }

    @Test
    fun snapshotProjectsIndependentActiveAndFrozenTerminalSummaries() {
        val runtime = SandboxGame.createRuntime(singleWaveRegistry())

        val active = runtime.snapshot()
        runtime.state.inventory = runtime.state.inventory.add("bolt", 99)

        assertEquals(RunStatus.ACTIVE, active.runStatus)
        assertEquals(null, active.terminalReason)
        assertEquals(Tick(0), active.runSummary.ticks)
        assertEquals(6, active.runSummary.resources.getValue("bolt"))

        val terminalRuntime = terminalWinningRuntime()
        val terminal = terminalRuntime.snapshot()
        val frozenSummary = assertNotNull(terminalRuntime.state.run.summary)
        terminalRuntime.step(10)

        assertEquals(RunStatus.WON, terminal.runStatus)
        assertEquals(TerminalReason.ALL_WAVES_CLEARED, terminal.terminalReason)
        assertEquals(frozenSummary, terminal.runSummary)
        assertEquals(Tick(2), terminal.terminalTick)
        assertEquals(terminal, terminalRuntime.snapshot())
    }

    @Test
    fun terminalSnapshotSummaryResourcesRejectMutationWithoutChangingRun() {
        val runtime = terminalWinningRuntime()
        val snapshot = runtime.snapshot()
        val beforeHash = runtime.state.stableHash()
        val beforeRun = runtime.state.run

        assertFailsWith<UnsupportedOperationException> {
            @Suppress("UNCHECKED_CAST")
            (snapshot.runSummary.resources as MutableMap<String, Int>)["bolt"] = 999
        }

        assertEquals(beforeHash, runtime.state.stableHash())
        assertEquals(beforeRun, runtime.state.run)
        assertEquals(beforeRun.summary, snapshot.runSummary)
    }

    @Test
    fun v9CompletedRunSaveRestorePreservesFrozenSummary() {
        val registry = singleWaveRegistry()
        val session = SandboxSession(terminalWinningRuntime(registry), seed = 41)
        val save = session.save()

        val restored = SandboxSession.restore(save, registry)

        assertEquals("10", saveProperty(save, "saveVersion"))
        assertEquals(session.runtime.state.run, restored.runtime.state.run)
        assertEquals(session.stableHash(), restored.stableHash())
        assertEquals(session.runtime.snapshot().runSummary, restored.runtime.snapshot().runSummary)
        assertEquals(session.runtime.snapshot().terminalTick, restored.runtime.snapshot().terminalTick)
        assertEquals(session.runtime.snapshot().terminalReason, restored.runtime.snapshot().terminalReason)
    }

    private fun terminalWinningRuntime(registry: ContentRegistry = singleWaveRegistry()): SandboxRuntime {
        val runtime = SandboxGame.createRuntime(registry)
        val queued = buildPulseAt(tick = 3, id = 2, x = 3, y = 3)
        runtime.submit(buildPulseAt(tick = 2, id = 1, x = 2, y = 2))
        runtime.submit(queued)
        runtime.step(2)
        assertEquals(RunStatus.WON, runtime.state.run.status, "test setup must reach a terminal victory")
        return runtime
    }

    private fun runNaturalLoss(coreHealth: Int, leakBudget: Int?): SandboxRuntime {
        val runtime = SandboxGame.createRuntime(singleWaveRegistry(leakBudget = leakBudget))
        runtime.state.defense = DefenseState(coreHealth = coreHealth)
        runtime.step(100)
        assertTrue(runtime.state.run.isTerminal, "test setup must reach a terminal loss")
        return runtime
    }

    private fun singleWaveRegistry(
        winCondition: MapWinCondition = MapWinCondition.FINITE_WAVES,
        leakBudget: Int? = null,
        waveStartTick: Long = 1,
    ): ContentRegistry {
        val base = SandboxGame.loadRegistry()
        val drift = base.enemies.getValue("drift").copy(health = 1)
        val map = base.requireMap().copy(terminalRules = MapTerminalRules(winCondition, leakBudget))
        return base.copy(
            enemies = base.enemies + (drift.id to drift),
            waves = mapOf("final-wave" to WaveContent("final-wave", waveStartTick, listOf(WaveSpawn(drift.id, 1)))),
            maps = mapOf(map.id to map),
        )
    }

    private fun buildPulseAt(tick: Long, id: Long, x: Int, y: Int): BuildTowerCommand =
        BuildTowerCommand(CommandId(id), Tick(tick), "pulse", TileCoordinate(x, y))

    private fun saveProperty(text: String, key: String): String? =
        java.util.Properties().also { it.load(java.io.StringReader(text)) }.getProperty(key)
}
