package dev.myengine.runtime

import dev.myengine.core.TickRate

/** Immutable identity and compatibility contract for one concrete game runtime. */
data class GameRuntimeDescriptor(
    val id: String,
    val tickRate: TickRate,
    val contentPackId: String,
    val contentPackVersion: String,
    val saveSchemaId: String,
    val saveSchemaVersion: Int,
) {
    init {
        require(id.isNotBlank()) { "Runtime id must not be blank." }
        require(contentPackId.isNotBlank()) { "Content pack id must not be blank." }
        require(contentPackVersion.isNotBlank()) { "Content pack version must not be blank." }
        require(saveSchemaId.isNotBlank()) { "Save schema id must not be blank." }
        require(saveSchemaVersion > 0) { "Save schema version must be positive." }
    }
}

/** Explicit result of command admission; rejected commands never enter the session queue. */
data class CommandSubmission(
    val accepted: Boolean,
    val reason: String? = null,
) {
    init {
        if (accepted) require(reason == null) { "Accepted submissions cannot carry a rejection reason." }
        else require(!reason.isNullOrBlank()) { "Rejected submissions require a reason." }
    }

    companion object {
        fun accepted(): CommandSubmission = CommandSubmission(accepted = true)

        fun rejected(reason: String): CommandSubmission = CommandSubmission(accepted = false, reason = reason)
    }
}

/** The platform-independent lifecycle surface consumed by input and rendering adapters. */
interface GameSession<COMMAND, SNAPSHOT, SAVE> {
    val descriptor: GameRuntimeDescriptor

    fun submit(command: COMMAND): CommandSubmission

    fun step(ticks: Int = 1)

    fun snapshot(): SNAPSHOT

    fun save(): SAVE
}

/** Backend seam: concrete games apply an already ordered batch and expose immutable projections. */
interface GameSessionBackend<COMMAND, SNAPSHOT> {
    val currentTick: Long

    fun submit(command: COMMAND): CommandSubmission

    fun canStep(): Boolean = true

    fun step(commands: List<COMMAND>)

    fun snapshot(): SNAPSHOT
}

/** Stable scheduling policy supplied by a concrete game's command vocabulary. */
interface CommandOrdering<COMMAND> {
    fun scheduledTick(command: COMMAND): Long

    fun compare(left: COMMAND, right: COMMAND): Int
}

/**
 * Default deterministic session implementation. It owns pending-command insertion order and
 * drains commands for each next tick before delegating the tick to the concrete backend.
 */
class QueuedGameSession<COMMAND, SNAPSHOT, SAVE>(
    override val descriptor: GameRuntimeDescriptor,
    private val ordering: CommandOrdering<COMMAND>,
    private val backend: GameSessionBackend<COMMAND, SNAPSHOT>,
    private val saveFactory: (pendingCommands: List<COMMAND>) -> SAVE,
    initialPendingCommands: List<COMMAND> = emptyList(),
    private val maxTicksPerStep: Int = MAX_TICKS_PER_STEP,
) : GameSession<COMMAND, SNAPSHOT, SAVE> {
    private val pending = initialPendingCommands.toMutableList()

    init {
        require(maxTicksPerStep > 0) { "maxTicksPerStep must be positive." }
    }

    override fun submit(command: COMMAND): CommandSubmission {
        val result = backend.submit(command)
        if (result.accepted) pending += command
        return result
    }

    override fun step(ticks: Int) {
        require(ticks in 1..maxTicksPerStep) {
            "Step ticks must be between 1 and $maxTicksPerStep."
        }
        repeat(ticks) {
            if (!backend.canStep()) return@repeat
            val nextTick = backend.currentTick + 1
            val readyIndices = pending.withIndex()
                .filter { ordering.scheduledTick(it.value) <= nextTick }
                .sortedWith(compareBy<IndexedValue<COMMAND>> { ordering.scheduledTick(it.value) }
                    .thenComparator { left, right -> ordering.compare(left.value, right.value) })
                .map { it.index }
            val ready = readyIndices.map(pending::get)
            if (readyIndices.isNotEmpty()) {
                val readyIndexSet = readyIndices.toSet()
                val remaining = pending.withIndex()
                    .filterNot { it.index in readyIndexSet }
                    .map { it.value }
                pending.clear()
                pending += remaining
            }
            backend.step(ready)
        }
    }

    override fun snapshot(): SNAPSHOT = backend.snapshot()

    override fun save(): SAVE = saveFactory(pending.toList())

    /** Ordered only for inspection; save/restore preserves the original insertion order. */
    fun pendingCommands(): List<COMMAND> = pending.toList()

    companion object {
        const val MAX_TICKS_PER_STEP: Int = 10_000
    }
}

interface GameRuntimeFactory<COMMAND, SNAPSHOT, SAVE> {
    val descriptor: GameRuntimeDescriptor

    fun start(seed: Long): GameSession<COMMAND, SNAPSHOT, SAVE>

    fun restore(save: SAVE): RestoreResult<GameSession<COMMAND, SNAPSHOT, SAVE>>
}

sealed interface RestoreResult<out VALUE> {
    data class Restored<VALUE>(val value: VALUE) : RestoreResult<VALUE>

    data class Rejected(val reason: String) : RestoreResult<Nothing> {
        init {
            require(reason.isNotBlank()) { "Restore rejection reason must not be blank." }
        }
    }
}
