package com.xiaoyinzi.player.library

import com.xiaoyinzi.player.data.TrackEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class SilverlinCatalogTest {
    @Test
    fun matchesNumberedAndVersionedLocalTitlesToAlbumTracks() {
        val local = track("01. 故城 (feat. 灰原穷)")
        val album = SilverlinCatalog.albums.first { it.title == "腐草为萤" }

        val match = SilverlinCatalog.matchAlbum(album, listOf(local)).first { it.title == "故城" }

        assertSame(local, match.track)
    }

    @Test
    fun keepsMissingAlbumTracks() {
        val albumTrack = track("不老梦")
        val album = SilverlinCatalog.albums.first { it.title == "蚍蜉渡海" }

        val matches = SilverlinCatalog.matchAlbum(album, listOf(albumTrack))

        assertEquals(album.tracks.size, matches.size)
        assertNull(matches.first { it.title == "灼" }.track)
    }

    @Test
    fun includesAllRequestedPresetAlbums() {
        assertEquals(
            listOf(
                "腐草为萤",
                "蚍蜉渡海",
                "风花雪月",
                "琉璃",
                "离地十公分·A面",
                "离地十公分·B面",
                "山色有无中",
                "粼粼",
            ),
            SilverlinCatalog.albums.map(PresetAlbum::title),
        )
        assertEquals(61, SilverlinCatalog.albums.sumOf { it.tracks.size })
    }

    @Test
    fun keepsTenCentimetersAboveGroundSidesAndFengHuaXueYueSeparate() {
        assertEquals(
            listOf("你是", "眉南边", "一命矣"),
            SilverlinCatalog.albums.first { it.title == "离地十公分·A面" }.tracks,
        )
        assertEquals(
            listOf("窗前明月光", "见夏如晤", "日出前起飞", "Drive Until Sunset"),
            SilverlinCatalog.albums.first { it.title == "离地十公分·B面" }.tracks,
        )
        assertEquals(
            listOf("无题雪", "珍珠", "月球", "焦骨"),
            SilverlinCatalog.albums.first { it.title == "风花雪月" }.tracks,
        )
    }

    @Test
    fun hidesOnlyRequestedPresetAlbums() {
        val hiddenGroupId = albumGroupId("feng-hua-xue-yue")

        val visibleAlbums = SilverlinCatalog.visibleAlbums(setOf(hiddenGroupId, "preset:album:unknown"))

        assertEquals(7, visibleAlbums.size)
        assertEquals(false, visibleAlbums.any { it.title == "风花雪月" })
        assertEquals(true, SilverlinCatalog.containsGroup(hiddenGroupId))
        assertEquals(false, SilverlinCatalog.containsGroup("custom:1"))
    }

    private fun track(title: String) = TrackEntity(
        uri = "file:///$title.mp3",
        rootUri = "file:///music",
        title = title,
        artist = "银临",
        album = "",
        durationMs = 0,
        mimeType = "audio/mpeg",
        lyricUri = null,
    )
}
