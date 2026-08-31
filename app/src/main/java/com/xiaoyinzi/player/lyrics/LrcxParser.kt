package com.xiaoyinzi.player.lyrics

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LyricLine(val timeMs: Long, val text: String)

class LrcxParser(private val contentResolver: ContentResolver) {
    suspend fun read(uri: String?): List<LyricLine> = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext emptyList()
        runCatching {
            contentResolver.openInputStream(Uri.parse(uri))?.bufferedReader()?.use { reader ->
                parse(reader.readText())
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    fun parse(content: String): List<LyricLine> = parseLrcx(content)
}

private val timestamp = Regex("\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?]")
private val wordTimestamp = Regex("<(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?>")

internal fun parseLrcx(content: String): List<LyricLine> = buildList {
    content.lineSequence().forEach { rawLine ->
        val lineMatches = timestamp.findAll(rawLine).toList()
        val matches = lineMatches.ifEmpty { wordTimestamp.findAll(rawLine).take(1).toList() }
        if (matches.isEmpty()) return@forEach
        val text = rawLine.replace(timestamp, "").replace(wordTimestamp, "").trim()
        matches.forEach { match ->
            val minute = match.groupValues[1].toLongOrNull() ?: 0
            val second = match.groupValues[2].toLongOrNull() ?: 0
            val fractionText = match.groupValues[3]
            val fractionMs = when (fractionText.length) {
                1 -> fractionText.toLongOrNull()?.times(100) ?: 0
                2 -> fractionText.toLongOrNull()?.times(10) ?: 0
                3 -> fractionText.toLongOrNull() ?: 0
                else -> 0
            }
            add(LyricLine((minute * 60 + second) * 1_000 + fractionMs, text))
        }
    }
}.sortedBy(LyricLine::timeMs)
