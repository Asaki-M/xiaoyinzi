package com.xiaoyinzi.player.data

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ThemeRepository(private val context: Context) {
    private val preferences = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val _settings = MutableStateFlow(readSettings())
    val settings = _settings.asStateFlow()

    private fun readSettings(): ThemeSettings = runCatching {
        val saved = preferences.getString("themes", null) ?: return ThemeSettings()
        val decoded = json.decodeFromString<ThemeSettings>(saved)
        ThemeSettings(
            activeTheme =
                decoded.activeTheme.normalized().takeIf { it.name.isNotBlank() }
                    ?: BuiltInThemes.default,
            customThemes =
                decoded.customThemes
                    .filter { !it.isPreset && it.name.isNotBlank() }
                    .distinctBy { it.id }
                    .map(AppThemeDefinition::normalized),
        )
    }
        .getOrDefault(ThemeSettings())

    suspend fun applyTheme(id: String) = change { state ->
        val theme =
            (BuiltInThemes.themes + state.customThemes).firstOrNull { it.id == id }
                ?: throw IllegalArgumentException("这套主题已不存在")
        state.copy(activeTheme = theme)
    }

    suspend fun saveTheme(theme: AppThemeDefinition, apply: Boolean) = change { state ->
        require(!theme.isPreset) { "请将预设另存为我的主题" }
        val normalized = theme.normalized()
        require(normalized.name.isNotBlank()) { "请填写主题名称" }
        val existing = state.customThemes.indexOfFirst { it.id == normalized.id }
        val themes =
            state.customThemes.toMutableList().apply {
                if (existing >= 0) set(existing, normalized) else add(normalized)
            }
        state.copy(
            customThemes = themes,
            activeTheme = if (apply) normalized else state.activeTheme,
        )
    }

    suspend fun renameTheme(id: String, name: String) = change { state ->
        val trimmed = name.trim().take(20)
        require(trimmed.isNotBlank()) { "请填写主题名称" }
        require(state.customThemes.any { it.id == id }) { "这套主题已不存在" }
        state.copy(
            customThemes =
                state.customThemes.map { if (it.id == id) it.copy(name = trimmed) else it },
            activeTheme =
                state.activeTheme.let { if (it.id == id) it.copy(name = trimmed) else it },
        )
    }

    suspend fun duplicateTheme(id: String) = change { state ->
        val theme =
            state.customThemes.firstOrNull { it.id == id }
                ?: throw IllegalArgumentException("这套主题已不存在")
        state.copy(customThemes = state.customThemes + theme.newCopy("${theme.name.take(16)} 副本"))
    }

    suspend fun deleteTheme(id: String) = change { state ->
        require(!BuiltInThemes.themes.any { it.id == id }) { "预设主题不能删除" }
        state.copy(
            customThemes = state.customThemes.filterNot { it.id == id },
            activeTheme =
                if (state.activeTheme.id == id) BuiltInThemes.default else state.activeTheme,
        )
    }

    @SuppressLint("UseKtx") // Keep the commit result so a failed write cannot be reported as saved.
    private suspend fun change(update: (ThemeSettings) -> ThemeSettings) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val next = update(_settings.value)
                if (!preferences.edit().putString("themes", json.encodeToString(next)).commit()) {
                    throw IOException("主题保存失败，请重试")
                }
                _settings.value = next
                removeUnusedImages(next)
            }
        }

    suspend fun cleanUnusedImages() =
        withContext(Dispatchers.IO) {
            mutex.withLock { removeUnusedImages(_settings.value) }
        }

    private fun removeUnusedImages(settings: ThemeSettings) {
        val used =
            (settings.customThemes + settings.activeTheme)
                .flatMap { theme ->
                    ThemeBackgroundSlot.entries.mapNotNull { theme.image(it).fileName }
                }
                .toSet()
        imageDirectory(context).listFiles()?.filter { it.name !in used }?.forEach(File::delete)
    }

    suspend fun importImage(uri: Uri): ThemeImage {
        var created: File? = null
        return try {
            withContext(Dispatchers.IO) {
                mutex.withLock {
                    val resolver = context.contentResolver
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    resolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it, null, bounds)
                    }
                    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "无法读取这张图片，请选择其他图片" }
                    var sample = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
                    val options = BitmapFactory.Options().apply { inSampleSize = sample }
                    val original =
                        resolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(it, null, options)
                        } ?: throw IOException("无法读取这张图片")
                    try {
                        val orientation = runCatching {
                            resolver.openInputStream(uri)?.use {
                                ExifInterface(it)
                                    .getAttributeInt(
                                        ExifInterface.TAG_ORIENTATION,
                                        ExifInterface.ORIENTATION_NORMAL,
                                    )
                            }
                        }
                            .getOrNull()
                        val matrix =
                            Matrix().apply {
                                when (orientation) {
                                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                                    ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                                    ExifInterface.ORIENTATION_TRANSPOSE -> {
                                        setRotate(90f)
                                        postScale(-1f, 1f)
                                    }
                                    ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                                    ExifInterface.ORIENTATION_TRANSVERSE -> {
                                        setRotate(-90f)
                                        postScale(-1f, 1f)
                                    }
                                    ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
                                }
                            }
                        val bitmap =
                            if (matrix.isIdentity) original
                            else
                                Bitmap.createBitmap(
                                    original,
                                    0,
                                    0,
                                    original.width,
                                    original.height,
                                    matrix,
                                    true,
                                )
                        try {
                            val directory = imageDirectory(context)
                            check(directory.isDirectory || directory.mkdirs()) { "无法创建主题图片目录" }
                            val hasAlpha = bitmap.hasAlpha()
                            val file =
                                File(
                                    directory,
                                    "${UUID.randomUUID()}.${if (hasAlpha) "png" else "jpg"}",
                                )
                            created = file
                            file.outputStream().use { output ->
                                check(
                                    bitmap.compress(
                                        if (hasAlpha) Bitmap.CompressFormat.PNG
                                        else Bitmap.CompressFormat.JPEG,
                                        88,
                                        output,
                                    )
                                ) {
                                    "图片保存失败"
                                }
                            }
                            currentCoroutineContext().ensureActive()
                            ThemeImage(ThemeImageKind.LOCAL, file.name)
                        } finally {
                            if (bitmap !== original) bitmap.recycle()
                        }
                    } finally {
                        original.recycle()
                    }
                }
            }
        } catch (error: Exception) {
            withContext(NonCancellable + Dispatchers.IO) { created?.delete() }
            throw error
        }
    }

    companion object {
        private fun imageDirectory(context: Context) = File(context.filesDir, "theme-backgrounds")

        fun imageFile(context: Context, image: ThemeImage): File? {
            val name = image.fileName ?: return null
            if (
                image.kind != ThemeImageKind.LOCAL ||
                    !name.matches(Regex("[a-fA-F0-9-]+\\.(jpg|png)"))
            )
                return null
            return File(imageDirectory(context), name).takeIf(File::isFile)
        }
    }
}
