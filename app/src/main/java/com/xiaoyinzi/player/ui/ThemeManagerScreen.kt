package com.xiaoyinzi.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaoyinzi.player.data.AppThemeDefinition
import com.xiaoyinzi.player.data.BuiltInThemes
import com.xiaoyinzi.player.data.ThemeBackgroundSlot
import com.xiaoyinzi.player.data.ThemeRepository
import com.xiaoyinzi.player.library.TrackArtworkLoader
import com.xiaoyinzi.player.lyrics.LyricLine
import com.xiaoyinzi.player.playback.PlayerUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun ThemeManagerScreen(
    repository: ThemeRepository,
    artworkLoader: TrackArtworkLoader,
    onDismiss: () -> Unit,
) {
    val settings by repository.settings.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var previewId by rememberSaveable { mutableStateOf<String?>(null) }
    var draft by
        rememberSaveable(stateSaver = ThemeDraftSaver) { mutableStateOf<AppThemeDefinition?>(null) }
    var renameId by rememberSaveable { mutableStateOf<String?>(null) }
    var renameText by rememberSaveable { mutableStateOf("") }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val themes = BuiltInThemes.themes + settings.customThemes
    val preview = themes.firstOrNull { it.id == previewId }
    fun execute(onSuccess: () -> Unit = {}, action: suspend () -> Unit) {
        if (busy) return
        scope.launch {
            busy = true
            try {
                action()
                onSuccess()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                snackbar.showSnackbar(error.localizedMessage ?: "操作失败，请重试")
            } finally {
                busy = false
            }
        }
    }
    fun closeEditor() {
        draft = null
        scope.launch { repository.cleanUnusedImages() }
    }
    BackHandler(enabled = draft == null) {
        if (previewId != null) previewId = null else onDismiss()
    }
    Scaffold(
        modifier =
            Modifier.fillMaxSize().pointerInput(Unit) {
                awaitPointerEventScope { while (true) awaitPointerEvent() }
            },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val editing = draft
            if (editing != null) {
                ThemeEditorScreen(
                    draft = editing,
                    existing = settings.customThemes.any { it.id == editing.id },
                    repository = repository,
                    saving = busy,
                    onChange = { draft = it },
                    onSave = { apply ->
                        execute(
                            onSuccess = {
                                draft = null
                                previewId = null
                                tab = 1
                            }
                        ) {
                            repository.saveTheme(editing, apply)
                        }
                    },
                    onDismiss = ::closeEditor,
                    onError = { snackbar.showSnackbar(it) },
                )
            } else if (preview != null) {
                ThemePreviewScreen(
                    theme = preview,
                    applied = preview == settings.activeTheme,
                    busy = busy,
                    artworkLoader = artworkLoader,
                    onBack = { previewId = null },
                    onEdit = { draft = if (preview.isPreset) preview.newCopy() else preview },
                    onApply = { execute { repository.applyTheme(preview.id) } },
                )
            } else {
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    ThemePageHeader("外观与主题", onBack = onDismiss)
                    Column(
                        Modifier.weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        Text(
                            "当前主题",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        ThemeCurrentCard(settings.activeTheme) {
                            previewId = settings.activeTheme.id
                        }
                        ScrollableTabRow(
                            selectedTabIndex = tab,
                            containerColor = Color.Transparent,
                            edgePadding = 0.dp,
                        ) {
                            Tab(
                                selected = tab == 0,
                                onClick = { tab = 0 },
                                text = { Text("预设主题  ${BuiltInThemes.themes.size}") },
                            )
                            Tab(
                                selected = tab == 1,
                                onClick = { tab = 1 },
                                text = { Text("我的主题  ${settings.customThemes.size}") },
                            )
                        }
                        if (tab == 0) {
                            BuiltInThemes.themes.chunked(2).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    row.forEach { theme ->
                                        ThemeTile(
                                            theme,
                                            theme == settings.activeTheme,
                                            Modifier.weight(1f),
                                        ) {
                                            previewId = theme.id
                                        }
                                    }
                                    if (row.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        } else if (settings.customThemes.isEmpty()) {
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.Palette,
                                    null,
                                    Modifier.size(32.dp),
                                    tint = LocalAccentText.current,
                                )
                                Text("收藏你的配色与风景", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "从当前主题开始，做一点自己的调整。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            settings.customThemes.forEach { theme ->
                                var menuOpen by remember(theme.id) { mutableStateOf(false) }
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    XiaoYinZiTheme(theme) {
                                        ThemeArtwork(
                                            ThemeBackgroundSlot.HEADER,
                                            Modifier.size(64.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { previewId = theme.id },
                                            blur = 0f,
                                        )
                                    }
                                    Column(
                                        Modifier.weight(1f)
                                            .clickable { previewId = theme.id }
                                            .padding(vertical = 10.dp)
                                    ) {
                                        Text(
                                            theme.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            if (theme == settings.activeTheme) "✓ 正在使用"
                                            else "自定义主题",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Box {
                                        IconButton(onClick = { menuOpen = true }, enabled = !busy) {
                                            Icon(Icons.Rounded.MoreVert, "管理${theme.name}")
                                        }
                                        DropdownMenu(
                                            expanded = menuOpen,
                                            onDismissRequest = { menuOpen = false },
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("编辑主题") },
                                                onClick = {
                                                    menuOpen = false
                                                    draft = theme
                                                },
                                            )
                                            DropdownMenuItem(
                                                text = { Text("复制主题") },
                                                onClick = {
                                                    menuOpen = false
                                                    execute { repository.duplicateTheme(theme.id) }
                                                },
                                            )
                                            DropdownMenuItem(
                                                text = { Text("重命名") },
                                                onClick = {
                                                    menuOpen = false
                                                    renameId = theme.id
                                                    renameText = theme.name
                                                },
                                            )
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        "删除主题",
                                                        color = MaterialTheme.colorScheme.error,
                                                    )
                                                },
                                                onClick = {
                                                    menuOpen = false
                                                    deleteId = theme.id
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = { draft = settings.activeTheme.newCopy() },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Rounded.Add, null, Modifier.size(20.dp))
                            Column(
                                Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text("新建我的主题", style = MaterialTheme.typography.titleMedium)
                                Text("选择背景、搭配颜色，保存为一整套", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
        }
    }
    renameId?.let { id ->
        YinlinDialog(
            "重命名主题",
            "保存",
            onConfirm = {
                execute(onSuccess = { renameId = null }) { repository.renameTheme(id, renameText) }
            },
            onDismiss = { if (!busy) renameId = null },
            confirmEnabled = renameText.isNotBlank() && !busy,
        ) {
            OutlinedTextField(
                value = renameText,
                onValueChange = { renameText = it.take(20) },
                label = { Text("主题名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    deleteId?.let { id ->
        YinlinDialog(
            "删除这套主题？",
            "删除",
            destructive = true,
            onConfirm = { execute(onSuccess = { deleteId = null }) { repository.deleteTheme(id) } },
            onDismiss = { if (!busy) deleteId = null },
            confirmEnabled = !busy,
        ) {
            Text(if (settings.activeTheme.id == id) "删除后会切回默认的原初 · 暖金。" else "这套主题将从我的主题中移除。")
        }
    }
}

@Composable
internal fun ThemePageHeader(
    title: String,
    onBack: () -> Unit,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回") }
        Text(
            title,
            Modifier.weight(1f).padding(start = 4.dp),
            style = MaterialTheme.typography.titleLarge,
        )
        action?.invoke()
    }
}

@Composable
private fun ThemeCurrentCard(theme: AppThemeDefinition, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(Modifier.fillMaxWidth().height(160.dp)) {
            ThemeArtwork(ThemeBackgroundSlot.HEADER, Modifier.fillMaxSize())
            val paper = MaterialTheme.colorScheme.background
            Box(
                Modifier.fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(paper.copy(alpha = .98f), paper.copy(alpha = .1f))
                        )
                    )
            )
            Column(
                Modifier.align(Alignment.CenterStart).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Check,
                        null,
                        Modifier.size(14.dp),
                        tint = LocalAccentText.current,
                    )
                    Text(
                        " 正在使用",
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalAccentText.current,
                    )
                }
                Text(theme.name, style = MaterialTheme.typography.headlineSmall)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ThemePalette(theme)
            Text(
                "查看整套效果 →",
                style = MaterialTheme.typography.bodySmall,
                color = readableText(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
    }
}

@Composable
private fun ThemeTile(
    theme: AppThemeDefinition,
    applied: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        XiaoYinZiTheme(theme) {
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        if (applied) 2.dp else 1.dp,
                        if (applied) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(14.dp),
                    )
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Box(Modifier.fillMaxWidth().height(85.dp)) {
                    ThemeArtwork(ThemeBackgroundSlot.HEADER, Modifier.fillMaxSize(), blur = 0f)
                    Box(
                        Modifier.fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(MaterialTheme.colorScheme.background, Color.Transparent)
                                )
                            )
                    )
                    Text(
                        "小银子",
                        Modifier.align(Alignment.BottomStart).padding(12.dp),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (applied)
                        Icon(
                            Icons.Rounded.Check,
                            "正在使用",
                            Modifier.align(Alignment.TopEnd)
                                .padding(7.dp)
                                .size(18.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                }
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.fillMaxWidth(.8f)
                            .height(3.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Box(
                        Modifier.fillMaxWidth(.6f)
                            .height(3.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                }
            }
        }
        Text(theme.name, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun ThemePalette(theme: AppThemeDefinition) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(theme.background, theme.panel, theme.accent).forEach { value ->
            Box(
                Modifier.size(15.dp)
                    .background(Color(value), CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
        }
    }
}

@Composable
private fun ThemePreviewScreen(
    theme: AppThemeDefinition,
    applied: Boolean,
    busy: Boolean,
    artworkLoader: TrackArtworkLoader,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onApply: () -> Unit,
) {
    var scene by rememberSaveable(theme.id) { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        ThemePageHeader(
            "主题预览",
            onBack,
            action = {
                TextButton(onClick = onEdit, enabled = !busy) {
                    Text(if (theme.isPreset) "自定义" else "编辑")
                }
            },
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    theme.name,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                )
                ThemePalette(theme)
            }
            ScrollableTabRow(scene, edgePadding = 0.dp, containerColor = Color.Transparent) {
                listOf("曲库", "播放", "侧栏", "弹窗").forEachIndexed { index, title ->
                    Tab(scene == index, onClick = { scene = index }, text = { Text(title) })
                }
            }
            XiaoYinZiTheme(theme) {
                Box(
                    Modifier.fillMaxWidth()
                        .height(if (scene == 1) 550.dp else 330.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(18.dp),
                        )
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    if (scene == 1) {
                        var lyrics by remember(scene) { mutableStateOf(false) }
                        var playing by remember { mutableStateOf(false) }
                        NowPlayingScreen(
                            state =
                                PlayerUiState(
                                    title = "棠梨煎雪",
                                    album = "腐草为萤",
                                    artist = "银临",
                                    positionMs = 91_000,
                                    durationMs = 246_000,
                                    isPlaying = playing,
                                ),
                            lyrics =
                                listOf(
                                    LyricLine(0, "歌词排版示例"),
                                    LyricLine(90_000, "让旋律留在此刻"),
                                    LyricLine(180_000, "把这一刻，留给歌声"),
                                ),
                            showLyrics = lyrics,
                            artworkLoader = artworkLoader,
                            onClose = {},
                            onTogglePlay = { playing = !playing },
                            onPrevious = {},
                            onNext = {},
                            onSeek = {},
                            onShowLyricsChange = { lyrics = it },
                            onCyclePlaybackMode = {},
                            onSelectQueueItem = {},
                            preview = true,
                        )
                    } else {
                        ThemeSampleScene(scene)
                    }
                }
            }
            Text(
                if (applied) "当前正在使用这套主题" else "仅预览，点击「应用主题」后生效",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(onClick = onEdit, enabled = !busy, modifier = Modifier.weight(1f)) {
                Text(if (theme.isPreset) "以此自定义" else "编辑主题")
            }
            Button(
                onClick = onApply,
                enabled = !busy && !applied,
                modifier = Modifier.weight(1.2f),
            ) {
                Text(if (applied) "正在使用" else "应用主题")
            }
        }
    }
}

@Composable
private fun ThemeSampleScene(scene: Int) {
    val slot =
        when (scene) {
            2 -> ThemeBackgroundSlot.SIDEBAR
            3 -> ThemeBackgroundSlot.OVERLAY
            else -> ThemeBackgroundSlot.HEADER
        }
    if (scene == 3) {
        Box(
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = .2f))
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                ThemeDraftPreview(LocalAppTheme.current, slot)
                Text(
                    "为喜欢的歌，留一个位置。",
                    Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    } else {
        Box(Modifier.fillMaxSize()) {
            if (scene == 2) {
                ThemeArtwork(slot, Modifier.fillMaxSize())
                Box(
                    Modifier.fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.background.copy(
                                alpha = LocalAppTheme.current.imageMask
                            )
                        )
                )
            }
            Column {
                if (scene == 0) ThemeDraftPreview(LocalAppTheme.current, slot)
                else Spacer(Modifier.height(36.dp))
                (if (scene == 0) listOf("棠梨煎雪", "锦鲤抄", "不老梦")
                    else listOf("Mac 实时歌词", "导入音乐", "重新扫描", "外观与主题"))
                    .forEach { title ->
                        Text(
                            title,
                            Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
            }
        }
    }
}
