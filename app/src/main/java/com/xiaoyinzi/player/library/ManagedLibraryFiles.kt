package com.xiaoyinzi.player.library

import android.net.Uri
import com.xiaoyinzi.player.data.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ManagedLibraryFiles(private val root: File) {
    suspend fun deleteTrack(track: TrackEntity): Int = withContext(Dispatchers.IO) {
        deleteManagedTrackFiles(
            root = root,
            audioFile = track.uri.toManagedFile(),
            lyricFile = track.lyricUri?.toManagedFile(),
        )
    }

    private fun String.toManagedFile(): File {
        val uri = Uri.parse(this)
        require(uri.scheme == "file" && uri.path != null) { "只能删除应用音乐目录中的文件" }
        return File(requireNotNull(uri.path))
    }
}

internal fun deleteManagedTrackFiles(root: File, audioFile: File, lyricFile: File?): Int {
    val canonicalRoot = root.canonicalFile
    val rootPrefix = canonicalRoot.path + File.separator
    val files = listOfNotNull(audioFile, lyricFile)
        .map(File::getCanonicalFile)
        .distinctBy(File::getPath)

    files.forEach { file ->
        require(file.path.startsWith(rootPrefix)) { "不能删除应用音乐目录之外的文件" }
    }

    var deletedCount = 0
    files.forEach { file ->
        if (file.exists()) {
            check(file.isFile && file.delete()) { "无法删除文件：${file.name}" }
            deletedCount += 1
        }
    }
    files.mapNotNull(File::getParentFile).distinct().forEach { directory ->
        deleteEmptyParents(directory, canonicalRoot)
    }
    return deletedCount
}

private fun deleteEmptyParents(start: File, root: File) {
    var directory = start
    while (directory != root && directory.path.startsWith(root.path + File.separator)) {
        if (directory.list()?.isNotEmpty() != false || !directory.delete()) return
        directory = directory.parentFile ?: return
    }
}
