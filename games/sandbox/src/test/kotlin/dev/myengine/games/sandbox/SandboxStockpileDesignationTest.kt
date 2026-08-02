package dev.myengine.games.sandbox

import dev.myengine.ai.JobStatus
import dev.myengine.core.CommandId
import dev.myengine.core.Tick
import dev.myengine.core.command.DefineStockpileZoneCommand
import dev.myengine.core.command.DesignateHarvestNodeCommand
import dev.myengine.core.command.TileCoordinate
import dev.myengine.render.Camera
import dev.myengine.render.PlaceholderRenderSurface
import dev.myengine.world.TilePosition
import dev.myengine.world.WorldSize
import java.io.StringReader
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SandboxStockpileDesignationTest {
    @Test
    fun stockpileCommandsNormalizeOrderAndValidateFiltersAgainstContentRegistry() {
        val registry = SandboxGame.loadRegistry()
        val runtime = SandboxGame.createRuntime(registry)

        runtime.submit(
            DefineStockpileZoneCommand(
                CommandId(1), Tick(1), "bad", listOf(TileCoordinate(8, 5)),
                allowedResourceIds = setOf("missing"),
            ),
        )
        runtime.step()
        assertEquals("unknown_stockpile_resource:bad", runtime.state.lastCommandOrError)
        assertEquals(emptyList(), runtime.state.zones.allStockpiles())

        runtime.submit(
            DefineStockpileZoneCommand(
                CommandId(2), Tick(2), "z-2",
                listOf(TileCoordinate(6, 5), TileCoordinate(5, 5)),
                allowedResourceIds = setOf("bolt"),
            ),
        )
        runtime.step()

        assertEquals(listOf("z-2"), runtime.state.zones.allStockpiles().map { it.id })
        assertEquals(
            listOf(TilePosition(5, 5), TilePosition(6, 5)),
            runtime.state.zones.stockpile("z-2")!!.normalizedTiles,
        )
        assertEquals(setOf("bolt"), runtime.state.zones.stockpile("z-2")!!.normalizedResourceIds)

        runtime.submit(
            DesignateHarvestNodeCommand(CommandId(3), Tick(3), "node-1", "bolt", TileCoordinate(5, 5)),
        )
        runtime.step()
        assertEquals(TilePosition(5, 5), runtime.state.zones.harvestDesignation("node-1")!!.position)
        assertEquals(1, runtime.state.zones.allHarvestDesignations().size)
    }

    @Test
    fun sameKindOverlapAndUnknownOrMismatchedHarvestAreRejectedAtomically() {
        val registry = SandboxGame.loadRegistry()
        val runtime = SandboxGame.createRuntime(registry)

        runtime.submit(DefineStockpileZoneCommand(CommandId(1), Tick(1), "z-1", listOf(TileCoordinate(8, 5))))
        runtime.submit(DefineStockpileZoneCommand(CommandId(2), Tick(1), "z-2", listOf(TileCoordinate(8, 5))))
        runtime.step()
        assertEquals(listOf("z-1"), runtime.state.zones.allStockpiles().map { it.id })
        assertTrue(runtime.state.lastCommandOrError!!.startsWith("stockpile_rejected:z-2:"))

        runtime.submit(DesignateHarvestNodeCommand(CommandId(3), Tick(2), "bad-resource", "missing", TileCoordinate(5, 5)))
        runtime.submit(DesignateHarvestNodeCommand(CommandId(4), Tick(2), "bad-node", "bolt", TileCoordinate(8, 5)))
        runtime.step()
        assertEquals(emptyList(), runtime.state.zones.allHarvestDesignations())
        assertEquals(null, runtime.state.jobBoard.get("harvest-node:bad-resource"))
        assertEquals(null, runtime.state.jobBoard.get("harvest-node:bad-node"))
    }

    @Test
    fun harvestDesignationMintsExactlyOneDeterministicOpenJob() {
        val registry = SandboxGame.loadRegistry()
        val runtime = SandboxGame.createRuntime(registry)
        val command = DesignateHarvestNodeCommand(
            CommandId(1), Tick(1), "node-1", "bolt", TileCoordinate(5, 5), actorId = 42L,
        )
        runtime.submit(command)
        runtime.step()

        assertEquals("harvest-node:node-1", runtime.state.zones.harvestDesignation("node-1")!!.jobId)
        assertEquals(
            dev.myengine.ai.Job(
                id = "harvest-node:node-1", type = "harvest_node",
                target = TilePosition(5, 5), priority = 0,
            ),
            runtime.state.jobBoard.get("harvest-node:node-1"),
        )

        runtime.submit(DesignateHarvestNodeCommand(CommandId(2), Tick(2), "node-1", "bolt", TileCoordinate(5, 5)))
        runtime.step()
        assertEquals(1, runtime.state.jobBoard.all().count { it.type == "harvest_node" })
        assertEquals("duplicate_harvest_designation:node-1", runtime.state.lastCommandOrError)
        assertEquals(JobStatus.OPEN, runtime.state.jobBoard.get("harvest-node:node-1")!!.status)
    }

    @Test
    fun snapshotZonesAreOrderedImmutableAndRenderProjectionDoesNotMutateState() {
        val registry = SandboxGame.loadRegistry()
        val runtime = SandboxGame.createRuntime(registry)
        runtime.submit(DefineStockpileZoneCommand(CommandId(1), Tick(1), "z-2", listOf(TileCoordinate(7, 5))))
        runtime.submit(DefineStockpileZoneCommand(CommandId(2), Tick(1), "z-1", listOf(TileCoordinate(8, 5))))
        runtime.submit(DesignateHarvestNodeCommand(CommandId(3), Tick(1), "node-1", "bolt", TileCoordinate(5, 5)))
        runtime.step()

        val snapshot = runtime.snapshot()
        val hashBefore = runtime.state.stableHash()
        assertEquals(listOf("z-1", "z-2", "node-1"), snapshot.zones.map { it.id })
        assertEquals(listOf(TilePosition(5, 5)), snapshot.zones.single { it.id == "node-1" }.tiles)
        assertFailsWith<UnsupportedOperationException> {
            @Suppress("UNCHECKED_CAST")
            (snapshot.zones as MutableList<Any?>).clear()
        }
        assertFailsWith<UnsupportedOperationException> {
            @Suppress("UNCHECKED_CAST")
            (snapshot.zones.first().tiles as MutableList<Any?>).clear()
        }

        PlaceholderRenderSurface().project(
            snapshot,
            Camera(WorldSize(snapshot.worldSize.width, snapshot.worldSize.height), 480f, 480f),
        )
        assertEquals(hashBefore, runtime.state.stableHash())

        runtime.submit(DefineStockpileZoneCommand(CommandId(4), Tick(2), "z-3", listOf(TileCoordinate(9, 5))))
        runtime.step()
        assertEquals(listOf("z-1", "z-2", "node-1"), snapshot.zones.map { it.id })
        assertNotEquals(snapshot, runtime.snapshot())
    }

    @Test
    fun zoneStateRoundtripAndReplayHashAreDeterministic() {
        fun run(): Pair<String, Map<String, String>> {
            val runtime = SandboxGame.createRuntime(SandboxGame.loadRegistry())
            runtime.submit(DefineStockpileZoneCommand(CommandId(1), Tick(1), "z-1", listOf(TileCoordinate(8, 5)), setOf("bolt")))
            runtime.submit(DesignateHarvestNodeCommand(CommandId(2), Tick(2), "node-1", "bolt", TileCoordinate(5, 5)))
            runtime.step(2)
            val save = SandboxSaveCodec.encode(runtime.state, seed = 7L)
            val properties = Properties().also { it.load(StringReader(save)) }
            return runtime.state.stableHash() to mapOf(
                "saveVersion" to properties.getProperty("saveVersion"),
                "stockpileZones" to properties.getProperty("stockpileZones"),
                "harvestDesignations" to properties.getProperty("harvestDesignations"),
                "jobs" to properties.getProperty("jobs"),
            )
        }

        val first = run()
        val second = run()
        assertEquals(first, second)

        val registry = SandboxGame.loadRegistry()
        val runtime = SandboxGame.createRuntime(registry)
        runtime.submit(DefineStockpileZoneCommand(CommandId(1), Tick(1), "z-1", listOf(TileCoordinate(8, 5)), setOf("bolt")))
        runtime.submit(DesignateHarvestNodeCommand(CommandId(2), Tick(2), "node-1", "bolt", TileCoordinate(5, 5)))
        runtime.step(2)
        val restored = SandboxSaveCodec.decode(SandboxSaveCodec.encode(runtime.state, seed = 7L), registry)
        assertEquals(runtime.state.stableHash(), restored.stableHash())
        assertEquals(listOf("z-1"), restored.zones.allStockpiles().map { it.id })
        assertEquals(listOf("node-1"), restored.zones.allHarvestDesignations().map { it.id })
        assertEquals(JobStatus.OPEN, restored.jobBoard.get("harvest-node:node-1")!!.status)
    }

    @Test
    fun v1ThroughV13MigrationProducesEmptyZoneStateAndFutureVersionIsRejected() {
        val registry = SandboxGame.loadRegistry()
        (1..13).forEach { version ->
            val save = fixture(version)
            val decoded = SandboxSaveCodec.decode(save, registry)
            assertEquals(emptyList(), decoded.zones.allStockpiles(), "v$version stockpiles")
            assertEquals(emptyList(), decoded.zones.allHarvestDesignations(), "v$version designations")
        }

        val current = SandboxSession.start(registry).save()
        val future = current.replace(
            "saveVersion=${SandboxSaveCodec.SAVE_VERSION}",
            "saveVersion=${SandboxSaveCodec.SAVE_VERSION + 1}",
        )
        assertFailsWith<IllegalArgumentException> { SandboxSaveCodec.decode(future, registry) }
        assertFailsWith<IllegalArgumentException> { SandboxSaveCodec.decodePendingCommands(future) }
    }

    @Test
    fun pendingZoneAndDesignationCommandsRestoreWithExactIdentityAndContinuation() {
        val registry = SandboxGame.loadRegistry()
        val define = DefineStockpileZoneCommand(
            CommandId(7), Tick(5), "z-1", listOf(TileCoordinate(8, 5)), setOf("bolt"), actorId = 11L,
        )
        val designate = DesignateHarvestNodeCommand(
            CommandId(8), Tick(6), "node-1", "bolt", TileCoordinate(5, 5), actorId = 12L,
        )

        val uninterrupted = SandboxSession.start(registry).also {
            it.submit(define)
            it.submit(designate)
            it.step(8)
        }
        val paused = SandboxSession.start(registry).also {
            it.submit(define)
            it.submit(designate)
            it.step(2)
        }
        val restored = SandboxSession.restore(paused.save(), registry)
        val pending = restored.runtime.pendingCommands()
        assertEquals(listOf(define, designate), pending)
        assertEquals(listOf(define.type, designate.type), pending.map { it.type })
        assertEquals(listOf(define.actorId, designate.actorId), pending.map { it.actorId })
        assertEquals(listOf(define.stablePayload(), designate.stablePayload()), pending.map { it.stablePayload() })

        restored.step(6)
        assertEquals(uninterrupted.stableHash(), restored.stableHash())
        assertEquals(listOf("z-1"), restored.runtime.state.zones.allStockpiles().map { it.id })
        assertEquals(listOf("node-1"), restored.runtime.state.zones.allHarvestDesignations().map { it.id })
    }

    private fun fixture(version: Int): String =
        requireNotNull(javaClass.getResourceAsStream("/save-fixtures/v$version.properties")) {
            "Missing checked-in save migration fixture v$version."
        }.bufferedReader().use { it.readText() }
}
