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
            listOf("腐草为萤", "蚍蜉渡海", "琉璃", "离地十公分·B面", "山色有无中", "粼粼"),
            SilverlinCatalog.albums.map(PresetAlbum::title),
        )
        assertEquals(54, SilverlinCatalog.albums.sumOf { it.tracks.size })
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
