package dev.myengine.games.sandbox

import dev.myengine.content.ContentRegistry
import dev.myengine.core.EngineCommand
import dev.myengine.render.EngineSnapshot
import dev.myengine.runtime.ExperimentalGameRuntimeApi
import dev.myengine.runtime.SessionStepResult
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
 * SAVE SOUNDNESS: [SandboxSaveCodec] v7 persists `state`, terminal run status/summary, selected
 * map id, content version, tower upgrade branch/tier/targeting-mode markers, and the runtime's pending
 * [dev.myengine.core.CommandQueue], so [save] is sound at ANY tick — a future-tick command still
 * queued at save time round-trips through [restore] and is re-queued on the reconstructed runtime.
 * The per-tick `SeededRandom(17)` incident cursor is NOT persisted, but is confirmed to be a fresh
 * instance constructed every tick rather than a persistent cursor, so there is nothing to persist
 * for it.
 */
@OptIn(ExperimentalGameRuntimeApi::class)
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

    fun stableHash(): String = runtime.stableHash()

    /** Immutable game-specific value for legacy Android/render callers. */
    fun snapshot(): EngineSnapshot = runtime.snapshot().value

    /** Thin delegate so a lifecycle/test can advance the simulation. */
    fun step(ticks: Int = 1) {
        require(ticks >= 0) { "Count 'times' must be non-negative, but was $ticks." }
        var remaining = ticks
        val maxTicksPerStep = runtime.descriptor.identity.maxTicksPerStep
        while (remaining > 0) {
            val requested = minOf(remaining, maxTicksPerStep)
            when (val result = runtime.step(requested)) {
                is SessionStepResult.Advanced -> {
                    remaining -= result.advancedTicks
                    if (result.terminal) return
                    check(result.advancedTicks == requested) {
                        "Sandbox runtime advanced ${result.advancedTicks} of $requested requested ticks without terminating."
                    }
                }
                is SessionStepResult.Rejected -> throw IllegalArgumentException(
                    "ticks must be in ${result.allowedTicks}; was ${result.requestedTicks}",
                )
            }
        }
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
        ): SandboxSession = SandboxSession(
            runtime = SandboxGame.createRuntime(registry, difficultyId, mapId, seed),
            seed = seed,
        )

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
            val runtime = SandboxRuntime(
                state = state,
                seed = seed,
                restoredPendingCommands = pendingCommands,
            )
            return SandboxSession(runtime, seed)
        }

        private fun parseSeed(text: String): Long {
            val props = Properties().also { it.load(StringReader(text)) }
            return props.getProperty("seed")?.toLongOrNull() ?: DEFAULT_SEED
        }
    }
}
