package com.xiaoyinzi.player.library

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale
import java.util.zip.ZipInputStream

data class ArchiveImportResult(
    val audioCount: Int,
    val lyricCount: Int,
)

class LibraryArchiveImporter(
    private val contentResolver: ContentResolver,
    private val destination: File,
) {
    suspend fun importArchive(uri: Uri): ArchiveImportResult = withContext(Dispatchers.IO) {
        destination.ensureDirectory()
        contentResolver.openInputStream(uri)?.buffered()?.use { input ->
            extractSupportedFiles(input, destination)
        } ?: error("无法打开压缩包")
    }
}

internal fun extractSupportedFiles(input: InputStream, destination: File): ArchiveImportResult {
    destination.ensureDirectory()
    var entryCount = 0
    var totalBytes = 0L
    var audioCount = 0
    var lyricCount = 0
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

    ZipInputStream(input.buffered()).use { archive ->
        var entry = archive.nextEntry
        while (entry != null) {
            entryCount += 1
            require(entryCount <= MAX_ENTRY_COUNT) { "压缩包内文件数量过多" }

            if (!entry.isDirectory) {
                val extension = entry.name.substringAfterLast('.', "").lowercase(Locale.ROOT)
                val supported = !isIgnoredLibraryPath(entry.name) &&
                    (extension == "mp3" || isSupportedLyricExtension(extension))
                val target = if (supported) safeTarget(destination, entry.name) else null
                target?.parentFile?.ensureDirectory()
                val temporaryFile = target?.let {
                    File.createTempFile(".xiaozi-", ".import", it.parentFile)
                }
                var entryBytes = 0L

                try {
                    temporaryFile?.outputStream()?.buffered()?.use { output ->
                        while (true) {
                            val read = archive.read(buffer)
                            if (read < 0) break
                            entryBytes += read
                            totalBytes += read
                            enforceArchiveLimits(entryBytes, totalBytes)
                            output.write(buffer, 0, read)
                        }
                    } ?: run {
                        while (true) {
                            val read = archive.read(buffer)
                            if (read < 0) break
                            entryBytes += read
                            totalBytes += read
                            enforceArchiveLimits(entryBytes, totalBytes)
                        }
                    }

                    if (target != null && temporaryFile != null) {
                        replaceFile(temporaryFile, target)
                        if (extension == "mp3") audioCount += 1 else lyricCount += 1
                    }
                } finally {
                    temporaryFile?.delete()
                }
            }
            archive.closeEntry()
            entry = archive.nextEntry
        }
    }
    require(entryCount > 0) { "压缩包中没有可读取的文件" }
    require(audioCount + lyricCount > 0) { "压缩包中没有 MP3 或歌词文件" }
    return ArchiveImportResult(audioCount = audioCount, lyricCount = lyricCount)
}

private fun replaceFile(source: File, target: File) {
    try {
        Files.move(
            source.toPath(),
            target.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
        )
    } catch (_: AtomicMoveNotSupportedException) {
        Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}

private fun safeTarget(destination: File, entryName: String): File {
    require(entryName.isNotBlank()) { "压缩包包含无效文件名" }
    val rootPath = destination.canonicalFile.path + File.separator
    val target = File(destination, entryName).canonicalFile
    require(target.path.startsWith(rootPath)) { "压缩包包含不安全的文件路径" }
    return target
}

private fun enforceArchiveLimits(entryBytes: Long, totalBytes: Long) {
    require(entryBytes <= MAX_ENTRY_BYTES) { "压缩包内单个文件过大" }
    require(totalBytes <= MAX_TOTAL_BYTES) { "压缩包解压后体积过大" }
}

private fun File.ensureDirectory() {
    check(isDirectory || mkdirs()) { "无法创建音乐目录：$absolutePath" }
}

private const val MAX_ENTRY_COUNT = 5_000
private const val MAX_ENTRY_BYTES = 1024L * 1024 * 1024
private const val MAX_TOTAL_BYTES = 4L * 1024 * 1024 * 1024
