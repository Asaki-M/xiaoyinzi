package com.xiaoyinzi.player.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.xiaoyinzi.player.R

internal data class GroupActionTarget(
    val id: String?,
    val name: String,
    val isPreset: Boolean = false,
    @DrawableRes val artworkRes: Int? = null,
)

@Composable
internal fun GroupActionsSheet(
    group: GroupActionTarget,
    isDefault: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit,
) {
    YinlinBottomSheet(
        title = group.name,
        onDismiss = onDismiss,
        header = { GroupSheetHeader(group, onDismiss) },
    ) {
        GroupActionsContent(group, isDefault, onCopy, onSetDefault, onDelete)
    }
}

@Composable
private fun GroupSheetHeader(group: GroupActionTarget, onDismiss: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp).size(32.dp, 4.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.outline.copy(alpha = .6f),
        ) {}
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val artworkRes = group.artworkRes
            if (artworkRes != null) {
                Image(
                    painter = painterResource(artworkRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                )
            } else {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.LibraryMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    when {
                        group.id == null -> "完整曲库"
                        group.isPreset -> "预设专辑"
                        else -> "自建分组"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    group.name,
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "关闭分组菜单",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        HorizontalDivider(Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun GroupActionsContent(
    group: GroupActionTarget,
    isDefault: Boolean,
    onCopy: () -> Unit,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 4.dp, bottom = 12.dp),
    ) {
        GroupActionRow(
            title = "复制分组",
            subtitle = if (group.isPreset) "复制已导入的歌曲" else "创建可单独编辑的副本",
            icon = Icons.Rounded.ContentCopy,
            accent = MaterialTheme.colorScheme.secondary,
            onClick = onCopy,
        )
        GroupActionRow(
            title = if (isDefault) "默认分组" else "设为默认分组",
            subtitle = "下次打开自动进入",
            icon = if (isDefault) Icons.Rounded.Star else Icons.Rounded.StarOutline,
            accent = MaterialTheme.colorScheme.primary,
            enabled = !isDefault,
            selected = isDefault,
            onClick = onSetDefault,
        )
        if (group.id != null) {
            HorizontalDivider(
                Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            GroupActionRow(
                title = "删除分组",
                subtitle = if (group.isPreset) "可在曲库菜单中恢复" else "歌曲文件仍会保留",
                icon = Icons.Rounded.DeleteOutline,
                accent = MaterialTheme.colorScheme.error,
                destructive = true,
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun GroupActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    enabled: Boolean = true,
    selected: Boolean = false,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 68.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = accent.copy(alpha = if (destructive) .06f else .1f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (destructive) accent else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = if (selected) Icons.Rounded.Check else Icons.Rounded.ChevronRight,
            contentDescription = if (selected) "已设为默认" else null,
            tint = if (selected) accent else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview(name = "预设专辑分组菜单", widthDp = 400, showBackground = true)
@Composable
private fun PresetGroupActionsPreview() = GroupActionsPreview(
    GroupActionTarget("preset:album:fu-cao-wei-ying", "腐草为萤", true, R.drawable.yinlin_fucao),
)

@Preview(name = "默认分组 · 小屏大字", widthDp = 320, fontScale = 1.3f, showBackground = true)
@Composable
private fun DefaultGroupActionsPreview() = GroupActionsPreview(
    GroupActionTarget("custom:1", "银临的晚安曲与夜晚散步歌单"),
    isDefault = true,
)

@Preview(name = "全部歌曲菜单", widthDp = 400, showBackground = true)
@Composable
private fun AllGroupActionsPreview() = GroupActionsPreview(GroupActionTarget(null, "全部"), isDefault = true)

@Composable
private fun GroupActionsPreview(group: GroupActionTarget, isDefault: Boolean = false) {
    XiaoYinZiTheme {
        Surface(shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
            Column {
                GroupSheetHeader(group, onDismiss = {})
                GroupActionsContent(group, isDefault, onCopy = {}, onSetDefault = {}, onDelete = {})
            }
        }
    }
}
