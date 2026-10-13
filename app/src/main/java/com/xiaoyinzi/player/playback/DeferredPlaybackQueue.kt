package com.xiaoyinzi.player.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.xiaoyinzi.player.data.TrackEntity

internal data class PlaybackAnchor(
    val uri: String,
    val completionGeneration: Long,
)

internal data class PlaybackQueueReplacement(
    val tracks: List<TrackEntity>,
    val anchor: PlaybackAnchor,
)

internal class PendingPlaybackQueue {
    private var completionGeneration = 0L
    private var replacement: PlaybackQueueReplacement? = null

    fun captureAnchor(uri: String): PlaybackAnchor = PlaybackAnchor(uri, completionGeneration)

    fun schedule(
        tracks: List<TrackEntity>,
        anchor: PlaybackAnchor,
        currentUri: String?,
        ended: Boolean,
    ): PlaybackQueueReplacement? {
        replacement = PlaybackQueueReplacement(tracks.toList(), anchor)
        // A repeat can finish while the group query is loading without changing the current URI.
        return if (ended || currentUri != anchor.uri || completionGeneration != anchor.completionGeneration) {
            take()
        } else {
            null
        }
    }

    fun onTrackFinished(): PlaybackQueueReplacement? {
        completionGeneration++
        return take()
    }

    fun take(): PlaybackQueueReplacement? = replacement.also { replacement = null }

    fun cancel() {
        replacement = null
    }
}

internal class DeferredPlaybackQueue {
    private val pending = PendingPlaybackQueue()
    private var player: Player? = null

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
            ) {
                apply(pending.onTrackFinished())
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) apply(pending.onTrackFinished())
        }
    }

    fun bindPlayer(player: Player) {
        unbindPlayer()
        this.player = player
        player.addListener(listener)
    }

    fun unbindPlayer() {
        player?.removeListener(listener)
        player = null
        pending.cancel()
    }

    fun captureAnchor(): PlaybackAnchor? = player?.currentMediaItem?.mediaId?.let(pending::captureAnchor)

    fun schedule(tracks: List<TrackEntity>, anchor: PlaybackAnchor) {
        val player = player ?: return
        apply(
            pending.schedule(
                tracks = tracks,
                anchor = anchor,
                currentUri = player.currentMediaItem?.mediaId,
                ended = player.playbackState == Player.STATE_ENDED,
            ),
        )
    }

    fun cancel() = pending.cancel()

    fun applyPending(): Boolean = apply(pending.take())

    private fun apply(replacement: PlaybackQueueReplacement?): Boolean {
        if (replacement == null) return false
        val player = player ?: return false
        if (replacement.tracks.isEmpty()) {
            player.clearMediaItems()
            return true
        }
        val shouldPlay = player.playWhenReady
        val startIndex = deferredQueueStartIndex(replacement.tracks.map(TrackEntity::uri), replacement.anchor.uri)
        player.setMediaItems(replacement.tracks.map { it.toMediaItem() }, startIndex, 0)
        player.prepare()
        if (shouldPlay) player.play()
        return true
    }
}
