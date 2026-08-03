package dev.myengine.devtools

import dev.myengine.games.sandbox.SandboxGame
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HeadlessStateInspectorTest {
    private val packRoot = SandboxGame.contentRoot()

    @Test
    fun identicalInputsProduceByteIdenticalStructuredOutput() {
        val inspector = HeadlessStateInspector()
        val first = inspector.inspect("sandbox", "default", packRoot, ticks = 6, seed = 17L)
        val second = inspector.inspect("sandbox", "default", packRoot, ticks = 6, seed = 17L)

        val firstJson = first.toJson()
        val secondJson = second.toJson()
        assertEquals(firstJson, secondJson)
        assertTrue(
            firstJson.toByteArray(StandardCharsets.UTF_8)
                .contentEquals(secondJson.toByteArray(StandardCharsets.UTF_8)),
            "same inspector arguments must produce byte-identical UTF-8 JSON",
        )
        val json = Json.parseToJsonElement(firstJson).jsonObject
        assertEquals(setOf("factory", "scenario", "pack_id", "requested_ticks", "ascii_frame", "state"), json.keys)
        assertEquals(6, json.getValue("requested_ticks").jsonPrimitive.content.toInt())
        val ascii = json.getValue("ascii_frame").jsonPrimitive.content
        assertTrue(ascii.startsWith("tick=6"))
        assertTrue(ascii.isNotBlank())
        assertTrue(ascii.all { it.code in 0..127 }, "ASCII frame must contain ASCII characters only")

        val state = json.getValue("state").jsonObject
        assertEquals(6L, state.getValue("tick").jsonPrimitive.content.toLong())
        assertTrue(state.getValue("entities").jsonArray.isEmpty())
        assertTrue(state.getValue("inventories").jsonObject.containsKey("global"))
        assertEquals(
            setOf(
                "core_health",
                "enemies_spawned",
                "enemies_killed",
                "enemies_leaked",
                "core_damage",
                "tower_shots",
                "tower_metrics",
            ),
            state.getValue("defense_metrics").jsonObject.keys,
        )
        assertTrue(state.getValue("hash").jsonPrimitive.content.isNotBlank())
        assertFalse(findKeys(json).any { it in setOf("sim_ms", "wall_clock_ms", "elapsed_ns") })
    }

    @Test
    fun optionalCommandScriptUsesAuthoritativeRuntimeAndRemainsDeterministic() {
        val inspector = HeadlessStateInspector()
        val script = listOf("1:build_tower:pulse:2:2")
        val baseline = inspector.inspect("sandbox", "default", packRoot, ticks = 3, seed = 7L)
        val first = inspector.inspect("sandbox", "default", packRoot, ticks = 3, script, seed = 7L)
        val second = inspector.inspect("sandbox", "default", packRoot, ticks = 3, script, seed = 7L)

        assertEquals(first.toJson(), second.toJson())
        assertTrue(
            first.toJson().toByteArray(StandardCharsets.UTF_8)
                .contentEquals(second.toJson().toByteArray(StandardCharsets.UTF_8)),
            "the same command script must produce identical bounded output",
        )
        assertEquals(3L, first.state.tick)
        val entities = Json.parseToJsonElement(first.toJson()).jsonObject
            .getValue("state").jsonObject.getValue("entities").jsonArray
        assertTrue(entities.any { it.jsonObject.getValue("type").jsonPrimitive.content == "tower:pulse" })
        assertTrue(first.state.hash != baseline.state.hash, "building a tower must affect state hash")
        assertTrue(first.state.inventories != baseline.state.inventories, "building a tower must affect inventory state")
    }

    @Test
    fun shortCliFormPrintsOneParseableInspectionObject() {
        val output = captureStdout { main(arrayOf("inspect", "0")) }.trim()
        val json = Json.parseToJsonElement(output).jsonObject

        assertEquals(1, output.lineSequence().count())
        assertEquals("sandbox", json.getValue("factory").jsonPrimitive.content)
        assertEquals(0, json.getValue("requested_ticks").jsonPrimitive.content.toInt())
        assertEquals(0L, json.getValue("state").jsonObject.getValue("tick").jsonPrimitive.content.toLong())
        assertTrue(json.getValue("state").jsonObject.containsKey("defense_metrics"))
        assertFalse(findKeys(json).any { it in setOf("sim_ms", "wall_clock_ms", "elapsed_ns") })
    }

    @Test
    fun invalidTickAndCommandScriptAreRejected() {
        val inspector = HeadlessStateInspector()

        assertFailsWith<IllegalArgumentException> {
            inspector.inspect("sandbox", "default", packRoot, ticks = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            inspector.inspect("sandbox", "default", packRoot, ticks = 1, commandScript = listOf("not-a-command"))
        }
    }

    @Test
    fun injectedFactoryProvesInspectorDoesNotDependOnSandboxProvider() {
        var steppedTicks = 0
        val factory = object : HeadlessScenarioFactory {
            override val id: String = "fake"

            override fun create(packRoot: java.nio.file.Path, scenarioId: String, seed: Long): HeadlessScenario =
                object : HeadlessScenario {
                    override val scenarioId: String = scenarioId
                    override val packId: String = "fake-pack"

                    override fun submitScriptCommand(command: String) = Unit

                    override fun step(ticks: Int) {
                        steppedTicks += ticks
                    }

                    override fun asciiFrame(): String = "fake-ascii"

                    override fun stateDump(): HeadlessStateDump = HeadlessStateDump(
                        tick = steppedTicks.toLong(),
                        entities = emptyList(),
                        inventories = mapOf("global" to emptyMap()),
                        defenseMetrics = HeadlessDefenseDump(
                            coreHealth = 1,
                            enemiesSpawned = 0,
                            enemiesKilled = 0,
                            enemiesLeaked = 0,
                            coreDamage = 0,
                            towerShots = 0,
                        ),
                        hash = "fake-hash-$seed",
                    )
                }
        }

        val report = HeadlessStateInspector(listOf(factory)).inspect(
            factoryId = "fake",
            scenarioId = "scenario",
            packRoot = Paths.get("."),
            ticks = 4,
            seed = 9L,
        )

        assertEquals("fake-pack", report.packId)
        assertEquals("fake-ascii", report.asciiFrame)
        assertEquals(4L, report.state.tick)
        assertEquals("fake-hash-9", report.state.hash)
    }

    private fun captureStdout(block: () -> Unit): String {
        val originalOut = System.out
        val out = ByteArrayOutputStream()
        System.setOut(PrintStream(out))
        return try {
            block()
            out.toString(Charsets.UTF_8)
        } finally {
            System.setOut(originalOut)
        }
    }

    private fun findKeys(element: JsonElement): Set<String> = when (element) {
        is JsonObject -> (element.keys + element.values.flatMap(::findKeys)).toSet()
        is JsonArray -> element.flatMap(::findKeys).toSet()
        else -> emptySet()
    }
}
