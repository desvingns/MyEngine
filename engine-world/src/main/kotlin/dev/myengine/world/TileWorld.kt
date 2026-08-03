package dev.myengine.world

import dev.myengine.core.StableHash

data class WorldSize(val width: Int, val height: Int) {
    init {
        require(width > 0) { "World width must be positive." }
        require(height > 0) { "World height must be positive." }
    }

    val tileCount: Int = width * height
}

data class TilePosition(val x: Int, val y: Int) : Comparable<TilePosition> {
    override fun compareTo(other: TilePosition): Int =
        compareValuesBy(this, other, TilePosition::y, TilePosition::x)

    fun neighbors4(): List<TilePosition> = listOf(
        TilePosition(x + 1, y),
        TilePosition(x, y + 1),
        TilePosition(x - 1, y),
        TilePosition(x, y - 1),
    )

    fun manhattanDistance(other: TilePosition): Int =
        kotlin.math.abs(x - other.x) + kotlin.math.abs(y - other.y)
}

data class TerrainRule(
    val id: String,
    val buildable: Boolean,
    val blocksMovement: Boolean,
    val isCore: Boolean = false,
)

data class ResourceNode(
    val resourceId: String,
    val amount: Int,
    val infinite: Boolean = false,
) {
    init {
        require(amount >= 0) { "Resource node amount must be non-negative." }
    }
}

data class WorldTile(
    val terrainId: String,
    val resourceNode: ResourceNode? = null,
    val occupiedBy: Long? = null,
)

data class TileView(
    val position: TilePosition,
    val tile: WorldTile,
    val terrain: TerrainRule,
)

class TileWorld(
    val size: WorldSize,
    private val terrainRules: Map<String, TerrainRule>,
    tiles: List<WorldTile>,
) {
    private val tiles: MutableList<WorldTile> = tiles.toMutableList()

    init {
        require(tiles.size == size.tileCount) { "Expected ${size.tileCount} tiles, got ${tiles.size}." }
        require(terrainRules.isNotEmpty()) { "At least one terrain rule is required." }
        this.tiles.forEach { require(terrainRules.containsKey(it.terrainId)) { "Unknown terrain '${it.terrainId}'." } }
    }

    fun inBounds(position: TilePosition): Boolean =
        position.x in 0 until size.width && position.y in 0 until size.height

    fun tileAt(position: TilePosition): TileView {
        require(inBounds(position)) { "Position $position is outside $size." }
        val tile = tiles[indexOf(position)]
        return TileView(position, tile, terrainRules.getValue(tile.terrainId))
    }

    fun setTile(position: TilePosition, tile: WorldTile) {
        require(inBounds(position)) { "Position $position is outside $size." }
        require(terrainRules.containsKey(tile.terrainId)) { "Unknown terrain '${tile.terrainId}'." }
        tiles[indexOf(position)] = tile
    }

    fun positions(): List<TilePosition> =
        (0 until size.height).flatMap { y -> (0 until size.width).map { x -> TilePosition(x, y) } }

    fun canBuild(position: TilePosition): Boolean {
        val view = tileAt(position)
        return view.terrain.buildable && view.tile.occupiedBy == null && view.tile.resourceNode == null
    }

    /** Returns the underlying node first, then adjacent matching nodes in stable tile order. */
    fun extractorNode(position: TilePosition, resourceId: String): TilePosition? {
        val candidates = buildList {
            add(position)
            addAll(position.neighbors4().sorted())
        }
        return candidates.firstOrNull { candidate ->
            inBounds(candidate) && tileAt(candidate).tile.resourceNode?.resourceId == resourceId
        }
    }

    /** Removes up to [amount] from a finite node; infinite nodes report the full amount unchanged. */
    fun extractResource(position: TilePosition, amount: Int): Int {
        require(amount > 0) { "Extracted amount must be positive." }
        val current = tileAt(position).tile.resourceNode ?: return 0
        val extracted = if (current.infinite) amount else minOf(amount, current.amount)
        if (!current.infinite && extracted > 0) {
            setTile(position, tileAt(position).tile.copy(resourceNode = current.copy(amount = current.amount - extracted)))
        }
        return extracted
    }

    fun canOccupy(position: TilePosition): Boolean {
        val view = tileAt(position)
        return !view.terrain.blocksMovement && view.tile.occupiedBy == null
    }

    fun occupy(position: TilePosition, entityId: Long) {
        val current = tileAt(position).tile
        require(current.occupiedBy == null || current.occupiedBy == entityId) { "Tile $position is occupied." }
        setTile(position, current.copy(occupiedBy = entityId))
    }

    fun clearOccupancy(position: TilePosition, entityId: Long? = null) {
        val current = tileAt(position).tile
        if (entityId == null || current.occupiedBy == entityId) {
            setTile(position, current.copy(occupiedBy = null))
        }
    }

    fun corePositions(): List<TilePosition> = positions()
        .filter { tileAt(it).terrain.isCore }
        .sorted()

    fun appendHash(hash: StableHash) {
        hash.add(size.width).add(size.height)
        terrainRules.values.sortedBy { it.id }.forEach {
            hash.add(it.id).add(it.buildable).add(it.blocksMovement).add(it.isCore)
        }
        positions().forEach { position ->
            val tile = tileAt(position).tile
            hash.add(position.x).add(position.y).add(tile.terrainId)
            hash.add(tile.resourceNode?.resourceId ?: "")
            hash.add(tile.resourceNode?.amount ?: -1)
            if (tile.resourceNode?.infinite == true) hash.add("resource-node-infinite")
            hash.add(tile.occupiedBy ?: -1L)
        }
    }

    fun toSaveLines(): List<String> =
        listOf("size=${size.width}x${size.height}") + positions().map { position ->
            val tile = tileAt(position).tile
            listOf(
                position.x,
                position.y,
                tile.terrainId,
                tile.resourceNode?.resourceId ?: "",
                tile.resourceNode?.amount ?: 0,
                tile.occupiedBy ?: "",
            ).joinToString(",")
        }

    private fun indexOf(position: TilePosition): Int = position.y * size.width + position.x

    companion object {
        fun filled(size: WorldSize, terrainRules: Map<String, TerrainRule>, terrainId: String): TileWorld =
            TileWorld(size, terrainRules, List(size.tileCount) { WorldTile(terrainId) })
    }
}
