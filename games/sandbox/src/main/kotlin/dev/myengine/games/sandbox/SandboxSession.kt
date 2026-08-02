package dev.myengine.games.sandbox

import dev.myengine.content.ContentRegistry
import dev.myengine.core.EngineCommand
import java.io.StringReader
import java.util.Properties

/**
 * Pure, Android-free lifecycle-persistence holder that wraps a live [SandboxRuntime] plus the
 * [seed] used to encode its saves, so an Android (or any) lifecycle can save/restore a sandbox run.
 *
 * This type is intentionally JVM-only with no Android imports: it is the "thin adapter" boundary
 * that keeps simulation Android-free. The owning lifecycle (e.g. an Activity) calls [save] on
 * pause and [restore] on recreate; it must not reach into the runtime directly.
 *
 * SAVE SOUNDNESS: [SandboxSaveCodec] v13 persists `state`, active status effects, effective enemy components, player-placed buildings, terminal run status/summary, selected
 * map id, content version, tower upgrade branch/tier/targeting-mode markers, and the runtime's pending
 * [dev.myengine.core.CommandQueue], so [save] is sound at ANY tick — a future-tick command still
 * queued at save time round-trips through [restore] and is re-queued on the reconstructed runtime.
 * The simulation RNG cursor, stateful incident director, selected effect history, and persistent
 * incident modifiers, the authoritative job board, and in-flight job actors are part of the v13
 * save so continuation does not restart randomness or lose job progress.
 */
class SandboxSession(
    val runtime: SandboxRuntime,
    val seed: Long,
) {
    /**
     * Encodes the current runtime state, including pending commands, to a save string.
     *
     * Sound at any tick — see the [SandboxSession] KDoc.
     */
    fun save(): String = SandboxSaveCodec.encode(runtime.state, seed, runtime.pendingCommands())

    fun stableHash(): String = runtime.state.stableHash()

    /** Thin delegate so a lifecycle/test can advance the simulation. */
    fun step(ticks: Int = 1) {
        runtime.step(ticks)
    }

    /** Thin delegate so a lifecycle/test can enqueue a command. */
    fun submit(command: EngineCommand) {
        runtime.submit(command)
    }

    companion object {
        /** Default sandbox seed, matching [SandboxGame.runScriptedScenario]'s default. */
        const val DEFAULT_SEED: Long = 7L

        /** Starts a fresh session after optionally materializing a data-defined difficulty. */
        fun start(
            registry: ContentRegistry = SandboxGame.loadRegistry(),
            seed: Long = DEFAULT_SEED,
            difficultyId: String? = null,
            mapId: String? = null,
        ): SandboxSession = SandboxSession(SandboxGame.createRuntime(registry, difficultyId, mapId, seed), seed)

        /**
         * Restores a session from a save [text] produced by [save].
         *
         * The [seed] is read back out of the save's `seed` property so a subsequent [save]
         * reproduces the same seed; it defaults to [DEFAULT_SEED] when the property is absent or
         * unparseable. `state` is reconstructed via [SandboxSaveCodec.decode], and any pending
         * commands are reconstructed via [SandboxSaveCodec.decodePendingCommands] and loaded into
         * the fresh runtime's queue — see the [SandboxSession] KDoc.
         */
        fun restore(
            text: String,
            registry: ContentRegistry = SandboxGame.loadRegistry(),
        ): SandboxSession {
            val state = SandboxSaveCodec.decode(text, registry)
            val pendingCommands = SandboxSaveCodec.decodePendingCommands(text)
            val seed = parseSeed(text)
            val runtime = SandboxRuntime(state, seed = seed)
            runtime.restorePendingCommands(pendingCommands)
            return SandboxSession(runtime, seed)
        }

        private fun parseSeed(text: String): Long {
            val props = Properties().also { it.load(StringReader(text)) }
            return props.getProperty("seed")?.toLongOrNull() ?: DEFAULT_SEED
        }
    }
}
