package com.xiaoyinzi.player

import android.app.Application
import androidx.room.Room
import com.xiaoyinzi.player.casting.LyricsCastManager
import com.xiaoyinzi.player.library.LibraryRepository
import com.xiaoyinzi.player.library.LibraryScanner
import com.xiaoyinzi.player.lyrics.LrcxParser
import com.xiaoyinzi.player.data.AppDatabase

class XiaoYinZiApplication : Application() {
    val database by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "xiaoyinzi.db").build()
    }
    val libraryRepository by lazy { LibraryRepository(database) }
    val libraryScanner by lazy { LibraryScanner(this) }
    val lyricParser by lazy { LrcxParser(contentResolver) }
    val lyricsCastManager by lazy { LyricsCastManager(this) }
}
