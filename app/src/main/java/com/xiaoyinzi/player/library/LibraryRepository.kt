package com.xiaoyinzi.player.library

import androidx.room.withTransaction
import com.xiaoyinzi.player.data.AppDatabase
import com.xiaoyinzi.player.data.GroupSummary
import com.xiaoyinzi.player.data.MusicGroupEntity
import com.xiaoyinzi.player.data.TrackEntity
import com.xiaoyinzi.player.data.TrackGroupCrossRef
import kotlinx.coroutines.flow.Flow

class LibraryRepository(private val database: AppDatabase) {
    private val dao = database.libraryDao()

    val tracks: Flow<List<TrackEntity>> = dao.observeTracks()
    val groups: Flow<List<GroupSummary>> = dao.observeGroups()
    val trackGroups: Flow<List<TrackGroupCrossRef>> = dao.observeTrackGroups()

    fun tracksInGroup(groupId: Long): Flow<List<TrackEntity>> = dao.observeTracksInGroup(groupId)

    suspend fun replaceScan(rootUri: String, tracks: List<TrackEntity>) {
        database.withTransaction {
            dao.deleteTracksOutsideRoot(rootUri)
            val scannedUris = tracks.mapTo(hashSetOf(), TrackEntity::uri)
            val removedUris = dao.trackUrisForRoot(rootUri).filterNot(scannedUris::contains)
            if (tracks.isNotEmpty()) dao.upsertTracks(tracks)
            if (removedUris.isNotEmpty()) dao.deleteTracks(removedUris)
        }
    }

    suspend fun createGroup(name: String): Long = dao.insertGroup(MusicGroupEntity(name = name.trim()))

    suspend fun copyGroup(sourceGroupId: String?, name: String): Long = database.withTransaction {
        require(name.isNotBlank()) { "分组名称不能为空" }
        val customId = sourceGroupId?.customGroupIdOrNull()
        if (customId != null) require(dao.groupExists(customId)) { "分组不存在" }
        val trackUris = groupTrackUrisForCopy(
            groupId = sourceGroupId,
            tracks = dao.allTracks(),
            memberships = customId?.let { dao.membershipsInGroup(it) }.orEmpty(),
        )
        val newGroupId = createGroup(name)
        if (trackUris.isNotEmpty()) {
            dao.addTracksToGroup(trackUris.map { TrackGroupCrossRef(it, newGroupId) })
        }
        newGroupId
    }

    suspend fun deleteGroup(groupId: Long) = dao.deleteGroup(groupId)

    suspend fun deleteTrack(trackUri: String) = dao.deleteTracks(listOf(trackUri))

    suspend fun addToGroup(trackUri: String, groupId: Long) {
        dao.addTrackToGroup(TrackGroupCrossRef(trackUri, groupId))
    }

    suspend fun removeFromGroup(trackUri: String, groupId: Long) {
        dao.removeTrackFromGroup(TrackGroupCrossRef(trackUri, groupId))
    }
}
