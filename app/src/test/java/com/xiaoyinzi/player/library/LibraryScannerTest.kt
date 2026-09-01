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
}
