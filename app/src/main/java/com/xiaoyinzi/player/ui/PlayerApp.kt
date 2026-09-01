package com.xiaoyinzi.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaoyinzi.player.R
import com.xiaoyinzi.player.LibraryUiState
import com.xiaoyinzi.player.MainViewModel
import com.xiaoyinzi.player.data.GroupSummary
import com.xiaoyinzi.player.data.TrackEntity
import com.xiaoyinzi.player.casting.CastConnectionStatus
import com.xiaoyinzi.player.casting.CastDevice
import com.xiaoyinzi.player.casting.CastUiState
import com.xiaoyinzi.player.lyrics.LyricLine
import com.xiaoyinzi.player.library.TrackArtworkLoader
import com.xiaoyinzi.player.playback.PlaybackMode
import com.xiaoyinzi.player.playback.PlayerUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerApp(viewModel: MainViewModel, onChooseFolder: () -> Unit) {
    val library by viewModel.uiState.collectAsStateWithLifecycle()
    val player by viewModel.playerState.collectAsStateWithLifecycle()
    val lyrics by viewModel.lyrics.collectAsStateWithLifecycle()
    val cast by viewModel.castState.collectAsStateWithLifecycle()
    var showCreateGroup by remember { mutableStateOf(false) }
    var showNowPlaying by remember { mutableStateOf(false) }
    var showCastPanel by remember { mutableStateOf(false) }
    val snackbarHost = remember { SnackbarHostState() }
    val artworkLoader = remember {
        TrackArtworkLoader(viewModel.getApplication())
    }

    LaunchedEffect(library.message) {
        library.message?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (player.currentTrackUri != null) {
                MiniPlayer(
                    state = player,
                    onOpen = { showNowPlaying = true },
                    onTogglePlay = viewModel.player::togglePlayPause,
                )
            }
        },
    ) { padding ->
        LibraryScreen(
            state = library,
            playingUri = player.currentTrackUri,
            artworkLoader = artworkLoader,
            modifier = Modifier.padding(padding),
            onChooseFolder = onChooseFolder,
            onRescan = viewModel::rescan,
            onCreateGroup = { showCreateGroup = true },
            onSelectGroup = viewModel::selectGroup,
            onDeleteGroup = viewModel::deleteSelectedGroup,
            onPlay = viewModel::play,
            onAddToGroup = viewModel::addTrackToGroup,
            onRemoveFromGroup = viewModel::removeTrackFromSelectedGroup,
            castConnected = cast.connectionStatus == CastConnectionStatus.CONNECTED,
            onOpenCast = {
                viewModel.startCastDiscovery()
                showCastPanel = true
            },
        )
    }

    if (showCreateGroup) {
        CreateGroupDialog(
            onDismiss = { showCreateGroup = false },
            onCreate = {
                viewModel.createGroup(it)
                showCreateGroup = false
            },
        )
    }

    if (showNowPlaying) {
        BackHandler { showNowPlaying = false }
        NowPlayingScreen(
            state = player,
            lyrics = lyrics,
            artworkLoader = artworkLoader,
            onClose = { showNowPlaying = false },
            onTogglePlay = viewModel.player::togglePlayPause,
            onPrevious = viewModel.player::seekPrevious,
            onNext = viewModel.player::seekNext,
            onSeek = viewModel.player::seekTo,
            onCyclePlaybackMode = viewModel.player::cyclePlaybackMode,
        )
    }

    if (showCastPanel) {
        ModalBottomSheet(
            onDismissRequest = {
                showCastPanel = false
                if (!cast.enabled) viewModel.stopCastDiscovery()
            },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            CastPanel(
                state = cast,
                onRefresh = viewModel::startCastDiscovery,
                onConnect = viewModel::connectCastDevice,
                onDisconnect = viewModel::disconnectCast,
                onPair = viewModel::submitCastPairingCode,
                onForgetPairing = viewModel::forgetCastPairing,
            )
        }
    }
}

@Composable
private fun LibraryScreen(
    state: LibraryUiState,
    playingUri: String?,
    artworkLoader: TrackArtworkLoader,
    modifier: Modifier,
    onChooseFolder: () -> Unit,
    onRescan: () -> Unit,
    onCreateGroup: () -> Unit,
    onSelectGroup: (Long?) -> Unit,
    onDeleteGroup: () -> Unit,
    onPlay: (TrackEntity) -> Unit,
    onAddToGroup: (String, Long) -> Unit,
    onRemoveFromGroup: (String) -> Unit,
    castConnected: Boolean,
    onOpenCast: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        LibraryHeader(
            state = state,
            onChooseFolder = onChooseFolder,
            onRescan = onRescan,
            castConnected = castConnected,
            onOpenCast = onOpenCast,
        )

        GroupSelector(
            groups = state.groups,
            selectedId = state.selectedGroupId,
            onSelect = onSelectGroup,
            onCreate = onCreateGroup,
            onDelete = onDeleteGroup,
        )

        if (state.tracks.isEmpty()) {
            EmptyLibrary(hasFolder = state.folderUri != null, onChooseFolder = onChooseFolder)
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${state.tracks.size} 首歌曲", style = MaterialTheme.typography.labelMedium)
                if (state.selectedGroupId != null) {
                    Text("歌曲菜单可移出分组", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TrackList(
                tracks = state.tracks,
                groups = state.groups,
                selectedGroupId = state.selectedGroupId,
                playingUri = playingUri,
                artworkLoader = artworkLoader,
                onPlay = onPlay,
                onAddToGroup = onAddToGroup,
                onRemoveFromGroup = onRemoveFromGroup,
            )
        }
    }
}

@Composable
private fun LibraryHeader(
    state: LibraryUiState,
    onChooseFolder: () -> Unit,
    onRescan: () -> Unit,
    castConnected: Boolean,
    onOpenCast: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(198.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.yinlin_portrait),
            contentDescription = "银临",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.CenterEnd,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to MaterialTheme.colorScheme.surface.copy(alpha = .98f),
                        .48f to MaterialTheme.colorScheme.surface.copy(alpha = .78f),
                        1f to MaterialTheme.colorScheme.surface.copy(alpha = .12f),
                    ),
                ),
        )
        HeaderFireflies(Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 22.dp, bottom = 22.dp),
        ) {
            Text("小银子", style = MaterialTheme.typography.displaySmall)
            Text(
                text = if (state.selectedGroupId == null) "银临 · 私人曲藏" else state.selectedGroupName.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp),
        ) {
            IconButton(
                onClick = onOpenCast,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = .78f)),
            ) {
                Icon(
                    if (castConnected) Icons.Rounded.CastConnected else Icons.Rounded.Cast,
                    contentDescription = "Mac 实时歌词",
                    tint = if (castConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.size(6.dp))
            IconButton(
                onClick = onChooseFolder,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = .78f)),
            ) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = "选择音乐目录")
            }
            Spacer(Modifier.size(6.dp))
            IconButton(
                onClick = onRescan,
                enabled = state.folderUri != null && !state.scanning,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = .78f)),
            ) {
                if (state.scanning) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.Refresh, contentDescription = "重新扫描")
                }
            }
        }
    }
}

@Composable
private fun HeaderFireflies(modifier: Modifier = Modifier) {
    val firefly = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "header fireflies")
    val glow by transition.animateFloat(
        initialValue = .22f,
        targetValue = .9f,
        animationSpec = infiniteRepeatable(tween(1_800), RepeatMode.Reverse),
        label = "firefly glow",
    )
    Canvas(modifier) {
        listOf(
            Offset(size.width * .54f, size.height * .28f),
            Offset(size.width * .68f, size.height * .68f),
            Offset(size.width * .42f, size.height * .54f),
        ).forEachIndexed { index, point ->
            drawCircle(firefly.copy(alpha = glow * (.55f + index * .18f)), 3.dp.toPx(), point)
            drawCircle(firefly.copy(alpha = glow * .13f), 10.dp.toPx(), point)
        }
    }
}

@Composable
private fun CastPanel(
    state: CastUiState,
    onRefresh: () -> Unit,
    onConnect: (CastDevice) -> Unit,
    onDisconnect: () -> Unit,
    onPair: (String) -> Unit,
    onForgetPairing: () -> Unit,
) {
    var pairingCode by remember(state.pairingRequired) { mutableStateOf("") }
    val statusColor by animateColorAsState(
        targetValue = when (state.connectionStatus) {
            CastConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primary
            CastConnectionStatus.ERROR -> MaterialTheme.colorScheme.error
            CastConnectionStatus.PAIRING -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "cast status",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Mac 实时歌词", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    castStatusText(state),
                    color = statusColor,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (
                state.discovering ||
                state.connectionStatus == CastConnectionStatus.CONNECTING ||
                state.connectionStatus == CastConnectionStatus.SEARCHING
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    if (state.connectionStatus == CastConnectionStatus.CONNECTED) Icons.Rounded.CastConnected else Icons.Rounded.Cast,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            "Mac 与手机连接同一局域网后，会在这里自动出现。歌词来自当前手机中的 .lrc / .lrcx 文件。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )

        if (state.pairingRequired) {
            Spacer(Modifier.height(20.dp))
            Text("首次配对", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = pairingCode,
                    onValueChange = { pairingCode = it.filter(Char::isDigit).take(6) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Mac 上的 6 位数字") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
                Spacer(Modifier.size(10.dp))
                Button(onClick = { onPair(pairingCode) }, enabled = pairingCode.length == 6) {
                    Text("配对")
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("发现的设备", style = MaterialTheme.typography.labelMedium)
            TextButton(onClick = onRefresh) { Text("重新搜索") }
        }

        if (state.devices.isEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Computer, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text("还没有发现 Mac", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "请先启动 Mac 菜单栏应用",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                if (state.enabled) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onDisconnect) { Text("停止连接") }
                }
            }
        } else {
            state.devices.forEach { device ->
                val selected = device.name == state.selectedDeviceName
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (selected && state.connectionStatus == CastConnectionStatus.CONNECTED) Icons.Rounded.CastConnected else Icons.Rounded.Computer,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(device.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${device.host}:${device.port}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    if (selected && state.enabled) {
                        TextButton(onClick = onDisconnect) { Text("断开") }
                    } else {
                        OutlinedButton(onClick = { onConnect(device) }) { Text("连接") }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }

        state.message?.let { currentMessage ->
            Spacer(Modifier.height(12.dp))
            Text(currentMessage, color = statusColor, style = MaterialTheme.typography.bodyMedium)
        }

        if (state.enabled) {
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onForgetPairing) { Text("清除配对并重新连接") }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            "仅向已连接的 Mac 发送歌曲名、播放位置和歌词，不发送音乐文件。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(Modifier.height(12.dp))
    }
}

private fun castStatusText(state: CastUiState): String = when (state.connectionStatus) {
    CastConnectionStatus.OFF -> "未开启"
    CastConnectionStatus.SEARCHING -> "正在寻找同一网络中的 Mac"
    CastConnectionStatus.CONNECTING -> "正在连接 ${state.selectedDeviceName.orEmpty()}"
    CastConnectionStatus.PAIRING -> "等待配对"
    CastConnectionStatus.CONNECTED -> "已连接 ${state.selectedDeviceName.orEmpty()}"
    CastConnectionStatus.ERROR -> "连接中断，正在自动重试"
}

@Composable
private fun GroupSelector(
    groups: List<GroupSummary>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onCreate: () -> Unit,
    onDelete: () -> Unit,
) {
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selectedId == null,
                onClick = { onSelect(null) },
                label = { Text("全部") },
                leadingIcon = { Icon(Icons.Rounded.LibraryMusic, null, Modifier.size(17.dp)) },
            )
        }
        items(groups, key = GroupSummary::id) { group ->
            FilterChip(
                selected = selectedId == group.id,
                onClick = { onSelect(group.id) },
                label = { Text("${group.name} · ${group.trackCount}") },
            )
        }
        item {
            OutlinedButton(onClick = onCreate, contentPadding = PaddingValues(horizontal = 12.dp)) {
                Icon(Icons.Rounded.Add, null, Modifier.size(17.dp))
                Spacer(Modifier.size(5.dp))
                Text("新分组")
            }
        }
        if (selectedId != null) {
            item {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "删除当前分组")
                }
            }
        }
    }
}

@Composable
private fun TrackList(
    tracks: List<TrackEntity>,
    groups: List<GroupSummary>,
    selectedGroupId: Long?,
    playingUri: String?,
    artworkLoader: TrackArtworkLoader,
    onPlay: (TrackEntity) -> Unit,
    onAddToGroup: (String, Long) -> Unit,
    onRemoveFromGroup: (String) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) {
        items(tracks, key = TrackEntity::uri) { track ->
            TrackRow(
                track = track,
                groups = groups,
                selectedGroupId = selectedGroupId,
                isPlaying = playingUri == track.uri,
                artworkLoader = artworkLoader,
                onPlay = { onPlay(track) },
                onAddToGroup = { onAddToGroup(track.uri, it) },
                onRemoveFromGroup = { onRemoveFromGroup(track.uri) },
            )
            HorizontalDivider(modifier = Modifier.padding(start = 76.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = .5f))
        }
    }
}

@Composable
private fun TrackRow(
    track: TrackEntity,
    groups: List<GroupSummary>,
    selectedGroupId: Long?,
    isPlaying: Boolean,
    artworkLoader: TrackArtworkLoader,
    onPlay: () -> Unit,
    onAddToGroup: (Long) -> Unit,
    onRemoveFromGroup: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val titleColor by animateColorAsState(
        if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        label = "track title",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrackArtwork(
            trackUri = track.uri,
            isPlaying = isPlaying,
            loader = artworkLoader,
            modifier = Modifier.size(44.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                track.title,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                trackSupportingText(track),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (track.lyricUri != null) {
                Text("歌词", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            }
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "歌曲操作")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                if (selectedGroupId != null) {
                    DropdownMenuItem(
                        text = { Text("移出当前分组") },
                        onClick = {
                            menuOpen = false
                            onRemoveFromGroup()
                        },
                    )
                }
                groups.filter { it.id != selectedGroupId }.forEach { group ->
                    DropdownMenuItem(
                        text = { Text("加入「${group.name}」") },
                        onClick = {
                            menuOpen = false
                            onAddToGroup(group.id)
                        },
                    )
                }
                if (groups.isEmpty()) {
                    DropdownMenuItem(text = { Text("请先创建分组") }, onClick = { menuOpen = false }, enabled = false)
                }
            }
        }
    }
}

@Composable
private fun EmptyLibrary(hasFolder: Boolean, onChooseFolder: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        FanCoverPair(modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(26.dp))
        Text(if (hasFolder) "此处暂时无曲" else "把喜欢的歌，藏在这里", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            if (hasFolder) "支持 MP3、M4A、FLAC、WAV、OGG 与 OPUS。"
            else "选择银临音乐所在目录，小银子会读取曲目与同名 .lrc / .lrcx 歌词。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onChooseFolder) {
            Icon(Icons.Rounded.FolderOpen, null)
            Spacer(Modifier.size(8.dp))
            Text(if (hasFolder) "换一个目录" else "选择音乐目录")
        }
    }
}

@Composable
private fun FanCoverPair(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(width = 214.dp, height = 146.dp)) {
        Image(
            painter = painterResource(R.drawable.yinlin_pifu),
            contentDescription = "《蚍蜉渡海》封面",
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(126.dp)
                .rotate(-7f)
                .shadow(8.dp, RoundedCornerShape(2.dp))
                .clip(RoundedCornerShape(2.dp)),
            contentScale = ContentScale.Crop,
        )
        Image(
            painter = painterResource(R.drawable.yinlin_fucao),
            contentDescription = "《腐草为萤》封面",
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(126.dp)
                .rotate(6f)
                .shadow(10.dp, RoundedCornerShape(2.dp))
                .clip(RoundedCornerShape(2.dp)),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun MiniPlayer(state: PlayerUiState, onOpen: () -> Unit, onTogglePlay: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .navigationBarsPadding()
            .clickable(onClick = onOpen),
    ) {
        val progress = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f
        Box(Modifier.fillMaxWidth().height(2.dp).background(MaterialTheme.colorScheme.outline)) {
            Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(2.dp).background(MaterialTheme.colorScheme.primary))
        }
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 10.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayingPulse(state.isPlaying)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(state.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                Text(state.artist, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            FilledIconButton(onClick = onTogglePlay) {
                AnimatedContent(state.isPlaying, label = "play pause") { playing ->
                    Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = if (playing) "暂停" else "播放")
                }
            }
        }
    }
}

@Composable
private fun PlayingPulse(isPlaying: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    val ink = MaterialTheme.colorScheme.onSurface
    val paper = MaterialTheme.colorScheme.surface
    val transition = rememberInfiniteTransition(label = "playing")
    val pulse by transition.animateFloat(
        initialValue = .45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "playing pulse",
    )
    Canvas(Modifier.size(36.dp).alpha(if (isPlaying) pulse else .45f)) {
        drawCircle(ink, radius = size.minDimension / 2)
        drawCircle(paper, radius = size.minDimension / 5)
        drawCircle(accent, radius = size.minDimension / 18)
    }
}

@Composable
private fun CreateGroupDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val trimmedName = name.trim()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = .14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.LibraryMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        title = { Text("新建歌曲分组", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                Text(
                    "给收藏的歌曲起一个容易找到的名字。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(20) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    label = { Text("分组名称") },
                    placeholder = { Text("例如：夜晚散步") },
                    supportingText = {
                        Text(
                            "${name.length}/20",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                        )
                    },
                    leadingIcon = { Icon(Icons.Rounded.Album, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { if (trimmedName.isNotEmpty()) onCreate(trimmedName) },
                    ),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(trimmedName) },
                enabled = trimmedName.isNotEmpty(),
            ) {
                Text("创建分组")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
    )
}

@Composable
private fun NowPlayingScreen(
    state: PlayerUiState,
    lyrics: List<LyricLine>,
    artworkLoader: TrackArtworkLoader,
    onClose: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onCyclePlaybackMode: () -> Unit,
) {
    var showLyrics by remember(state.currentTrackUri) { mutableStateOf(false) }
    val currentLine = lyrics.indexOfLast { it.timeMs <= state.positionMs }
    val lyricListState = rememberLazyListState()
    LaunchedEffect(showLyrics, currentLine) {
        if (showLyrics && currentLine >= 0) lyricListState.animateScrollToItem(currentLine, -120)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent()
                }
            }
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (showLyrics) "歌词" else "正在播放",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                IconButton(onClick = onClose) { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "收起") }
            }
            Spacer(Modifier.height(4.dp))
            AnimatedContent(
                targetState = showLyrics,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                transitionSpec = {
                    (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = .97f)) togetherWith
                        (fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 1.03f))
                },
                label = "唱片歌词切换",
            ) { lyricsVisible ->
                if (lyricsVisible) {
                    LyricsStage(
                        lyrics = lyrics,
                        currentLine = currentLine,
                        listState = lyricListState,
                        onShowVinyl = { showLyrics = false },
                    )
                } else {
                    VinylStage(
                        trackUri = state.currentTrackUri,
                        isPlaying = state.isPlaying,
                        artworkLoader = artworkLoader,
                        onShowLyrics = { showLyrics = true },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(state.title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(state.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Spacer(Modifier.height(12.dp))

            Slider(
                value = state.positionMs.toFloat().coerceAtMost(state.durationMs.coerceAtLeast(1).toFloat()),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..state.durationMs.coerceAtLeast(1).toFloat(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(state.positionMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatDuration(state.durationMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPrevious) { Icon(Icons.Rounded.SkipPrevious, "上一首", Modifier.size(34.dp)) }
                FilledIconButton(onClick = onTogglePlay, modifier = Modifier.size(62.dp), shape = CircleShape) {
                    Icon(if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (state.isPlaying) "暂停" else "播放", Modifier.size(34.dp))
                }
                IconButton(onClick = onNext) { Icon(Icons.Rounded.SkipNext, "下一首", Modifier.size(34.dp)) }
                PlaybackModeButton(mode = state.playbackMode, onClick = onCyclePlaybackMode)
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
    val interactionSource = remember { MutableInteractionSource() }
    Box(modifier = Modifier.fillMaxSize()) {
        VinylArtwork(
            trackUri = trackUri,
            isPlaying = isPlaying,
            artworkLoader = artworkLoader,
            modifier = Modifier
                .align(Alignment.Center)
                .clip(CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClickLabel = "显示歌词",
                    onClick = onShowLyrics,
                ),
        )
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
                modifier = Modifier
                    .weight(1f)
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
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClickLabel = "返回唱片",
                        onClick = onShowVinyl,
                    ),
                contentPadding = PaddingValues(vertical = 44.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(lyrics.size) { index ->
                    val line = lyrics[index]
                    Text(
                        text = line.text.ifBlank { "♪" },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = if (index == currentLine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (index == currentLine) FontWeight.Bold else FontWeight.Normal,
                        style = if (index == currentLine) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun VinylArtwork(
    trackUri: String?,
    isPlaying: Boolean,
    artworkLoader: TrackArtworkLoader,
    modifier: Modifier = Modifier,
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val paper = MaterialTheme.colorScheme.surface
    val celadon = MaterialTheme.colorScheme.surfaceVariant
    val firefly = MaterialTheme.colorScheme.primary
    val artwork = rememberTrackArtwork(trackUri, artworkLoader, 256.dp)
    val transition = rememberInfiniteTransition(label = "vinyl")
    val glow by transition.animateFloat(
        initialValue = .4f,
        targetValue = if (isPlaying) 1f else .4f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "vinyl glow",
    )
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18_000), RepeatMode.Restart),
        label = "vinyl rotation",
    )
    Box(
        modifier = modifier
            .size(218.dp)
            .rotate(if (isPlaying) rotation else 0f)
            .clip(CircleShape)
            .background(celadon),
    ) {
        if (artwork != null) {
            Image(
                bitmap = artwork.asImageBitmap(),
                contentDescription = "当前歌曲封面",
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Canvas(Modifier.matchParentSize()) {
                val radius = size.minDimension / 2
                drawCircle(celadon, radius)
                drawCircle(ink.copy(alpha = .22f), radius, style = Stroke(1.dp.toPx()))
                repeat(5) { ring ->
                    drawCircle(ink.copy(alpha = .2f), radius * (.48f + ring * .09f), style = Stroke(1.dp.toPx()))
                }
                drawCircle(paper, radius * .25f)
                drawCircle(firefly.copy(alpha = glow), radius * .1f)
            }
        }
        Canvas(Modifier.matchParentSize()) {
            val radius = size.minDimension / 2
            drawCircle(ink.copy(alpha = .24f), radius - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
            drawCircle(paper.copy(alpha = .9f), radius * .035f)
        }
    }
}

@Composable
private fun PlaybackModeButton(mode: PlaybackMode, onClick: () -> Unit) {
    val icon = when (mode) {
        PlaybackMode.SEQUENTIAL -> Icons.AutoMirrored.Rounded.QueueMusic
        PlaybackMode.REPEAT_ALL -> Icons.Rounded.Repeat
        PlaybackMode.REPEAT_ONE -> Icons.Rounded.RepeatOne
        PlaybackMode.SHUFFLE -> Icons.Rounded.Shuffle
    }
    val description = when (mode) {
        PlaybackMode.SEQUENTIAL -> "顺序播放，点击切换为列表循环"
        PlaybackMode.REPEAT_ALL -> "列表循环，点击切换为单曲循环"
        PlaybackMode.REPEAT_ONE -> "单曲循环，点击切换为随机播放"
        PlaybackMode.SHUFFLE -> "随机播放，点击切换为顺序播放"
    }
    IconButton(onClick = onClick) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (mode == PlaybackMode.SEQUENTIAL) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0) / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun trackSupportingText(track: TrackEntity): String = listOfNotNull(
    track.album.takeIf(String::isNotBlank),
    track.artist.takeIf {
        it.isNotBlank() && it != "银临" && it != "未知音乐人"
    },
    formatDuration(track.durationMs),
).joinToString("  ·  ")
