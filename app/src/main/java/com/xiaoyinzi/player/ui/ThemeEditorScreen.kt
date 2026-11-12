package com.xiaoyinzi.player.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xiaoyinzi.player.data.AppThemeDefinition
import com.xiaoyinzi.player.data.ThemeBackgroundSlot
import com.xiaoyinzi.player.data.ThemeImage
import com.xiaoyinzi.player.data.ThemeImageKind
import com.xiaoyinzi.player.data.ThemeRepository
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal val ThemeDraftSaver =
    Saver<AppThemeDefinition?, String>(
        save = { it?.let(Json::encodeToString).orEmpty() },
        restore = { value ->
            runCatching { Json.decodeFromString<AppThemeDefinition>(value) }.getOrNull()
        },
    )

@Composable
internal fun ThemeEditorScreen(
    draft: AppThemeDefinition,
    existing: Boolean,
    repository: ThemeRepository,
    saving: Boolean,
    onChange: (AppThemeDefinition) -> Unit,
    onSave: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onError: suspend (String) -> Unit,
) {
    var initial by
        rememberSaveable(draft.id, stateSaver = ThemeDraftSaver) {
            mutableStateOf<AppThemeDefinition?>(draft)
        }
    var tab by rememberSaveable(draft.id) { mutableIntStateOf(0) }
    var slotIndex by rememberSaveable(draft.id) { mutableIntStateOf(0) }
    var showDiscard by remember { mutableStateOf(false) }
    var colorField by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val slot = ThemeBackgroundSlot.entries[slotIndex]
    val busy = saving || importing
    val leave = {
        if (!busy) {
            if (initial != draft) showDiscard = true else onDismiss()
        }
    }
    BackHandler(onBack = leave)
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                val targetSlot = slot
                scope.launch {
                    importing = true
                    try {
                        val image = repository.importImage(uri)
                        onChange(draft.withImage(targetSlot, image))
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        onError(error.localizedMessage ?: "图片导入失败")
                    } finally {
                        importing = false
                    }
                }
            }
        }

    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        ThemePageHeader(
            title = if (existing) "编辑主题" else "自定义主题",
            onBack = leave,
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = draft.name,
                onValueChange = { onChange(draft.copy(name = it.take(20))) },
                label = { Text("主题名称") },
                singleLine = true,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            )
            ThemeDraftPreview(draft, slot)
            ScrollableTabRow(
                selectedTabIndex = tab,
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
            ) {
                listOf("配色", "背景", "效果").forEachIndexed { index, label ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
                }
            }
            when (tab) {
                0 -> {
                    ThemeColorRow("主题色", "按钮、进度条、当前歌词", draft.accent, !busy) {
                        colorField = "accent"
                    }
                    ThemeColorRow("页面底色", "曲库与播放页面的基础颜色", draft.background, !busy) {
                        colorField = "background"
                    }
                    ThemeColorRow("面板颜色", "分组、迷你播放器、输入框", draft.panel, !busy) {
                        colorField = "panel"
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("文字颜色", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "随底色自动调整深浅",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            Icons.Rounded.Check,
                            null,
                            tint = LocalAccentText.current,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            "自动适配",
                            style = MaterialTheme.typography.labelMedium,
                            color = LocalAccentText.current,
                        )
                    }
                }
                1 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ThemeBackgroundSlot.entries.chunked(2).forEach { slots ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                slots.forEach { item ->
                                    FilterChip(
                                        selected = item == slot,
                                        onClick = { slotIndex = item.ordinal },
                                        label = { Text(item.title) },
                                        enabled = !busy,
                                    )
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                                ThemeImageKind.NONE to "纯色",
                                ThemeImageKind.PORTRAIT to "原初",
                                ThemeImageKind.MOUNTAIN to "山色",
                                ThemeImageKind.STAGE to "演出",
                            )
                            .forEach { (kind, label) ->
                                val selected = draft.image(slot).kind == kind
                                Column(
                                    Modifier.weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            if (selected) 2.dp else 1.dp,
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant,
                                            RoundedCornerShape(10.dp),
                                        )
                                        .selectable(
                                            selected = selected,
                                            enabled = !busy,
                                            role = Role.RadioButton,
                                        ) {
                                            onChange(draft.withImage(slot, ThemeImage(kind)))
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    ThemeArtwork(
                                        slot,
                                        Modifier.fillMaxWidth().height(64.dp),
                                        image = ThemeImage(kind),
                                        blur = 0f,
                                    )
                                    Text(
                                        label,
                                        Modifier.padding(vertical = 7.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                    }
                    Text(
                        "可为每个位置单独选图，也可保留纯色。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = { picker.launch(arrayOf("image/*")) },
                        enabled = !busy,
                    ) {
                        if (importing)
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Rounded.AddPhotoAlternate, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(if (importing) "正在导入图片" else "从相册选择图片")
                    }
                }
                2 -> {
                    ThemeEffectSlider(
                        "背景遮罩",
                        "${(draft.imageMask * 100).roundToInt()}%",
                        draft.imageMask,
                        .4f..1f,
                        !busy,
                    ) {
                        onChange(draft.copy(imageMask = it))
                    }
                    Text(
                        "数值越高，背景越淡，文字越清楚。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ThemeEffectSlider(
                        "背景模糊",
                        "${draft.imageBlur.roundToInt()}",
                        draft.imageBlur,
                        0f..12f,
                        !busy,
                    ) {
                        onChange(draft.copy(imageBlur = it))
                    }
                    Text("图片焦点", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0f to "偏左", .5f to "居中", 1f to "偏右").forEach { (focus, label) ->
                            FilterChip(
                                selected = draft.imageFocus == focus,
                                onClick = { onChange(draft.copy(imageFocus = focus)) },
                                label = { Text(label) },
                                enabled = !busy,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = { onSave(false) },
                enabled = !busy && draft.name.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                Text("仅保存")
            }
            Button(
                onClick = { onSave(true) },
                enabled = !busy && draft.name.isNotBlank(),
                modifier = Modifier.weight(1.3f),
            ) {
                if (saving)
                    CircularProgressIndicator(
                        Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                else Text("保存并应用")
            }
        }
    }
    if (showDiscard) {
        YinlinDialog(
            "放弃这次调整？",
            "放弃修改",
            onConfirm = onDismiss,
            onDismiss = { showDiscard = false },
        ) {
            Text("这次修改还没有保存。")
        }
    }
    colorField?.let { field ->
        val value =
            when (field) {
                "accent" -> draft.accent
                "background" -> draft.background
                else -> draft.panel
            }
        ThemeColorDialog(value, onDismiss = { colorField = null }) { color ->
            onChange(
                when (field) {
                    "accent" -> draft.copy(accent = color)
                    "background" -> draft.copy(background = color)
                    else -> draft.copy(panel = color)
                }
            )
            colorField = null
        }
    }
}

@Composable
internal fun ThemeDraftPreview(theme: AppThemeDefinition, slot: ThemeBackgroundSlot) {
    XiaoYinZiTheme(theme) {
        val paper = MaterialTheme.colorScheme.background
        Box(
            Modifier.fillMaxWidth().height(182.dp).clip(RoundedCornerShape(16.dp)).background(paper)
        ) {
            ThemeArtwork(slot, Modifier.fillMaxSize())
            Box(
                Modifier.fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(paper.copy(alpha = .95f), paper.copy(alpha = theme.imageMask))
                        )
                    )
            )
            Column(
                Modifier.align(Alignment.CenterStart).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    if (slot == ThemeBackgroundSlot.PLAYER) "棠梨煎雪"
                    else if (slot == ThemeBackgroundSlot.OVERLAY) "新建分组" else "小银子",
                    style = MaterialTheme.typography.headlineSmall,
                    color =
                        if (slot == ThemeBackgroundSlot.PLAYER) LocalAccentText.current
                        else MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    "银临 · 私人曲藏",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                "${slot.title} · 实时预览",
                modifier =
                    Modifier.align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(paper.copy(alpha = .9f))
                        .padding(6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun ThemeColorRow(
    title: String,
    description: String,
    value: Long,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column {
        Row(
            Modifier.fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("#%06X".format(value and 0xFFFFFF), style = MaterialTheme.typography.labelSmall)
            Box(
                Modifier.size(30.dp)
                    .clip(CircleShape)
                    .background(Color(value))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun ThemeEffectSlider(
    title: String,
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, enabled = enabled)
    }
}

@Composable
private fun ThemeColorDialog(initial: Long, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    var hex by rememberSaveable(initial) { mutableStateOf("%06X".format(initial and 0xFFFFFF)) }
    val parsed = hex.takeIf { it.length == 6 }?.toLongOrNull(16)?.or(0xFF000000)
    YinlinDialog(
        "选择颜色",
        "确定",
        onConfirm = { parsed?.let(onConfirm) },
        onDismiss = onDismiss,
        confirmEnabled = parsed != null,
    ) {
        Box(
            Modifier.fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(parsed ?: initial))
        )
        OutlinedTextField(
            value = hex,
            onValueChange = {
                hex =
                    it.removePrefix("#")
                        .filter { char -> char in "0123456789abcdefABCDEF" }
                        .take(6)
                        .uppercase()
            },
            label = { Text("十六进制颜色") },
            prefix = { Text("#") },
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii,
                ),
            modifier = Modifier.fillMaxWidth(),
        )
        listOf("红" to 16, "绿" to 8, "蓝" to 0).forEach { (label, shift) ->
            val color = parsed ?: initial
            val channel = ((color shr shift) and 0xFF).toFloat()
            ThemeEffectSlider(label, channel.roundToInt().toString(), channel, 0f..255f, true) {
                selected ->
                val next =
                    (color and (0xFFL shl shift).inv()) or
                        (selected.roundToInt().toLong() shl shift)
                hex = "%06X".format(next and 0xFFFFFF)
            }
        }
    }
}
