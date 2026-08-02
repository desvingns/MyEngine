package dev.myengine.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class GameplayEventTest {
    @Test
    fun gameplayEventTypeIdsAreStableAndNormalizeUnderscores() {
        assertEquals(
            listOf("shot", "hit", "death", "wave-start", "build", "sell"),
            GameplayEventType.entries.map { it.id },
        )
        assertEquals(GameplayEventType.WAVE_START, GameplayEventType.fromId(" WAVE_START "))
        assertEquals(GameplayEventType.WAVE_START, GameplayEventType.fromId("wave-start"))
        assertEquals(GameplayEventType.SHOT, GameplayEventType.fromId("SHOT"))
        assertNull(GameplayEventType.fromId("unknown-event"))
    }

    @Test
    fun gameplayEventCarriesTickOrdinalAndSequenceAlias() {
        val event = GameplayEvent(
            tick = Tick(12),
            type = GameplayEventType.HIT,
            ordinal = 3,
            sourceEntityId = 7L,
            targetEntityId = 11L,
            contentId = "pulse",
        )

        assertEquals(Tick(12), event.tick)
        assertEquals(3, event.ordinal)
        assertEquals(3, event.sequence)
        assertFailsWith<IllegalArgumentException> {
            GameplayEvent(Tick(1), GameplayEventType.SHOT, ordinal = -1)
        }
    }

    @Test
    fun combatEventsKeepsExistingShotsAndHitsAlongsideOrderedGameplayFeed() {
        val shot = ShotEvent(sourceEntityId = 7L, targetEntityId = 11L, tick = Tick(12))
        val hit = HitEvent(sourceEntityId = 7L, targetEntityId = 11L, tick = Tick(12))
        val feed = listOf(
            GameplayEvent(Tick(12), GameplayEventType.SHOT, ordinal = 0),
            GameplayEvent(Tick(12), GameplayEventType.HIT, ordinal = 1),
        )

        val events = CombatEvents(
            shots = listOf(shot),
            hits = listOf(hit),
            gameplayEvents = feed,
        )

        assertEquals(listOf(shot), events.shots)
        assertEquals(listOf(hit), events.hits)
        assertEquals(feed, events.gameplayEvents)
        assertEquals(feed, events.events)
        assertEquals(CombatEvents.EMPTY, CombatEvents())
    }
}
