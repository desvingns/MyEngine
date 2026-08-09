package dev.myengine.devtools

import dev.myengine.games.sandbox.SandboxGame
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaytestBotTest {
    @Test
    fun repeatedInputsProduceByteIdenticalReportWithStableCurve() {
        val first = PlaytestBot.run(SandboxGame.contentRoot(), seedStart = 7L, seedCount = 2, maxTicks = 200)
        val second = PlaytestBot.run(SandboxGame.contentRoot(), seedStart = 7L, seedCount = 2, maxTicks = 200)

        assertEquals(first.toJson(), second.toJson())
        val json = Json.parseToJsonElement(first.toJson()).jsonObject
        assertEquals("proc-008-playtest-v1", json.getValue("schema").jsonPrimitive.content)
        assertEquals("sandbox", json.getValue("pack_id").jsonPrimitive.content)
        assertEquals(2, json.getValue("seed_count").jsonPrimitive.content.toInt())
        assertEquals(3, json.getValue("strategies").jsonArray.size)
        assertEquals(listOf("wave-1", "wave-2"), json.getValue("difficulty_curve").jsonArray.map {
            it.jsonObject.getValue("wave_id").jsonPrimitive.content
        })
        assertEquals(2, json.getValue("strategies").jsonArray.first().jsonObject.getValue("run_count").jsonPrimitive.content.toInt())
        assertTrue(json.getValue("difficulty_curve").jsonArray.all { row ->
            row.jsonObject.getValue("enemy_count").jsonPrimitive.content.toLong() > 0L &&
                row.jsonObject.getValue("total_health").jsonPrimitive.content.toLong() > 0L
        })
        assertTrue(findKeys(json).none { it in setOf("sim_ms", "elapsed_ns", "wall_clock_ms") })
    }

    @Test
    fun seedRangeAndStrategyAggregatesAreExplicit() {
        val report = PlaytestBot.run(SandboxGame.contentRoot(), seedStart = 41L, seedCount = 3, maxTicks = 200)

        assertEquals(listOf("no-build", "spawn-tower", "late-tower"), report.strategies.map { it.strategyId })
        report.strategies.forEach { strategy ->
            assertEquals(listOf(41L, 42L, 43L), strategy.runs.map { it.seed })
            assertTrue(strategy.wins + strategy.losses + strategy.timeouts == 3)
            assertTrue(strategy.runs.all { it.status in setOf("won", "lost", "timeout") })
        }
    }

    @Test
    fun maxTicksProducesTypedTimeoutWithoutChangingReportShape() {
        val report = PlaytestBot.run(SandboxGame.contentRoot(), seedCount = 1, maxTicks = 1)

        assertTrue(report.strategies.all { strategy -> strategy.runs.single().status == "timeout" })
        assertTrue(report.strategies.all { strategy -> strategy.runs.single().ticks == 1L })
        assertTrue(report.toJson().isNotBlank())
    }

    private fun findKeys(element: JsonElement): Set<String> = when (element) {
        is JsonObject -> (element.keys + element.values.flatMap(::findKeys)).toSet()
        is JsonArray -> element.flatMap(::findKeys).toSet()
        else -> emptySet()
    }
}
