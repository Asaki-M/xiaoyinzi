package com.xiaoyinzi.player.lyrics

import org.junit.Assert.assertEquals
import org.junit.Test

class LrcxParserTest {
    @Test
    fun parsesAndSortsCommonLrcTimestamps() {
        val content = """
            [00:12.50]第二句
            [00:02.125][00:04.25]第一句
            <00:20.00>逐<00:20.20>字
            [ar:Artist]
        """.trimIndent()

        assertEquals(
            listOf(
                LyricLine(2_125, "第一句"),
                LyricLine(4_250, "第一句"),
                LyricLine(12_500, "第二句"),
                LyricLine(20_000, "逐字"),
            ),
            parseLrcx(content),
        )
    }
}
