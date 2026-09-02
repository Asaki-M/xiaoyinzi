package com.xiaoyinzi.player.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.xiaoyinzi.player.data.TrackEntity
import com.xiaoyinzi.player.casting.MEDIA_EXTRA_LYRIC_URI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val connected: Boolean = false,
    val isPlaying: Boolean = false,
    val currentTrackUri: String? = null,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val playbackMode: PlaybackMode = PlaybackMode.SEQUENTIAL,
    val queue: List<QueueItemUiState> = emptyList(),
    val currentQueueIndex: Int = 0,
)

data class QueueItemUiState(
    val mediaItemIndex: Int,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
)

enum class PlaybackMode {
    SEQUENTIAL,
    REPEAT_ALL,
    REPEAT_ONE,
    SHUFFLE;

    fun next(): PlaybackMode = entries[(ordinal + 1) % entries.size]

    companion object {
        fun from(shuffleEnabled: Boolean, repeatMode: Int): PlaybackMode = when {
            shuffleEnabled -> SHUFFLE
            repeatMode == Player.REPEAT_MODE_ALL -> REPEAT_ALL
            repeatMode == Player.REPEAT_MODE_ONE -> REPEAT_ONE
            else -> SEQUENTIAL
        }

        fun fromStoredValue(value: String?): PlaybackMode = entries
            .firstOrNull { it.name == value }
            ?: SEQUENTIAL
    }
}

internal fun buildQueueIndices(
    mediaItemCount: Int,
    firstIndex: Int,
    nextIndex: (Int) -> Int,
): List<Int> {
    if (mediaItemCount == 0 || firstIndex == C.INDEX_UNSET) return emptyList()

    val indices = ArrayList<Int>(mediaItemCount)
    var index = firstIndex
    while (index != C.INDEX_UNSET && index !in indices && indices.size < mediaItemCount) {
        indices += index
        index = nextIndex(index)
    }
    return indices
}

class PlayerConnection(context: Context) : AutoCloseable {
    private val applicationContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()
    private var controller: MediaController? = null
    private var positionJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publishState(player)
    }

    init {
        val token = SessionToken(applicationContext, ComponentName(applicationContext, PlaybackService::class.java))
        val future = MediaController.Builder(applicationContext, token).buildAsync()
        future.addListener(
            {
                runCatching { future.get() }.onSuccess { connectedController ->
                    controller = connectedController
                    connectedController.addListener(listener)
                    publishState(connectedController)
                    startPositionUpdates()
                }
            },
            ContextCompat.getMainExecutor(applicationContext),
        )
    }

    fun play(track: TrackEntity, queue: List<TrackEntity>) {
        val player = controller ?: return
        val mediaItems = queue.map { it.toMediaItem() }
        val selectedIndex = queue.indexOfFirst { it.uri == track.uri }.coerceAtLeast(0)
        player.setMediaItems(mediaItems, selectedIndex, 0)
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun seekNext() {
        controller?.seekToNextMediaItem()
    }

    fun seekPrevious() {
        controller?.seekToPreviousMediaItem()
    }

    fun playQueueItem(index: Int) {
        controller?.let { player ->
            if (index !in 0 until player.mediaItemCount) return
            player.seekToDefaultPosition(index)
            player.play()
        }
    }

    fun cyclePlaybackMode() {
        controller?.let { player ->
            when (PlaybackMode.from(player.shuffleModeEnabled, player.repeatMode).next()) {
                PlaybackMode.SEQUENTIAL -> {
                    player.shuffleModeEnabled = false
                    player.repeatMode = Player.REPEAT_MODE_OFF
                }
                PlaybackMode.REPEAT_ALL -> {
                    player.shuffleModeEnabled = false
                    player.repeatMode = Player.REPEAT_MODE_ALL
                }
                PlaybackMode.REPEAT_ONE -> {
                    player.shuffleModeEnabled = false
                    player.repeatMode = Player.REPEAT_MODE_ONE
                }
                PlaybackMode.SHUFFLE -> {
                    player.shuffleModeEnabled = true
                    player.repeatMode = Player.REPEAT_MODE_ALL
                }
            }
        }
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                controller?.let(::publishState)
                delay(250)
            }
        }
    }

    private fun publishState(player: Player) {
        val metadata = player.mediaMetadata
        val timeline = player.currentTimeline
        val queueIndices = buildQueueIndices(
            mediaItemCount = player.mediaItemCount,
            firstIndex = timeline.getFirstWindowIndex(player.shuffleModeEnabled),
            nextIndex = { index ->
                timeline.getNextWindowIndex(
                    index,
                    Player.REPEAT_MODE_OFF,
                    player.shuffleModeEnabled,
                )
            },
        )
        _state.value = PlayerUiState(
            connected = true,
            isPlaying = player.isPlaying,
            currentTrackUri = player.currentMediaItem?.mediaId,
            title = metadata.title?.toString().orEmpty(),
            artist = metadata.artist?.toString().orEmpty(),
            album = metadata.albumTitle?.toString().orEmpty(),
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.coerceAtLeast(0),
            playbackMode = PlaybackMode.from(player.shuffleModeEnabled, player.repeatMode),
            queue = queueIndices.map { index ->
                player.getMediaItemAt(index).let { item ->
                    QueueItemUiState(
                        mediaItemIndex = index,
                        uri = item.mediaId,
                        title = item.mediaMetadata.title?.toString().orEmpty(),
                        artist = item.mediaMetadata.artist?.toString().orEmpty(),
                        album = item.mediaMetadata.albumTitle?.toString().orEmpty(),
                    )
                }
            },
            currentQueueIndex = queueIndices.indexOf(player.currentMediaItemIndex).coerceAtLeast(0),
        )
    }

    override fun close() {
        positionJob?.cancel()
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        scope.cancel()
    }

    private fun TrackEntity.toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(uri)
        .setUri(uri)
        .setMimeType(mimeType)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setExtras(
                    Bundle().apply {
                        lyricUri?.let { putString(MEDIA_EXTRA_LYRIC_URI, it) }
                    },
                )
                .build(),
        )
        .build()
}
