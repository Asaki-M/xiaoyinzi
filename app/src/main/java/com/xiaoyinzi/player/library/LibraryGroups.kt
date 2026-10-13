package com.xiaoyinzi.player.library

import com.xiaoyinzi.player.data.TrackEntity
import com.xiaoyinzi.player.data.TrackGroupCrossRef
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

internal data class LibraryGroupTracks(
    val groupId: String?,
    val tracks: List<TrackEntity>,
)

@OptIn(ExperimentalCoroutinesApi::class)
internal fun observeGroupTracks(
    selectedGroupIds: Flow<String?>,
    tracksForGroup: (String?) -> Flow<List<TrackEntity>>,
): Flow<LibraryGroupTracks> = selectedGroupIds.distinctUntilChanged().flatMapLatest { groupId ->
    // Keep the group ID with its query result so a new selection cannot reuse the old group's tracks.
    tracksForGroup(groupId).map { LibraryGroupTracks(groupId, it) }
}

internal fun isLibraryGroupAvailable(
    groupId: String?,
    customGroupIds: Set<Long>,
    hiddenPresetGroupIds: Set<String>,
): Boolean = when {
    groupId == null -> true
    SilverlinCatalog.containsGroup(groupId) -> groupId !in hiddenPresetGroupIds
    else -> groupId.customGroupIdOrNull() in customGroupIds
}

internal fun groupTrackUrisForCopy(
    groupId: String?,
    tracks: List<TrackEntity>,
    memberships: List<TrackGroupCrossRef>,
): List<String> {
    if (groupId == null) return tracks.map(TrackEntity::uri).distinct()
    val customId = groupId.customGroupIdOrNull()
    if (customId != null) {
        val memberUris = memberships.filter { it.groupId == customId }.mapTo(hashSetOf()) { it.trackUri }
        return tracks.filter { it.uri in memberUris }.map(TrackEntity::uri).distinct()
    }
    val album = requireNotNull(SilverlinCatalog.albums.firstOrNull { albumGroupId(it.id) == groupId }) {
        "分组不存在"
    }
    return SilverlinCatalog.matchAlbum(album, tracks).mapNotNull { it.track?.uri }.distinct()
}
