package com.xiaoyinzi.player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyinzi.player.R
import com.xiaoyinzi.player.data.BuiltInThemes
import com.xiaoyinzi.player.data.ThemeBackgroundSlot
import com.xiaoyinzi.player.library.TrackArtworkLoader
import com.xiaoyinzi.player.lyrics.LyricLine
import com.xiaoyinzi.player.playback.PlaybackMode
import com.xiaoyinzi.player.playback.PlayerUiState
import com.xiaoyinzi.player.playback.QueueItemUiState
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NowPlayingScreen(
    state: PlayerUiState,
    lyrics: List<LyricLine>,
    showLyrics: Boolean,
    artworkLoader: TrackArtworkLoader,
    onClose: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onShowLyricsChange: (Boolean) -> Unit,
    onCyclePlaybackMode: () -> Unit,
    onSelectQueueItem: (Int) -> Unit,
    onOpenThemes: () -> Unit,
    preview: Boolean = false,
) {
    var showQueue by remember { mutableStateOf(false) }
    var scrubbing by remember(state.currentTrackUri) { mutableStateOf<Float?>(null) }
    val currentLine = lyrics.indexOfLast { it.timeMs <= state.positionMs }
    val listState = key(state.currentTrackUri) { rememberLazyListState() }
    val theme = LocalAppTheme.current
    val accent = Color(theme.accent)
    val paper = MaterialTheme.colorScheme.background
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(
        Modifier.fillMaxSize().background(paper).pointerInput(Unit) {
            awaitPointerEventScope { while (true) awaitPointerEvent() }
        }
    ) {
        ThemeArtwork(
            ThemeBackgroundSlot.PLAYER,
            Modifier.matchParentSize().alpha(((1f - theme.imageMask) * 1.7f).coerceIn(0f, .9f)),
            blur = theme.imageBlur + 6f,
        )
        Box(
            Modifier.matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to paper.copy(alpha = .45f),
                        .55f to paper.copy(alpha = .7f),
                        1f to paper,
                    )
                )
        )
        val scroll = maxHeight / fontScale < 500.dp
        val insets = if (preview) Modifier else Modifier.statusBarsPadding().navigationBarsPadding()
        Column(
            Modifier.fillMaxSize()
                .then(insets)
                .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = if (preview) 14.dp else 22.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose, enabled = !preview) {
                    Icon(Icons.Rounded.KeyboardArrowDown, "收起播放页")
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "小银子 · 正在播放",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        state.album.ifBlank { "银临 · 私人曲藏" },
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onOpenThemes, enabled = !preview) {
                    Icon(Icons.Rounded.Palette, "切换主题")
                }
            }
            AnimatedContent(
                targetState = showLyrics,
                modifier =
                    (if (scroll) Modifier.height(230.dp) else Modifier.weight(1f)).fillMaxWidth(),
                transitionSpec = {
                    (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = .97f)) togetherWith
                        (fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 1.03f))
                },
                label = "点击切换唱片和歌词",
            ) { lyricsVisible ->
                if (lyricsVisible) {
                    LyricsStage(
                        lyrics,
                        currentLine,
                        listState,
                        onShowVinyl = { onShowLyricsChange(false) },
                    )
                } else {
                    VinylStage(
                        state.currentTrackUri,
                        state.isPlaying,
                        artworkLoader,
                        onShowLyrics = { onShowLyricsChange(true) },
                    )
                }
            }
            Spacer(Modifier.height(if (preview) 8.dp else 16.dp))
            Text(
                state.title,
                style =
                    MaterialTheme.typography.headlineSmall.copy(
                        fontSize = if (preview) 24.sp else 30.sp,
                        lineHeight = if (preview) 30.sp else 38.sp,
                    ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                playerSupportingText(state.album, state.artist),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            val duration = state.durationMs.coerceAtLeast(1).toFloat()
            Slider(
                value = (scrubbing ?: state.positionMs.toFloat()).coerceIn(0f, duration),
                onValueChange = { scrubbing = it },
                onValueChangeFinished = {
                    scrubbing?.let { onSeek(it.toLong()) }
                    scrubbing = null
                },
                valueRange = 0f..duration,
                enabled = preview || state.currentTrackUri != null,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    formatDuration(scrubbing?.toLong() ?: state.positionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    formatDuration(state.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(top = if (preview) 6.dp else 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlaybackModeButton(state.playbackMode, onCyclePlaybackMode)
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Rounded.SkipPrevious, "上一首", Modifier.size(30.dp))
                }
                FilledIconButton(
                    onClick = onTogglePlay,
                    modifier =
                        Modifier.size(if (preview) 56.dp else 68.dp)
                            .shadow(
                                10.dp,
                                CircleShape,
                                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = .2f),
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = .2f),
                            ),
                    colors =
                        IconButtonDefaults.filledIconButtonColors(
                                containerColor = accent,
                                contentColor = readableText(accent),
                        ),
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        if (state.isPlaying) "暂停" else "播放",
                        Modifier.size(32.dp),
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.SkipNext, "下一首", Modifier.size(30.dp))
                }
                IconButton(onClick = { showQueue = true }) {
                    Icon(Icons.AutoMirrored.Rounded.QueueMusic, "播放队列")
                }
            }
        }
    }
    if (showQueue) {
        YinlinBottomSheet(title = "接下来播放", onDismiss = { showQueue = false }) {
            PlaybackQueue(state.queue, state.currentQueueIndex, artworkLoader) { index ->
                onSelectQueueItem(state.queue[index].mediaItemIndex)
                showQueue = false
            }
        }
    }
}

@Composable
private fun VinylStage(
    trackUri: String?,
    isPlaying: Boolean,
    artworkLoader: TrackArtworkLoader,
    onShowLyrics: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val accent = Color(LocalAppTheme.current.accent)
    val paper = MaterialTheme.colorScheme.background
    val rotation = remember(trackUri) { Animatable(0f) }
    LaunchedEffect(trackUri, isPlaying) {
        if (isPlaying) {
            while (isActive) {
                rotation.animateTo(rotation.value + 360f, tween(18_000, easing = LinearEasing))
                rotation.snapTo(rotation.value % 360f)
            }
        }
    }
    val armAngle by animateFloatAsState(if (isPlaying) 0f else -20f, tween(420), label = "唱针抬起")
    val artwork = rememberTrackArtwork(trackUri, artworkLoader, 256.dp)
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClickLabel = "显示歌词",
                onClick = onShowLyrics,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val diameter = minOf(maxWidth - 42.dp, maxHeight - 40.dp, 292.dp).coerceAtLeast(50.dp)
        Box(Modifier.size(diameter + 36.dp)) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(
                    accent.copy(alpha = .14f),
                    size.minDimension * .5f,
                    style = Stroke(1.dp.toPx()),
                )
                drawCircle(
                    accent.copy(alpha = .07f),
                    size.minDimension * .46f,
                    style = Stroke(1.dp.toPx()),
                )
            }
            Box(
                Modifier.align(Alignment.Center)
                    .size(diameter)
                    .shadow(16.dp, CircleShape)
                    .rotate(rotation.value)
                    .clip(CircleShape)
                    .background(Color(0xFF172126)),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.matchParentSize()) {
                    val radius = size.minDimension / 2
                    repeat(38) { index ->
                        drawCircle(
                            Color(0xFF64716F).copy(alpha = .16f),
                            radius * (.67f + index * .008f),
                            style = Stroke(.6.dp.toPx()),
                        )
                    }
                    drawCircle(
                        Brush.sweepGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = .1f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.White.copy(alpha = .07f),
                                Color.Transparent,
                            )
                        ),
                        radius,
                    )
                }
                if (artwork != null) {
                    Image(
                        artwork.asImageBitmap(),
                        "当前歌曲封面",
                        Modifier.fillMaxSize(.67f).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Image(
                        painterResource(R.drawable.yinlin_portrait),
                        null,
                        Modifier.fillMaxSize(.67f).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                }
                Box(
                    Modifier.size(10.dp)
                        .background(paper, CircleShape)
                        .padding(3.dp)
                        .background(Color(0xFF172126), CircleShape)
                )
            }
            Canvas(Modifier.matchParentSize()) {
                val anchor = Offset(size.width * .92f, size.height * .1f)
                drawCircle(accent.copy(alpha = .18f), 17.dp.toPx(), anchor)
                drawCircle(accent, 12.dp.toPx(), anchor)
                drawCircle(paper, 7.dp.toPx(), anchor)
                rotate(armAngle, anchor) {
                    val path =
                        Path().apply {
                            moveTo(anchor.x, anchor.y)
                            lineTo(size.width * .84f, size.height * .43f)
                            quadraticTo(
                                size.width * .83f,
                                size.height * .46f,
                                size.width * .78f,
                                size.height * .47f,
                            )
                            lineTo(size.width * .72f, size.height * .49f)
                        }
                    drawPath(
                        path,
                        accent,
                        style = Stroke(6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                    drawLine(
                        accent,
                        Offset(size.width * .69f, size.height * .5f),
                        Offset(size.width * .76f, size.height * .48f),
                        strokeWidth = 10.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

private const val ACTIVE_LYRIC_POSITION = .46f

@Composable
private fun PlaybackQueue(
    queue: List<QueueItemUiState>,
    currentIndex: Int,
    artworkLoader: TrackArtworkLoader,
    onSelect: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(.68f).padding(top = 22.dp)) {
        Text(
            "${queue.size} 首歌曲",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 22.dp, top = 4.dp, bottom = 12.dp),
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 20.dp),
        ) {
            items(queue.size, key = { queue[it].uri }) { index ->
                val item = queue[index]
                val isCurrent = index == currentIndex
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clickable { onSelect(index) }
                            .padding(horizontal = 18.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier =
                            Modifier.size(width = 3.dp, height = 34.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (isCurrent) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                    )
                    Spacer(Modifier.size(9.dp))
                    TrackArtwork(
                        trackUri = item.uri,
                        loader = artworkLoader,
                        modifier = Modifier.size(46.dp),
                    )
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(
                            item.title,
                            color =
                                if (isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            playerSupportingText(item.album, item.artist),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (isCurrent) {
                        Text(
                            "当前",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsStage(
    lyrics: List<LyricLine>,
    currentLine: Int,
    listState: LazyListState,
    onShowVinyl: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = Modifier.fillMaxSize()) {
        if (lyrics.isEmpty()) {
            Box(
                modifier =
                    Modifier.weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClickLabel = "返回唱片",
                            onClick = onShowVinyl,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "没有找到同名 .lrc 或 .lrcx 歌词",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val focusPadding = maxHeight * ACTIVE_LYRIC_POSITION

                LaunchedEffect(listState, lyrics, currentLine, focusPadding) {
                    if (currentLine < 0) {
                        listState.scrollToItem(0)
                    } else {
                        // Content padding already positions the line near the viewport center.
                        listState.animateScrollToItem(currentLine)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier =
                        Modifier.fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClickLabel = "返回唱片",
                                onClick = onShowVinyl,
                            ),
                    contentPadding =
                        PaddingValues(
                            top = focusPadding,
                            bottom = maxHeight - focusPadding,
                        ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(lyrics.size) { index ->
                        val line = lyrics[index]
                        Text(
                            text = line.text.ifBlank { "♪" },
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start,
                            color =
                                if (index == currentLine) LocalAccentText.current
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight =
                                if (index == currentLine) FontWeight.Bold else FontWeight.Normal,
                            style =
                                if (index == currentLine) MaterialTheme.typography.headlineSmall
                                else MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaybackModeButton(mode: PlaybackMode, onClick: () -> Unit) {
    val icon =
        when (mode) {
            PlaybackMode.SEQUENTIAL -> Icons.AutoMirrored.Rounded.QueueMusic
            PlaybackMode.REPEAT_ALL -> Icons.Rounded.Repeat
            PlaybackMode.REPEAT_ONE -> Icons.Rounded.RepeatOne
            PlaybackMode.SHUFFLE -> Icons.Rounded.Shuffle
        }
    val description =
        when (mode) {
            PlaybackMode.SEQUENTIAL -> "顺序播放，点击切换为列表循环"
            PlaybackMode.REPEAT_ALL -> "列表循环，点击切换为单曲循环"
            PlaybackMode.REPEAT_ONE -> "单曲循环，点击切换为随机播放"
            PlaybackMode.SHUFFLE -> "随机播放，点击切换为顺序播放"
        }
    IconButton(onClick = onClick) {
        Icon(
            icon,
            contentDescription = description,
            tint =
                if (mode == PlaybackMode.SEQUENTIAL) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.primary
                },
        )
    }
}

@Preview(name = "暖金 · 唱片", widthDp = 390, heightDp = 844)
@Composable
private fun WarmPlayerPreview() = PlayerDesignPreview(night = false, lyrics = false)

@Preview(name = "夜色 · 唱片", widthDp = 390, heightDp = 844)
@Composable
private fun NightPlayerPreview() = PlayerDesignPreview(night = true, lyrics = false)

@Preview(name = "小屏大字 · 歌词", widthDp = 320, heightDp = 640, fontScale = 1.3f)
@Composable
private fun SmallLyricsPreview() = PlayerDesignPreview(night = true, lyrics = true)

@Composable
private fun PlayerDesignPreview(night: Boolean, lyrics: Boolean) {
    val context = LocalContext.current
    val loader = remember { TrackArtworkLoader(context) }
    XiaoYinZiTheme(if (night) BuiltInThemes.themes.last() else BuiltInThemes.default) {
        NowPlayingScreen(
            state =
                PlayerUiState(
                    title = "棠梨煎雪",
                    album = "腐草为萤",
                    artist = "银临",
                    positionMs = 91_000,
                    durationMs = 246_000,
                ),
            lyrics =
                listOf(
                    LyricLine(0, "歌词排版示例"),
                    LyricLine(90_000, "让旋律留在此刻"),
                    LyricLine(180_000, "把这一刻，留给歌声"),
                ),
            showLyrics = lyrics,
            artworkLoader = loader,
            onClose = {},
            onTogglePlay = {},
            onPrevious = {},
            onNext = {},
            onSeek = {},
            onShowLyricsChange = {},
            onCyclePlaybackMode = {},
            onSelectQueueItem = {},
            onOpenThemes = {},
            preview = true,
        )
    }
}
