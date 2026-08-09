package dev.myengine.games.sandbox

import dev.myengine.content.ContentRegistry
import dev.myengine.core.CommandQueue
import dev.myengine.core.EngineCommand
import dev.myengine.core.TickRate
import dev.myengine.render.EngineSnapshot
import dev.myengine.runtime.CommandOrdering
import dev.myengine.runtime.CommandSubmission
import dev.myengine.runtime.GameRuntimeDescriptor
import dev.myengine.runtime.GameSession
import dev.myengine.runtime.GameSessionBackend
import dev.myengine.runtime.QueuedGameSession
import dev.myengine.runtime.GameRuntimeFactory
import dev.myengine.runtime.RestoreResult
import java.io.StringReader
import java.util.Properties

/**
 * Android-free sandbox adapter over the reusable runtime/session contract.
 *
 * The generic session owns the pending command queue and tick dispatch. Sandbox keeps its
 * existing text save API for compatibility while supplying the queue to [SandboxSaveCodec].
 */
class SandboxSession(
    val runtime: SandboxRuntime,
    val seed: Long,
    initialPendingCommands: List<EngineCommand>? = null,
) : GameSession<EngineCommand, EngineSnapshot, String> {
    private val restoredPendingCommands: List<EngineCommand> =
        initialPendingCommands ?: runtime.pendingCommands()

    override val descriptor: GameRuntimeDescriptor = GameRuntimeDescriptor(
        id = SandboxGame.descriptor.id,
        tickRate = TickRate(SANDBOX_TICKS_PER_SECOND),
        contentPackId = runtime.state.registry.manifest.id,
        contentPackVersion = runtime.state.registry.manifest.version,
        saveSchemaId = SANDBOX_SAVE_SCHEMA_ID,
        saveSchemaVersion = SandboxSaveCodec.SAVE_VERSION,
    )

    private val session: QueuedGameSession<EngineCommand, EngineSnapshot, String> =
        QueuedGameSession(
            descriptor = descriptor,
            ordering = SANDBOX_COMMAND_ORDERING,
            backend = object : GameSessionBackend<EngineCommand, EngineSnapshot> {
                override val currentTick: Long get() = runtime.state.tick.value

                override fun submit(command: EngineCommand): CommandSubmission =
                    if (runtime.state.run.isTerminal) {
                        CommandSubmission.rejected("run_terminal")
                    } else {
                        CommandSubmission.accepted()
                    }

                override fun canStep(): Boolean = !runtime.state.run.isTerminal

                override fun step(commands: List<EngineCommand>) {
                    runtime.step(commands)
                }

                override fun snapshot(): EngineSnapshot = runtime.snapshot()
            },
            saveFactory = { pendingCommands ->
                SandboxSaveCodec.encode(runtime.state, seed, pendingCommands)
            },
            initialPendingCommands = restoredPendingCommands,
        )

    init {
        syncRuntimePendingCommands()
    }

    override fun submit(command: EngineCommand): CommandSubmission =
        session.submit(command).also { syncRuntimePendingCommands() }

    override fun step(ticks: Int) {
        // Preserve the historical sandbox no-op used by fixed-seed fuzz fixtures; the generic
        // session returned by [asGameSession] keeps the stricter positive-tick contract.
        if (ticks == 0) return
        session.step(ticks)
        syncRuntimePendingCommands()
    }

    override fun snapshot(): EngineSnapshot = session.snapshot()

    override fun save(): String = session.save()

    fun stableHash(): String = runtime.state.stableHash()

    /** Exposes the generic session for future game/platform consumers without sandbox state types. */
    fun asGameSession(): GameSession<EngineCommand, EngineSnapshot, String> = session

    /** Compatibility inspection surface retained for existing sandbox tests and tools. */
    fun pendingCommands(): List<EngineCommand> = session.pendingCommands()

    private fun syncRuntimePendingCommands() {
        runtime.attachSessionPendingCommands(session.pendingCommands())
    }

    companion object {
        /** Default sandbox seed, matching [SandboxGame.runScriptedScenario]'s default. */
        const val DEFAULT_SEED: Long = 7L

        private const val SANDBOX_TICKS_PER_SECOND: Int = 20
        private const val SANDBOX_SAVE_SCHEMA_ID: String = "sandbox-properties-save"

        private val SANDBOX_COMMAND_ORDERING = object : CommandOrdering<EngineCommand> {
            override fun scheduledTick(command: EngineCommand): Long = command.scheduledTick.value

            override fun compare(left: EngineCommand, right: EngineCommand): Int =
                CommandQueue.commandComparator.compare(left, right)
        }

        /** Starts a fresh session after optionally materializing a data-defined difficulty. */
        fun start(
            registry: ContentRegistry = SandboxGame.loadRegistry(),
            seed: Long = DEFAULT_SEED,
            difficultyId: String? = null,
            mapId: String? = null,
        ): SandboxSession = SandboxSession(SandboxGame.createRuntime(registry, difficultyId, mapId, seed), seed)

        /** Starts a session with a deterministic generated map and the same seed in its save. */
        fun startProcedural(
            registry: ContentRegistry = SandboxGame.loadRegistry(),
            seed: Long = DEFAULT_SEED,
            wallDensityPercent: Int = 18,
            maxAttempts: Int = 16,
        ): SandboxSession = SandboxSession(
            SandboxGame.createProceduralRuntime(registry, seed, wallDensityPercent, maxAttempts),
            seed,
        )

        /** Restores a versioned sandbox save after the concrete content/save checks complete. */
        fun restore(
            text: String,
            registry: ContentRegistry = SandboxGame.loadRegistry(),
        ): SandboxSession {
            val state = SandboxSaveCodec.decode(text, registry)
            val pendingCommands = SandboxSaveCodec.decodePendingCommands(text)
            val seed = parseSeed(text)
            return SandboxSession(SandboxRuntime(state, seed = seed), seed, pendingCommands)
        }

        private fun parseSeed(text: String): Long {
            val props = Properties().also { it.load(StringReader(text)) }
            return props.getProperty("seed")?.toLongOrNull() ?: DEFAULT_SEED
        }
    }
}

/** Typed sandbox factory used by headless or future game-host integrations. */
class SandboxSessionFactory(
    private val registry: ContentRegistry = SandboxGame.loadRegistry(),
    private val difficultyId: String? = null,
    private val mapId: String? = null,
) : GameRuntimeFactory<EngineCommand, EngineSnapshot, String> {
    override val descriptor: GameRuntimeDescriptor = SandboxSession.start(
        registry = registry,
        difficultyId = difficultyId,
        mapId = mapId,
    ).descriptor

    override fun start(seed: Long): GameSession<EngineCommand, EngineSnapshot, String> =
        SandboxSession.start(registry, seed, difficultyId, mapId)

    override fun restore(save: String): RestoreResult<GameSession<EngineCommand, EngineSnapshot, String>> =
        runCatching { SandboxSession.restore(save, registry) }
            .fold(
                onSuccess = { RestoreResult.Restored(it) },
                onFailure = { RestoreResult.Rejected(it.message ?: "sandbox_restore_rejected") },
            )
}
