package com.xiaoyinzi.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xiaoyinzi.player.R

@Composable
internal fun YinlinDialog(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    confirmEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val visible = remember { MutableTransitionState(false).apply { targetState = true } }
        AnimatedVisibility(
            visibleState = visible,
            enter = fadeIn(tween(180)) + scaleIn(tween(220), initialScale = .96f),
        ) {
            YinlinDialogContent(title, confirmLabel, onConfirm, onDismiss, destructive, confirmEnabled, content)
        }
    }
}

@Composable
private fun YinlinDialogContent(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean,
    confirmEnabled: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.padding(24.dp).widthIn(max = 400.dp).fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
    ) {
        Column {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                YinlinOverlayHeader(title = title)
                Column(
                    Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    content()
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) { Text("取消") }
                Button(
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        contentColor = if (destructive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.surface,
                    ),
                ) { Text(confirmLabel) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun YinlinBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    header: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .42f),
        dragHandle = null,
    ) {
        if (header != null) header() else YinlinOverlayHeader(title = title, onDismiss = onDismiss)
        content()
    }
}

@Composable
private fun YinlinOverlayHeader(title: String, onDismiss: (() -> Unit)? = null) {
    val paper = Color(0xFFF1E3DF)
    Box(Modifier.fillMaxWidth().heightIn(min = 88.dp).background(paper)) {
        Box(Modifier.matchParentSize()) {
            Image(
                painter = painterResource(R.drawable.yinlin_portrait),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxWidth(.55f).fillMaxHeight(),
            )
        }
        Box(
            Modifier.matchParentSize().background(
                Brush.horizontalGradient(0f to paper, .42f to paper, 1f to paper.copy(alpha = .55f)),
            ),
        )
        Column(
            Modifier.align(Alignment.CenterStart).fillMaxWidth()
                .padding(start = 24.dp, end = if (onDismiss != null) 64.dp else 24.dp, top = 18.dp, bottom = 18.dp),
        ) {
            Text(
                title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalDivider(Modifier.width(18.dp), color = MaterialTheme.colorScheme.primary)
                Text("银临 · 私人曲藏", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        if (onDismiss != null) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    .clip(CircleShape).background(MaterialTheme.colorScheme.surface.copy(alpha = .85f)),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "关闭", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Preview(name = "删除分组", widthDp = 400)
@Preview(name = "小屏大字", widthDp = 320, fontScale = 1.3f)
@Composable
private fun DeleteGroupDialogPreview() {
    XiaoYinZiTheme {
        YinlinDialogContent(
            title = "夜晚散步",
            confirmLabel = "删除分组",
            onConfirm = {},
            onDismiss = {},
            destructive = true,
            confirmEnabled = true,
        ) {
            Text("确定删除这个分组吗？设备中的歌曲会保留。", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
