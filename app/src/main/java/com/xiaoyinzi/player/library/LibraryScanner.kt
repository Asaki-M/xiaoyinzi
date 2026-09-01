package com.xiaoyinzi.player.library

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.xiaoyinzi.player.data.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class LibraryScanner(private val context: Context) {
    private val audioExtensions = setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus")

    suspend fun scan(rootUri: Uri): List<TrackEntity> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, rootUri) ?: return@withContext emptyList()
        val files = mutableListOf<FoundFile>()
        collectFiles(root, files)

        val lyricByStem = files
            .filter { isSupportedLyricExtension(it.file.extension()) }
            .sortedBy { if (it.file.extension().equals("lrcx", ignoreCase = true)) 1 else 0 }
            .associateBy { it.matchKey() }

        files.asSequence()
            .filter { it.file.isAudio() }
            .map { found -> found.file.toTrack(rootUri, lyricByStem[found.matchKey()]?.file) }
            .toList()
    }

    private fun collectFiles(directory: DocumentFile, output: MutableList<FoundFile>) {
        directory.listFiles().forEach { file ->
            when {
                file.isDirectory -> collectFiles(file, output)
                file.isFile -> output += FoundFile(file, directory.uri.toString())
            }
        }
    }

    private fun DocumentFile.isAudio(): Boolean {
        val extension = extension().lowercase(Locale.ROOT)
        return type?.startsWith("audio/") == true || extension in audioExtensions
    }

    private fun DocumentFile.extension(): String = name?.substringAfterLast('.', "").orEmpty()

    private fun DocumentFile.stem(): String = name?.substringBeforeLast('.').orEmpty()

    private fun DocumentFile.toTrack(rootUri: Uri, lyric: DocumentFile?): TrackEntity {
        val fallbackTitle = stem().ifBlank { "未知曲目" }
        val metadata = readMetadata(context.contentResolver, uri)
        return TrackEntity(
            uri = uri.toString(),
            rootUri = rootUri.toString(),
            title = metadata.title.ifBlank { fallbackTitle },
            artist = metadata.artist.ifBlank { "未知音乐人" },
            album = metadata.album,
            durationMs = metadata.durationMs,
            mimeType = type ?: "audio/*",
            lyricUri = lyric?.uri?.toString(),
        )
    }

    private fun readMetadata(resolver: ContentResolver, uri: Uri): Metadata {
        val retriever = MediaMetadataRetriever()
        return runCatching {
            resolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                retriever.setDataSource(descriptor.fileDescriptor)
                Metadata(
                    title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).orEmpty(),
                    artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).orEmpty(),
                    album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).orEmpty(),
                    durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0,
                )
            } ?: Metadata()
        }.getOrDefault(Metadata()).also { retriever.release() }
    }

    private data class Metadata(
        val title: String = "",
        val artist: String = "",
        val album: String = "",
        val durationMs: Long = 0,
    )

    private data class FoundFile(val file: DocumentFile, val parentUri: String) {
        fun matchKey(): String {
            val stem = file.name?.substringBeforeLast('.').orEmpty().lowercase(Locale.ROOT)
            return "$parentUri/$stem"
        }
    }
}

private val lyricExtensions = setOf("lrc", "lrcx")

internal fun isSupportedLyricExtension(extension: String): Boolean =
    extension.lowercase(Locale.ROOT) in lyricExtensions
