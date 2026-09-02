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

    suspend fun deleteGroup(groupId: Long) = dao.deleteGroup(groupId)

    suspend fun deleteTrack(trackUri: String) = dao.deleteTracks(listOf(trackUri))

    suspend fun addToGroup(trackUri: String, groupId: Long) {
        dao.addTrackToGroup(TrackGroupCrossRef(trackUri, groupId))
    }

    suspend fun removeFromGroup(trackUri: String, groupId: Long) {
        dao.removeTrackFromGroup(TrackGroupCrossRef(trackUri, groupId))
    }
}
