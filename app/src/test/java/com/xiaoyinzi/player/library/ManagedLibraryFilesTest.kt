package com.xiaoyinzi.player.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ManagedLibraryFilesTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun deletesAudioLyricsAndEmptyAlbumDirectory() {
        val root = temporaryFolder.newFolder("xiaozi")
        val album = root.resolve("album").apply { mkdirs() }
        val audio = album.resolve("song.mp3").apply { writeText("audio") }
        val lyric = album.resolve("song.lrc").apply { writeText("lyric") }

        val deletedCount = deleteManagedTrackFiles(root, audio, lyric)

        assertEquals(2, deletedCount)
        assertFalse(audio.exists())
        assertFalse(lyric.exists())
        assertFalse(album.exists())
        assertTrue(root.isDirectory)
    }

    @Test
    fun refusesToDeleteFilesOutsideManagedDirectory() {
        val root = temporaryFolder.newFolder("xiaozi")
        val outside = temporaryFolder.newFile("outside.mp3")

        assertThrows(IllegalArgumentException::class.java) {
            deleteManagedTrackFiles(root, outside, lyricFile = null)
        }
        assertTrue(outside.isFile)
    }
}
