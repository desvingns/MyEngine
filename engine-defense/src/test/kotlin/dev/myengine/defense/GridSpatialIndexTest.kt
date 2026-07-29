package dev.myengine.defense

import dev.myengine.ai.GoalField
import dev.myengine.core.SeededRandom
import dev.myengine.core.command.TargetingMode
import dev.myengine.entities.Entity
import dev.myengine.entities.EntityId
import dev.myengine.entities.EntityStore
import dev.myengine.entities.HealthComponent
import dev.myengine.entities.PositionComponent
import dev.myengine.world.TerrainRule
import dev.myengine.world.TilePosition
import dev.myengine.world.TileWorld
import dev.myengine.world.WorldSize
import kotlin.test.Test
import kotlin.test.assertEquals

class GridSpatialIndexTest {
    @Test
    fun seededQueriesMatchReferenceScan() {
        val entities = EntityStore()
        val positionsRandom = SeededRandom(0x5EED_020L)
        repeat(96) { index ->
            val position = TilePosition(
                positionsRandom.nextInt(25) - 12,
                positionsRandom.nextInt(25) - 12,
            )
            val health = positionsRandom.nextInt(6)
            entities.upsert(
                Entity(
                    id = EntityId(100L + index * 3L),
                    type = "enemy:test",
                    tags = setOf("enemy"),
                    position = PositionComponent(position),
                    health = HealthComponent(health, 5),
                ),
            )
        }
        entities.upsert(
            Entity(
                id = EntityId(10_000),
                type = "enemy:unpositioned",
                tags = setOf("enemy"),
                health = HealthComponent(5, 5),
            ),
        )

        val index = GridSpatialIndex.build(entities.byTag("enemy"))
        val queriesRandom = SeededRandom(0xC0FF_EE020L)
        repeat(128) { queryNumber ->
            val center = TilePosition(
                queriesRandom.nextInt(31) - 15,
                queriesRandom.nextInt(31) - 15,
            )
            val radius = queriesRandom.nextInt(7)
            val expected = entities.byTag("enemy")
                .filter { entity ->
                    val position = entity.position?.tile ?: return@filter false
                    center.manhattanDistance(position) <= radius
                }
                .map { it.id.value }
                .sorted()
            val actual = index.query(center, radius, entities).map { it.id.value }

            assertEquals(expected, actual, "query=$queryNumber center=$center radius=$radius")
        }
    }

    @Test
    fun queryUsesInclusiveManhattanBoundariesAndStableEntityOrder() {
        val entities = EntityStore(
            initialEntities = listOf(
                enemy(1, TilePosition(0, 0)),
                enemy(2, TilePosition(1, 0)),
                enemy(3, TilePosition(0, 1)),
                enemy(4, TilePosition(-1, 0)),
                enemy(5, TilePosition(0, -1)),
                enemy(6, TilePosition(1, 1)),
                enemy(7, TilePosition(2, 0)),
                enemy(8, TilePosition(0, 2)),
                enemy(9, TilePosition(2, 1)),
            ),
        )
        val index = GridSpatialIndex.build(entities.byTag("enemy"))

        assertEquals(
            listOf(1L, 2L, 3L, 4L, 5L),
            index.query(TilePosition(0, 0), 1, entities).map { it.id.value },
        )
        assertEquals(
            (1L..8L).toList(),
            index.query(TilePosition(0, 0), 2, entities).map { it.id.value },
        )
    }

    @Test
    fun queryResolvesCurrentStoreAndOmitsRemovedEntity() {
        val entities = EntityStore(
            initialEntities = listOf(
                enemy(1, TilePosition(0, 0), health = 5),
                enemy(2, TilePosition(1, 0), health = 5),
            ),
        )
        val index = GridSpatialIndex.build(entities.byTag("enemy"))

        entities.update(EntityId(1)) { it.copy(health = HealthComponent(1, 5)) }
        entities.remove(EntityId(2))

        val result = index.query(TilePosition(0, 0), 1, entities)
        assertEquals(listOf(1L), result.map { it.id.value })
        assertEquals(1, result.single().health?.current)
    }

    @Test
    fun indexedCandidatesPreserveAllTargetingModeTieBreaks() {
        val world = TileWorld.filled(
            WorldSize(5, 5),
            mapOf("floor" to TerrainRule("floor", buildable = true, blocksMovement = false)),
            "floor",
        )
        val goalField = GoalField.build(world, TilePosition(4, 2))
        val entities = EntityStore(
            initialEntities = listOf(
                enemy(11, TilePosition(3, 2), health = 5),
                enemy(4, TilePosition(2, 1), health = 10),
                enemy(7, TilePosition(2, 3), health = 10),
                enemy(5, TilePosition(1, 2), health = 2),
            ),
        )
        val tower = TilePosition(2, 2)
        val candidates = GridSpatialIndex.build(entities.byTag("enemy"))
            .query(tower, 2, entities)

        assertEquals(listOf(4L, 5L, 7L, 11L), candidates.map { it.id.value })
        val expected = mapOf(
            TargetingMode.FIRST to 11L,
            TargetingMode.LAST to 4L,
            TargetingMode.NEAREST to 4L,
            TargetingMode.STRONGEST to 4L,
            TargetingMode.WEAKEST to 5L,
        )
        expected.forEach { (mode, expectedId) ->
            assertEquals(
                expectedId,
                TargetSelector.select(mode, tower, 2, candidates, goalField)?.id?.value,
                mode.name,
            )
        }
    }

    private fun enemy(id: Long, position: TilePosition, health: Int = 5): Entity = Entity(
        id = EntityId(id),
        type = "enemy:test",
        tags = setOf("enemy"),
        position = PositionComponent(position),
        health = HealthComponent(health, maxOf(health, 5)),
    )
}
