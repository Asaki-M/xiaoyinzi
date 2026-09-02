package com.xiaoyinzi.player.playback

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackModeTest {
    @Test
    fun cyclesThroughAllPlaybackModes() {
        assertEquals(PlaybackMode.REPEAT_ALL, PlaybackMode.SEQUENTIAL.next())
        assertEquals(PlaybackMode.REPEAT_ONE, PlaybackMode.REPEAT_ALL.next())
        assertEquals(PlaybackMode.SHUFFLE, PlaybackMode.REPEAT_ONE.next())
        assertEquals(PlaybackMode.SEQUENTIAL, PlaybackMode.SHUFFLE.next())
    }

    @Test
    fun shuffleTakesPriorityWhenReadingPlayerState() {
        assertEquals(
            PlaybackMode.SHUFFLE,
            PlaybackMode.from(shuffleEnabled = true, repeatMode = Player.REPEAT_MODE_ALL),
        )
    }

    @Test
    fun restoresStoredPlaybackModeAndFallsBackForInvalidValues() {
        assertEquals(PlaybackMode.SHUFFLE, PlaybackMode.fromStoredValue("SHUFFLE"))
        assertEquals(PlaybackMode.SEQUENTIAL, PlaybackMode.fromStoredValue("invalid"))
        assertEquals(PlaybackMode.SEQUENTIAL, PlaybackMode.fromStoredValue(null))
    }

    @Test
    fun buildsQueueUsingTimelineOrder() {
        val nextIndices = mapOf(2 to 0, 0 to 3, 3 to 1, 1 to -1)

        assertEquals(
            listOf(2, 0, 3, 1),
            buildQueueIndices(
                mediaItemCount = 4,
                firstIndex = 2,
                nextIndex = { nextIndices.getValue(it) },
            ),
        )
    }
}
