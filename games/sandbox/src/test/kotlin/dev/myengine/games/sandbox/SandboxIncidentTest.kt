package dev.myengine.games.sandbox

import dev.myengine.content.IncidentContent
import dev.myengine.content.IncidentEffectDescriptor
import dev.myengine.core.Tick
import dev.myengine.logistics.Inventory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SandboxIncidentTest {
    private fun registry(incident: IncidentContent) =
        SandboxGame.loadRegistry().copy(incidents = mapOf(incident.id to incident))

    @Test
    fun typedEffectsExecuteInDeclaredOrderExactlyOnce() {
        val runtime = SandboxGame.createRuntime(
            registry(
                IncidentContent(
                    id = "storm",
                    minThreat = 0,
                    maxThreat = 100,
                    weight = 1,
                    cadenceStartTick = 1,
                    cadenceIntervalTicks = 1,
                    cooldownTicks = 100,
                    effects = listOf(
                        IncidentEffectDescriptor.ResourceEvent("bolt", 1),
                        IncidentEffectDescriptor.Modifier("storm-pressure", 3, 4),
                        IncidentEffectDescriptor.SpawnWave("wave-1"),
                    ),
                ),
            ),
            seed = 19,
        )

        runtime.step(2)

        assertEquals(1, runtime.state.incidentState.executions.size)
        assertEquals("storm", runtime.state.incidentState.executions.single().incidentId)
        assertEquals(7, runtime.state.inventory.amount("bolt"))
        assertEquals(setOf("wave-1"), runtime.state.defense.spawnedWaveIds)
        assertEquals(SandboxIncidentModifier(3, 3), runtime.state.incidentModifiers["storm-pressure"])
        assertTrue(runtime.state.lastCommandOrError == "incident_applied:storm")
    }

    @Test
    fun failedResourcePreflightIsAtomicAcrossAllEffects() {
        val runtime = SandboxGame.createRuntime(
            registry(
                IncidentContent(
                    id = "atomic",
                    minThreat = 0,
                    maxThreat = 100,
                    weight = 1,
                    cadenceStartTick = 1,
                    cadenceIntervalTicks = 1,
                    effects = listOf(
                        IncidentEffectDescriptor.SpawnWave("wave-1"),
                        IncidentEffectDescriptor.ResourceEvent("bolt", 1),
                        IncidentEffectDescriptor.Modifier("atomic", 1, 2),
                    ),
                ),
            ),
        )
        runtime.state.inventory = Inventory(mapOf("bolt" to 6), capacity = 6)
        val beforeCursor = runtime.state.randomCursor

        runtime.step()

        assertEquals(beforeCursor, runtime.state.randomCursor)
        assertTrue(runtime.state.incidentState.executions.isEmpty())
        assertTrue(runtime.state.defense.spawnedWaveIds.isEmpty())
        assertTrue(runtime.state.incidentModifiers.isEmpty())
        assertEquals(6, runtime.state.inventory.amount("bolt"))
        assertEquals("incident_rejected:inventory_capacity", runtime.state.lastCommandOrError)
    }

    @Test
    fun repeatedResourceEventsRejectAggregateOverflowWithoutMutation() {
        val runtime = SandboxGame.createRuntime(
            registry(
                IncidentContent(
                    id = "resource-overflow",
                    minThreat = 0,
                    maxThreat = 100,
                    weight = 1,
                    cadenceStartTick = 1,
                    cadenceIntervalTicks = 1,
                    effects = listOf(
                        IncidentEffectDescriptor.SpawnWave("wave-1"),
                        IncidentEffectDescriptor.ResourceEvent("bolt", Int.MAX_VALUE),
                        IncidentEffectDescriptor.ResourceEvent("bolt", Int.MAX_VALUE),
                        IncidentEffectDescriptor.Modifier("pressure", 1, 3),
                    ),
                ),
            ),
            seed = 23,
        )
        val beforeInventory = runtime.state.inventory
        val beforeCursor = runtime.state.randomCursor

        runtime.step()

        assertEquals(beforeInventory, runtime.state.inventory)
        assertEquals(beforeCursor, runtime.state.randomCursor)
        assertTrue(runtime.state.defense.spawnedWaveIds.isEmpty())
        assertTrue(runtime.state.incidentModifiers.isEmpty())
        assertEquals("incident_rejected:resource_amount_overflow:bolt", runtime.state.lastCommandOrError)
    }

    @Test
    fun repeatedModifierEventsRejectNearIntMinAggregateWrapWithoutMutation() {
        val runtime = SandboxGame.createRuntime(
            registry(
                IncidentContent(
                    id = "modifier-overflow",
                    minThreat = 0,
                    maxThreat = 100,
                    weight = 1,
                    cadenceStartTick = 1,
                    cadenceIntervalTicks = 1,
                    effects = listOf(
                        IncidentEffectDescriptor.SpawnWave("wave-1"),
                        IncidentEffectDescriptor.Modifier("pressure", Int.MAX_VALUE, durationTicks = 1),
                        IncidentEffectDescriptor.Modifier("pressure", Int.MAX_VALUE, durationTicks = 1),
                    ),
                ),
            ),
            seed = 29,
        )
        val beforeCursor = runtime.state.randomCursor

        runtime.step()

        assertEquals(beforeCursor, runtime.state.randomCursor)
        assertTrue(runtime.state.defense.spawnedWaveIds.isEmpty())
        assertTrue(runtime.state.incidentModifiers.isEmpty())
        assertEquals("incident_rejected:modifier_amount_overflow:pressure", runtime.state.lastCommandOrError)
    }

    @Test
    fun repeatedEffectsCanReachIntMaxWithoutFalseOverflow() {
        val runtime = SandboxGame.createRuntime(
            registry(
                IncidentContent(
                    id = "bounds",
                    minThreat = 0,
                    maxThreat = 100,
                    weight = 1,
                    cadenceStartTick = 1,
                    cadenceIntervalTicks = 1,
                    effects = listOf(
                        IncidentEffectDescriptor.ResourceEvent("bolt", 1),
                        IncidentEffectDescriptor.ResourceEvent("bolt", 1),
                        IncidentEffectDescriptor.Modifier("pressure", 1, 2),
                        IncidentEffectDescriptor.Modifier("pressure", 1, 3),
                    ),
                ),
            ),
            seed = 31,
        )
        runtime.state.inventory = Inventory(mapOf("bolt" to Int.MAX_VALUE - 2))
        runtime.state.incidentModifiers = mapOf("pressure" to SandboxIncidentModifier(Int.MAX_VALUE - 2, 10))

        runtime.step()

        assertEquals(Int.MAX_VALUE, runtime.state.inventory.amount("bolt"))
        assertEquals(SandboxIncidentModifier(Int.MAX_VALUE, 9), runtime.state.incidentModifiers["pressure"])
        assertEquals("incident_applied:bounds", runtime.state.lastCommandOrError)
    }

    @Test
    fun v11SaveRestoresRngDirectorAndContinuation() {
        val incident = IncidentContent(
            id = "pulse",
            minThreat = 0,
            maxThreat = 100,
            weight = 1,
            cadenceStartTick = 1,
            cadenceIntervalTicks = 2,
            cooldownTicks = 3,
            effects = listOf(IncidentEffectDescriptor.ResourceEvent("bolt", 1)),
        )
        val registry = registry(incident)
        val uninterrupted = SandboxGame.createRuntime(registry, seed = 41).also { it.step(7) }
        val paused = SandboxGame.createRuntime(registry, seed = 41).also { it.step(3) }
        val save = SandboxSaveCodec.encode(paused.state, seed = 41)
        val restored = SandboxRuntime(SandboxSaveCodec.decode(save, registry), seed = 41)
        restored.step(4)

        assertEquals(12, SandboxSaveCodec.SAVE_VERSION)
        assertEquals(uninterrupted.state.stableHash(), restored.state.stableHash())
        assertEquals(uninterrupted.state.incidentState, restored.state.incidentState)
        assertEquals(uninterrupted.state.randomCursor, restored.state.randomCursor)
        assertTrue(save.contains("randomCursor="))
        assertTrue(save.contains("incidentExecutions="))
    }

    @Test
    fun v9MigrationUsesDeterministicEmptyIncidentDefaults() {
        val registry = SandboxGame.loadRegistry()
        val current = SandboxSaveCodec.encode(SandboxGame.createInitialState(registry), seed = 41)
        val legacy = current.lines()
            .filterNot { it.startsWith("saveVersion=") || it.startsWith("randomCursor=") || it.startsWith("incident") }
            .plus("saveVersion=9")
            .joinToString("\n")

        val first = SandboxSaveCodec.decode(legacy, registry)
        val second = SandboxSaveCodec.decode(legacy, registry)

        assertEquals(first.stableHash(), second.stableHash())
        assertTrue(first.incidentState.executions.isEmpty())
        assertTrue(first.incidentModifiers.isEmpty())
        assertEquals(41L, first.randomCursor)
    }
}
