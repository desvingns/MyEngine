package dev.myengine.core

@JvmInline
value class CommandId(val value: Long) : Comparable<CommandId> {
    init {
        require(value >= 0) { "CommandId must be non-negative." }
    }

    override fun compareTo(other: CommandId): Int = value.compareTo(other.value)
}

interface EngineCommand {
    val id: CommandId
    val scheduledTick: Tick
    val actorId: Long?
    val type: String
    fun stablePayload(): String
}

data class TextCommand(
    override val id: CommandId,
    override val scheduledTick: Tick,
    override val type: String,
    private val payload: String,
    override val actorId: Long? = null,
) : EngineCommand {
    override fun stablePayload(): String = payload
}

class CommandQueue {
    private val pending = mutableListOf<EngineCommand>()

    fun submit(command: EngineCommand) {
        pending += command
    }

    fun drainFor(tick: Tick): List<EngineCommand> {
        val ready = pending.filter { it.scheduledTick <= tick }
            .sortedWith(commandComparator)
        pending.removeAll(ready.toSet())
        return ready
    }

    fun isEmpty(): Boolean = pending.isEmpty()

    companion object {
        val commandComparator: Comparator<EngineCommand> =
            compareBy<EngineCommand> { it.scheduledTick.value }
                .thenBy { it.id.value }
                .thenBy { it.type }
                .thenBy { it.actorId ?: Long.MIN_VALUE }
                .thenBy { it.stablePayload() }
    }
}
