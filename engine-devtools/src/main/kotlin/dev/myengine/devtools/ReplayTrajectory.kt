package dev.myengine.devtools

import dev.myengine.games.sandbox.SandboxGame
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal const val DX003_TRAJECTORY_FORMAT = "dx-003-trajectory-v1"

private val TRAJECTORY_HASH_PATTERN = Regex("[0-9a-f]{16}")
private val TRAJECTORY_JSON = Json {
    ignoreUnknownKeys = false
    isLenient = false
}

data class ReplayDefinition(
    val scenario: String,
    val factory: String = "sandbox",
    val packRoot: Path = SandboxGame.contentRoot(),
    val pack: String = "games/sandbox/content/sandbox",
    val seed: Long = 7L,
    val requestedTicks: Int = 35,
    val commands: List<String> = defaultCommands(scenario),
    val resistPercent: Int? = if (scenario == "resist") 50 else null,
) {
    init {
        require(scenario in setOf("canonical", "kill", "resist")) {
            "Unknown DX-003 replay scenario '$scenario'."
        }
        require(factory == "sandbox") { "Unknown DX-003 replay factory '$factory'." }
        require(requestedTicks >= 0) { "Requested replay ticks must be non-negative." }
        require(resistPercent == null || resistPercent in 0..100) {
            "Resistance must be between 0 and 100 percent."
        }
        require(commands.all { it.isNotBlank() }) { "Replay commands cannot be blank." }
    }

    fun metadata(): ReplayTrajectoryMetadata = ReplayTrajectoryMetadata(
        format = DX003_TRAJECTORY_FORMAT,
        factory = factory,
        scenario = scenario,
        pack = pack,
        seed = seed,
        requestedTicks = requestedTicks,
        commands = commands,
        resistPercent = resistPercent,
    )

    companion object {
        fun fromMetadata(metadata: ReplayTrajectoryMetadata, packRoot: Path): ReplayDefinition =
            ReplayDefinition(
                scenario = metadata.scenario,
                factory = metadata.factory,
                packRoot = packRoot,
                pack = metadata.pack,
                seed = metadata.seed,
                requestedTicks = metadata.requestedTicks,
                commands = metadata.commands,
                resistPercent = metadata.resistPercent,
            )
    }
}

data class ReplayTrajectoryMetadata(
    val format: String,
    val factory: String,
    val scenario: String,
    val pack: String,
    val seed: Long,
    val requestedTicks: Int,
    val commands: List<String>,
    val resistPercent: Int?,
) {
    fun toJson(): String = buildJson(
        "format" to format,
        "factory" to factory,
        "scenario" to scenario,
        "pack" to pack,
        "seed" to seed,
        "requested_ticks" to requestedTicks,
        "commands" to RawJson(commands.joinToString(",", prefix = "[", postfix = "]") { "\"${escape(it)}\"" }),
        "resist_percent" to resistPercent,
    )
}

data class ReplayTrajectoryRecord(
    val tick: Long,
    val hash: String,
    val state: JsonObject,
) {
    fun toJson(): String = buildJson(
        "tick" to tick,
        "hash" to hash,
        "state" to RawJson(state.toString()),
    )
}

data class ReplayTrajectory(
    val metadata: ReplayTrajectoryMetadata,
    val records: List<ReplayTrajectoryRecord>,
) {
    init {
        require(records.isNotEmpty()) { "DX-003 trajectory must contain tick 0." }
        require(records.first().tick == 0L) { "DX-003 trajectory must start at tick 0." }
        records.zipWithNext().forEach { (previous, current) ->
            require(current.tick == previous.tick + 1L) {
                "DX-003 trajectory ticks must be consecutive: ${previous.tick}, ${current.tick}."
            }
        }
        require(records.last().tick <= metadata.requestedTicks) {
            "DX-003 trajectory exceeds requested_ticks."
        }
    }

    fun toJsonLines(): String = buildString {
        append(metadata.toJson())
        records.forEach { record ->
            append('\n')
            append(record.toJson())
        }
    }

    fun write(path: Path) {
        path.parent?.let(Files::createDirectories)
        Files.writeString(path, toJsonLines(), StandardCharsets.UTF_8)
    }

    companion object {
        fun read(path: Path): ReplayTrajectory = parse(Files.readString(path, StandardCharsets.UTF_8))

        fun parse(text: String): ReplayTrajectory = try {
            parseUnchecked(text)
        } catch (error: ReplayTrajectoryFormatException) {
            throw error
        } catch (error: Exception) {
            throw ReplayTrajectoryFormatException("Invalid DX-003 trajectory: ${error.message ?: error::class.simpleName}.", error)
        }

        private fun parseUnchecked(text: String): ReplayTrajectory {
            val rawLines = text.split('\n').map { it.removeSuffix("\r") }
            val lines = if (rawLines.lastOrNull() == "") rawLines.dropLast(1) else rawLines
            requireFormat(lines.isNotEmpty() && lines.first().isNotBlank(), "Missing trajectory metadata line.")
            requireFormat(lines.drop(1).none { it.isBlank() }, "Trajectory JSONL cannot contain blank lines.")

            val metadata = parseMetadata(parseObject(lines.first(), 1))
            val records = lines.drop(1).mapIndexed { index, line ->
                parseRecord(parseObject(line, index + 2), index + 2)
            }
            requireFormat(records.isNotEmpty(), "DX-003 trajectory must contain at least one record.")
            return try {
                ReplayTrajectory(metadata, records)
            } catch (error: IllegalArgumentException) {
                throw ReplayTrajectoryFormatException(error.message ?: "Invalid trajectory ordering.", error)
            }
        }

        private fun parseMetadata(value: JsonObject): ReplayTrajectoryMetadata {
            requireKeys(
                value,
                setOf("format", "factory", "scenario", "pack", "seed", "requested_ticks", "commands", "resist_percent"),
                "metadata",
            )
            val format = requiredString(value, "format")
            requireFormat(format == DX003_TRAJECTORY_FORMAT, "Unsupported trajectory format '$format'.")
            val factory = requiredString(value, "factory")
            val scenario = requiredString(value, "scenario")
            val pack = requiredString(value, "pack")
            val seed = requiredLong(value, "seed")
            val requestedTicks = requiredInt(value, "requested_ticks")
            requireFormat(requestedTicks >= 0, "requested_ticks must be non-negative.")
            val commandsElement = value["commands"]
            val commands = commandsElement?.jsonArray?.mapIndexed { index, item ->
                val command = item.jsonPrimitive.content
                requireFormat(command.isNotBlank(), "commands[$index] cannot be blank.")
                command
            } ?: invalid("commands must be a JSON array of strings.")
            val resistElement = value["resist_percent"]
            val resistPercent = when (resistElement) {
                null, JsonNull -> null
                else -> resistElement.jsonPrimitive.content.toIntOrNull()
                    ?: invalid("resist_percent must be an integer or null.")
            }
            requireFormat(resistPercent == null || resistPercent in 0..100, "resist_percent must be between 0 and 100.")
            return try {
                ReplayTrajectoryMetadata(format, factory, scenario, pack, seed, requestedTicks, commands, resistPercent)
            } catch (error: IllegalArgumentException) {
                throw ReplayTrajectoryFormatException(error.message ?: "Invalid trajectory metadata.", error)
            }
        }

        private fun parseRecord(value: JsonObject, line: Int): ReplayTrajectoryRecord {
            requireKeys(value, setOf("tick", "hash", "state"), "record on line $line")
            val tick = requiredLong(value, "tick")
            requireFormat(tick >= 0L, "Line $line has a negative tick.")
            val hash = requiredString(value, "hash")
            requireFormat(hash.matches(TRAJECTORY_HASH_PATTERN), "Line $line has an invalid hash '$hash'.")
            val state = value["state"]?.jsonObject ?: invalid("Line $line state must be a JSON object.")
            val stateTick = state["tick"]?.jsonPrimitive?.content?.toLongOrNull()
                ?: invalid("Line $line state.tick is missing or invalid.")
            val stateHash = state["hash"]?.jsonPrimitive?.content
                ?: invalid("Line $line state.hash is missing or invalid.")
            requireFormat(stateTick == tick, "Line $line state.tick does not match record tick.")
            requireFormat(stateHash == hash, "Line $line state.hash does not match record hash.")
            return ReplayTrajectoryRecord(tick, hash, state)
        }

        private fun parseObject(line: String, lineNumber: Int): JsonObject = try {
            TRAJECTORY_JSON.parseToJsonElement(line).jsonObject
        } catch (error: Exception) {
            throw ReplayTrajectoryFormatException("Line $lineNumber is not a JSON object: ${error.message ?: "invalid JSON"}.", error)
        }

        private fun requiredString(value: JsonObject, key: String): String =
            value[key]?.jsonPrimitive?.content?.takeIf(String::isNotBlank)
                ?: invalid("$key must be a non-blank JSON string.")

        private fun requiredLong(value: JsonObject, key: String): Long =
            value[key]?.jsonPrimitive?.content?.toLongOrNull()
                ?: invalid("$key must be an integer.")

        private fun requiredInt(value: JsonObject, key: String): Int =
            value[key]?.jsonPrimitive?.content?.toIntOrNull()
                ?: invalid("$key must be an integer.")

        private fun requireKeys(value: JsonObject, expected: Set<String>, label: String) {
            requireFormat(value.keys == expected, "$label keys must be exactly ${expected.sorted()}.")
        }

        private fun requireFormat(condition: Boolean, message: String) {
            if (!condition) throw ReplayTrajectoryFormatException(message)
        }

        private fun invalid(message: String): Nothing = throw ReplayTrajectoryFormatException(message)
    }
}

class ReplayTrajectoryFormatException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)

object ReplayTrajectoryCapture {
    fun capture(
        definition: ReplayDefinition,
        factory: HeadlessScenarioFactory = SandboxHeadlessScenarioFactory(),
    ): ReplayTrajectory {
        require(factory.id == definition.factory) {
            "Replay factory '${factory.id}' does not match '${definition.factory}'."
        }
        val scenario = factory.create(definition.packRoot, definition.scenario, definition.seed)
        definition.commands.forEach(scenario::submitScriptCommand)
        val records = mutableListOf<ReplayTrajectoryRecord>()
        var previousTick = -1L
        fun record() {
            val dump = scenario.stateDump()
            require(dump.tick >= 0L) { "Replay state tick cannot be negative." }
            require(dump.tick == previousTick + 1L) {
                "Replay state ticks must advance consecutively; got $previousTick then ${dump.tick}."
            }
            val state = TRAJECTORY_JSON.parseToJsonElement(dump.toJson()).jsonObject
            records += ReplayTrajectoryRecord(dump.tick, dump.hash, state)
            previousTick = dump.tick
        }

        record()
        for (step in 1..definition.requestedTicks) {
            scenario.step(1)
            val tick = scenario.stateDump().tick
            if (tick == previousTick) break
            record()
        }
        return ReplayTrajectory(definition.metadata(), records)
    }
}

data class ReplayComparison(
    val status: String,
    val firstDivergentTick: Long?,
    val expectedHash: String?,
    val actualHash: String?,
    val changedFields: List<String>,
    val message: String? = null,
) {
    val exitCode: Int get() = when (status) {
        "match" -> 0
        "divergence" -> 1
        else -> 2
    }

    fun toJson(): String = buildJson(
        "status" to status,
        "first_divergent_tick" to firstDivergentTick,
        "expected_hash" to expectedHash,
        "actual_hash" to actualHash,
        "changed_fields" to RawJson(changedFields.joinToString(",", prefix = "[", postfix = "]") { "\"${escape(it)}\"" }),
        "message" to message,
    )
}

object ReplayTrajectoryComparer {
    fun compare(expected: ReplayTrajectory, actual: ReplayTrajectory): ReplayComparison {
        val metadataDiff = metadataDiff(expected.metadata, actual.metadata)
        if (metadataDiff != null) {
            return ReplayComparison("invalid", null, null, null, listOf(metadataDiff), "Trajectory metadata mismatch.")
        }
        val maxRecords = maxOf(expected.records.size, actual.records.size)
        for (index in 0 until maxRecords) {
            val expectedRecord = expected.records.getOrNull(index)
            val actualRecord = actual.records.getOrNull(index)
            if (expectedRecord == null || actualRecord == null) {
                val record = expectedRecord ?: actualRecord!!
                val field = if (expectedRecord == null) "unexpected_record" else "missing_record"
                return ReplayComparison(
                    status = "divergence",
                    firstDivergentTick = record.tick,
                    expectedHash = expectedRecord?.hash,
                    actualHash = actualRecord?.hash,
                    changedFields = listOf(field),
                )
            }
            if (expectedRecord.tick != actualRecord.tick) {
                return ReplayComparison(
                    status = "divergence",
                    firstDivergentTick = minOf(expectedRecord.tick, actualRecord.tick),
                    expectedHash = expectedRecord.hash,
                    actualHash = actualRecord.hash,
                    changedFields = listOf("tick"),
                )
            }
            val changedFields = semanticDiff(expectedRecord.state, actualRecord.state)
            if (expectedRecord.hash != actualRecord.hash || changedFields.isNotEmpty()) {
                return ReplayComparison(
                    status = "divergence",
                    firstDivergentTick = expectedRecord.tick,
                    expectedHash = expectedRecord.hash,
                    actualHash = actualRecord.hash,
                    changedFields = changedFields.ifEmpty { listOf("hash_only") },
                )
            }
        }
        return ReplayComparison("match", null, null, null, emptyList())
    }

    private fun metadataDiff(expected: ReplayTrajectoryMetadata, actual: ReplayTrajectoryMetadata): String? = when {
        expected.format != actual.format -> "metadata.format"
        expected.factory != actual.factory -> "metadata.factory"
        expected.scenario != actual.scenario -> "metadata.scenario"
        expected.pack != actual.pack -> "metadata.pack"
        expected.seed != actual.seed -> "metadata.seed"
        expected.requestedTicks != actual.requestedTicks -> "metadata.requested_ticks"
        expected.commands != actual.commands -> "metadata.commands"
        expected.resistPercent != actual.resistPercent -> "metadata.resist_percent"
        else -> null
    }

    private fun semanticDiff(expected: JsonObject, actual: JsonObject): List<String> {
        val expectedValues = flatten(expected)
        val actualValues = flatten(actual)
        return (expectedValues.keys + actualValues.keys)
            .filter { it != "hash" }
            .distinct()
            .filter { expectedValues[it] != actualValues[it] }
            .sorted()
    }

    private fun flatten(value: JsonElement, path: String = "", output: MutableMap<String, String> = sortedMapOf()): Map<String, String> {
        when (value) {
            is JsonObject -> {
                if (value.isEmpty() && path.isNotEmpty()) output[path] = "{}"
                value.keys.sorted().forEach { key ->
                    flatten(value.getValue(key), if (path.isEmpty()) key else "$path.$key", output)
                }
            }
            is JsonArray -> {
                if (value.isEmpty()) output[path] = "[]"
                value.forEachIndexed { index, element -> flatten(element, "$path[$index]", output) }
            }
            JsonNull -> output[path] = "null"
            is JsonPrimitive -> output[path] = value.toString()
        }
        return output
    }
}

private fun defaultCommands(scenario: String): List<String> = when (scenario) {
    "canonical" -> listOf("1:build_tower:pulse:30:32")
    "kill", "resist" -> listOf("1:build_tower:pulse:2:2")
    else -> emptyList()
}
