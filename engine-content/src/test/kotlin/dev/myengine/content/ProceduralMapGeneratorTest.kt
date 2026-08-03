package dev.myengine.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProceduralMapGeneratorTest {
    private val tiles = mapOf(
        "floor" to TileContent("floor", buildable = true, blocksMovement = false, isCore = false),
        "wall" to TileContent("wall", buildable = false, blocksMovement = true, isCore = false),
        "core" to TileContent("core", buildable = false, blocksMovement = false, isCore = true),
    )

    private val parameters = ProceduralMapParameters(
        mapId = "generated-test",
        width = 24,
        height = 18,
        floorTileId = "floor",
        wallTileId = "wall",
        coreTileId = "core",
        spawnId = "entry",
        spawn = MapCoordinate(1, 1),
        core = MapCoordinate(20, 15),
        wallDensityPercent = 70,
        maxAttempts = 4,
        terminalRules = MapTerminalRules(MapWinCondition.NO_WIN),
    )

    @Test
    fun sameSeedProducesSameMapHashAndRows() {
        val first = ProceduralMapGenerator.generate(41L, parameters)
        val second = ProceduralMapGenerator.generate(41L, parameters)

        assertEquals(first, second)
        assertEquals(first.hash, second.hash)
        assertEquals(first.map.terrainRows, second.map.terrainRows)
    }

    @Test
    fun generatedMapHasAValidSpawnToCorePathAfterBoundedAttempts() {
        val generated = ProceduralMapGenerator.generate(99L, parameters)
        val map = generated.map
        val open = map.terrainRows.map(String::toCharArray)
        val queue = ArrayDeque<MapCoordinate>()
        val visited = Array(map.height) { BooleanArray(map.width) }
        queue += map.primarySpawn.position
        visited[map.primarySpawn.position.y][map.primarySpawn.position.x] = true

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            listOf(
                MapCoordinate(current.x + 1, current.y),
                MapCoordinate(current.x, current.y + 1),
                MapCoordinate(current.x - 1, current.y),
                MapCoordinate(current.x, current.y - 1),
            ).forEach { next ->
                if (next.x in 0 until map.width && next.y in 0 until map.height &&
                    !visited[next.y][next.x] && open[next.y][next.x] != '#'
                ) {
                    visited[next.y][next.x] = true
                    queue += next
                }
            }
        }

        assertTrue(visited[map.core.y][map.core.x])
        assertTrue(generated.attempt in 0..parameters.maxAttempts)
    }

    @Test
    fun parametersCanBeDerivedFromValidatedContentMap() {
        val map = MapContent(
            id = "template",
            width = 5,
            height = 5,
            terrainRows = listOf("#####", "#...#", "#...#", "#..C#", "#####"),
            terrainMapping = mapOf(
                '.' to MapTerrainSymbol("floor"),
                '#' to MapTerrainSymbol("wall"),
                'C' to MapTerrainSymbol("core"),
            ),
            spawns = mapOf("entry" to MapSpawn("entry", MapCoordinate(1, 1))),
            core = MapCoordinate(3, 3),
        )

        val derived = ProceduralMapParameters.fromContentMap(map, tiles)

        assertEquals("template-generated", derived.mapId)
        assertEquals("floor", derived.floorTileId)
        assertEquals("wall", derived.wallTileId)
        assertEquals("core", derived.coreTileId)
        assertEquals(MapCoordinate(1, 1), derived.spawn)
        assertEquals(MapCoordinate(3, 3), derived.core)
    }
}
