package dev.myengine.android

import dev.myengine.core.GameplayEvent
import dev.myengine.core.GameplayEventType
import dev.myengine.core.Tick
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SoundEventPresentationTest {
    @Test
    fun cursorDeduplicatesRepeatedSnapshotsPreservesNewEventsAndHandlesGaps() {
        val firstSnapshot = listOf(
            GameplayEvent(Tick(2), GameplayEventType.SHOT, ordinal = 0),
            GameplayEvent(Tick(2), GameplayEventType.HIT, ordinal = 1),
        )
        val first = SoundEventCursor().observe(firstSnapshot)

        assertEquals(firstSnapshot, first.events)
        assertEquals(SoundEventCursor(lastTick = 2, lastOrdinal = 1), first.cursor)

        val repeated = first.cursor.observe(firstSnapshot)
        assertTrue(repeated.events.isEmpty())
        assertEquals(first.cursor, repeated.cursor)

        val afterGap = repeated.cursor.observe(
            listOf(
                GameplayEvent(Tick(2), GameplayEventType.HIT, ordinal = 1),
                GameplayEvent(Tick(2), GameplayEventType.DEATH, ordinal = 2),
                GameplayEvent(Tick(5), GameplayEventType.WAVE_START, ordinal = 4),
                GameplayEvent(Tick(5), GameplayEventType.BUILD, ordinal = 5),
            ),
        )

        assertEquals(
            listOf(
                GameplayEvent(Tick(2), GameplayEventType.DEATH, ordinal = 2),
                GameplayEvent(Tick(5), GameplayEventType.WAVE_START, ordinal = 4),
                GameplayEvent(Tick(5), GameplayEventType.BUILD, ordinal = 5),
            ),
            afterGap.events,
        )
        assertEquals(SoundEventCursor(lastTick = 5, lastOrdinal = 5), afterGap.cursor)
    }

    @Test
    fun presentationStateClampsVolumeAndTracksMuteWithoutChangingOtherState() {
        val initial = SoundPresentationState()

        assertEquals(0f, initial.withVolume(-0.5f).volume)
        assertEquals(1f, initial.withVolume(1.5f).volume)
        assertEquals(0.35f, initial.withVolume(0.35f).volume)
        assertEquals(false, initial.muted)
        assertEquals(true, initial.withMuted(true).muted)
        assertEquals(0.35f, initial.withMuted(true).withVolume(0.35f).volume)
    }
}
