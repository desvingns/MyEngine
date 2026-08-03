package dev.myengine.logistics

import dev.myengine.content.BeltDirectionContent
import dev.myengine.content.BeltGeometryContent
import dev.myengine.content.BuildingContent
import dev.myengine.core.StableHash
import dev.myengine.world.TilePosition

/** The two geometry primitives supported by the conveyor MVP. */
enum class BeltGeometry {
    STRAIGHT,
    CORNER,
}

enum class BeltDirection {
    NORTH,
    EAST,
    SOUTH,
    WEST,
}

data class BeltItem(
    val id: String,
    val resourceId: String,
    val amount: Int,
    val cellIndex: Int,
    val progressTicks: Int = 0,
) {
    init {
        require(id.isNotBlank()) { "Belt item id cannot be blank." }
        require(resourceId.isNotBlank()) { "Belt item resource cannot be blank." }
        require(amount > 0) { "Belt item amount must be positive." }
        require(cellIndex >= 0) { "Belt item cell index cannot be negative." }
        require(progressTicks >= 0) { "Belt item progress cannot be negative." }
    }
}

data class BeltCell(
    val position: TilePosition,
    val geometry: BeltGeometry,
    val direction: BeltDirection,
)

data class BeltLine(
    val id: String,
    val cells: List<BeltCell>,
    val ticksPerCell: Int,
    val items: List<BeltItem> = emptyList(),
    /** Optional stable producer/haul source id feeding cell zero. */
    val inputSourceId: String? = null,
    /** Null targets the owning/core inventory; non-null targets an entity inventory. */
    val destinationEntityId: Long? = null,
) {
    init {
        require(id.isNotBlank()) { "Belt id cannot be blank." }
        require(cells.isNotEmpty()) { "A belt must contain at least one cell." }
        require(ticksPerCell > 0) { "Belt ticks per cell must be positive." }
        require(cells.map(BeltCell::position).distinct().size == cells.size) { "Belt cells cannot overlap." }
        require(items.map(BeltItem::id).distinct().size == items.size) { "Belt item ids must be unique." }
        require(items.all { it.cellIndex in cells.indices }) { "Belt item cell index is outside the belt." }
        require(items.groupBy(BeltItem::cellIndex).values.all { it.size == 1 }) {
            "A belt cell can hold only one item."
        }
        require(items.all { it.progressTicks <= ticksPerCell }) { "Belt item progress cannot exceed ticks per cell." }
    }

    fun itemAt(cellIndex: Int): BeltItem? = items.firstOrNull { it.cellIndex == cellIndex }

    fun appendHash(hash: StableHash) {
        hash.add(id).add(ticksPerCell).add(inputSourceId ?: "").add(destinationEntityId ?: -1L)
        cells.forEach { cell ->
            hash.add(cell.position.x).add(cell.position.y).add(cell.geometry.name).add(cell.direction.name)
        }
        items.sortedBy { it.id }.forEach { item ->
            hash.add(item.id).add(item.resourceId).add(item.amount).add(item.cellIndex).add(item.progressTicks)
        }
    }
}

data class BeltTransportState(
    val belts: List<BeltLine> = emptyList(),
) {
    init {
        require(belts.map(BeltLine::id).distinct().size == belts.size) { "Belt ids must be unique." }
    }

    fun canonical(): BeltTransportState = copy(belts = belts.sortedBy { it.id })

    fun appendHash(hash: StableHash) = canonical().belts.forEach { it.appendHash(hash) }
}

data class BeltTransportResult(
    val state: BeltTransportState,
    val pulled: List<BeltItem> = emptyList(),
    val delivered: List<BeltItem> = emptyList(),
)

/**
 * Deterministic conveyor tick. Existing items advance sink-to-source, then an empty source cell
 * pulls one item. A blocked destination keeps progress at the cell boundary, which is the
 * explicit backpressure behavior for a full belt or a rejecting endpoint.
 */
class BeltTransportSystem {
    fun tick(
        state: BeltTransportState,
        pull: (BeltLine) -> BeltItem?,
        push: (BeltLine, BeltItem) -> Boolean,
    ): BeltTransportResult {
        val pulled = mutableListOf<BeltItem>()
        val delivered = mutableListOf<BeltItem>()
        val nextBelts = state.canonical().belts.map { belt ->
            val occupied = belt.items.associateBy { it.cellIndex }.toMutableMap()
            var nextItems = belt.items.associateBy { it.id }.toMutableMap()
            var deliveredOnThisBelt = false
            belt.items
                .sortedWith(compareByDescending<BeltItem> { it.cellIndex }.thenBy { it.id })
                .forEach { item ->
                    val current = nextItems[item.id] ?: return@forEach
                    val progressed = (current.progressTicks + 1).coerceAtMost(belt.ticksPerCell)
                    if (current.cellIndex == belt.cells.lastIndex && progressed >= belt.ticksPerCell) {
                        if (push(belt, current)) {
                            nextItems.remove(current.id)
                            occupied.remove(current.cellIndex)
                            deliveredOnThisBelt = true
                            delivered += current.copy(progressTicks = belt.ticksPerCell)
                        } else {
                            nextItems[current.id] = current.copy(progressTicks = belt.ticksPerCell)
                        }
                        return@forEach
                    }
                    if (progressed >= belt.ticksPerCell && current.cellIndex < belt.cells.lastIndex) {
                        val target = current.cellIndex + 1
                        if (occupied[target] == null) {
                            occupied.remove(current.cellIndex)
                            occupied[target] = current
                            nextItems[current.id] = current.copy(cellIndex = target, progressTicks = 0)
                        } else {
                            nextItems[current.id] = current.copy(progressTicks = belt.ticksPerCell)
                        }
                    } else {
                        nextItems[current.id] = current.copy(progressTicks = progressed)
                    }
                }
            val sourceFree = occupied[0] == null && !deliveredOnThisBelt
            if (sourceFree) {
                val candidate = pull(belt)
                if (candidate != null) {
                    require(candidate.cellIndex == 0) { "Pulled belt items must enter at cell zero." }
                    require(candidate.progressTicks == 0) { "Pulled belt items must enter with zero progress." }
                    require(nextItems.values.none { it.id == candidate.id }) { "Duplicate belt item id '${candidate.id}'." }
                    nextItems[candidate.id] = candidate
                    occupied[0] = candidate
                    pulled += candidate
                }
            }
            belt.copy(items = nextItems.values.sortedWith(compareBy<BeltItem> { it.cellIndex }.thenBy { it.id }))
        }
        return BeltTransportResult(BeltTransportState(nextBelts), pulled, delivered)
    }
}

fun BuildingContent.toBeltCell(position: TilePosition): BeltCell? {
    val geometry = beltGeometry ?: return null
    val direction = beltDirection ?: return null
    return BeltCell(
        position = position,
        geometry = when (geometry) {
            BeltGeometryContent.STRAIGHT -> BeltGeometry.STRAIGHT
            BeltGeometryContent.CORNER -> BeltGeometry.CORNER
        },
        direction = when (direction) {
            BeltDirectionContent.NORTH -> BeltDirection.NORTH
            BeltDirectionContent.EAST -> BeltDirection.EAST
            BeltDirectionContent.SOUTH -> BeltDirection.SOUTH
            BeltDirectionContent.WEST -> BeltDirection.WEST
        },
    )
}
