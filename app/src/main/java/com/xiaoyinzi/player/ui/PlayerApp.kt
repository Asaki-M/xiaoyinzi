package com.xiaoyinzi.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Restore
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.xiaoyinzi.player.LibraryGroupUiState
import com.xiaoyinzi.player.LibraryTrackUiState
import com.xiaoyinzi.player.MainViewModel
import com.xiaoyinzi.player.data.GroupSummary
import com.xiaoyinzi.player.data.TrackEntity
import com.xiaoyinzi.player.casting.CastConnectionStatus
import com.xiaoyinzi.player.casting.CastUiState
import com.xiaoyinzi.player.lyrics.LyricLine
import com.xiaoyinzi.player.library.TrackArtworkLoader
import com.xiaoyinzi.player.playback.PlaybackMode
import com.xiaoyinzi.player.playback.PlayerUiState
import com.xiaoyinzi.player.playback.QueueItemUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerApp(viewModel: MainViewModel, onImportArchive: () -> Unit) {
    val library by viewModel.uiState.collectAsStateWithLifecycle()
    val player by viewModel.playerState.collectAsStateWithLifecycle()
    val lyrics by viewModel.lyrics.collectAsStateWithLifecycle()
    val cast by viewModel.castState.collectAsStateWithLifecycle()
    var showCreateGroup by remember { mutableStateOf(false) }
    var showNowPlaying by remember { mutableStateOf(false) }
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    var showCastPanel by remember { mutableStateOf(false) }
    var showLibraryActions by remember { mutableStateOf(false) }
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHost) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (player.currentTrackUri != null) {
                    MiniPlayer(
                        state = player,
                        artworkLoader = artworkLoader,
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
                onImportArchive = onImportArchive,
                onCreateGroup = { showCreateGroup = true },
                onSelectGroup = viewModel::selectGroup,
                onDeleteGroup = viewModel::deleteSelectedGroup,
                onHidePresetGroup = viewModel::hidePresetGroup,
                onPlay = viewModel::play,
                onAddToGroup = viewModel::addTrackToGroup,
                onRemoveFromGroup = viewModel::removeTrackFromSelectedGroup,
                onDeleteTrack = viewModel::deleteTrack,
                onOpenActions = { showLibraryActions = true },
            )
        }

        if (showLibraryActions) {
            BackHandler { showLibraryActions = false }
        }
        AnimatedVisibility(
            visible = showLibraryActions,
            modifier = Modifier.matchParentSize(),
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(140)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = .38f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showLibraryActions = false },
                    ),
            )
        }
        AnimatedVisibility(
            visible = showLibraryActions,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally(tween(260)) { it },
            exit = slideOutHorizontally(tween(220)) { it },
        ) {
            LibraryActionsSidebar(
                scanning = library.scanning,
                castConnected = cast.connectionStatus == CastConnectionStatus.CONNECTED,
                hiddenPresetGroupCount = library.hiddenPresetGroupCount,
                onClose = { showLibraryActions = false },
                onOpenCast = {
                    showLibraryActions = false
                    showCastPanel = true
                },
                onImportArchive = {
                    showLibraryActions = false
                    onImportArchive()
                },
                onRescan = {
                    showLibraryActions = false
                    viewModel.rescan()
                },
                onRestorePresetGroups = {
                    showLibraryActions = false
                    viewModel.restorePresetGroups()
                },
            )
        }
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
            showLyrics = showLyrics,
            artworkLoader = artworkLoader,
            onClose = { showNowPlaying = false },
            onTogglePlay = viewModel.player::togglePlayPause,
            onPrevious = viewModel.player::seekPrevious,
            onNext = viewModel.player::seekNext,
            onSeek = viewModel.player::seekTo,
            onShowLyricsChange = { showLyrics = it },
            onCyclePlaybackMode = viewModel.player::cyclePlaybackMode,
            onSelectQueueItem = viewModel.player::playQueueItem,
        )
    }

    if (showCastPanel) {
        ModalBottomSheet(
            onDismissRequest = {
                showCastPanel = false
            },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            CastPanel(
                state = cast,
                onConnectManually = viewModel::connectCastManually,
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
    onImportArchive: () -> Unit,
    onCreateGroup: () -> Unit,
    onSelectGroup: (String?) -> Unit,
    onDeleteGroup: () -> Unit,
    onHidePresetGroup: (String) -> Unit,
    onPlay: (TrackEntity) -> Unit,
    onAddToGroup: (String, Long) -> Unit,
    onRemoveFromGroup: (String) -> Unit,
    onDeleteTrack: (TrackEntity) -> Unit,
    onOpenActions: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        LibraryHeader(
            state = state,
            onOpenActions = onOpenActions,
        )

        GroupSelector(
            groups = state.groups,
            selectedId = state.selectedGroupId,
            onSelect = onSelectGroup,
            onCreate = onCreateGroup,
            onDelete = onDeleteGroup,
            onHidePreset = onHidePresetGroup,
        )

        if (state.tracks.isEmpty()) {
            EmptyLibrary(
                importing = state.scanning,
                onImportArchive = onImportArchive,
            )
        } else {
            if (state.selectedCustomGroupId != null) {
                Text(
                    "歌曲菜单可移出分组",
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Spacer(Modifier.height(8.dp))
            }
            TrackList(
                tracks = state.tracks,
                groups = state.customGroups,
                selectedGroupId = state.selectedCustomGroupId,
                playingUri = playingUri,
                artworkLoader = artworkLoader,
                onPlay = onPlay,
                onAddToGroup = onAddToGroup,
                onRemoveFromGroup = onRemoveFromGroup,
                onDeleteTrack = onDeleteTrack,
            )
        }
    }
}

@Composable
private fun LibraryHeader(
    state: LibraryUiState,
    onOpenActions: () -> Unit,
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

        IconButton(
            onClick = onOpenActions,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = .78f)),
        ) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "打开功能侧边栏")
        }
    }
}

@Composable
private fun LibraryActionsSidebar(
    scanning: Boolean,
    castConnected: Boolean,
    hiddenPresetGroupCount: Int,
    onClose: () -> Unit,
    onOpenCast: () -> Unit,
    onImportArchive: () -> Unit,
    onRescan: () -> Unit,
    onRestorePresetGroups: () -> Unit,
) {
    val panelShape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)
    Box(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .shadow(18.dp, panelShape)
            .clip(panelShape),
    ) {
        LibraryActionsSidebarBackdrop(Modifier.matchParentSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 14.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 10.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭功能侧边栏")
                }
            }
            SidebarAction(
                icon = if (castConnected) Icons.Rounded.CastConnected else Icons.Rounded.Cast,
                title = "Mac 实时歌词",
                supportingText = if (castConnected) "已连接" else "输入地址连接同一网络中的 Mac",
                accent = castConnected,
                onClick = onOpenCast,
            )
            SidebarAction(
                icon = Icons.Rounded.Archive,
                title = "导入音乐",
                supportingText = "导入包含 MP3 和歌词的 ZIP 压缩包",
                enabled = !scanning,
                onClick = onImportArchive,
            )
            SidebarAction(
                icon = Icons.Rounded.Refresh,
                title = "重新扫描",
                supportingText = if (scanning) "正在扫描音乐目录" else "重新读取本地音乐文件",
                enabled = !scanning,
                onClick = onRescan,
            )
            if (hiddenPresetGroupCount > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                SidebarAction(
                    icon = Icons.Rounded.Restore,
                    title = "恢复预设分组",
                    supportingText = "找回已删除的 $hiddenPresetGroupCount 个专辑分组",
                    onClick = onRestorePresetGroups,
                )
            }
        }
    }
}

@Composable
private fun LibraryActionsSidebarBackdrop(modifier: Modifier = Modifier) {
    val surface = MaterialTheme.colorScheme.surface
    Box(modifier = modifier.background(surface)) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(.95f),
        ) {
            Image(
                painter = painterResource(R.drawable.yinlin_sidebar),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                alignment = BiasAlignment(horizontalBias = .65f, verticalBias = 1f),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to surface,
                            .12f to surface.copy(alpha = .96f),
                            .32f to surface.copy(alpha = .78f),
                            .55f to surface.copy(alpha = .30f),
                            .78f to surface.copy(alpha = .08f),
                            1f to Color.Transparent,
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun SidebarAction(
    icon: ImageVector,
    title: String,
    supportingText: String,
    enabled: Boolean = true,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = Color.Transparent,
        contentColor = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp))
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    supportingText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
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
    onConnectManually: (String) -> Unit,
    onDisconnect: () -> Unit,
    onPair: (String) -> Unit,
    onForgetPairing: () -> Unit,
) {
    var pairingCode by remember(state.pairingRequired) { mutableStateOf("") }
    var manualAddress by rememberSaveable(state.manualAddress) { mutableStateOf(state.manualAddress) }
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
            .verticalScroll(rememberScrollState())
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
            if (state.connectionStatus == CastConnectionStatus.CONNECTING) {
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
            "手机与 Mac 连接同一局域网，输入 Mac 窗口显示的地址即可连接。",
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

        Spacer(Modifier.height(20.dp))
        Text("手动连接", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = manualAddress,
            onValueChange = { manualAddress = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Mac 连接地址") },
            placeholder = { Text("192.168.1.10:49200") },
            supportingText = { Text("打开 Mac 菜单栏的小银子窗口，查看连接地址") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onConnectManually(manualAddress) }),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { onConnectManually(manualAddress) },
            enabled = manualAddress.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("连接这个地址") }

        if (state.enabled) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) {
                Text("断开连接")
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
    CastConnectionStatus.CONNECTING -> "正在连接 ${state.selectedDeviceName.orEmpty()}"
    CastConnectionStatus.PAIRING -> "等待配对"
    CastConnectionStatus.CONNECTED -> "已连接 ${state.selectedDeviceName.orEmpty()}"
    CastConnectionStatus.ERROR -> "连接中断，正在自动重试"
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun GroupSelector(
    groups: List<LibraryGroupUiState>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onCreate: () -> Unit,
    onDelete: () -> Unit,
    onHidePreset: (String) -> Unit,
) {
    var presetPendingDeletion by remember { mutableStateOf<LibraryGroupUiState?>(null) }

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        GroupBlock(
            label = "全部",
            selected = selectedId == null,
            onClick = { onSelect(null) },
        ) {
            Icon(Icons.Rounded.LibraryMusic, contentDescription = null, Modifier.size(20.dp))
        }
        groups.forEach { group ->
            val artworkRes = group.albumArtworkRes()
            if (group.isPreset && artworkRes != null) {
                GroupBlock(
                    label = group.name,
                    selected = selectedId == group.id,
                    onClick = { onSelect(group.id) },
                    onLongClick = { presetPendingDeletion = group },
                ) {
                    Image(
                        painter = painterResource(artworkRes),
                        contentDescription = "${group.name}专辑封面",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(5.dp)),
                    )
                }
            } else {
                GroupBlock(
                    label = group.name,
                    selected = selectedId == group.id,
                    onClick = { onSelect(group.id) },
                    onLongClick = if (group.isPreset) {
                        { presetPendingDeletion = group }
                    } else {
                        null
                    },
                ) {
                    Icon(Icons.Rounded.LibraryMusic, contentDescription = null, Modifier.size(20.dp))
                }
            }
        }
        GroupBlock(
            label = "新分组",
            selected = false,
            onClick = onCreate,
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, Modifier.size(20.dp))
        }
        if (groups.firstOrNull { it.id == selectedId }?.isPreset == false) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = "删除当前分组")
            }
        }
    }

    presetPendingDeletion?.let { group ->
        AlertDialog(
            onDismissRequest = { presetPendingDeletion = null },
            title = { Text("删除预设分组？") },
            text = {
                Text("将删除预设分组《${group.name}》，不会删除设备中的歌曲。之后可以通过“恢复预设”找回。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onHidePreset(group.id)
                        presetPendingDeletion = null
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { presetPendingDeletion = null }) { Text("取消") }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun GroupBlock(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    leadingContent: @Composable () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        label = "album group background",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "album group content",
    )

    Box(modifier = Modifier.padding(top = 4.dp)) {
        Surface(
            modifier = if (onLongClick == null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier.combinedClickable(
                    onClick = onClick,
                    onLongClickLabel = "删除预设分组",
                    onLongClick = onLongClick,
                )
            },
            shape = RoundedCornerShape(8.dp),
            color = containerColor,
            contentColor = contentColor,
            tonalElevation = if (selected) 3.dp else 0.dp,
        ) {
            Row(
                modifier = Modifier.padding(start = 5.dp, top = 5.dp, end = 18.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    leadingContent()
                }
                Spacer(Modifier.size(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }

        AnimatedVisibility(
            visible = selected,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 3.dp, y = (-5).dp),
            enter = fadeIn(tween(140)) + scaleIn(tween(180), initialScale = .6f),
            exit = fadeOut(tween(100)) + scaleOut(tween(120), targetScale = .6f),
        ) {
            SelectedGroupBadge()
        }
    }
}

@Composable
private fun SelectedGroupBadge() = Image(
    painter = painterResource(R.drawable.yinlin_album_selected_badge),
    contentDescription = "当前分组",
    contentScale = ContentScale.Fit,
    modifier = Modifier.size(width = 20.dp, height = 27.dp),
)

private fun LibraryGroupUiState.albumArtworkRes(): Int? = when (id) {
    "preset:album:fu-cao-wei-ying" -> R.drawable.yinlin_fucao
    "preset:album:pi-fu-du-hai" -> R.drawable.yinlin_pifu
    "preset:album:feng-hua-xue-yue" -> R.drawable.yinlin_fenghua
    "preset:album:liu-li" -> R.drawable.yinlin_liuli
    "preset:album:li-di-shi-gong-fen-a-mian" -> R.drawable.yinlin_lidi_a
    "preset:album:li-di-shi-gong-fen-b-mian" -> R.drawable.yinlin_lidi
    "preset:album:shan-se-you-wu-zhong" -> R.drawable.yinlin_shanse
    "preset:album:lin-lin" -> R.drawable.yinlin_linlin
    else -> null
}

@Composable
private fun TrackList(
    tracks: List<LibraryTrackUiState>,
    groups: List<GroupSummary>,
    selectedGroupId: Long?,
    playingUri: String?,
    artworkLoader: TrackArtworkLoader,
    onPlay: (TrackEntity) -> Unit,
    onAddToGroup: (String, Long) -> Unit,
    onRemoveFromGroup: (String) -> Unit,
    onDeleteTrack: (TrackEntity) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) {
        items(tracks, key = LibraryTrackUiState::key) { item ->
            TrackRow(
                item = item,
                groups = groups,
                selectedGroupId = selectedGroupId,
                isPlaying = playingUri == item.track?.uri,
                artworkLoader = artworkLoader,
                onPlay = { item.track?.let(onPlay) },
                onAddToGroup = { groupId -> item.track?.let { onAddToGroup(it.uri, groupId) } },
                onRemoveFromGroup = { item.track?.let { onRemoveFromGroup(it.uri) } },
                onDelete = { item.track?.let(onDeleteTrack) },
            )
            HorizontalDivider(modifier = Modifier.padding(start = 90.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = .5f))
        }
    }
}

@Composable
private fun TrackRow(
    item: LibraryTrackUiState,
    groups: List<GroupSummary>,
    selectedGroupId: Long?,
    isPlaying: Boolean,
    artworkLoader: TrackArtworkLoader,
    onPlay: () -> Unit,
    onAddToGroup: (Long) -> Unit,
    onRemoveFromGroup: () -> Unit,
    onDelete: () -> Unit,
) {
    val track = item.track
    val available = track != null
    var menuOpen by remember { mutableStateOf(false) }
    var deleteConfirmationOpen by remember { mutableStateOf(false) }
    val titleColor by animateColorAsState(
        if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        label = "track title",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (available) 1f else .42f)
            .clickable(enabled = available, onClick = onPlay)
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 38.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isPlaying) MaterialTheme.colorScheme.primary else Color.Transparent),
        )
        Spacer(Modifier.size(9.dp))
        TrackArtwork(
            trackUri = track?.uri,
            loader = artworkLoader,
            modifier = Modifier.size(52.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                item.title,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                track?.let(::trackSupportingText) ?: "本地暂无音乐文件",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (track?.lyricUri != null) {
            Text(
                "词",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        if (track != null) Box {
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
                DropdownMenuItem(
                    text = { Text("删除歌曲文件", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        menuOpen = false
                        deleteConfirmationOpen = true
                    },
                )
            }
        }
    }

    if (deleteConfirmationOpen) {
        AlertDialog(
            onDismissRequest = { deleteConfirmationOpen = false },
            title = { Text("删除《${item.title}》？") },
            text = {
                Text(
                    if (track?.lyricUri == null) {
                        "歌曲文件将从设备中永久删除，此操作无法撤销。"
                    } else {
                        "歌曲文件和关联歌词将从设备中永久删除，此操作无法撤销。"
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteConfirmationOpen = false
                        onDelete()
                    },
                ) {
                    Text("永久删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmationOpen = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun EmptyLibrary(importing: Boolean, onImportArchive: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        FanCoverPair(modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(26.dp))
        Text("把喜欢的歌，藏在这里", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "导入包含 MP3 和同名 .lrc / .lrcx 歌词的 ZIP 压缩包。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onImportArchive, enabled = !importing) {
            Icon(Icons.Rounded.Archive, null)
            Spacer(Modifier.size(8.dp))
            Text(if (importing) "正在导入" else "导入音乐压缩包")
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
private fun MiniPlayer(
    state: PlayerUiState,
    artworkLoader: TrackArtworkLoader,
    onOpen: () -> Unit,
    onTogglePlay: () -> Unit,
) {
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
            modifier = Modifier.padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrackArtwork(
                trackUri = state.currentTrackUri.orEmpty(),
                loader = artworkLoader,
                modifier = Modifier.size(46.dp),
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(state.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                Text(
                    playerSupportingText(state.album, state.artist),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NowPlayingScreen(
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
) {
    var showQueue by remember { mutableStateOf(false) }
    val currentLine = lyrics.indexOfLast { it.timeMs <= state.positionMs }
    val lyricListState = key(state.currentTrackUri) {
        rememberLazyListState()
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
                    if (showLyrics) "银临 · 歌词" else "银临 · 正在播放",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row {
                    IconButton(onClick = { showQueue = true }) {
                        Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "播放队列")
                    }
                    IconButton(onClick = onClose) { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "收起") }
                }
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
                        onShowVinyl = { onShowLyricsChange(false) },
                    )
                } else {
                    VinylStage(
                        trackUri = state.currentTrackUri,
                        isPlaying = state.isPlaying,
                        artworkLoader = artworkLoader,
                        onShowLyrics = { onShowLyricsChange(true) },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(state.title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                playerSupportingText(state.album, state.artist),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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

    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            PlaybackQueue(
                queue = state.queue,
                currentIndex = state.currentQueueIndex,
                artworkLoader = artworkLoader,
                onSelect = { index ->
                    onSelectQueueItem(state.queue[index].mediaItemIndex)
                    showQueue = false
                },
            )
        }
    }
}

@Composable
private fun PlaybackQueue(
    queue: List<QueueItemUiState>,
    currentIndex: Int,
    artworkLoader: TrackArtworkLoader,
    onSelect: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(.68f)
            .navigationBarsPadding(),
    ) {
        Text(
            "接下来播放",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 22.dp),
        )
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(index) }
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 3.dp, height = 34.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent),
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
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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
                        Text("当前", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                    }
                }
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
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
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
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClickLabel = "返回唱片",
                            onClick = onShowVinyl,
                        ),
                    contentPadding = PaddingValues(
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

private fun playerSupportingText(album: String, artist: String): String = listOfNotNull(
    album.takeIf(String::isNotBlank)?.let { "《$it》" },
    artist.takeIf { it.isNotBlank() && it != "未知音乐人" },
).joinToString("  ·  ")

private const val ACTIVE_LYRIC_POSITION = .46f
