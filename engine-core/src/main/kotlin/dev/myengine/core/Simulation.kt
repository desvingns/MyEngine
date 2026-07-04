package dev.myengine.core

data class SimulationContext<S : HashableState>(
    val tick: Tick,
    val state: S,
    val random: SeededRandom,
    val eventLog: EventLog,
    val commands: List<EngineCommand>,
)

interface EngineSystem<S : HashableState> {
    val id: String
    val order: Int
    fun update(context: SimulationContext<S>)
}

data class TickResult(
    val tick: Tick,
    val commandsProcessed: Int,
    val stateHash: String,
)

class DeterministicEngine<S : HashableState>(
    val state: S,
    systems: List<EngineSystem<S>>,
    seed: Long,
    private val commandQueue: CommandQueue = CommandQueue(),
    private val eventLog: EventLog = EventLog(),
) {
    private val systemsInOrder = systems.sortedWith(compareBy<EngineSystem<S>> { it.order }.thenBy { it.id })
    private val random = SeededRandom(seed)
    var currentTick: Tick = Tick(0)
        private set

    fun submit(command: EngineCommand) {
        commandQueue.submit(command)
    }

    fun step(ticks: Int = 1): List<TickResult> {
        require(ticks >= 0) { "ticks must be non-negative." }
        val results = mutableListOf<TickResult>()
        repeat(ticks) {
            currentTick = currentTick.next()
            val commands = commandQueue.drainFor(currentTick)
            val context = SimulationContext(currentTick, state, random, eventLog, commands)
            systemsInOrder.forEach { it.update(context) }
            results += TickResult(currentTick, commands.size, stateHash())
        }
        return results
    }

    fun events(): List<SimulationEvent> = eventLog.all()

    fun stateHash(): String = stableHashOf {
        add(currentTick.value)
        state.appendHash(this)
    }
}

data class ScenarioDefinition(
    val seed: Long,
    val ticks: Int,
    val commands: List<EngineCommand>,
)

data class ScenarioResult(
    val finalTick: Tick,
    val finalHash: String,
    val tickHashes: List<String>,
)

class ScenarioRunner<S : HashableState>(
    private val stateFactory: () -> S,
    private val systemsFactory: () -> List<EngineSystem<S>>,
) {
    fun run(definition: ScenarioDefinition): ScenarioResult {
        val engine = DeterministicEngine(
            state = stateFactory(),
            systems = systemsFactory(),
            seed = definition.seed,
        )
        definition.commands.sortedWith(CommandQueue.commandComparator).forEach(engine::submit)
        val results = engine.step(definition.ticks)
        return ScenarioResult(
            finalTick = engine.currentTick,
            finalHash = engine.stateHash(),
            tickHashes = results.map { it.stateHash },
        )
    }
}
