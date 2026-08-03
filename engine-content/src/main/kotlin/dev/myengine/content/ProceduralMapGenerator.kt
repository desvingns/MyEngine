package dev.myengine.content

import dev.myengine.core.SeededRandom
import dev.myengine.core.StableHash
import java.util.ArrayDeque

/** Typed, content-owned inputs for the deterministic procedural map generator. */
data class ProceduralMapParameters(
    val mapId: String,
    val width: Int,
    val height: Int,
    val floorTileId: String,
    val wallTileId: String,
    val coreTileId: String,
    val spawnId: String,
    val spawn: MapCoordinate,
    val core: MapCoordinate,
    val wallDensityPercent: Int = 18,
    val maxAttempts: Int = 16,
    val terminalRules: MapTerminalRules = MapTerminalRules(),
) {
    init {
        require(mapId.matches(ID_REGEX)) { "Generated map id must match ${ID_REGEX.pattern}." }
        require(width >= 3) { "Generated map width must be at least 3." }
        require(height >= 3) { "Generated map height must be at least 3." }
        require(floorTileId.isNotBlank()) { "Generated map floor tile id cannot be blank." }
        require(wallTileId.isNotBlank()) { "Generated map wall tile id cannot be blank." }
        require(coreTileId.isNotBlank()) { "Generated map core tile id cannot be blank." }
        require(spawnId.isNotBlank() && '|' !in spawnId) { "Generated map spawn id must be non-blank and cannot contain '|'." }
        require(spawn.x in 1 until width - 1 && spawn.y in 1 until height - 1) {
            "Generated map spawn must be inside the border: $spawn for ${width}x$height."
        }
        require(core.x in 1 until width - 1 && core.y in 1 until height - 1) {
            "Generated map core must be inside the border: $core for ${width}x$height."
        }
        require(spawn != core) { "Generated map spawn and core must differ." }
        require(wallDensityPercent in 0..100) { "Generated map wall density must be from 0 to 100 percent." }
        require(maxAttempts in 1..MAX_ATTEMPTS) { "Generated map maxAttempts must be from 1 to $MAX_ATTEMPTS." }
    }

    companion object {
        private val ID_REGEX = Regex("[A-Za-z0-9_-]+")
        private const val MAX_ATTEMPTS = 1_024

        /** Derives a generation template from an already validated content map. */
        fun fromContentMap(
            map: MapContent,
            tiles: Map<String, TileContent>,
            generatedMapId: String = "${map.id}-generated",
            wallDensityPercent: Int = 18,
            maxAttempts: Int = 16,
        ): ProceduralMapParameters {
            val floorTileId = map.terrainMapping.entries
                .map { it.value.terrainId }
                .firstOrNull { tileId -> tiles[tileId]?.let { !it.blocksMovement && !it.isCore } == true }
                ?: error("Map '${map.id}' needs a non-blocking, non-core floor tile for procedural generation.")
            val wallTileId = map.terrainMapping.entries
                .map { it.value.terrainId }
                .firstOrNull { tileId -> tiles[tileId]?.blocksMovement == true }
                ?: error("Map '${map.id}' needs a blocking wall tile for procedural generation.")
            val coreTileId = map.terrainMapping[map.symbolAt(MapCoordinate(map.core.x, map.core.y))]?.terrainId
                ?.takeIf { tiles[it]?.isCore == true }
                ?: error("Map '${map.id}' core coordinate does not resolve to a core tile.")
            return ProceduralMapParameters(
                mapId = generatedMapId,
                width = map.width,
                height = map.height,
                floorTileId = floorTileId,
                wallTileId = wallTileId,
                coreTileId = coreTileId,
                spawnId = map.primarySpawn.id,
                spawn = map.primarySpawn.position.let { MapCoordinate(it.x, it.y) },
                core = MapCoordinate(map.core.x, map.core.y),
                wallDensityPercent = wallDensityPercent,
                maxAttempts = maxAttempts,
                terminalRules = map.terminalRules,
            )
        }
    }
}

/** The generated map plus deterministic provenance needed by devtools and replay diagnostics. */
data class GeneratedMap(
    val seed: Long,
    val attempt: Int,
    val map: MapContent,
    val hash: String,
)

/**
 * Generates small grid maps without touching simulation state. Random walls are regenerated using
 * stable attempt streams until spawn/core connectivity succeeds; a deterministic open-corridor
 * fallback makes the bounded-attempt contract total even at extreme densities.
 */
object ProceduralMapGenerator {
    private const val FLOOR_SYMBOL = '.'
    private const val WALL_SYMBOL = '#'
    private const val CORE_SYMBOL = 'C'

    fun generate(seed: Long, parameters: ProceduralMapParameters): GeneratedMap {
        repeat(parameters.maxAttempts) { attempt ->
            val rows = candidateRows(seed, parameters, attempt)
            if (hasPath(rows, parameters.spawn, parameters.core)) {
                return result(seed, attempt, rows, parameters)
            }
        }

        val fallback = openRows(parameters).also { rows ->
            carveCorridor(rows, parameters.spawn, parameters.core)
        }
        return result(seed, parameters.maxAttempts, fallback, parameters)
    }

    private fun candidateRows(
        seed: Long,
        parameters: ProceduralMapParameters,
        attempt: Int,
    ): Array<CharArray> {
        val random = SeededRandom(seed).fork("procedural-map-attempt:$attempt")
        val rows = openRows(parameters)
        for (y in 1 until parameters.height - 1) {
            for (x in 1 until parameters.width - 1) {
                val position = MapCoordinate(x, y)
                if (position == parameters.spawn || position == parameters.core) continue
                if (random.nextInt(100) < parameters.wallDensityPercent) {
                    rows[y][x] = WALL_SYMBOL
                }
            }
        }
        rows[parameters.spawn.y][parameters.spawn.x] = FLOOR_SYMBOL
        rows[parameters.core.y][parameters.core.x] = CORE_SYMBOL
        return rows
    }

    private fun openRows(parameters: ProceduralMapParameters): Array<CharArray> =
        Array(parameters.height) { y ->
            CharArray(parameters.width) { x ->
                if (x == 0 || y == 0 || x == parameters.width - 1 || y == parameters.height - 1) {
                    WALL_SYMBOL
                } else {
                    FLOOR_SYMBOL
                }
            }
        }

    private fun carveCorridor(rows: Array<CharArray>, from: MapCoordinate, to: MapCoordinate) {
        var x = from.x
        var y = from.y
        rows[y][x] = FLOOR_SYMBOL
        while (x != to.x) {
            x += if (to.x > x) 1 else -1
            rows[y][x] = FLOOR_SYMBOL
        }
        while (y != to.y) {
            y += if (to.y > y) 1 else -1
            rows[y][x] = FLOOR_SYMBOL
        }
        rows[to.y][to.x] = CORE_SYMBOL
    }

    private fun hasPath(rows: Array<CharArray>, from: MapCoordinate, to: MapCoordinate): Boolean {
        val visited = Array(rows.size) { BooleanArray(rows.first().size) }
        val queue = ArrayDeque<MapCoordinate>()
        queue += from
        visited[from.y][from.x] = true
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current == to) return true
            listOf(
                MapCoordinate(current.x + 1, current.y),
                MapCoordinate(current.x, current.y + 1),
                MapCoordinate(current.x - 1, current.y),
                MapCoordinate(current.x, current.y - 1),
            ).forEach { next ->
                if (next.y in rows.indices && next.x in rows[next.y].indices &&
                    !visited[next.y][next.x] && rows[next.y][next.x] != WALL_SYMBOL
                ) {
                    visited[next.y][next.x] = true
                    queue += next
                }
            }
        }
        return false
    }

    private fun result(
        seed: Long,
        attempt: Int,
        rows: Array<CharArray>,
        parameters: ProceduralMapParameters,
    ): GeneratedMap {
        val map = MapContent(
            id = parameters.mapId,
            width = parameters.width,
            height = parameters.height,
            terrainRows = rows.map(CharArray::concatToString),
            terrainMapping = mapOf(
                FLOOR_SYMBOL to MapTerrainSymbol(parameters.floorTileId),
                WALL_SYMBOL to MapTerrainSymbol(parameters.wallTileId),
                CORE_SYMBOL to MapTerrainSymbol(parameters.coreTileId),
            ),
            spawns = mapOf(parameters.spawnId to MapSpawn(parameters.spawnId, parameters.spawn)),
            core = parameters.core,
            terminalRules = parameters.terminalRules,
        )
        return GeneratedMap(seed, attempt, map, hash(map))
    }

    private fun hash(map: MapContent): String = StableHash().apply {
        add(map.id).add(map.width).add(map.height)
        map.terrainRows.forEach(::add)
        map.spawns.toSortedMap().forEach { (id, spawn) ->
            add(id).add(spawn.position.x).add(spawn.position.y)
        }
        add(map.core.x).add(map.core.y)
    }.digest()
}
