package com.xiaoyinzi.player.library

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryScannerTest {
    @Test
    fun acceptsStandardAndExtendedLyricsFiles() {
        assertTrue(isSupportedLyricExtension("lrc"))
        assertTrue(isSupportedLyricExtension("LRCX"))
        assertFalse(isSupportedLyricExtension("txt"))
    }

    @Test
    fun ignoresMacOsMetadataPaths() {
        assertTrue(isIgnoredLibraryPath("__MACOSX/album/._song.mp3"))
        assertTrue(isIgnoredLibraryPath("album/._song.mp3"))
        assertTrue(isIgnoredLibraryPath("album/.DS_Store"))
        assertFalse(isIgnoredLibraryPath("album/song.mp3"))
    }
}
