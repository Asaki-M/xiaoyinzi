package com.xiaoyinzi.player.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.xiaoyinzi.player.LibraryTrackUiState
import com.xiaoyinzi.player.data.GroupSummary
import com.xiaoyinzi.player.library.TrackArtworkLoader

@Composable
internal fun TrackActionsSheet(
    item: LibraryTrackUiState,
    groups: List<GroupSummary>,
    selectedGroupId: Long?,
    artworkLoader: TrackArtworkLoader,
    onDismiss: () -> Unit,
    onAddToGroup: (Long) -> Unit,
    onRemoveFromGroup: () -> Unit,
    onDelete: () -> Unit,
) {
    val availableGroups = groups.filter { it.id != selectedGroupId && it.id !in item.groupIds }
    YinlinBottomSheet(title = item.title, onDismiss = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(top = 22.dp, bottom = 16.dp),
        ) {
            Row(
                Modifier.padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TrackArtwork(item.track?.uri, artworkLoader, Modifier.size(44.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        listOfNotNull(item.track?.artist?.takeIf(String::isNotBlank), item.track?.album?.takeIf(String::isNotBlank))
                            .joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.outlineVariant)
            if (selectedGroupId != null) {
                TrackActionRow(
                    title = "移出当前分组",
                    subtitle = "保留歌曲文件",
                    icon = Icons.Rounded.RemoveCircleOutline,
                    onClick = {
                        onDismiss()
                        onRemoveFromGroup()
                    },
                )
                HorizontalDivider(Modifier.padding(horizontal = 24.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }
            Text(
                "加入分组",
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            if (availableGroups.isEmpty()) {
                Text(
                    if (groups.isEmpty()) "还没有分组，可在曲库中创建。" else "已加入全部分组",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            availableGroups.forEach { group ->
                TrackActionRow(
                    title = "加入「${group.name}」",
                    subtitle = "${group.trackCount} 首歌曲",
                    icon = Icons.Rounded.Add,
                    onClick = {
                        onDismiss()
                        onAddToGroup(group.id)
                    },
                )
            }
            HorizontalDivider(
                Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            TrackActionRow(
                title = "删除歌曲文件",
                subtitle = "从设备中永久删除",
                icon = Icons.Rounded.DeleteOutline,
                destructive = true,
                onClick = {
                    onDismiss()
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun TrackActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = color)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
