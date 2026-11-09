package com.xiaoyinzi.player.playback

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.common.Player
import com.xiaoyinzi.player.data.TrackEntity
import com.xiaoyinzi.player.library.LibraryRepository
import com.xiaoyinzi.player.library.SilverlinCatalog
import com.xiaoyinzi.player.library.albumGroupId
import com.xiaoyinzi.player.library.customGroupId
import com.xiaoyinzi.player.library.groupTrackUrisForCopy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class PlaybackLibraryGroup(val id: String?, val name: String, val trackCount: Int)

internal data class PlaybackLibraryState(
    val selectedGroupId: String? = null,
    val groups: List<PlaybackLibraryGroup> = emptyList(),
    val tracks: List<TrackEntity> = emptyList(),
)

internal class LibraryPlaybackController(
    context: Context,
    repository: LibraryRepository,
    private val deferredQueue: DeferredPlaybackQueue,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val preferences = context.getSharedPreferences("library_preferences", Context.MODE_PRIVATE)
    val selectedGroupId = MutableStateFlow(preferences.getString("default_group_id", null))
    private val hiddenPresetIds = MutableStateFlow(readHiddenPresetIds())
    private var player: Player? = null
    private var queueChangeJob: Job? = null
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "hidden_preset_group_ids") hiddenPresetIds.value = readHiddenPresetIds()
    }

    val library = combine(
        repository.tracks,
        repository.groups,
        repository.trackGroups,
        selectedGroupId,
        hiddenPresetIds,
    ) { tracks, groups, memberships, selectedId, hiddenIds ->
        val tracksByUri = tracks.associateBy(TrackEntity::uri)
        fun tracksInGroup(groupId: String?): List<TrackEntity> =
            groupTrackUrisForCopy(groupId, tracks, memberships).mapNotNull(tracksByUri::get)

        val availableGroups = buildList {
            add(PlaybackLibraryGroup(null, "全部", tracks.size))
            SilverlinCatalog.visibleAlbums(hiddenIds).forEach { album ->
                val id = albumGroupId(album.id)
                add(PlaybackLibraryGroup(id, album.title, tracksInGroup(id).size))
            }
            groups.forEach { group ->
                add(PlaybackLibraryGroup(customGroupId(group.id), group.name, group.trackCount))
            }
        }
        val validId = selectedId.takeIf { id -> availableGroups.any { it.id == id } }
        PlaybackLibraryState(validId, availableGroups, tracksInGroup(validId))
    }.stateIn(scope, SharingStarted.Eagerly, PlaybackLibraryState())

    init {
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    fun bindPlayer(player: Player) {
        this.player = player
    }

    fun unbindPlayer() {
        cancelPendingQueueChange()
        player = null
    }

    fun selectGroup(groupId: String?) {
        if (selectedGroupId.value == groupId) return
        cancelPendingQueueChange()
        selectedGroupId.value = groupId
        val anchor = deferredQueue.captureAnchor() ?: return
        queueChangeJob = scope.launch {
            val snapshot = library.first { it.selectedGroupId == groupId && it.groups.isNotEmpty() }
            if (selectedGroupId.value == groupId) deferredQueue.schedule(snapshot.tracks, anchor)
        }
    }

    suspend fun selectRemoteGroup(groupId: String?) {
        require(library.value.groups.any { it.id == groupId }) { "分组不存在，请刷新后重试" }
        selectGroup(groupId)
        library.first { it.selectedGroupId == groupId || selectedGroupId.value != groupId }
        check(selectedGroupId.value == groupId) { "手机上的分组已切换，请重新选择" }
        queueChangeJob?.join()
    }

    fun cancelPendingQueueChange() {
        queueChangeJob?.cancel()
        queueChangeJob = null
        deferredQueue.cancel()
    }

    fun playTrack(trackUri: String) {
        val activePlayer = checkNotNull(player) { "手机播放器尚未就绪" }
        check(library.value.selectedGroupId == selectedGroupId.value) { "分组正在切换，请稍后再试" }
        val tracks = library.value.tracks
        val index = tracks.indexOfFirst { it.uri == trackUri }
        require(index >= 0) { "歌曲已不在当前分组，请重新选择" }
        cancelPendingQueueChange()
        activePlayer.setMediaItems(tracks.map { it.toMediaItem() }, index, 0)
        activePlayer.prepare()
        activePlayer.play()
    }

    suspend fun next() {
        val activePlayer = checkNotNull(player) { "手机播放器尚未就绪" }
        queueChangeJob?.join()
        if (!deferredQueue.applyPending()) activePlayer.seekToNextMediaItem()
    }

    fun previous() {
        checkNotNull(player) { "手机播放器尚未就绪" }.seekToPreviousMediaItem()
    }

    fun togglePlayPause() {
        val activePlayer = checkNotNull(player) { "手机播放器尚未就绪" }
        require(activePlayer.mediaItemCount > 0) { "请先选择歌曲" }
        if (activePlayer.isPlaying) activePlayer.pause() else {
            if (activePlayer.playbackState == Player.STATE_ENDED) activePlayer.seekToDefaultPosition()
            if (activePlayer.playbackState == Player.STATE_IDLE) activePlayer.prepare()
            activePlayer.play()
        }
    }

    private fun readHiddenPresetIds(): Set<String> =
        preferences.getStringSet("hidden_preset_group_ids", emptySet()).orEmpty().toSet()
}
