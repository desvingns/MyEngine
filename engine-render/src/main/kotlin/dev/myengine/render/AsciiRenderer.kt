package dev.myengine.render

class AsciiRenderer {
    fun render(snapshot: EngineSnapshot, maxWidth: Int = 16, maxHeight: Int = 12): String {
        val width = minOf(snapshot.worldSize.width, maxWidth)
        val height = minOf(snapshot.worldSize.height, maxHeight)
        val entityPositions = snapshot.entities.associateBy { it.position }
        val tiles = snapshot.tiles.associateBy { it.position }
        return buildString {
            appendLine("tick=${snapshot.debug.tick.value} core=${snapshot.coreHealth} entities=${snapshot.entities.size}")
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val position = dev.myengine.world.TilePosition(x, y)
                    val entity = entityPositions[position]
                    val tile = tiles[position]
                    append(
                        when {
                            entity?.type?.startsWith("tower") == true -> 'T'
                            entity?.type?.startsWith("enemy") == true -> 'e'
                            tile?.terrainId == "wall" -> '#'
                            tile?.terrainId == "core" -> 'C'
                            tile?.terrainId == "resource" -> 'r'
                            else -> '.'
                        },
                    )
                }
                appendLine()
            }
        }
    }
}
