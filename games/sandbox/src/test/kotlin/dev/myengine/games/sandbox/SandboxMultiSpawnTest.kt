package dev.myengine.games.sandbox

import dev.myengine.content.ContentPackLoader
import dev.myengine.content.ContentRegistry
import dev.myengine.content.IncidentContent
import dev.myengine.content.IncidentEffectDescriptor
import dev.myengine.content.MapContent
import dev.myengine.content.MapCoordinate
import dev.myengine.content.MapSpawn
import dev.myengine.content.MapTerminalRules
import dev.myengine.content.MapTerrainSymbol
import dev.myengine.content.MapWinCondition
import dev.myengine.content.WaveContent
import dev.myengine.content.WaveSpawn
import dev.myengine.core.CommandId
import dev.myengine.core.Tick
import dev.myengine.core.command.CallWaveEarlyCommand
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SandboxMultiSpawnTest {
    @Test
    fun omittedDefaultAndAllWaveSelectionRouteThroughEverySortedSpawnId() {
        listOf("omitted/default" to "", "all" to "all").forEach { (selectionCase, rawSelection) ->
            val runtime = fixtureRuntime(
                selection = null,
                startTick = 1,
                sourceWaveSelection = rawSelection,
            )

            runtime.step()

            assertEquals(
                listOf(MapCoordinate(0, 0), MapCoordinate(6, 6)),
                spawnedCoordinates(runtime),
                "The $selectionCase selection must route through alpha then zulu.",
            )
            assertEquals(2, runtime.state.defense.metrics.enemiesSpawned)
        }
    }

    @Test
    fun earlyCalledWaveUsesTheSameSortedMultiSpawnRouting() {
        val runtime = fixtureRuntime(selection = null, startTick = 5)
        runtime.submit(CallWaveEarlyCommand(CommandId(1), Tick(1)))

        runtime.step()

        assertEquals(listOf(MapCoordinate(0, 0), MapCoordinate(6, 6)), spawnedCoordinates(runtime))
        assertEquals(setOf("multi-wave"), runtime.state.defense.spawnedWaveIds)
        assertEquals("wave_called:multi-wave", runtime.state.lastCommandOrError)
    }

    @Test
    fun incidentTriggeredWaveUsesTheSameSortedMultiSpawnRouting() {
        val incident = IncidentContent(
            id = "route-incident",
            minThreat = 0,
            maxThreat = 100,
            weight = 1,
            cadenceStartTick = 1,
            cadenceIntervalTicks = 1,
            cooldownTicks = 10,
            effects = listOf(IncidentEffectDescriptor.SpawnWave("multi-wave")),
        )
        val runtime = fixtureRuntime(
            selection = null,
            startTick = 50,
            incidents = mapOf(incident.id to incident),
        )

        runtime.step()

        assertEquals(listOf(MapCoordinate(0, 0), MapCoordinate(6, 6)), spawnedCoordinates(runtime))
        assertEquals(setOf("multi-wave"), runtime.state.defense.spawnedWaveIds)
        assertEquals("incident_applied:route-incident", runtime.state.lastCommandOrError)
    }

    @Test
    fun saveBeforeFutureScheduledRoutedWaveRestoresContinuationAndStableHashWithoutVersionBump() {
        val registry = fixtureRegistry(selection = null, startTick = 3)
        val uninterrupted = SandboxSession.start(registry, seed = 37).also { it.step(4) }
        val paused = SandboxSession.start(registry, seed = 37).also { it.step(1) }
        val save = paused.save()

        assertTrue(paused.runtime.state.entities.byTag("enemy").isEmpty())
        assertEquals(16, SandboxSaveCodec.SAVE_VERSION)
        assertEquals("16", saveProperty(save, "saveVersion"))

        val restored = SandboxSession.restore(save, registry)

        assertEquals(paused.stableHash(), restored.stableHash())
        assertTrue(restored.runtime.state.entities.byTag("enemy").isEmpty())

        paused.step(3)
        restored.step(3)

        assertEquals(uninterrupted.stableHash(), paused.stableHash())
        assertEquals(uninterrupted.stableHash(), restored.stableHash())
        assertEquals(
            uninterrupted.runtime.state.entities.all(),
            restored.runtime.state.entities.all(),
        )
        assertEquals(setOf("multi-wave"), restored.runtime.state.defense.spawnedWaveIds)
    }

    @Test
    fun sandboxRoutesEachWaveBySortedSpawnIdAndRetainsAuthoredSpawnOrder() {
        fun runOnce(): SandboxRuntime {
            val runtime = SandboxGame.createRuntime(multiSpawnRegistry(), mapId = "multi-spawn", seed = 19)
            runtime.step()
            return runtime
        }

        val first = runOnce()
        val second = runOnce()
        val enemies = first.state.entities.byTag("enemy")

        assertEquals(first.state.stableHash(), second.state.stableHash())
        assertEquals(
            listOf(
                "enemy:scout", "enemy:drift", "enemy:drift",
                "enemy:scout", "enemy:drift", "enemy:drift",
            ),
            enemies.map { it.type },
        )
        assertEquals(
            listOf(
                MapCoordinate(0, 0), MapCoordinate(0, 0), MapCoordinate(0, 0),
                MapCoordinate(6, 6), MapCoordinate(6, 6), MapCoordinate(6, 6),
            ),
            enemies.map { entity ->
                val position = requireNotNull(entity.position).tile
                MapCoordinate(position.x, position.y)
            },
        )
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L, 6L), enemies.map { it.id.value })
        assertEquals(6, first.state.defense.metrics.enemiesSpawned)
        assertEquals(setOf("multi-wave"), first.state.defense.spawnedWaveIds)
    }

    @Test
    fun midWaveSaveRestorePreservesHashAndContinuationWithoutSaveVersionBump() {
        val registry = multiSpawnRegistry()
        val uninterrupted = SandboxSession(
            SandboxGame.createRuntime(registry, mapId = "multi-spawn", seed = 23),
            seed = 23,
        )
        uninterrupted.step(3)

        val paused = SandboxSession(
            SandboxGame.createRuntime(registry, mapId = "multi-spawn", seed = 23),
            seed = 23,
        )
        paused.step()
        val save = paused.save()
        val restored = SandboxSession.restore(save, registry)

        assertEquals(16, SandboxSaveCodec.SAVE_VERSION)
        assertEquals("16", saveProperty(save, "saveVersion"))
        assertEquals(paused.stableHash(), restored.stableHash())
        assertEquals(
            paused.runtime.state.entities.all(),
            restored.runtime.state.entities.all(),
            "The active multi-spawn wave must survive the save boundary.",
        )

        paused.step(2)
        restored.step(2)

        assertEquals(uninterrupted.stableHash(), paused.stableHash())
        assertEquals(uninterrupted.stableHash(), restored.stableHash())
        assertTrue(restored.runtime.state.entities.byTag("enemy").isNotEmpty())
    }

    private fun multiSpawnRegistry(): ContentRegistry {
        val base = SandboxGame.loadRegistry()
        val drift = base.requireEnemy("drift").copy(speedTilesPerTick = 0)
        val scout = drift.copy(id = "scout")
        val map = MapContent(
            id = "multi-spawn",
            width = 7,
            height = 7,
            terrainRows = listOf(
                ".......",
                ".......",
                ".......",
                "...C...",
                ".......",
                ".......",
                ".......",
            ),
            terrainMapping = mapOf(
                '.' to MapTerrainSymbol("floor"),
                'C' to MapTerrainSymbol("core"),
            ),
            spawns = mapOf(
                "zulu" to MapSpawn("zulu", MapCoordinate(6, 6)),
                "alpha" to MapSpawn("alpha", MapCoordinate(0, 0)),
            ),
            core = MapCoordinate(3, 3),
            terminalRules = MapTerminalRules(MapWinCondition.NO_WIN),
        )
        return base.copy(
            enemies = base.enemies + (drift.id to drift) + (scout.id to scout),
            waves = mapOf(
                "multi-wave" to WaveContent(
                    id = "multi-wave",
                    startTick = 1,
                    spawns = listOf(WaveSpawn("scout", 1), WaveSpawn("drift", 2)),
                    spawnSelection = listOf("zulu", "alpha"),
                ),
            ),
            maps = mapOf(map.id to map),
            incidents = emptyMap(),
        )
    }

    private fun fixtureRuntime(
        selection: List<String>?,
        startTick: Long,
        incidents: Map<String, IncidentContent> = emptyMap(),
        sourceWaveSelection: String? = null,
    ): SandboxRuntime = SandboxGame.createRuntime(
        fixtureRegistry(selection, startTick, incidents, sourceWaveSelection),
        mapId = "multi-spawn-fixture-map",
        seed = 29,
    )

    private fun fixtureRegistry(
        selection: List<String>?,
        startTick: Long,
        incidents: Map<String, IncidentContent> = emptyMap(),
        sourceWaveSelection: String? = null,
    ): ContentRegistry {
        val sourceRoot = sourceWaveSelection?.let(::fixtureRootWithWaveSelection) ?: fixtureRoot()
        val loaded = ContentPackLoader.load(sourceRoot)
        check(loaded.isValid) { loaded.errors.joinToString("\n") }
        val registry = loaded.registry!!
        val stationaryScout = registry.requireEnemy("scout").copy(speedTilesPerTick = 0)
        val fixtureRecipe = registry.recipes.getValue("generator")
        val wave = registry.waves.getValue("multi-wave").copy(
            startTick = startTick,
            spawnSelection = selection,
        )
        return registry.copy(
            enemies = registry.enemies + (stationaryScout.id to stationaryScout),
            recipes = registry.recipes + ("bolt-generator" to fixtureRecipe.copy(id = "bolt-generator")),
            waves = mapOf(wave.id to wave),
            incidents = incidents,
        )
    }

    private fun fixtureRoot(): Path {
        val manifest = requireNotNull(javaClass.getResource("/content-fixtures/multi-spawn/manifest.properties"))
        return Paths.get(manifest.toURI()).parent
    }

    private fun fixtureRootWithWaveSelection(rawSelection: String): Path {
        val source = fixtureRoot()
        val target = Files.createTempDirectory("myengine-multi-spawn-fixture")
        Files.walk(source).use { paths ->
            paths.filter { Files.isRegularFile(it) }.forEach { file ->
                val destination = target.resolve(source.relativize(file).toString())
                Files.createDirectories(destination.parent)
                Files.copy(file, destination)
            }
        }
        val waves = target.resolve("waves.properties")
        val withoutSelection = Files.readString(waves).replace(
            Regex("(?m)^multi-wave\\.spawnSelection=.*(?:\\r?\\n|$)"),
            "",
        )
        val replacement = rawSelection.takeIf(String::isNotBlank)
            ?.let { "multi-wave.spawnSelection=$it\n" }
            .orEmpty()
        Files.writeString(waves, withoutSelection + replacement)
        return target
    }

    private fun spawnedCoordinates(runtime: SandboxRuntime): List<MapCoordinate> =
        runtime.state.entities.byTag("enemy").map { entity ->
            val position = requireNotNull(entity.position).tile
            MapCoordinate(position.x, position.y)
        }

    private fun saveProperty(text: String, key: String): String? =
        Properties().also { it.load(StringReader(text)) }.getProperty(key)
}
