package dev.myengine.games.sandbox

import dev.myengine.content.StatusEffectContent
import dev.myengine.content.StatusEffectKind
import dev.myengine.content.StatusEffectStackingRule
import dev.myengine.core.CommandId
import dev.myengine.core.Tick
import dev.myengine.core.command.BuildTowerCommand
import dev.myengine.core.command.TileCoordinate
import dev.myengine.entities.Entity
import dev.myengine.entities.EntityId
import dev.myengine.entities.HealthComponent
import dev.myengine.entities.MovementComponent
import dev.myengine.entities.PositionComponent
import dev.myengine.entities.StatusEffectComponent
import dev.myengine.render.Camera
import dev.myengine.render.PlaceholderRenderSurface
import dev.myengine.world.TilePosition
import dev.myengine.world.WorldSize
import java.io.StringReader
import java.io.StringWriter
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SandboxStatusEffectTest {
    @Test
    fun snapshotAndRenderExposeSortedEffectTags() {
        val registry = SandboxGame.loadRegistry().copy(
            effects = mapOf(
                "burn" to StatusEffectContent(
                    "burn", StatusEffectKind.DOT, 1, 3, StatusEffectStackingRule.STACK,
                ),
                "slow" to StatusEffectContent(
                    "slow", StatusEffectKind.SLOW, 40, 2, StatusEffectStackingRule.REFRESH,
                ),
            ),
        )
        val runtime = SandboxGame.createRuntime(registry)
        val enemy = Entity(
            id = EntityId(99),
            type = "enemy:drift",
            tags = setOf("enemy"),
            position = PositionComponent(TilePosition(4, 4)),
            health = HealthComponent(10, 10),
            movement = MovementComponent(),
            statusEffects = listOf(
                StatusEffectComponent("slow", 2),
                StatusEffectComponent("burn", 3),
            ),
        )
        runtime.state.entities.upsert(enemy)

        val snapshot = runtime.snapshot()
        val snapshotEnemy = snapshot.entities.single { it.id == enemy.id.value }
        assertEquals(listOf("burn", "slow"), snapshotEnemy.activeEffectTags)

        val frame = PlaceholderRenderSurface().project(
            snapshot,
            Camera(WorldSize(snapshot.worldSize.width, snapshot.worldSize.height), 400f, 400f),
        )
        assertEquals(
            snapshotEnemy.activeEffectTags,
            frame.primitives.single { it.entityId == enemy.id.value }.activeEffectTags,
        )
    }

    @Test
    fun v9RoundTripPreservesEffectsAndV8MigratesToEmptyEffects() {
        val registry = SandboxGame.loadRegistry().copy(
            effects = mapOf(
                "burn" to StatusEffectContent(
                    "burn", StatusEffectKind.DOT, 2, 3, StatusEffectStackingRule.STACK,
                ),
            ),
        )
        val runtime = SandboxGame.createRuntime(registry)
        val enemy = Entity(
            id = EntityId(99),
            type = "enemy:drift",
            tags = setOf("enemy"),
            position = PositionComponent(TilePosition(4, 4)),
            health = HealthComponent(10, 10),
            movement = MovementComponent(),
            statusEffects = listOf(StatusEffectComponent("burn", 2, 3)),
        )
        runtime.state.entities.upsert(enemy)

        val save = SandboxSaveCodec.encode(runtime.state, seed = 17)
        val restored = SandboxSaveCodec.decode(save, registry)
        assertEquals(listOf(StatusEffectComponent("burn", 2, 3)), restored.entities.require(enemy.id).statusEffects)
        assertEquals(runtime.state.stableHash(), restored.stableHash())

        val v8 = Properties().also { it.load(StringReader(save)) }
        v8["saveVersion"] = "8"
        v8["entities"] = v8.getProperty("entities").split(';').joinToString(";") { row ->
            row.split('|').take(16).joinToString("|")
        }
        val migrated = SandboxSaveCodec.decode(
            StringWriter().also { v8.store(it, "v8 status-effect migration fixture") }.toString(),
            registry,
        )
        assertTrue(migrated.entities.require(enemy.id).statusEffects.isEmpty())
    }

    @Test
    fun slowTowerScenarioHasAStableReplayHash() {
        fun run(): String {
            val base = SandboxGame.loadRegistry()
            val registry = base.copy(
                effects = mapOf(
                    "slow" to StatusEffectContent(
                        "slow", StatusEffectKind.SLOW, 50, 100, StatusEffectStackingRule.REFRESH,
                    ),
                ),
                towers = base.towers + (
                    "pulse" to base.requireTower("pulse").copy(range = 64, damage = 0, effectId = "slow")
                    ),
            )
            val runtime = SandboxGame.createRuntime(registry)
            runtime.submit(BuildTowerCommand(CommandId(1), Tick(1), "pulse", TileCoordinate(2, 2)))
            runtime.step()
            return runtime.state.stableHash()
        }

        val first = run()
        assertEquals("017516c12eb955e1", first)
        assertEquals(first, run())
    }
}
