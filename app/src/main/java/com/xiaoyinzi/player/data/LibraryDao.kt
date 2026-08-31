package com.xiaoyinzi.player.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE")
    fun observeTracks(): Flow<List<TrackEntity>>

    @Query(
        """
        SELECT music_groups.id, music_groups.name, COUNT(track_group_cross_ref.trackUri) AS trackCount
        FROM music_groups
        LEFT JOIN track_group_cross_ref ON music_groups.id = track_group_cross_ref.groupId
        GROUP BY music_groups.id
        ORDER BY music_groups.createdAt DESC
        """,
    )
    fun observeGroups(): Flow<List<GroupSummary>>

    @Query(
        """
        SELECT tracks.* FROM tracks
        INNER JOIN track_group_cross_ref ON tracks.uri = track_group_cross_ref.trackUri
        WHERE track_group_cross_ref.groupId = :groupId
        ORDER BY tracks.title COLLATE NOCASE
        """,
    )
    fun observeTracksInGroup(groupId: Long): Flow<List<TrackEntity>>

    @Query("SELECT uri FROM tracks WHERE rootUri = :rootUri")
    suspend fun trackUrisForRoot(rootUri: String): List<String>

    @Upsert
    suspend fun upsertTracks(tracks: List<TrackEntity>)

    @Query("DELETE FROM tracks WHERE uri IN (:uris)")
    suspend fun deleteTracks(uris: List<String>)

    @Query("DELETE FROM tracks WHERE rootUri != :rootUri")
    suspend fun deleteTracksOutsideRoot(rootUri: String)

    @Insert
    suspend fun insertGroup(group: MusicGroupEntity): Long

    @Query("DELETE FROM music_groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTrackToGroup(crossRef: TrackGroupCrossRef)

    @Delete
    suspend fun removeTrackFromGroup(crossRef: TrackGroupCrossRef)
}
