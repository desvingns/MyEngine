package dev.myengine.games.sandbox

import dev.myengine.content.ContentRegistry
import dev.myengine.core.EngineInfo
import dev.myengine.core.TickRate
import dev.myengine.render.EngineSnapshot
import dev.myengine.runtime.CommandIdPolicy
import dev.myengine.runtime.ContentPackIdentity
import dev.myengine.runtime.ExperimentalGameRuntimeApi
import dev.myengine.runtime.GameRuntimeDescriptor
import dev.myengine.runtime.GameRuntimeIdentity
import dev.myengine.runtime.SaveSchemaIdentity
import dev.myengine.runtime.SessionRestoreResult
import dev.myengine.runtime.SessionStartResult
import dev.myengine.runtime.VersionedGameSave
import dev.myengine.runtime.validateSaveCompatibility

/**
 * Validated sandbox factory/compatibility boundary. Content loading remains game-owned; the generic
 * engine-runtime module sees only immutable identity values and an opaque save payload.
 */
@OptIn(ExperimentalGameRuntimeApi::class)
class SandboxDescriptor(
    registry: ContentRegistry = SandboxGame.loadRegistry(),
    difficultyId: String? = null,
    mapId: String? = null,
    private val runtimeId: String = RUNTIME_ID,
    val engineName: String = EngineInfo.NAME,
) : GameRuntimeDescriptor<EngineSnapshot> {
    /** Retains the pre-ENG-036 `(id, engineName)` construction surface. */
    constructor(
        id: String,
        engineName: String = EngineInfo.NAME,
    ) : this(
        registry = SandboxGame.loadRegistry(),
        runtimeId = id,
        engineName = engineName,
    )

    internal val registry: ContentRegistry = difficultyId?.let(registry::resolveDifficulty) ?: registry
    internal val mapId: String? = mapId?.also { this.registry.requireMap(it) }

    override val identity: GameRuntimeIdentity = GameRuntimeIdentity(
        id = runtimeId,
        tickRate = TickRate(TICKS_PER_SECOND),
        contentPack = ContentPackIdentity(this.registry.manifest.id, this.registry.manifest.version),
        saveSchema = SaveSchemaIdentity(
            id = SAVE_SCHEMA_ID,
            oldestSupportedVersion = 1,
            currentVersion = SandboxSaveCodec.SAVE_VERSION,
        ),
        stableSystemOrder = SYSTEM_ORDER,
        commandIdPolicy = CommandIdPolicy.CALLER_OWNED,
    )

    /** Compatibility aliases retained for the pre-ENG-036 sandbox descriptor surface. */
    val id: String get() = identity.id

    /** Data-class-shaped compatibility helpers retained for existing sandbox callers. */
    operator fun component1(): String = id

    operator fun component2(): String = engineName

    fun copy(
        id: String = this.id,
        engineName: String = this.engineName,
    ): SandboxDescriptor = SandboxDescriptor(
        registry = registry,
        mapId = mapId,
        runtimeId = id,
        engineName = engineName,
    )

    override fun equals(other: Any?): Boolean =
        this === other || (other is SandboxDescriptor && id == other.id && engineName == other.engineName)

    override fun hashCode(): Int = 31 * id.hashCode() + engineName.hashCode()

    override fun toString(): String = "SandboxDescriptor(id=$id, engineName=$engineName)"

    override fun start(seed: Long): SessionStartResult<EngineSnapshot> = try {
        SessionStartResult.Started(
            SandboxRuntime(
                state = SandboxGame.createInitialState(registry = registry, mapId = mapId, seed = seed),
                seed = seed,
                descriptor = this,
            ),
        )
    } catch (failure: RuntimeException) {
        SessionStartResult.Failed(failure.message ?: "Sandbox session creation failed.")
    }

    override fun restore(save: VersionedGameSave): SessionRestoreResult<EngineSnapshot> {
        validateSaveCompatibility(identity, save)?.let { return it }

        val metadata = when (val inspected = SandboxSaveCodec.inspectMetadata(save.payload)) {
            is SandboxSaveMetadataResult.Valid -> inspected.metadata
            is SandboxSaveMetadataResult.Invalid -> return SessionRestoreResult.InvalidSave(inspected.reason)
        }
        if (metadata.saveVersion != save.schemaVersion) {
            return SessionRestoreResult.InvalidSave(
                "Save envelope version ${save.schemaVersion} does not match payload version ${metadata.saveVersion}.",
            )
        }
        if (metadata.packId != null && metadata.packId != save.contentPack.id) {
            return SessionRestoreResult.InvalidSave(
                "Save envelope pack '${save.contentPack.id}' does not match payload pack '${metadata.packId}'.",
            )
        }
        if (metadata.contentVersion != null && metadata.contentVersion != save.contentPack.version) {
            return SessionRestoreResult.InvalidSave(
                "Save envelope content version '${save.contentPack.version}' does not match payload version '${metadata.contentVersion}'.",
            )
        }

        return try {
            val state = SandboxSaveCodec.decode(save.payload, registry)
            val pendingCommands = SandboxSaveCodec.decodePendingCommands(save.payload)
            SessionRestoreResult.Restored(
                SandboxRuntime(
                    state = state,
                    seed = metadata.seed ?: SandboxSession.DEFAULT_SEED,
                    descriptor = this,
                    restoredPendingCommands = pendingCommands,
                ),
            )
        } catch (failure: RuntimeException) {
            SessionRestoreResult.InvalidSave(failure.message ?: "Sandbox save restoration failed.")
        }
    }

    /** Bridges existing raw v1-v7 sandbox saves into the typed generic restore result. */
    fun restoreText(text: String): SessionRestoreResult<EngineSnapshot> {
        val metadata = when (val inspected = SandboxSaveCodec.inspectMetadata(text)) {
            is SandboxSaveMetadataResult.Valid -> inspected.metadata
            is SandboxSaveMetadataResult.Invalid -> return SessionRestoreResult.InvalidSave(inspected.reason)
        }
        return restore(
            VersionedGameSave(
                runtimeId = identity.id,
                contentPack = ContentPackIdentity(
                    metadata.packId ?: identity.contentPack.id,
                    metadata.contentVersion ?: identity.contentPack.version,
                ),
                schemaId = identity.saveSchema.id,
                schemaVersion = metadata.saveVersion,
                payload = text,
            ),
        )
    }

    companion object {
        const val RUNTIME_ID: String = "sandbox"
        const val SAVE_SCHEMA_ID: String = "sandbox-save"
        const val TICKS_PER_SECOND: Int = 20

        val SYSTEM_ORDER: List<String> = listOf(
            "incident-modifiers",
            "commands",
            "needs",
            "construction-jobs",
            "hauling",
            "jobs",
            "production",
            "belts",
            "wave-spawn",
            "status-effects",
            "tower-attacks",
            "reward-deposit",
            "enemy-movement",
            "terminal-evaluation",
            "incident-selection",
        )
    }
}
