package com.xiaoyinzi.player.data

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
enum class ThemeImageKind {
    NONE,
    PORTRAIT,
    MOUNTAIN,
    STAGE,
    LOCAL,
}

@Serializable
data class ThemeImage(
    val kind: ThemeImageKind = ThemeImageKind.NONE,
    val fileName: String? = null,
)

enum class ThemeBackgroundSlot(val title: String) {
    HEADER("首页顶部"),
    PLAYER("播放 / 歌词"),
    SIDEBAR("侧栏"),
    OVERLAY("弹窗顶部"),
}

@Serializable
data class AppThemeDefinition(
    val id: String,
    val name: String,
    val accent: Long = 0xFFB98A43,
    val background: Long = 0xFFF5F1E7,
    val panel: Long = 0xFFDDE7DF,
    val headerImage: ThemeImage = ThemeImage(ThemeImageKind.PORTRAIT),
    val playerImage: ThemeImage = ThemeImage(ThemeImageKind.PORTRAIT),
    val sidebarImage: ThemeImage = ThemeImage(ThemeImageKind.STAGE),
    val overlayImage: ThemeImage = ThemeImage(ThemeImageKind.PORTRAIT),
    val imageMask: Float = .8f,
    val imageBlur: Float = 0f,
    val imageFocus: Float = .5f,
) {
    val isPreset: Boolean
        get() = BuiltInThemes.themes.any { it.id == id }

    fun image(slot: ThemeBackgroundSlot): ThemeImage =
        when (slot) {
            ThemeBackgroundSlot.HEADER -> headerImage
            ThemeBackgroundSlot.PLAYER -> playerImage
            ThemeBackgroundSlot.SIDEBAR -> sidebarImage
            ThemeBackgroundSlot.OVERLAY -> overlayImage
        }

    fun withImage(slot: ThemeBackgroundSlot, image: ThemeImage): AppThemeDefinition =
        when (slot) {
            ThemeBackgroundSlot.HEADER -> copy(headerImage = image)
            ThemeBackgroundSlot.PLAYER -> copy(playerImage = image)
            ThemeBackgroundSlot.SIDEBAR -> copy(sidebarImage = image)
            ThemeBackgroundSlot.OVERLAY -> copy(overlayImage = image)
        }

    fun newCopy(name: String = "我的${this.name.substringAfter(" · ")}") =
        copy(
            id = "custom:${UUID.randomUUID()}",
            name = name.take(20),
        )

    internal fun normalized(): AppThemeDefinition =
        copy(
            name = name.trim().take(20),
            accent = accent and 0xFFFFFF or 0xFF000000,
            background = background and 0xFFFFFF or 0xFF000000,
            panel = panel and 0xFFFFFF or 0xFF000000,
            imageMask = imageMask.takeIf(Float::isFinite)?.coerceIn(.4f, 1f) ?: .8f,
            imageBlur = imageBlur.takeIf(Float::isFinite)?.coerceIn(0f, 12f) ?: 0f,
            imageFocus = imageFocus.takeIf(Float::isFinite)?.coerceIn(0f, 1f) ?: .5f,
        )
}

object BuiltInThemes {
    val default = AppThemeDefinition(id = "paper", name = "原初 · 暖金")
    val themes =
        listOf(
            default,
            AppThemeDefinition(
                id = "celadon",
                name = "山色 · 青瓷",
                accent = 0xFF457A68,
                background = 0xFFEAF1EA,
                panel = 0xFFD8E5DC,
                headerImage = ThemeImage(ThemeImageKind.MOUNTAIN),
                playerImage = ThemeImage(ThemeImageKind.MOUNTAIN),
                sidebarImage = ThemeImage(ThemeImageKind.MOUNTAIN),
                overlayImage = ThemeImage(ThemeImageKind.MOUNTAIN),
                imageMask = .82f,
            ),
            AppThemeDefinition(
                id = "night",
                name = "琉璃 · 夜色",
                accent = 0xFF8CC7DE,
                background = 0xFF101F2E,
                panel = 0xFF1D3447,
                headerImage = ThemeImage(ThemeImageKind.STAGE),
                playerImage = ThemeImage(ThemeImageKind.STAGE),
                sidebarImage = ThemeImage(ThemeImageKind.STAGE),
                overlayImage = ThemeImage(ThemeImageKind.STAGE),
                imageMask = .83f,
            ),
        )
}

@Serializable
data class ThemeSettings(
    val activeTheme: AppThemeDefinition = BuiltInThemes.default,
    val customThemes: List<AppThemeDefinition> = emptyList(),
)
