package com.xiaoyinzi.player.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val uri: String,
    val rootUri: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val mimeType: String,
    val lyricUri: String?,
)

@Entity(tableName = "music_groups")
data class MusicGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "track_group_cross_ref",
    primaryKeys = ["trackUri", "groupId"],
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["uri"],
            childColumns = ["trackUri"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MusicGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("trackUri"), Index("groupId")],
)
data class TrackGroupCrossRef(
    val trackUri: String,
    val groupId: Long,
)

data class GroupSummary(
    val id: Long,
    val name: String,
    val trackCount: Int,
)

