package dev.myengine.games.sandbox

import dev.myengine.content.BeltDirectionContent
import dev.myengine.content.BeltGeometryContent
import dev.myengine.core.CommandId
import dev.myengine.core.Tick
import dev.myengine.core.command.PlaceBuildingCommand
import dev.myengine.entities.Entity
import dev.myengine.entities.EntityId
import dev.myengine.entities.InventoryComponent
import dev.myengine.entities.PositionComponent
import dev.myengine.logistics.BeltCell
import dev.myengine.logistics.BeltDirection
import dev.myengine.logistics.BeltGeometry
import dev.myengine.logistics.BeltItem
import dev.myengine.logistics.BeltLine
import dev.myengine.logistics.BeltTransportState
import dev.myengine.logistics.HaulSource
import dev.myengine.logistics.HaulSourceStore
import dev.myengine.logistics.Inventory
import dev.myengine.world.TilePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SandboxBeltTransportTest {
    @Test
    fun sandboxPullsExtractorSourceAndPushesItemToCoreInventory() {
        val state = SandboxGame.createInitialState().also {
            it.inventory = Inventory(mapOf("bolt" to 2))
            it.producers = emptyList()
        }
        val runtime = SandboxRuntime(state)
        runtime.submit(
            PlaceBuildingCommand(
                id = CommandId(1),
                scheduledTick = Tick(1),
                buildingId = "bolt-extractor",
                position = dev.myengine.core.command.TileCoordinate(5, 5),
            ),
        )
        runtime.step(3)
        assertEquals(7, state.haulSources.get("producer:extractor:1")?.resources?.get("bolt"))

        state.producers = emptyList()
        state.belts = BeltTransportState(
            listOf(
                BeltLine(
                    id = "extractor-to-core",
                    cells = listOf(cell(0)),
                    ticksPerCell = 1,
                    inputSourceId = "producer:extractor:1",
                ),
            ),
        )

        runtime.step()
        assertEquals(6, state.haulSources.get("producer:extractor:1")?.resources?.get("bolt"))
        assertEquals(1, state.belts.belts.single().items.size)
        assertEquals(0, state.inventory.amount("bolt"))

        runtime.step()
        assertTrue(state.belts.belts.single().items.isEmpty())
        assertEquals(1, state.inventory.amount("bolt"))
    }

    @Test
    fun sandboxPushesBeltItemToEntityInventoryAndRespectsCapacity() {
        val state = SandboxGame.createInitialState().also {
            it.producers = emptyList()
            it.entities.upsert(
                Entity(
                    id = EntityId(9),
                    type = "building:core-module",
                    position = PositionComponent(TilePosition(3, 3)),
                    inventory = InventoryComponent(capacity = 1),
                ),
            )
            it.belts = BeltTransportState(
                listOf(
                    BeltLine(
                        id = "belt-to-entity",
                        cells = listOf(cell(0)),
                        ticksPerCell = 1,
                        items = listOf(BeltItem("entity-item", "bolt", 1, cellIndex = 0)),
                        destinationEntityId = 9,
                    ),
                ),
            )
        }
        val runtime = SandboxRuntime(state)

        runtime.step()

        assertTrue(state.belts.belts.single().items.isEmpty())
        assertEquals(mapOf("bolt" to 1), state.entities.require(EntityId(9)).inventory?.resources)

        state.belts = BeltTransportState(
            listOf(
                BeltLine(
                    id = "blocked-entity-belt",
                    cells = listOf(cell(0)),
                    ticksPerCell = 1,
                    items = listOf(BeltItem("blocked-item", "bolt", 1, cellIndex = 0)),
                    destinationEntityId = 9,
                ),
            ),
        )
        runtime.step()

        assertEquals(listOf(BeltItem("blocked-item", "bolt", 1, cellIndex = 0, progressTicks = 1)), state.belts.belts.single().items)
        assertEquals(mapOf("bolt" to 1), state.entities.require(EntityId(9)).inventory?.resources)
    }

    @Test
    fun sandboxSaveV20RoundTripPreservesBeltItemsAndHash() {
        val state = SandboxGame.createInitialState().also {
            it.producers = emptyList()
            it.haulSources = HaulSourceStore(
                listOf(HaulSource("producer:fixture", TilePosition(1, 1), mapOf("bolt" to 4))),
            )
            it.belts = BeltTransportState(
                listOf(
                    BeltLine(
                        id = "zeta-belt",
                        cells = listOf(cell(0), cell(1, BeltGeometry.CORNER, BeltDirection.SOUTH)),
                        ticksPerCell = 3,
                        items = listOf(BeltItem("fixture-item", "bolt", 2, cellIndex = 1, progressTicks = 2)),
                        inputSourceId = "producer:fixture",
                        destinationEntityId = 9,
                    ),
                ),
            )
        }

        val save = SandboxSaveCodec.encode(state, seed = 31L)
        val restored = SandboxSaveCodec.decode(save, state.registry)

        assertTrue(save.contains("saveVersion=${SandboxSaveCodec.SAVE_VERSION}"))
        assertEquals(21, SandboxSaveCodec.SAVE_VERSION)
        assertEquals(state.belts, restored.belts)
        assertEquals(state.stableHash(), restored.stableHash())
    }

    @Test
    fun v1ThroughV19SavesMigrateWithEmptyBeltState() {
        val registry = SandboxGame.loadRegistry()

        (1..19).forEach { version ->
            val restored = SandboxSaveCodec.decode(fixture(version), registry)
            assertTrue(restored.belts.belts.isEmpty(), "v$version must migrate with empty belts")
        }
    }

    @Test
    fun futureSaveVersionIsRejected() {
        val save = SandboxSaveCodec.encode(SandboxGame.createInitialState(), seed = 31L)
            .replace("saveVersion=${SandboxSaveCodec.SAVE_VERSION}", "saveVersion=${SandboxSaveCodec.SAVE_VERSION + 1}")

        assertFailsWith<IllegalArgumentException> {
            SandboxSaveCodec.decode(save, SandboxGame.loadRegistry())
        }
    }

    private fun fixture(version: Int): String =
        requireNotNull(javaClass.getResourceAsStream("/save-fixtures/v$version.properties")) {
            "Missing checked-in save migration fixture v$version."
        }.bufferedReader().use { it.readText() }

    private fun cell(index: Int, geometry: BeltGeometry = BeltGeometry.STRAIGHT, direction: BeltDirection = BeltDirection.EAST) =
        BeltCell(TilePosition(index, 0), geometry, direction)
}
