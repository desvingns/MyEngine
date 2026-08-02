package dev.myengine.games.sandbox

import dev.myengine.content.ContentRegistry
import java.io.StringWriter
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.channels.FileChannel
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE
import java.util.Locale
import java.util.Properties

/** Configuration for deterministic, tick-based rotating autosaves. */
data class SandboxAutosavePolicy(
    val cadenceTicks: Long = DEFAULT_CADENCE_TICKS,
    val slotNames: List<String> = DEFAULT_SLOT_NAMES,
) {
    init {
        require(cadenceTicks > 0) { "Autosave cadence must be positive." }
        require(slotNames.isNotEmpty()) { "Autosave requires at least one slot." }
        require(slotNames.distinct().size == slotNames.size) { "Autosave slot names must be unique." }
        slotNames.forEach(::validateSandboxSaveSlotName)
    }

    private companion object {
        private const val DEFAULT_CADENCE_TICKS = 300L
        private val DEFAULT_SLOT_NAMES = listOf("autosave-1", "autosave-2", "autosave-3")
    }
}

/** Metadata that can be read without decoding the authoritative sandbox state. */
data class SandboxSlotMetadata(
    val slotName: String,
    val mapId: String,
    val wave: Int,
    val contentVersion: String,
    val timestampMillis: Long,
    val codecVersion: Int,
    val autosaveSequence: Long? = null,
)

private fun validateSandboxSaveSlotName(slotName: String) {
    require(slotName.isNotBlank() && slotName == slotName.trim()) {
        "Save slot name must be non-blank and trimmed."
    }
    require(slotName != "." && slotName != "..") { "Save slot name cannot be a path segment." }
    require(slotName == slotName.lowercase(Locale.ROOT)) {
        "Save slot names must be lowercase to remain isolated on case-insensitive filesystems."
    }
    require(slotName.none { it.isISOControl() || it in "\\/:*?\"<>|" }) {
        "Save slot name contains an unsupported path character."
    }
}

/**
 * Android-free file boundary for named sandbox saves and rotating autosaves.
 *
 * The existing [SandboxSaveCodec] v13 text remains the authoritative payload. Slot metadata is
 * appended as additional properties, so [SandboxSession.restore] and the Android Bundle path do
 * not need a new save format or a lifecycle adapter.
 */
class SandboxSaveSlotStore(
    private val directory: Path,
    val autosavePolicy: SandboxAutosavePolicy = SandboxAutosavePolicy(),
    private val timestampMillis: () -> Long = System::currentTimeMillis,
) {
    private var nextAutosaveIndex: Int = discoverNextAutosaveIndex()
    private var nextAutosaveSequence: Long = discoverNextAutosaveSequence()
    private var nextAutosaveTick: Long? = null

    /** Returns the stable on-disk path for a named slot. */
    fun pathFor(slotName: String): Path {
        validateSandboxSaveSlotName(slotName)
        return if (slotName in autosavePolicy.slotNames) {
            autosaveSlotPath(slotName)
        } else {
            namedSlotPath(slotName)
        }
    }

    /** Atomically stores the current session in a named slot and returns its metadata. */
    fun save(slotName: String, session: SandboxSession): SandboxSlotMetadata =
        writeSlot(namedSlotPath(slotName), slotName, session, autosaveSequence = null)

    /**
     * Saves the current session when the configured tick cadence is due.
     *
     * At most one slot is written per call. If a caller skips several due ticks, the next
     * deadline is advanced from the observed tick, keeping rotation stable and avoiding a burst
     * of identical writes.
     */
    fun maybeAutosave(session: SandboxSession): SandboxSlotMetadata? {
        val currentTick = session.runtime.state.tick.value
        val dueAt = nextAutosaveTick ?: currentTick.also { nextAutosaveTick = it }
        if (currentTick < dueAt) return null

        val slotName = autosavePolicy.slotNames[nextAutosaveIndex]
        val sequence = nextAutosaveSequence
        val metadata = writeSlot(autosaveSlotPath(slotName), slotName, session, sequence)
        nextAutosaveIndex = (nextAutosaveIndex + 1) % autosavePolicy.slotNames.size
        nextAutosaveSequence = sequence.safelyIncrement("autosave sequence")
        nextAutosaveTick = safelyAdd(currentTick, autosavePolicy.cadenceTicks)
        return metadata
    }

    /** Reads only slot properties; no simulation state or command queue is reconstructed. */
    fun readMetadata(slotName: String): SandboxSlotMetadata {
        val path = pathFor(slotName)
        require(Files.isRegularFile(path)) { "Save slot '$slotName' does not exist." }
        return readMetadata(path, expectedSlotName = slotName)
    }

    /** Lists readable metadata for all store-owned slot files without decoding their state. */
    fun listMetadata(): List<SandboxSlotMetadata> {
        if (!Files.isDirectory(directory)) return emptyList()
        val metadata = mutableListOf<SandboxSlotMetadata>()
        listOf(namedSlotsDirectory(), autosaveDirectory()).forEach { slotDirectory ->
            if (Files.isDirectory(slotDirectory)) {
                Files.list(slotDirectory).use { paths ->
                    paths.forEach { path ->
                        if (Files.isRegularFile(path) && path.fileName.toString().endsWith(".save")) {
                            val expectedName = path.fileName.toString().removeSuffix(".save")
                            runCatching { readMetadata(path, expectedSlotName = expectedName) }
                                .onSuccess(metadata::add)
                        }
                    }
                }
            }
        }
        return metadata.sortedBy { it.slotName }
    }

    /**
     * Restores a named slot. If its payload is corrupt, the newest valid configured autosave is
     * returned instead. The failing payload is left untouched for diagnostics.
     */
    fun restore(
        slotName: String,
        registry: ContentRegistry = SandboxGame.loadRegistry(),
    ): SandboxSession {
        val path = pathFor(slotName)
        require(Files.isRegularFile(path)) { "Save slot '$slotName' does not exist." }
        if (isCompatibilitySave(path, registry)) {
            return SandboxSession.restore(Files.readString(path, StandardCharsets.UTF_8), registry)
        }
        val restored = runCatching { restorePayload(path, registry) }
        if (restored.isSuccess) return restored.getOrThrow()
        val failure = requireNotNull(restored.exceptionOrNull())

        findLastGoodAutosave(excludeSlotName = slotName, registry = registry)?.let { return it }
        throw IllegalStateException(
            "Save slot '$slotName' is corrupt and no good autosave is available.",
            failure,
        )
    }

    private fun isCompatibilitySave(path: Path, registry: ContentRegistry): Boolean {
        val properties = runCatching {
            Properties().also {
                Files.newBufferedReader(path, StandardCharsets.UTF_8).use(it::load)
            }
        }.getOrNull() ?: return false
        val version = properties.getProperty("saveVersion")?.toIntOrNull() ?: return true
        if (version !in 1..SandboxSaveCodec.SAVE_VERSION) return true
        if (version < 4) return false
        return properties.getProperty("packId") != registry.manifest.id ||
            properties.getProperty("contentVersion") != registry.manifest.version ||
            properties.getProperty("mapId") !in registry.maps.keys
    }

    private fun writeSlot(
        path: Path,
        slotName: String,
        session: SandboxSession,
        autosaveSequence: Long?,
    ): SandboxSlotMetadata {
        validateSandboxSaveSlotName(slotName)
        val timestamp = timestampMillis().also {
            require(it >= 0) { "Save timestamp must not be negative." }
        }
        val state = session.runtime.state
        val metadata = SandboxSlotMetadata(
            slotName = slotName,
            mapId = state.mapId,
            wave = state.defense.spawnedWaveIds.size,
            contentVersion = state.registry.manifest.version,
            timestampMillis = timestamp,
            codecVersion = SandboxSaveCodec.SAVE_VERSION,
            autosaveSequence = autosaveSequence,
        )
        val payload = appendMetadata(session.save(), metadata)
        atomicWrite(path, payload)
        return metadata
    }

    private fun restorePayload(path: Path, registry: ContentRegistry): SandboxSession =
        SandboxSession.restore(Files.readString(path, StandardCharsets.UTF_8), registry)

    private fun namedSlotsDirectory(): Path = directory.resolve("slots")

    private fun autosaveDirectory(): Path = directory.resolve("autosave")

    private fun namedSlotPath(slotName: String): Path {
        validateSandboxSaveSlotName(slotName)
        require(slotName !in autosavePolicy.slotNames) {
            "Slot name '$slotName' is reserved for the autosave ring."
        }
        return namedSlotsDirectory().resolve("$slotName.save")
    }

    private fun autosaveSlotPath(slotName: String): Path {
        require(slotName in autosavePolicy.slotNames) {
            "Slot name '$slotName' is not part of the autosave ring."
        }
        return autosaveDirectory().resolve("$slotName.save")
    }

    private fun findLastGoodAutosave(
        excludeSlotName: String,
        registry: ContentRegistry,
    ): SandboxSession? {
        val candidates = autosavePolicy.slotNames.mapNotNull { slotName ->
            if (slotName == excludeSlotName) return@mapNotNull null
            val path = autosaveSlotPath(slotName)
            if (!Files.isRegularFile(path)) return@mapNotNull null
            runCatching { readMetadata(path, expectedSlotName = slotName) }.getOrNull()
                ?.let { it to path }
        }.sortedWith(
            compareByDescending<Pair<SandboxSlotMetadata, Path>> { it.first.autosaveSequence ?: Long.MIN_VALUE }
                .thenByDescending { it.first.timestampMillis }
                .thenBy { it.first.slotName },
        )

        candidates.forEach { (_, path) ->
            runCatching { restorePayload(path, registry) }.getOrNull()?.let { return it }
        }
        return null
    }

    private fun discoverNextAutosaveIndex(): Int {
        val latest = autosavePolicy.slotNames.mapNotNull { slotName ->
            val path = autosaveSlotPath(slotName)
            if (!Files.isRegularFile(path)) return@mapNotNull null
            runCatching { readMetadata(path, expectedSlotName = slotName) }.getOrNull()
        }.maxWithOrNull(
            compareBy<SandboxSlotMetadata> { it.autosaveSequence ?: Long.MIN_VALUE }
                .thenBy { it.timestampMillis }
                .thenBy { it.slotName },
        ) ?: return 0
        val sequence = latest.autosaveSequence ?: return 0
        return ((sequence.safelyIncrement("autosave sequence") % autosavePolicy.slotNames.size).toInt())
    }

    private fun discoverNextAutosaveSequence(): Long {
        val largest = autosavePolicy.slotNames.mapNotNull { slotName ->
            val path = autosaveSlotPath(slotName)
            if (!Files.isRegularFile(path)) return@mapNotNull null
            runCatching { readMetadata(path, expectedSlotName = slotName) }.getOrNull()?.autosaveSequence
        }.maxOrNull() ?: -1L
        return largest.safelyIncrement("autosave sequence")
    }

    private fun readMetadata(path: Path, expectedSlotName: String? = null): SandboxSlotMetadata {
        val props = Properties().also {
            Files.newBufferedReader(path, StandardCharsets.UTF_8).use(it::load)
        }
        val slotName = props.required("slotName")
        validateSandboxSaveSlotName(slotName)
        if (expectedSlotName != null) {
            require(slotName == expectedSlotName) {
                "Save metadata slot '$slotName' does not match '$expectedSlotName'."
            }
        }
        require(props.getProperty("slotMetadataVersion") == SLOT_METADATA_VERSION.toString()) {
            "Unsupported metadata version for save slot '$slotName'."
        }
        val codecVersion = props.requiredInt("saveVersion")
        require(codecVersion in 1..SandboxSaveCodec.SAVE_VERSION) {
            "Unsupported save codec version '$codecVersion' in slot '$slotName'."
        }
        return SandboxSlotMetadata(
            slotName = slotName,
            mapId = props.required("slotMapId"),
            wave = props.requiredNonNegativeInt("slotWave"),
            contentVersion = props.required("slotContentVersion"),
            timestampMillis = props.requiredNonNegativeLong("slotTimestampMillis"),
            codecVersion = codecVersion,
            autosaveSequence = props.optionalNonNegativeLong("slotAutosaveSequence"),
        )
    }

    private fun appendMetadata(saveText: String, metadata: SandboxSlotMetadata): String {
        val props = Properties().apply {
            setProperty("slotMetadataVersion", SLOT_METADATA_VERSION.toString())
            setProperty("slotName", metadata.slotName)
            setProperty("slotMapId", metadata.mapId)
            setProperty("slotWave", metadata.wave.toString())
            setProperty("slotContentVersion", metadata.contentVersion)
            setProperty("slotTimestampMillis", metadata.timestampMillis.toString())
            metadata.autosaveSequence?.let { setProperty("slotAutosaveSequence", it.toString()) }
        }
        val metadataText = StringWriter().also { props.store(it, "MyEngine sandbox slot metadata") }.toString()
        return buildString(saveText.length + metadataText.length + 1) {
            append(saveText)
            if (!saveText.endsWith('\n')) append('\n')
            append(metadataText)
        }
    }

    private fun atomicWrite(path: Path, text: String) {
        val parent = requireNotNull(path.parent) { "Save path must have a parent directory." }
        Files.createDirectories(parent)
        val temporary = parent.resolve(".${path.fileName}.tmp")
        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        FileChannel.open(temporary, CREATE, WRITE, TRUNCATE_EXISTING).use { channel ->
            val buffer = ByteBuffer.wrap(bytes)
            while (buffer.hasRemaining()) channel.write(buffer)
            channel.force(true)
        }
        try {
            Files.move(temporary, path, ATOMIC_MOVE, REPLACE_EXISTING)
        } catch (failure: AtomicMoveNotSupportedException) {
            throw java.io.IOException(
                "Filesystem does not support atomic save replacement for '$path'.",
                failure,
            )
        }
    }

    private companion object {
        private const val SLOT_METADATA_VERSION = 1

        private fun Properties.required(key: String): String =
            getProperty(key)?.takeIf { it.isNotBlank() }
                ?: error("Save slot metadata is missing '$key'.")

        private fun Properties.requiredInt(key: String): Int =
            required(key).toIntOrNull() ?: error("Save slot metadata '$key' is not an integer.")

        private fun Properties.requiredNonNegativeInt(key: String): Int =
            requiredInt(key).also { require(it >= 0) { "Save slot metadata '$key' must not be negative." } }

        private fun Properties.requiredNonNegativeLong(key: String): Long =
            (required(key).toLongOrNull() ?: error("Save slot metadata '$key' is not a long.")).also {
                require(it >= 0) { "Save slot metadata '$key' must not be negative." }
            }

        private fun Properties.optionalNonNegativeLong(key: String): Long? {
            val raw = getProperty(key) ?: return null
            val value = raw.toLongOrNull() ?: error("Save slot metadata '$key' is not a long.")
            require(value >= 0) { "Save slot metadata '$key' must not be negative." }
            return value
        }

        private fun Long.safelyIncrement(label: String): Long {
            require(this < Long.MAX_VALUE) { "$label overflow." }
            return this + 1
        }

        private fun safelyAdd(value: Long, increment: Long): Long =
            if (value > Long.MAX_VALUE - increment) Long.MAX_VALUE else value + increment
    }
}
