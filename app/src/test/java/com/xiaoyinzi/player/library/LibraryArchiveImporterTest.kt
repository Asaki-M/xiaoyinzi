package com.xiaoyinzi.player.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class LibraryArchiveImporterTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun importsMp3AndLyricsWhileIgnoringOtherFiles() {
        val destination = temporaryFolder.newFolder("xiaozi")
        val archive = zipOf(
            "album/song.mp3" to "audio",
            "album/song.lrc" to "[00:01]歌词",
            "cover.jpg" to "image",
        )

        val result = extractSupportedFiles(ByteArrayInputStream(archive), destination)

        assertEquals(ArchiveImportResult(audioCount = 1, lyricCount = 1), result)
        assertTrue(destination.resolve("album/song.mp3").isFile)
        assertTrue(destination.resolve("album/song.lrc").isFile)
        assertFalse(destination.resolve("cover.jpg").exists())
    }

    @Test
    fun ignoresMacOsMetadataThatLooksLikeMp3Files() {
        val destination = temporaryFolder.newFolder("xiaozi")
        val archive = zipOf(
            "album/song.mp3" to "audio",
            "album/song.lrc" to "[00:01]歌词",
            "__MACOSX/album/._song.mp3" to "apple-double",
            "album/._song.mp3" to "apple-double",
        )

        val result = extractSupportedFiles(ByteArrayInputStream(archive), destination)

        assertEquals(ArchiveImportResult(audioCount = 1, lyricCount = 1), result)
        assertFalse(destination.resolve("__MACOSX/album/._song.mp3").exists())
        assertFalse(destination.resolve("album/._song.mp3").exists())
    }

    @Test
    fun rejectsEntriesOutsideManagedDirectory() {
        val destination = temporaryFolder.newFolder("xiaozi")
        val archive = zipOf("../escape.mp3" to "audio")

        assertThrows(IllegalArgumentException::class.java) {
            extractSupportedFiles(ByteArrayInputStream(archive), destination)
        }
        assertFalse(destination.parentFile!!.resolve("escape.mp3").exists())
    }

    private fun zipOf(vararg entries: Pair<String, String>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { archive ->
            entries.forEach { (name, content) ->
                archive.putNextEntry(ZipEntry(name))
                archive.write(content.toByteArray())
                archive.closeEntry()
            }
        }
        return output.toByteArray()
    }
}
