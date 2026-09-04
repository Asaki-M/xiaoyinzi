package com.xiaoyinzi.player

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoyinzi.player.data.GroupSummary
import com.xiaoyinzi.player.data.TrackEntity
import com.xiaoyinzi.player.casting.CastDevice
import com.xiaoyinzi.player.library.LibraryRepository
import com.xiaoyinzi.player.library.LibraryScanner
import com.xiaoyinzi.player.library.SilverlinCatalog
import com.xiaoyinzi.player.library.albumGroupId
import com.xiaoyinzi.player.library.customGroupId
import com.xiaoyinzi.player.library.customGroupIdOrNull
import com.xiaoyinzi.player.lyrics.LrcxParser
import com.xiaoyinzi.player.lyrics.LyricLine
import com.xiaoyinzi.player.playback.PlayerConnection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val tracks: List<LibraryTrackUiState> = emptyList(),
    val groups: List<LibraryGroupUiState> = emptyList(),
    val customGroups: List<GroupSummary> = emptyList(),
    val selectedGroupId: String? = null,
    val selectedCustomGroupId: Long? = null,
    val selectedGroupName: String? = null,
    val scanning: Boolean = false,
    val message: String? = null,
)

data class LibraryTrackUiState(
    val key: String,
    val title: String,
    val track: TrackEntity?,
)

data class LibraryGroupUiState(
    val id: String,
    val name: String,
    val isPreset: Boolean,
)

private data class LibraryContent(
    val visibleTracks: List<TrackEntity>,
    val allTracks: List<TrackEntity>,
    val customGroups: List<GroupSummary>,
    val selectedGroupId: String?,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as XiaoYinZiApplication
    private val repository: LibraryRepository = app.libraryRepository
    private val scanner: LibraryScanner = app.libraryScanner
    private val archiveImporter = app.libraryArchiveImporter
    private val managedMusicDirectory = app.managedMusicDirectory
    private val managedLibraryFiles = app.managedLibraryFiles
    private val lyricParser: LrcxParser = app.lyricParser
    private val selectedGroupId = MutableStateFlow<String?>(null)
    private val scanning = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    val player = PlayerConnection(application)
    val playerState = player.state
    val castState = app.lyricsCastManager.state
    private var deferredQueueJob: Job? = null

    private val repositorySnapshot = repository.tracks
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val lyrics: StateFlow<List<LyricLine>> = combine(playerState, repositorySnapshot) { state, tracks ->
        tracks.firstOrNull { it.uri == state.currentTrackUri }?.lyricUri
    }.mapLatest(lyricParser::read)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val visibleTracks = selectedGroupId.flatMapLatest { groupId ->
        groupId?.customGroupIdOrNull()?.let(repository::tracksInGroup) ?: repository.tracks
    }

    private val libraryContent = combine(
        visibleTracks,
        repositorySnapshot,
        repository.groups,
        selectedGroupId,
    ) { tracks, allTracks, groups, groupId ->
        LibraryContent(tracks, allTracks, groups, groupId)
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        libraryContent,
        scanning,
        message,
    ) { content, isScanning, currentMessage ->
        val presetGroups = SilverlinCatalog.albums.map { album ->
            LibraryGroupUiState(
                id = albumGroupId(album.id),
                name = album.title,
                isPreset = true,
            )
        }
        val groups = presetGroups + content.customGroups.map { group ->
            LibraryGroupUiState(
                id = customGroupId(group.id),
                name = group.name,
                isPreset = false,
            )
        }
        val displayTracks = when (content.selectedGroupId) {
            null -> content.allTracks.map(TrackEntity::toLibraryTrackUiState)
            else -> {
                val album = SilverlinCatalog.albums.firstOrNull {
                    albumGroupId(it.id) == content.selectedGroupId
                }
                if (album == null) {
                    content.visibleTracks.map(TrackEntity::toLibraryTrackUiState)
                } else {
                    SilverlinCatalog.matchAlbum(album, content.allTracks).mapIndexed { index, match ->
                        LibraryTrackUiState(
                            key = match.track?.uri ?: "catalog:${album.id}:$index",
                            title = match.title,
                            track = match.track,
                        )
                    }
                }
            }
        }
        val selectedGroup = groups.firstOrNull { it.id == content.selectedGroupId }
        LibraryUiState(
            tracks = displayTracks,
            groups = groups,
            customGroups = content.customGroups,
            selectedGroupId = content.selectedGroupId,
            selectedCustomGroupId = content.selectedGroupId?.customGroupIdOrNull(),
            selectedGroupName = selectedGroup?.name,
            scanning = isScanning,
            message = currentMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    init {
        viewModelScope.launch { scanManagedLibrary(showMessage = false) }
    }

    fun rescan() {
        viewModelScope.launch { scanManagedLibrary(showMessage = true) }
    }

    fun importArchive(uri: Uri) {
        viewModelScope.launch {
            scanning.value = true
            message.value = null
            runCatching {
                val result = archiveImporter.importArchive(uri)
                val tracks = scanner.scan(managedMusicDirectory)
                repository.replaceScan(managedMusicDirectory.toUriString(), tracks)
                result
            }.onSuccess { result ->
                message.value = "已导入 ${result.audioCount} 首音乐、${result.lyricCount} 个歌词文件"
            }.onFailure {
                message.value = "导入失败：${it.localizedMessage}"
            }
            scanning.value = false
        }
    }

    fun selectGroup(groupId: String?) {
        if (selectedGroupId.value == groupId) return

        deferredQueueJob?.cancel()
        player.cancelDeferredQueueReplacement()
        selectedGroupId.value = groupId
        val anchorUri = playerState.value.currentTrackUri
        if (anchorUri == null) {
            return
        }

        deferredQueueJob = viewModelScope.launch {
            val selectedState = uiState.first { it.selectedGroupId == groupId }
            if (selectedGroupId.value != groupId) return@launch
            player.deferQueueReplacement(
                queue = selectedState.tracks.mapNotNull(LibraryTrackUiState::track),
                anchorUri = anchorUri,
            )
        }
    }

    fun createGroup(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            selectGroup(customGroupId(repository.createGroup(name)))
        }
    }

    fun deleteSelectedGroup() {
        val id = selectedGroupId.value?.customGroupIdOrNull() ?: return
        viewModelScope.launch {
            repository.deleteGroup(id)
            selectGroup(null)
        }
    }

    fun addTrackToGroup(trackUri: String, groupId: Long) {
        viewModelScope.launch { repository.addToGroup(trackUri, groupId) }
    }

    fun removeTrackFromSelectedGroup(trackUri: String) {
        val groupId = selectedGroupId.value?.customGroupIdOrNull() ?: return
        viewModelScope.launch { repository.removeFromGroup(trackUri, groupId) }
    }

    fun deleteTrack(track: TrackEntity) {
        viewModelScope.launch {
            message.value = null
            runCatching {
                managedLibraryFiles.deleteTrack(track)
                player.removeFromQueue(track.uri)
                repository.deleteTrack(track.uri)
            }.onSuccess {
                message.value = "已删除《${track.title}》及关联歌词"
            }.onFailure {
                message.value = "删除失败：${it.localizedMessage}"
            }
        }
    }

    fun play(track: TrackEntity) {
        deferredQueueJob?.cancel()
        player.play(track, uiState.value.tracks.mapNotNull(LibraryTrackUiState::track))
    }

    fun clearMessage() {
        message.value = null
    }

    fun startCastDiscovery() = app.lyricsCastManager.startDiscovery()

    fun stopCastDiscovery() = app.lyricsCastManager.stopDiscovery()

    fun connectCastDevice(device: CastDevice) = app.lyricsCastManager.connect(device)

    fun disconnectCast() = app.lyricsCastManager.disconnect()

    fun submitCastPairingCode(code: String) = app.lyricsCastManager.submitPairingCode(code)

    fun forgetCastPairing() = app.lyricsCastManager.forgetPairing()

    private suspend fun scanManagedLibrary(showMessage: Boolean) {
        scanning.value = true
        message.value = null
        runCatching { scanner.scan(managedMusicDirectory) }
            .onSuccess { tracks ->
                repository.replaceScan(managedMusicDirectory.toUriString(), tracks)
                if (showMessage) message.value = "已找到 ${tracks.size} 首音乐"
            }
            .onFailure { message.value = "扫描失败：${it.localizedMessage}" }
        scanning.value = false
    }

    override fun onCleared() {
        deferredQueueJob?.cancel()
        player.close()
        super.onCleared()
    }
}

private fun java.io.File.toUriString(): String = Uri.fromFile(this).toString()

private fun TrackEntity.toLibraryTrackUiState(): LibraryTrackUiState = LibraryTrackUiState(
    key = uri,
    title = title,
    track = this,
)
