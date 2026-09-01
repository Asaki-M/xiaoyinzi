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
}
