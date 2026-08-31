package com.xiaoyinzi.player

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoyinzi.player.data.GroupSummary
import com.xiaoyinzi.player.data.TrackEntity
import com.xiaoyinzi.player.library.LibraryRepository
import com.xiaoyinzi.player.library.LibraryScanner
import com.xiaoyinzi.player.lyrics.LrcxParser
import com.xiaoyinzi.player.lyrics.LyricLine
import com.xiaoyinzi.player.playback.PlayerConnection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val tracks: List<TrackEntity> = emptyList(),
    val groups: List<GroupSummary> = emptyList(),
    val selectedGroupId: Long? = null,
    val selectedGroupName: String? = null,
    val folderUri: String? = null,
    val scanning: Boolean = false,
    val message: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as XiaoYinZiApplication
    private val repository: LibraryRepository = app.libraryRepository
    private val scanner: LibraryScanner = app.libraryScanner
    private val lyricParser: LrcxParser = app.lyricParser
    private val preferences = application.getSharedPreferences("library", 0)
    private val selectedGroupId = MutableStateFlow<Long?>(null)
    private val scanning = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    val player = PlayerConnection(application)
    val playerState = player.state

    private val repositorySnapshot = repository.tracks
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val lyrics: StateFlow<List<LyricLine>> = combine(playerState, repositorySnapshot) { state, tracks ->
        tracks.firstOrNull { it.uri == state.currentTrackUri }?.lyricUri
    }.mapLatest(lyricParser::read)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val visibleTracks = selectedGroupId.flatMapLatest { groupId ->
        if (groupId == null) repository.tracks else repository.tracksInGroup(groupId)
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        visibleTracks,
        repository.groups,
        selectedGroupId,
        scanning,
        message,
    ) { tracks, groups, groupId, isScanning, currentMessage ->
        LibraryUiState(
            tracks = tracks,
            groups = groups,
            selectedGroupId = groupId,
            selectedGroupName = groups.firstOrNull { it.id == groupId }?.name,
            folderUri = preferences.getString(KEY_FOLDER_URI, null),
            scanning = isScanning,
            message = currentMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    init {
        preferences.getString(KEY_FOLDER_URI, null)?.let { scan(Uri.parse(it)) }
    }

    fun selectFolder(uri: Uri) {
        runCatching {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            preferences.edit().putString(KEY_FOLDER_URI, uri.toString()).apply()
            scan(uri)
        }.onFailure { message.value = "无法读取所选目录：${it.localizedMessage}" }
    }

    fun rescan() {
        preferences.getString(KEY_FOLDER_URI, null)?.let { scan(Uri.parse(it)) }
    }

    fun selectGroup(groupId: Long?) {
        selectedGroupId.value = groupId
    }

    fun createGroup(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            selectedGroupId.value = repository.createGroup(name)
        }
    }

    fun deleteSelectedGroup() {
        val id = selectedGroupId.value ?: return
        viewModelScope.launch {
            repository.deleteGroup(id)
            selectedGroupId.value = null
        }
    }

    fun addTrackToGroup(trackUri: String, groupId: Long) {
        viewModelScope.launch { repository.addToGroup(trackUri, groupId) }
    }

    fun removeTrackFromSelectedGroup(trackUri: String) {
        val groupId = selectedGroupId.value ?: return
        viewModelScope.launch { repository.removeFromGroup(trackUri, groupId) }
    }

    fun play(track: TrackEntity) {
        player.play(track, uiState.value.tracks)
    }

    fun clearMessage() {
        message.value = null
    }

    private fun scan(uri: Uri) {
        viewModelScope.launch {
            scanning.value = true
            message.value = null
            runCatching { scanner.scan(uri) }
                .onSuccess { tracks ->
                    repository.replaceScan(uri.toString(), tracks)
                    message.value = "已找到 ${tracks.size} 首音乐"
                }
                .onFailure { message.value = "扫描失败：${it.localizedMessage}" }
            scanning.value = false
        }
    }

    override fun onCleared() {
        player.close()
        super.onCleared()
    }

    private companion object {
        const val KEY_FOLDER_URI = "folder_uri"
    }
}
