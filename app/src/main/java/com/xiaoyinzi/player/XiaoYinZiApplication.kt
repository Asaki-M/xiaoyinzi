package com.xiaoyinzi.player

import android.app.Application
import android.os.Environment
import androidx.room.Room
import com.xiaoyinzi.player.casting.LyricsCastManager
import com.xiaoyinzi.player.library.LibraryArchiveImporter
import com.xiaoyinzi.player.library.LibraryRepository
import com.xiaoyinzi.player.library.LibraryScanner
import com.xiaoyinzi.player.library.ManagedLibraryFiles
import com.xiaoyinzi.player.lyrics.LrcxParser
import com.xiaoyinzi.player.data.AppDatabase

class XiaoYinZiApplication : Application() {
    val managedMusicDirectory by lazy {
        (getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: filesDir)
            .resolve("xiaozi")
            .apply { check(isDirectory || mkdirs()) { "无法创建音乐目录：$absolutePath" } }
    }
    val database by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "xiaoyinzi.db").build()
    }
    val libraryRepository by lazy { LibraryRepository(database) }
    val libraryScanner by lazy { LibraryScanner() }
    val libraryArchiveImporter by lazy {
        LibraryArchiveImporter(contentResolver, managedMusicDirectory)
    }
    val managedLibraryFiles by lazy { ManagedLibraryFiles(managedMusicDirectory) }
    val lyricParser by lazy { LrcxParser(contentResolver) }
    val lyricsCastManager by lazy { LyricsCastManager(this) }
}
