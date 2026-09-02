package com.xiaoyinzi.player.library

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.webkit.MimeTypeMap
import com.xiaoyinzi.player.data.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class LibraryScanner {
    private val audioExtensions = setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus")

    suspend fun scan(root: File): List<TrackEntity> = withContext(Dispatchers.IO) {
        if (!root.isDirectory) return@withContext emptyList()
        val rootUri = Uri.fromFile(root).toString()
        val files = root.walkTopDown()
            .filter { file ->
                file.isFile && !isIgnoredLibraryPath(file.relativeTo(root).invariantSeparatorsPath)
            }
            .map(::FoundFile)
            .toList()

        val lyricByStem = files
            .filter { isSupportedLyricExtension(it.file.extension) }
            .sortedBy { if (it.file.extension.equals("lrcx", ignoreCase = true)) 1 else 0 }
            .associateBy { it.matchKey() }

        files.asSequence()
            .filter { it.file.isAudio() }
            .map { found -> found.file.toTrack(rootUri, lyricByStem[found.matchKey()]?.file) }
            .toList()
    }

    private fun File.isAudio(): Boolean = extension.lowercase(Locale.ROOT) in audioExtensions

    private fun File.toTrack(rootUri: String, lyric: File?): TrackEntity {
        val fallbackTitle = nameWithoutExtension.ifBlank { "未知曲目" }
        val metadata = readMetadata(this)
        val extension = extension.lowercase(Locale.ROOT)
        return TrackEntity(
            uri = Uri.fromFile(this).toString(),
            rootUri = rootUri,
            title = metadata.title.ifBlank { fallbackTitle },
            artist = metadata.artist.ifBlank { "未知音乐人" },
            album = metadata.album,
            durationMs = metadata.durationMs,
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "audio/*",
            lyricUri = lyric?.let(Uri::fromFile)?.toString(),
        )
    }

    private fun readMetadata(file: File): Metadata {
        val retriever = MediaMetadataRetriever()
        return runCatching {
            retriever.setDataSource(file.absolutePath)
            Metadata(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).orEmpty(),
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).orEmpty(),
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).orEmpty(),
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0,
            )
        }.getOrDefault(Metadata()).also { retriever.release() }
    }

    private data class Metadata(
        val title: String = "",
        val artist: String = "",
        val album: String = "",
        val durationMs: Long = 0,
    )

    private data class FoundFile(val file: File) {
        val extension: String = file.extension

        fun matchKey(): String {
            val stem = file.nameWithoutExtension.lowercase(Locale.ROOT)
            return "${file.parentFile?.absolutePath}/$stem"
        }
    }
}

private val lyricExtensions = setOf("lrc", "lrcx")

internal fun isSupportedLyricExtension(extension: String): Boolean =
    extension.lowercase(Locale.ROOT) in lyricExtensions

internal fun isIgnoredLibraryPath(path: String): Boolean = path
    .replace('\\', '/')
    .split('/')
    .filter(String::isNotEmpty)
    .any { segment ->
        segment.equals("__MACOSX", ignoreCase = true) ||
            segment.equals(".DS_Store", ignoreCase = true) ||
            segment.startsWith("._")
    }
