package com.xiaoyinzi.player.playback

import com.xiaoyinzi.player.data.TrackEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DeferredPlaybackQueueTest {
    @Test
    fun sharedTrackFinishesBeforeTheTargetQueueIsApplied() {
        val pending = PendingPlaybackQueue()
        val current = track("current")
        val next = track("new-next")
        val target = listOf(current, next)

        assertNull(pending.schedule(target, pending.captureAnchor(current.uri), current.uri, ended = false))
        val replacement = requireNotNull(pending.onTrackFinished())
        assertEquals(target, replacement.tracks)
        assertEquals(1, deferredQueueStartIndex(replacement.tracks.map(TrackEntity::uri), replacement.anchor.uri))
        assertNull(pending.take())
    }

    @Test
    fun repeatingTheSameTrackStillAppliesThePendingGroup() {
        val pending = PendingPlaybackQueue()
        val current = track("current")
        // Previous repeats must not prevent a later group switch from being handled.
        repeat(3) { assertNull(pending.onTrackFinished()) }
        val target = listOf(current, track("new-next"))

        assertNull(pending.schedule(target, pending.captureAnchor(current.uri), current.uri, ended = false))
        assertEquals(target, pending.onTrackFinished()?.tracks)
        assertNull(pending.onTrackFinished())
    }

    @Test
    fun repeatDuringGroupLoadingAppliesTheResultEvenWhenTheUriIsUnchanged() {
        val pending = PendingPlaybackQueue()
        val current = track("current")
        val anchor = pending.captureAnchor(current.uri)
        pending.onTrackFinished()
        val target = listOf(current, track("new-next"))

        assertEquals(target, pending.schedule(target, anchor, current.uri, ended = false)?.tracks)
        assertNull(pending.onTrackFinished())
    }

    @Test
    fun finishingOutsideTheNewGroupStartsAtItsFirstTrack() {
        val pending = PendingPlaybackQueue()
        val anchor = pending.captureAnchor("old")
        val target = listOf(track("new-first"), track("new-second"))

        assertNull(pending.schedule(target, anchor, "old", ended = false))
        val replacement = requireNotNull(pending.onTrackFinished())
        assertEquals(0, deferredQueueStartIndex(replacement.tracks.map(TrackEntity::uri), replacement.anchor.uri))
    }

    @Test
    fun alreadyEndedPlaybackAppliesTheTargetQueueImmediately() {
        val pending = PendingPlaybackQueue()
        val anchor = pending.captureAnchor("current")

        assertNotNull(pending.schedule(listOf(track("next")), anchor, "current", ended = true))
        assertNull(pending.take())
    }

    @Test
    fun latestGroupSwitchWinsAndExplicitPlaybackCancelsTheSwitch() {
        val pending = PendingPlaybackQueue()
        val anchor = pending.captureAnchor("current")
        pending.schedule(listOf(track("first-group")), anchor, "current", ended = false)
        val latest = listOf(track("last-group"))
        pending.schedule(latest, anchor, "current", ended = false)
        assertEquals(latest, pending.onTrackFinished()?.tracks)

        pending.schedule(latest, pending.captureAnchor("current"), "current", ended = false)
        pending.cancel()
        assertNull(pending.onTrackFinished())
    }

    @Test
    fun switchingToAnEmptyGroupWaitsUntilTheCurrentSongFinishes() {
        val pending = PendingPlaybackQueue()
        val anchor = pending.captureAnchor("current")

        assertNull(pending.schedule(emptyList(), anchor, "current", ended = false))
        assertEquals(emptyList<TrackEntity>(), pending.onTrackFinished()?.tracks)
    }

    private fun track(id: String) = TrackEntity(
        uri = "file:///$id.mp3",
        rootUri = "file:///music",
        title = id,
        artist = "银临",
        album = "",
        durationMs = 0,
        mimeType = "audio/mpeg",
        lyricUri = null,
    )
}
