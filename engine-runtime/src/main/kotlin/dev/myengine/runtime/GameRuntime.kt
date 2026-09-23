package dev.myengine.runtime

import dev.myengine.core.CommandId
import dev.myengine.core.CommandQueue
import dev.myengine.core.EngineCommand
import dev.myengine.core.Tick
import dev.myengine.core.TickRate
import java.util.Collections

/** Marks the cross-game runtime/session API while its source compatibility is still evolving. */
@RequiresOptIn(
    message = "The reusable game runtime API is experimental and must be pinned by consumers.",
    level = RequiresOptIn.Level.WARNING,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class ExperimentalGameRuntimeApi

/** Stable identity of one validated concrete content pack. */
data class ContentPackIdentity(
    val id: String,
    val version: String,
) {
    init {
        require(id.isNotBlank()) { "Content-pack id must not be blank." }
        require(version.isNotBlank()) { "Content-pack version must not be blank." }
    }
}

/** Version range owned by one concrete game's save codec. */
data class SaveSchemaIdentity(
    val id: String,
    val oldestSupportedVersion: Int,
    val currentVersion: Int,
) {
    init {
        require(id.isNotBlank()) { "Save-schema id must not be blank." }
        require(oldestSupportedVersion > 0) { "Oldest save version must be positive." }
        require(currentVersion >= oldestSupportedVersion) {
            "Current save version must not precede the oldest supported version."
        }
    }

    fun supports(version: Int): Boolean = version in oldestSupportedVersion..currentVersion
}

enum class CommandIdPolicy {
    /**
     * Commands already carry IDs allocated by the game/application boundary.
     *
     * The experimental runtime contract does not allocate IDs or persist an allocation cursor.
     * Callers are responsible for uniqueness. Duplicate IDs are accepted by the generic queue and
     * remain deterministic because the complete command comparator supplies the drain order.
     */
    CALLER_OWNED,
}

/** Immutable lifecycle identity consumed by launchers and game-owned adapters. */
class GameRuntimeIdentity(
    val id: String,
    val tickRate: TickRate,
    val contentPack: ContentPackIdentity,
    val saveSchema: SaveSchemaIdentity,
    stableSystemOrder: List<String>,
    val commandIdPolicy: CommandIdPolicy,
    val maxTicksPerStep: Int = DEFAULT_MAX_TICKS_PER_STEP,
) {
    /** Defensive, unmodifiable copy keeps equality/hash semantics stable after construction. */
    val stableSystemOrder: List<String> = Collections.unmodifiableList(stableSystemOrder.toList())

    init {
        require(id.isNotBlank()) { "Runtime id must not be blank." }
        require(this.stableSystemOrder.isNotEmpty()) { "Stable system order must not be empty." }
        require(this.stableSystemOrder.none { it.isBlank() }) { "Stable system IDs must not be blank." }
        require(this.stableSystemOrder.distinct().size == this.stableSystemOrder.size) {
            "Stable system order must not contain duplicates."
        }
        require(maxTicksPerStep > 0) { "maxTicksPerStep must be positive." }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameRuntimeIdentity) return false
        return id == other.id &&
            tickRate == other.tickRate &&
            contentPack == other.contentPack &&
            saveSchema == other.saveSchema &&
            stableSystemOrder == other.stableSystemOrder &&
            commandIdPolicy == other.commandIdPolicy &&
            maxTicksPerStep == other.maxTicksPerStep
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + tickRate.hashCode()
        result = 31 * result + contentPack.hashCode()
        result = 31 * result + saveSchema.hashCode()
        result = 31 * result + stableSystemOrder.hashCode()
        result = 31 * result + commandIdPolicy.hashCode()
        result = 31 * result + maxTicksPerStep
        return result
    }

    override fun toString(): String =
        "GameRuntimeIdentity(" +
            "id=$id, " +
            "tickRate=$tickRate, " +
            "contentPack=$contentPack, " +
            "saveSchema=$saveSchema, " +
            "stableSystemOrder=$stableSystemOrder, " +
            "commandIdPolicy=$commandIdPolicy, " +
            "maxTicksPerStep=$maxTicksPerStep" +
            ")"

    companion object {
        const val DEFAULT_MAX_TICKS_PER_STEP: Int = 10_000
    }
}

/**
 * Transport-neutral save envelope. [payload] is opaque to engine-runtime; only the concrete
 * descriptor/codec may interpret it.
 */
data class VersionedGameSave(
    val runtimeId: String,
    val contentPack: ContentPackIdentity,
    val schemaId: String,
    val schemaVersion: Int,
    val payload: String,
) {
    init {
        require(runtimeId.isNotBlank()) { "Save runtime id must not be blank." }
        require(schemaId.isNotBlank()) { "Save schema id must not be blank." }
        require(schemaVersion > 0) { "Save schema version must be positive." }
    }
}

sealed interface SessionSubmitResult {
    data class Accepted(
        val commandId: CommandId,
        val scheduledTick: Tick,
    ) : SessionSubmitResult

    data class Rejected(
        val commandId: CommandId,
        val reason: Reason,
    ) : SessionSubmitResult

    enum class Reason {
        TERMINAL_SESSION,
    }
}

/** Immutable projection plus the authoritative tick that produced it. */
data class SessionSnapshot<out S : Any>(
    val tick: Tick,
    val value: S,
)

sealed interface SessionStepResult {
    data class Advanced(
        val requestedTicks: Int,
        val advancedTicks: Int,
        val tick: Tick,
        val terminal: Boolean,
    ) : SessionStepResult

    data class Rejected(
        val requestedTicks: Int,
        val allowedTicks: IntRange,
        val reason: Reason,
        val tick: Tick,
        val terminal: Boolean,
    ) : SessionStepResult

    enum class Reason {
        INVALID_TICK_COUNT,
    }
}

sealed interface SessionSaveResult {
    data class Saved(val save: VersionedGameSave) : SessionSaveResult
    data class Failed(val reason: String) : SessionSaveResult
}

sealed interface SessionStartResult<out S : Any> {
    data class Started<S : Any>(val session: GameSession<S>) : SessionStartResult<S>
    data class Failed(val reason: String) : SessionStartResult<Nothing>
}

enum class SaveIncompatibility {
    RUNTIME_ID,
    CONTENT_PACK_ID,
    CONTENT_PACK_VERSION,
    SAVE_SCHEMA_ID,
    SAVE_SCHEMA_VERSION,
}

sealed interface SessionRestoreResult<out S : Any> {
    data class Restored<S : Any>(val session: GameSession<S>) : SessionRestoreResult<S>

    data class Incompatible(
        val kind: SaveIncompatibility,
        val expected: String,
        val actual: String,
    ) : SessionRestoreResult<Nothing>

    data class InvalidSave(val reason: String) : SessionRestoreResult<Nothing>
}

/** Factory/compatibility boundary owned by one concrete game and validated content selection. */
@ExperimentalGameRuntimeApi
interface GameRuntimeDescriptor<out S : Any> {
    val identity: GameRuntimeIdentity

    fun start(seed: Long): SessionStartResult<S>

    /** Returns a typed failure and never exposes a partially restored runtime. */
    fun restore(save: VersionedGameSave): SessionRestoreResult<S>
}

/** Android-free authoritative runtime lifecycle. */
@ExperimentalGameRuntimeApi
interface GameSession<out S : Any> {
    val descriptor: GameRuntimeDescriptor<S>
    val seed: Long
    val currentTick: Tick
    val isTerminal: Boolean

    fun stableHash(): String

    /**
     * Enqueues a command carrying an ID allocated according to [GameRuntimeIdentity.commandIdPolicy].
     * Under the current caller-owned policy, uniqueness is a caller invariant rather than generic
     * session state. A command scheduled at or before [currentTick] is intentionally eligible on
     * the next step, matching [CommandQueue.drainFor]'s catch-up semantics.
     */
    fun submit(command: EngineCommand): SessionSubmitResult
    fun step(ticks: Int = 1): SessionStepResult
    fun snapshot(): SessionSnapshot<S>
    fun save(): SessionSaveResult
}

/**
 * Shared deterministic ownership for concrete game sessions.
 *
 * The base owns the pending command queue, stable queue drain order, seed, bounded positive step
 * contract, and save-envelope construction. A concrete game owns authoritative state, systems,
 * immutable snapshot projection, and payload encoding. Command IDs are caller-owned: this base
 * deliberately does not reject or remember duplicate IDs, because consumed-ID history is not part
 * of the generic save envelope. Commands sharing an ID are still drained deterministically by the
 * full [CommandQueue.commandComparator].
 */
@ExperimentalGameRuntimeApi
abstract class DeterministicGameSession<S : Any>(
    final override val descriptor: GameRuntimeDescriptor<S>,
    final override val seed: Long,
    restoredPendingCommands: List<EngineCommand> = emptyList(),
) : GameSession<S> {
    private val commandQueue = CommandQueue().also { queue ->
        restoredPendingCommands.forEach(queue::submit)
    }

    protected abstract val authoritativeTick: Tick
    protected abstract val terminal: Boolean

    final override val currentTick: Tick get() = authoritativeTick

    /**
     * `Long.MAX_VALUE` is the last representable authoritative tick. Treating it as a generic
     * terminal boundary prevents a valid restored session from overflowing through [Tick.next].
     */
    final override val isTerminal: Boolean
        get() = terminal || authoritativeTick.value == Long.MAX_VALUE

    final override fun stableHash(): String = stableHashValue()

    final override fun submit(command: EngineCommand): SessionSubmitResult {
        if (isTerminal) {
            return SessionSubmitResult.Rejected(command.id, SessionSubmitResult.Reason.TERMINAL_SESSION)
        }
        commandQueue.submit(command)
        return SessionSubmitResult.Accepted(command.id, command.scheduledTick)
    }

    /** Batch convenience restricted to an explicitly ordered collection. */
    fun submitAll(commands: List<EngineCommand>): List<SessionSubmitResult> = commands.map(::submit)

    final override fun step(ticks: Int): SessionStepResult {
        val maxTicks = descriptor.identity.maxTicksPerStep
        if (ticks < 1 || ticks > maxTicks) {
            return SessionStepResult.Rejected(
                requestedTicks = ticks,
                allowedTicks = 1..maxTicks,
                reason = SessionStepResult.Reason.INVALID_TICK_COUNT,
                tick = authoritativeTick,
                terminal = isTerminal,
            )
        }

        var advanced = 0
        while (advanced < ticks && !isTerminal) {
            val nextTick = authoritativeTick.next()
            val commands = commandQueue.drainFor(nextTick)
            advanceOneTick(nextTick, commands)
            check(authoritativeTick == nextTick) {
                "Concrete runtime must advance exactly to tick ${nextTick.value}; was ${authoritativeTick.value}."
            }
            advanced += 1
        }
        return SessionStepResult.Advanced(
            requestedTicks = ticks,
            advancedTicks = advanced,
            tick = authoritativeTick,
            terminal = isTerminal,
        )
    }

    final override fun snapshot(): SessionSnapshot<S> = SessionSnapshot(
        tick = authoritativeTick,
        value = projectSnapshot(),
    )

    final override fun save(): SessionSaveResult = try {
        val identity = descriptor.identity
        SessionSaveResult.Saved(
            VersionedGameSave(
                runtimeId = identity.id,
                contentPack = identity.contentPack,
                schemaId = identity.saveSchema.id,
                schemaVersion = identity.saveSchema.currentVersion,
                payload = encodeSavePayload(commandQueue.pending()),
            ),
        )
    } catch (failure: RuntimeException) {
        SessionSaveResult.Failed(failure.message ?: "Concrete save encoding failed.")
    }

    /** Non-destructive insertion-order snapshot for concrete codecs and diagnostics. */
    protected fun pendingCommandSnapshot(): List<EngineCommand> = commandQueue.pending()

    /**
     * Optional game-owned input boundary for commands that take effect without advancing time.
     *
     * Only commands scheduled at or before the current tick are drained, in the same canonical
     * order as [step]; future commands remain queued and saveable. A concrete adapter must apply
     * these commands immediately as one serialized operation, without running simulation systems
     * or changing [authoritativeTick]. This lets pause/resume and direct controls work while the
     * fixed-tick clock is stopped, without granting extra income/cooldown/movement ticks per tap.
     * Games that do not opt in retain the existing catch-up-on-next-step behavior unchanged.
     * Terminal sessions never drain their remaining commands.
     */
    protected fun drainCommandsAtCurrentTick(): List<EngineCommand> =
        if (isTerminal) emptyList() else commandQueue.drainFor(authoritativeTick)

    protected abstract fun advanceOneTick(tick: Tick, commands: List<EngineCommand>)
    protected abstract fun projectSnapshot(): S
    protected abstract fun stableHashValue(): String
    protected abstract fun encodeSavePayload(pendingCommands: List<EngineCommand>): String
}

/** Common identity checks descriptors apply before invoking game-specific decoders. */
fun validateSaveCompatibility(
    descriptor: GameRuntimeIdentity,
    save: VersionedGameSave,
): SessionRestoreResult.Incompatible? = when {
    save.runtimeId != descriptor.id -> SessionRestoreResult.Incompatible(
        SaveIncompatibility.RUNTIME_ID,
        descriptor.id,
        save.runtimeId,
    )
    save.contentPack.id != descriptor.contentPack.id -> SessionRestoreResult.Incompatible(
        SaveIncompatibility.CONTENT_PACK_ID,
        descriptor.contentPack.id,
        save.contentPack.id,
    )
    save.contentPack.version != descriptor.contentPack.version -> SessionRestoreResult.Incompatible(
        SaveIncompatibility.CONTENT_PACK_VERSION,
        descriptor.contentPack.version,
        save.contentPack.version,
    )
    save.schemaId != descriptor.saveSchema.id -> SessionRestoreResult.Incompatible(
        SaveIncompatibility.SAVE_SCHEMA_ID,
        descriptor.saveSchema.id,
        save.schemaId,
    )
    !descriptor.saveSchema.supports(save.schemaVersion) -> SessionRestoreResult.Incompatible(
        SaveIncompatibility.SAVE_SCHEMA_VERSION,
        "${descriptor.saveSchema.oldestSupportedVersion}..${descriptor.saveSchema.currentVersion}",
        save.schemaVersion.toString(),
    )
    else -> null
}
