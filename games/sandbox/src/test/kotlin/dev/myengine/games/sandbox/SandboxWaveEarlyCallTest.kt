package dev.myengine.games.sandbox

import dev.myengine.content.ContentRegistry
import dev.myengine.content.WaveEarlyCallBonus
import dev.myengine.core.CommandId
import dev.myengine.core.Tick
import dev.myengine.core.command.CallWaveEarlyCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertIs

/** End-to-end acceptance coverage for ENG-030's preview, early-call, replay, and save paths. */
class SandboxWaveEarlyCallTest {
    @Test
    fun earlyCallSpawnsNextWaveImmediatelyDepositsBonusAndDoesNotDuplicateAtSchedule() {
        val registry = registryWithEarlyCallBonus()
        val baseline = SandboxGame.createRuntime(registry)
        baseline.step()

        val runtime = SandboxGame.createRuntime(registry)
        runtime.submit(CallWaveEarlyCommand(CommandId(1), Tick(1), actorId = 42L))
        runtime.step()

        assertEquals(Tick(1), runtime.state.tick)
        assertEquals(setOf("wave-1"), runtime.state.defense.spawnedWaveIds)
        assertEquals(3, runtime.state.defense.metrics.enemiesSpawned)
        assertEquals(3, runtime.state.entities.byTag("enemy").size)
        assertEquals(
            baseline.state.inventory.amount("bolt") + EARLY_CALL_BONUS,
            runtime.state.inventory.amount("bolt"),
        )
        assertEquals("wave_called:wave-1", runtime.state.lastCommandOrError)

        runtime.step(9)

        assertEquals(Tick(10), runtime.state.tick)
        assertEquals(3, runtime.state.defense.metrics.enemiesSpawned)
        assertEquals(3, runtime.state.entities.byTag("enemy").size)
        assertEquals(setOf("wave-1"), runtime.state.defense.spawnedWaveIds)
    }

    @Test
    fun earlyCallIsRejectedWhileAnyEnemyIsAliveWithoutCommandMutation() {
        val registry = registryWithEarlyCallBonus()
        val subject = startedWithEarlyCall(registry)
        val control = startedWithEarlyCall(registry)

        subject.submit(CallWaveEarlyCommand(CommandId(2), Tick(2), actorId = 99L))
        subject.step()
        control.step()

        assertEquals("wave_active", subject.state.lastCommandOrError)
        assertEquals(control.state.stableHash(), subject.state.stableHash())
        assertEquals(control.state.inventory, subject.state.inventory)
        assertEquals(control.state.defense, subject.state.defense)
        assertEquals(3, subject.state.defense.metrics.enemiesSpawned)
        assertEquals(setOf("wave-1"), subject.state.defense.spawnedWaveIds)
    }

    @Test
    fun earlyCallAtOrAfterScheduledTickIsRejectedWithoutBonusOrStateMutation() {
        val registry = registryWithEarlyCallBonus()

        val atStart = SandboxGame.createRuntime(registry)
        val atStartControl = SandboxGame.createRuntime(registry)
        atStart.submit(CallWaveEarlyCommand(CommandId(11), Tick(10), actorId = 501L))
        atStart.step(10)
        atStartControl.step(10)

        assertEquals(atStartControl.state.stableHash(), atStart.state.stableHash())
        assertEquals(atStartControl.state.defense, atStart.state.defense)
        assertEquals(atStartControl.state.inventory, atStart.state.inventory)
        assertEquals(atStartControl.state.entities.all(), atStart.state.entities.all())
        assertEquals("wave_already_due:wave-1", atStart.state.lastCommandOrError)

        val afterScheduled = SandboxGame.createRuntime(registry)
        val afterScheduledControl = SandboxGame.createRuntime(registry)
        afterScheduled.submit(CallWaveEarlyCommand(CommandId(12), Tick(11), actorId = 502L))
        afterScheduled.step(11)
        afterScheduledControl.step(11)

        assertEquals(afterScheduledControl.state.stableHash(), afterScheduled.state.stableHash())
        assertEquals(afterScheduledControl.state.defense, afterScheduled.state.defense)
        assertEquals(afterScheduledControl.state.inventory, afterScheduled.state.inventory)
        assertEquals(afterScheduledControl.state.entities.all(), afterScheduled.state.entities.all())
        assertEquals("wave_active", afterScheduled.state.lastCommandOrError)
    }

    @Test
    fun fixedTickEarlyCallReplayIsStableAndDiffersFromScheduledBaseline() {
        val registry = registryWithEarlyCallBonus()

        fun runEarlyCall(): String {
            val runtime = SandboxGame.createRuntime(registry)
            runtime.submit(CallWaveEarlyCommand(CommandId(1), Tick(1), actorId = 7L))
            runtime.step(35)
            return runtime.state.stableHash()
        }

        val first = runEarlyCall()
        val second = runEarlyCall()
        val baseline = SandboxGame.createRuntime(registry).also { it.step(35) }.state.stableHash()

        assertEquals(first, second)
        assertNotEquals(baseline, first)
    }

    @Test
    fun midCountdownSaveRestorePreservesHudHashAndPendingEarlyCallIdentity() {
        val registry = registryWithEarlyCallBonus()
        val earlyCall = CallWaveEarlyCommand(CommandId(17), Tick(5), actorId = 303L)

        val uninterrupted = SandboxSession.start(registry)
        uninterrupted.submit(earlyCall)
        uninterrupted.step(20)

        val paused = SandboxSession.start(registry)
        paused.submit(earlyCall)
        paused.step(3)
        val expectedHash = paused.stableHash()
        val expectedHud = paused.runtime.snapshot().hud
        assertEquals(7L, expectedHud.nextWaveInTicks)

        val save = paused.save()
        val restored = SandboxSession.restore(save, registry)
        val pending = SandboxSaveCodec.decodePendingCommands(save)

        assertEquals(expectedHash, restored.stableHash())
        assertEquals(expectedHud, restored.runtime.snapshot().hud)
        assertEquals(listOf(earlyCall), pending)
        val restoredCommand = assertIs<CallWaveEarlyCommand>(restored.runtime.pendingCommands().single())
        assertEquals(earlyCall.id, restoredCommand.id)
        assertEquals(earlyCall.scheduledTick, restoredCommand.scheduledTick)
        assertEquals(earlyCall.actorId, restoredCommand.actorId)
        assertEquals(earlyCall.stablePayload(), restoredCommand.stablePayload())

        restored.step(17)

        assertEquals(uninterrupted.stableHash(), restored.stableHash())
    }

    private fun startedWithEarlyCall(registry: ContentRegistry): SandboxRuntime =
        SandboxGame.createRuntime(registry).also { runtime ->
            runtime.submit(CallWaveEarlyCommand(CommandId(1), Tick(1), actorId = 42L))
            runtime.step()
        }

    private fun registryWithEarlyCallBonus(): ContentRegistry {
        val registry = SandboxGame.loadRegistry()
        val wave = registry.waves.getValue("wave-1")
        return registry.copy(
            waves = registry.waves + (
                wave.id to wave.copy(
                    earlyCallBonus = WaveEarlyCallBonus("bolt", EARLY_CALL_BONUS),
                )
                ),
        )
    }

    private companion object {
        const val EARLY_CALL_BONUS = 4
    }
}
