package dev.myengine.defense

import dev.myengine.ai.GoalField
import dev.myengine.core.command.TargetingMode
import dev.myengine.entities.Entity
import dev.myengine.entities.EntityStore
import dev.myengine.entities.HealthComponent
import dev.myengine.entities.PositionComponent
import dev.myengine.world.TerrainRule
import dev.myengine.world.TilePosition
import dev.myengine.world.TileWorld
import dev.myengine.world.WorldSize
import kotlin.test.Test
import kotlin.test.assertEquals

class TargetSelectorTest {
    @Test
    fun modesUsePinnedMetricsAndEntityIdTieBreaks() {
        val world = TileWorld.filled(
            WorldSize(5, 5),
            mapOf("floor" to TerrainRule("floor", buildable = true, blocksMovement = false)),
            "floor",
        )
        val goalField = GoalField.build(world, TilePosition(4, 2))
        val enemies = EntityStore().also { store ->
            store.enemy(TilePosition(3, 2), health = 5)  // id 1: first and nearest
            store.enemy(TilePosition(2, 1), health = 10) // id 2: strongest, tied for last
            store.enemy(TilePosition(1, 2), health = 2)  // id 3: weakest, tied for last
        }.byTag("enemy")
        val tower = TilePosition(2, 2)

        val expected = mapOf(
            TargetingMode.FIRST to 1L,
            TargetingMode.LAST to 2L,
            TargetingMode.NEAREST to 1L,
            TargetingMode.STRONGEST to 2L,
            TargetingMode.WEAKEST to 3L,
        )

        expected.forEach { (mode, entityId) ->
            assertEquals(entityId, TargetSelector.select(mode, tower, 2, enemies, goalField)?.id?.value, mode.name)
        }
    }

    private fun EntityStore.enemy(position: TilePosition, health: Int) {
        create("enemy:test", setOf("enemy")) { id ->
            Entity(
                id = id,
                type = "enemy:test",
                tags = setOf("enemy"),
                position = PositionComponent(position),
                health = HealthComponent(health, health),
            )
        }
    }
}
