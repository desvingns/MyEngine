package dev.myengine.games.sandbox

import dev.myengine.ai.GoalField
import dev.myengine.content.IncidentEffectDescriptor
import dev.myengine.core.GameplayEvent
import dev.myengine.core.Tick
import dev.myengine.defense.DefenseRuntime
import dev.myengine.storyteller.IncidentSelection
import dev.myengine.world.TilePosition

data class IncidentEffectApplication(
    val applied: Boolean,
    val reason: String? = null,
)

private data class ModifierAggregate(
    val amount: Long,
    val durationTicks: Int,
)

/**
 * Sandbox-owned interpreter for content-neutral incident effects.
 *
 * All references, amounts, capacity, and arithmetic are checked before the first authoritative
 * mutation. Repeated resource and modifier effects are aggregated by id in sorted order before
 * range checks, so their application cannot wrap an Int. Once preflight succeeds, spawn effects
 * are applied in declared order and aggregate maps are canonicalized at the state boundary.
 */
class SandboxIncidentEffectInterpreter(
    private val defenseRuntime: DefenseRuntime,
) {
    fun apply(
        selection: IncidentSelection,
        state: SandboxState,
        spawn: TilePosition,
        core: TilePosition,
        goalField: GoalField,
        tick: Tick,
        eventSink: MutableList<GameplayEvent>,
        spawnRoutes: Map<String, TilePosition> = emptyMap(),
        airGoalField: GoalField? = null,
    ): IncidentEffectApplication {
        val effects = selection.effects.toList()
        val resourceTotals = effects.filterIsInstance<IncidentEffectDescriptor.ResourceEvent>()
            .groupingBy { it.resourceId }
            .fold(0L) { total, effect -> total + effect.amount.toLong() }
            .toSortedMap()
        val modifierTotals = effects.filterIsInstance<IncidentEffectDescriptor.Modifier>()
            .groupingBy { it.modifierId }
            .fold(ModifierAggregate(0L, 0)) { aggregate, effect ->
                ModifierAggregate(
                    amount = aggregate.amount + effect.amount.toLong(),
                    durationTicks = maxOf(aggregate.durationTicks, effect.durationTicks),
                )
            }
            .toSortedMap()

        resourceTotals.forEach { (resourceId, total) ->
            if (!state.registry.resources.containsKey(resourceId)) {
                return IncidentEffectApplication(false, "unknown_resource:$resourceId")
            }
            if (total !in 1L..Int.MAX_VALUE.toLong() ||
                state.inventory.amount(resourceId).toLong() + total > Int.MAX_VALUE.toLong()
            ) {
                return IncidentEffectApplication(false, "resource_amount_overflow:$resourceId")
            }
        }
        val capacity = state.inventory.capacity
        if (capacity != null &&
            state.inventory.resources.values.sumOf { it.toLong() } + resourceTotals.values.sumOf { it } > capacity.toLong()
        ) {
            return IncidentEffectApplication(false, "inventory_capacity")
        }
        effects.filterIsInstance<IncidentEffectDescriptor.SpawnWave>()
            .forEach { effect ->
                if (!state.registry.waves.containsKey(effect.waveId)) {
                    return IncidentEffectApplication(false, "unknown_wave:${effect.waveId}")
                }
            }
        modifierTotals.forEach { (modifierId, aggregate) ->
            val currentAmount = state.incidentModifiers[modifierId]?.amount?.toLong() ?: 0L
            if (aggregate.amount !in 1L..Int.MAX_VALUE.toLong() ||
                currentAmount + aggregate.amount > Int.MAX_VALUE.toLong()
            ) {
                return IncidentEffectApplication(false, "modifier_amount_overflow:$modifierId")
            }
        }

        var nextDefense = state.defense
        effects.filterIsInstance<IncidentEffectDescriptor.SpawnWave>().forEach { effect ->
            val wave = state.registry.waves.getValue(effect.waveId)
            nextDefense = defenseRuntime.spawnWave(
                wave = wave,
                state = nextDefense,
                registry = state.registry,
                world = state.world,
                entities = state.entities,
                spawn = spawn,
                core = core,
                goalField = goalField,
                airGoalField = airGoalField,
                tick = tick,
                eventSink = eventSink,
                spawnRoutes = spawnRoutes,
            )
        }
        var nextInventory = state.inventory
        resourceTotals.forEach { (resourceId, total) ->
            nextInventory = nextInventory.add(resourceId, total.toInt())
        }
        val nextModifiers = state.incidentModifiers.toMutableMap()
        modifierTotals.forEach { (modifierId, aggregate) ->
            val existing = nextModifiers[modifierId]
            nextModifiers[modifierId] = SandboxIncidentModifier(
                amount = ((existing?.amount?.toLong() ?: 0L) + aggregate.amount).toInt(),
                remainingTicks = maxOf(existing?.remainingTicks ?: 0, aggregate.durationTicks),
            )
        }
        state.defense = nextDefense
        state.inventory = nextInventory
        state.incidentModifiers = nextModifiers.toSortedMap()
        return IncidentEffectApplication(true)
    }
}
