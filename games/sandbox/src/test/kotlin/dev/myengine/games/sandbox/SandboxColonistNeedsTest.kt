package dev.myengine.games.sandbox

import dev.myengine.ai.Job
import dev.myengine.ai.JobBoard
import dev.myengine.ai.JobStatus
import dev.myengine.entities.Entity
import dev.myengine.entities.EntityId
import dev.myengine.entities.EntityStore
import dev.myengine.entities.JobActorComponent
import dev.myengine.entities.NeedsComponent
import dev.myengine.entities.PositionComponent
import dev.myengine.render.HudNeedBar
import dev.myengine.world.TilePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SandboxColonistNeedsTest {
    @Test
    fun thresholdJobWinsAgainstSamePriorityWorkAndRecoversNeed() {
        val state = colonistState()
        state.jobBoard = JobBoard(listOf(Job("work:1", "build", TilePosition(2, 2), priority = 100)))

        SandboxRuntime(state).step()

        val colonist = state.entities.require(EntityId(1))
        assertEquals(75, colonist.needs?.level("hunger"))
        assertEquals(JobStatus.DONE, state.jobBoard.get("need:1:hunger:0")?.status)
        assertEquals(JobStatus.OPEN, state.jobBoard.get("work:1")?.status)
    }

    @Test
    fun needsRoundTripThroughV17SaveAndImmutableHudBars() {
        val state = colonistState()
        val runtime = SandboxRuntime(state)
        runtime.step()

        val save = SandboxSaveCodec.encode(state, seed = 7L)
        val restored = SandboxSaveCodec.decode(save, state.registry)
        assertEquals(19, SandboxSaveCodec.SAVE_VERSION)
        assertEquals(state.entities.require(EntityId(1)), restored.entities.require(EntityId(1)))
        assertEquals(state.stableHash(), restored.stableHash())

        val bars = runtime.snapshot().hud.needBars
        assertEquals(
            listOf(HudNeedBar(1, "hunger", "Hunger", 75, 25), HudNeedBar(1, "rest", "Rest", 99, 25)),
            bars,
        )
        assertTrue(bars.sortedWith(compareBy<HudNeedBar> { it.entityId }.thenBy { it.needId }) == bars)
    }

    @Test
    fun v16StyleEntityWithoutNeedsMigratesWithNoNeedState() {
        val state = colonistState().copy(
            entities = EntityStore(
                nextEntityId = 2,
                initialEntities = listOf(
                    Entity(
                        id = EntityId(1),
                        type = "colonist",
                        position = PositionComponent(TilePosition(2, 2)),
                        jobActor = JobActorComponent(),
                    ),
                ),
            ),
        )
        val save = SandboxSaveCodec.encode(state, seed = 7L)
            .replace("saveVersion=19", "saveVersion=16")
        val restored = SandboxSaveCodec.decode(save, state.registry)
        assertEquals(null, restored.entities.require(EntityId(1)).needs)
    }

    private fun colonistState(): SandboxState {
        val registry = SandboxGame.loadRegistry()
        return SandboxGame.createInitialState(registry).copy(
            entities = EntityStore(
                nextEntityId = 2,
                initialEntities = listOf(
                    Entity(
                        id = EntityId(1),
                        type = "colonist",
                        position = PositionComponent(TilePosition(2, 2)),
                        jobActor = JobActorComponent(),
                        needs = NeedsComponent(mapOf("hunger" to 26, "rest" to 100)),
                    ),
                ),
            ),
            producers = emptyList(),
        )
    }
}
