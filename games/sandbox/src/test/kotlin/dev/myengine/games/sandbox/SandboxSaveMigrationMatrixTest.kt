package dev.myengine.games.sandbox

import dev.myengine.core.Tick
import dev.myengine.core.CommandId
import dev.myengine.core.command.PlaceBuildingCommand
import dev.myengine.core.command.TileCoordinate
import dev.myengine.defense.DefenseState
import dev.myengine.entities.Entity
import dev.myengine.entities.EntityId
import dev.myengine.entities.EntityStore
import dev.myengine.entities.HealthComponent
import dev.myengine.entities.MovementComponent
import dev.myengine.entities.PositionComponent
import dev.myengine.logistics.Inventory
import dev.myengine.logistics.Producer
import dev.myengine.storyteller.IncidentDirectorState
import dev.myengine.world.ResourceNode
import dev.myengine.world.TerrainRule
import dev.myengine.world.TilePosition
import dev.myengine.world.TileWorld
import dev.myengine.world.WorldSize
import dev.myengine.world.WorldTile
import java.io.StringReader
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SandboxSaveMigrationMatrixTest {

    @Test
    fun everyReleasedSaveFixtureMigratesToTheCanonicalStateHashDeterministically() {
        val registry = SandboxGame.loadRegistry()
        val expected = canonicalInitialState(registry)
        assertCanonicalState(expected)
        val expectedHash = expected.stableHash()

        (1..11).plus(13).plus(14).plus(15).plus(16).plus(17).plus(18).plus(19).plus(20).forEach { version ->
            val text = fixture(version)
            val properties = Properties().also { it.load(StringReader(text)) }
            assertEquals(version.toString(), properties.getProperty("saveVersion"), "v$version fixture version")

            val first = SandboxSaveCodec.decode(text, registry)
            assertCanonicalState(first)
            assertTrue(first.zones.allStockpiles().isEmpty(), "v$version must migrate stockpiles to empty state")
            assertTrue(first.zones.allHarvestDesignations().isEmpty(), "v$version must migrate designations to empty state")
            assertEquals(expectedHash, first.stableHash(), "v$version post-migration state hash")

            val second = SandboxSaveCodec.decode(fixture(version), registry)
            assertEquals(expectedHash, second.stableHash(), "v$version repeated post-migration state hash")
            assertEquals(first.stableHash(), second.stableHash(), "v$version migration must be deterministic")
        }
    }

    @Test
    fun v12BuildingFixtureRestoresWallOccupancyHealthAndPendingPlacement() {
        val registry = SandboxGame.loadRegistry()
        val save = fixture(12)

        val state = SandboxSaveCodec.decode(save, registry)
        val wall = state.entities.byTag("building").single()

        assertEquals(EntityId(1), wall.id)
        assertEquals("building:wall", wall.type)
        assertEquals(17, wall.health?.current)
        assertEquals(20, wall.health?.max)
        assertEquals(1L, state.world.tileAt(TilePosition(4, 1)).tile.occupiedBy)
        assertEquals(
            listOf(
                PlaceBuildingCommand(
                    id = CommandId(2),
                    scheduledTick = Tick(10),
                    buildingId = "wall",
                    position = TileCoordinate(5, 1),
                ),
            ),
            SandboxSaveCodec.decodePendingCommands(save),
        )
    }

    @Test
    fun v12MigrationCreatesEmptyJobsAndUnassignedLegacyEntities() {
        val registry = SandboxGame.loadRegistry()

        val state = SandboxSaveCodec.decode(fixture(12), registry)

        assertTrue(state.jobBoard.all().isEmpty())
        assertTrue(state.entities.all().all { it.jobActor == null })
        assertTrue(state.zones.allStockpiles().isEmpty())
        assertTrue(state.zones.allHarvestDesignations().isEmpty())
    }

    private fun fixture(version: Int): String =
        requireNotNull(javaClass.getResourceAsStream("/save-fixtures/v$version.properties")) {
            "Missing checked-in save migration fixture v$version."
        }.bufferedReader().use { it.readText() }

    private fun assertCanonicalState(state: SandboxState) {
        assertEquals(Tick(3), state.tick)
        assertEquals("sandbox-canonical", state.mapId)
        assertEquals(mapOf("bolt" to 9), state.inventory.resources)
        assertEquals(listOf(Producer("generator-1", "bolt-generator", progressTicks = 2)), state.producers)
        assertEquals(17, state.defense.coreHealth)
        assertEquals(dev.myengine.defense.DefenseMetrics(1, 0, 0, 0, 0), state.defense.metrics)
        assertEquals(emptyMap(), state.defense.towerMetrics)
        assertEquals(7L, state.randomCursor)
        assertEquals(IncidentDirectorState(), state.incidentState)
        assertTrue(state.incidentModifiers.isEmpty())
        assertEquals(1, state.entities.all().size)
        assertEquals(EntityId(2), state.entities.all().single().id)
        assertEquals("enemy:spark", state.entities.all().single().type)
        assertEquals(3L, state.entities.nextIdSnapshot())
    }

    private fun canonicalInitialState(registry: dev.myengine.content.ContentRegistry): SandboxState {
        val map = registry.requireMap("sandbox-canonical")
        val terrain = registry.tiles.values.associate {
            it.id to TerrainRule(it.id, it.buildable, it.blocksMovement, it.isCore)
        }
        val tiles = map.terrainRows.flatMap { row ->
            row.map { symbol ->
                val mapping = map.terrainMapping.getValue(symbol)
                WorldTile(
                    terrainId = mapping.terrainId,
                    resourceNode = mapping.resourceNode?.let { ResourceNode(it.resourceId, it.amount) },
                )
            }
        }
        return SandboxState(
            tick = Tick(3),
            registry = registry,
            mapId = map.id,
            world = TileWorld(WorldSize(map.width, map.height), terrain, tiles),
            entities = EntityStore(
                nextEntityId = 3,
                initialEntities = listOf(
                    Entity(
                        id = EntityId(2),
                        type = "enemy:spark",
                        tags = setOf("enemy"),
                        position = PositionComponent(TilePosition(1, 1)),
                        health = HealthComponent(10, 10),
                        movement = MovementComponent(),
                    ),
                ),
            ),
            inventory = Inventory(mapOf("bolt" to 9)),
            producers = listOf(Producer("generator-1", "bolt-generator", progressTicks = 2)),
            defense = DefenseState(
                coreHealth = 17,
                metrics = dev.myengine.defense.DefenseMetrics(1, 0, 0, 0, 0),
            ),
            randomCursor = 7L,
        )
    }
}
