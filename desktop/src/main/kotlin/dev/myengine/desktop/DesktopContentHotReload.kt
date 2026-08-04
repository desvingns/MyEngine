package dev.myengine.desktop

import dev.myengine.content.ContentLoadResult
import dev.myengine.content.ContentPackLoader
import dev.myengine.content.ContentRegistry
import dev.myengine.content.ContentValidationError
import dev.myengine.games.sandbox.SandboxGame
import dev.myengine.games.sandbox.SandboxScenarioResult
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.StandardWatchEventKinds.OVERFLOW
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.concurrent.TimeUnit

enum class DesktopReloadStatus(val id: String) {
    STARTED("started"),
    RELOADED("reloaded"),
    REJECTED("rejected"),
}

data class DesktopReloadOutcome(
    val status: DesktopReloadStatus,
    val packRoot: Path,
    val seed: Long,
    val elapsedMillis: Long,
    val scenario: SandboxScenarioResult?,
    val errors: List<ContentValidationError>,
    val lastGoodHash: String?,
)

/**
 * Validates and atomically swaps the desktop scenario at pack boundaries.
 *
 * A new [SandboxScenarioResult] is published only after both content validation and the
 * deterministic scenario restart succeed. A failed reload therefore leaves [currentScenario]
 * untouched, which is the desktop equivalent of keeping the last-good pack running.
 */
class DesktopContentHotReloadSession(
    packRoot: Path,
    private val seed: Long = DEFAULT_SEED,
    private val loader: (Path) -> ContentLoadResult = ContentPackLoader::load,
    private val scenarioRunner: (ContentRegistry, Long) -> SandboxScenarioResult =
        { registry, scenarioSeed -> SandboxGame.runScriptedScenario(registry, scenarioSeed) },
) {
    val packRoot: Path = packRoot.toAbsolutePath().normalize()

    var currentScenario: SandboxScenarioResult? = null
        private set

    fun start(): DesktopReloadOutcome = loadAndRestart(DesktopReloadStatus.STARTED)

    fun reload(): DesktopReloadOutcome = loadAndRestart(DesktopReloadStatus.RELOADED)

    private fun loadAndRestart(status: DesktopReloadStatus): DesktopReloadOutcome {
        val startedAt = System.nanoTime()
        val loaded = try {
            loader(packRoot)
        } catch (error: Exception) {
            ContentLoadResult(
                registry = null,
                errors = listOf(
                    ContentValidationError(
                        file = "<pack>",
                        id = "pack",
                        field = "load",
                        message = error.message ?: error::class.simpleName.orEmpty(),
                    ),
                ),
            )
        }

        if (!loaded.isValid) {
            return rejected(startedAt, loaded.errors)
        }

        val registry = loaded.registry
            ?: return rejected(
                startedAt,
                listOf(ContentValidationError("<pack>", "pack", "registry", "Valid load had no registry.")),
            )
        val restarted = try {
            scenarioRunner(registry, seed)
        } catch (error: Exception) {
            return rejected(
                startedAt,
                listOf(
                    ContentValidationError(
                        file = "<scenario>",
                        id = registry.manifest.id,
                        field = "restart",
                        message = error.message ?: error::class.simpleName.orEmpty(),
                    ),
                ),
            )
        }

        currentScenario = restarted
        return DesktopReloadOutcome(
            status = status,
            packRoot = packRoot,
            seed = seed,
            elapsedMillis = elapsedMillisSince(startedAt),
            scenario = restarted,
            errors = emptyList(),
            lastGoodHash = restarted.hash,
        )
    }

    private fun rejected(
        startedAt: Long,
        errors: List<ContentValidationError>,
    ): DesktopReloadOutcome = DesktopReloadOutcome(
        status = DesktopReloadStatus.REJECTED,
        packRoot = packRoot,
        seed = seed,
        elapsedMillis = elapsedMillisSince(startedAt),
        scenario = currentScenario,
        errors = errors.ifEmpty {
            listOf(ContentValidationError("<pack>", "pack", "load", "Content pack was rejected."))
        },
        lastGoodHash = currentScenario?.hash,
    )

    private fun elapsedMillisSince(startedAt: Long): Long =
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)

    companion object {
        const val DEFAULT_SEED: Long = 7L
    }
}

/**
 * Desktop-only recursive [WatchService] adapter. It reports a single change after a short burst
 * debounce so a multi-file content save produces one validate-then-restart operation.
 */
class DesktopContentPackWatcher(
    private val packRoot: Path,
    private val debounceMillis: Long = DEFAULT_DEBOUNCE_MILLIS,
) : AutoCloseable {
    private val service: WatchService = FileSystems.getDefault().newWatchService()
    private val directoriesByKey = mutableMapOf<WatchKey, Path>()

    init {
        require(Files.isDirectory(packRoot)) { "Content pack directory does not exist: $packRoot" }
        registerDirectories(packRoot)
    }

    /** Blocks until interrupted, invoking [onChange] once per debounced filesystem burst. */
    fun runUntilInterrupted(onChange: () -> Unit) {
        try {
            while (!Thread.currentThread().isInterrupted) {
                val key = service.take()
                var changed = process(key)
                if (changed && debounceMillis > 0) {
                    Thread.sleep(debounceMillis)
                    while (true) {
                        val pending = service.poll() ?: break
                        changed = process(pending) || changed
                    }
                }
                if (changed) onChange()
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            close()
        }
    }

    override fun close() {
        service.close()
        directoriesByKey.clear()
    }

    private fun process(key: WatchKey): Boolean {
        var changed = false
        val watchedDirectory = directoriesByKey[key]
        for (event in key.pollEvents()) {
            if (event.kind() == OVERFLOW) {
                changed = true
                continue
            }
            changed = true
            if (event.kind() == ENTRY_CREATE && watchedDirectory != null) {
                val created = watchedDirectory.resolve(event.context() as Path)
                if (Files.isDirectory(created)) registerDirectories(created)
            }
        }
        if (!key.reset()) directoriesByKey.remove(key)
        return changed
    }

    private fun registerDirectories(root: Path) {
        try {
            Files.walk(root).use { paths ->
                paths.filter { Files.isDirectory(it) }.forEach { directory ->
                    val key = directory.register(service, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE)
                    directoriesByKey[key] = directory
                }
            }
        } catch (error: IOException) {
            throw IllegalStateException("Unable to watch content pack directory $root", error)
        }
    }

    companion object {
        const val DEFAULT_DEBOUNCE_MILLIS: Long = 100L
    }
}
