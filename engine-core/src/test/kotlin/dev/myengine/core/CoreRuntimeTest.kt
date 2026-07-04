package dev.myengine.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

private data class CounterState(var value: Int = 0) : HashableState {
    override fun appendHash(hash: StableHash) {
        hash.add(value)
    }
}

private class CounterSystem : EngineSystem<CounterState> {
    override val id: String = "counter"
    override val order: Int = 10

    override fun update(context: SimulationContext<CounterState>) {
        context.commands.forEach {
            if (it.type == "add") {
                context.state.value += it.stablePayload().toInt()
            }
        }
        context.state.value += context.random.nextInt(3)
    }
}

class CoreRuntimeTest {
    @Test
    fun tickSchedulerUsesFixedRateCarry() {
        val scheduler = TickScheduler(TickRate(10))

        assertEquals(0, scheduler.advance(0.05))
        assertEquals(1, scheduler.advance(0.05))
        assertEquals(3, scheduler.advance(0.31))
    }

    @Test
    fun commandQueueDrainsInStableOrder() {
        val queue = CommandQueue()
        queue.submit(TextCommand(CommandId(3), Tick(2), "add", "3"))
        queue.submit(TextCommand(CommandId(1), Tick(2), "add", "1"))
        queue.submit(TextCommand(CommandId(2), Tick(1), "add", "2"))

        assertEquals(listOf(2L, 1L, 3L), queue.drainFor(Tick(2)).map { it.id.value })
    }

    @Test
    fun seededRandomIsRepeatableAndForkable() {
        val first = SeededRandom(7)
        val second = SeededRandom(7)

        assertEquals(List(5) { first.nextLong() }, List(5) { second.nextLong() })
        assertEquals(
            List(3) { SeededRandom(7).fork("waves").nextInt(10) },
            List(3) { SeededRandom(7).fork("waves").nextInt(10) },
        )
    }

    @Test
    fun replaySameInputsProducesSameHash() {
        val runner = ScenarioRunner(::CounterState) { listOf(CounterSystem()) }
        val scenario = ScenarioDefinition(
            seed = 42,
            ticks = 5,
            commands = listOf(TextCommand(CommandId(1), Tick(2), "add", "5")),
        )

        val first = runner.run(scenario)
        val second = runner.run(scenario)

        assertEquals(first.finalHash, second.finalHash)
    }

    @Test
    fun replayChangedCommandChangesHash() {
        val runner = ScenarioRunner(::CounterState) { listOf(CounterSystem()) }
        val first = runner.run(
            ScenarioDefinition(42, 5, listOf(TextCommand(CommandId(1), Tick(2), "add", "5"))),
        )
        val second = runner.run(
            ScenarioDefinition(42, 5, listOf(TextCommand(CommandId(1), Tick(2), "add", "6"))),
        )

        assertNotEquals(first.finalHash, second.finalHash)
    }
}
