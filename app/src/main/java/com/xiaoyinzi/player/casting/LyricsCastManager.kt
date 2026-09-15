package com.xiaoyinzi.player.casting

import android.content.Context
import androidx.core.content.edit
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.xiaoyinzi.player.lyrics.LrcxParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.min

class LyricsCastManager(context: Context) : AutoCloseable {
    private val applicationContext = context.applicationContext
    private val preferences = applicationContext.getSharedPreferences(PREFERENCES_NAME, 0)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val json = CastJson
    private val sendMutex = Mutex()
    private val _state = MutableStateFlow(
        CastUiState(
            enabled = preferences.getBoolean(KEY_ENABLED, false),
            selectedDeviceName = preferences.getString(KEY_DEVICE_NAME, null),
            manualAddress = preferences.getString(KEY_MANUAL_ADDRESS, null)
                ?: storedDevice()?.let { "${it.host}:${it.port}" }.orEmpty(),
        ),
    )
    val state: StateFlow<CastUiState> = _state.asStateFlow()

    private val deviceId: String = preferences.getString(KEY_DEVICE_ID, null)
        ?: UUID.randomUUID().toString().also { id -> preferences.edit { putString(KEY_DEVICE_ID, id) } }
    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    private var connectionJob: Job? = null
    private var syncJob: Job? = null
    private var player: Player? = null
    private var lyricParser: LrcxParser? = null
    private var currentTrack: TrackMessage? = null

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            loadCurrentTrack(mediaItem)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            sendSyncNow()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            sendSyncNow()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            sendSyncNow()
        }
    }

    init {
        if (_state.value.enabled) storedDevice()?.let(::reconnect)
    }

    fun connectManually(address: String) {
        val device = runCatching { parseManualCastAddress(address) }.getOrElse { error ->
            _state.update { it.copy(message = error.message) }
            return
        }
        val normalized = "${device.host}:${device.port}"
        preferences.edit { putString(KEY_MANUAL_ADDRESS, normalized) }
        _state.update { it.copy(manualAddress = normalized) }
        connect(device)
    }

    private fun connect(device: CastDevice) {
        preferences.edit {
            putBoolean(KEY_ENABLED, true)
            putString(KEY_DEVICE_NAME, device.name)
        }
        persistEndpoint(device)
        _state.update {
            it.copy(
                enabled = true,
                selectedDeviceName = device.name,
                connectionStatus = CastConnectionStatus.CONNECTING,
                message = null,
                pairingRequired = false,
            )
        }
        reconnect(device)
    }

    fun disconnect() {
        preferences.edit { putBoolean(KEY_ENABLED, false) }
        _state.update {
            it.copy(
                enabled = false,
                connectionStatus = CastConnectionStatus.OFF,
                pairingRequired = false,
                message = null,
            )
        }
        connectionJob?.cancel()
        connectionJob = null
        closeTransport()
    }

    fun submitPairingCode(code: String) {
        val normalized = code.filter(Char::isDigit).take(6)
        if (normalized.length != 6) {
            _state.update { it.copy(message = "请输入 Mac 上显示的 6 位配对码") }
            return
        }
        scope.launch {
            sendMessage(PairMessage(code = normalized, deviceId = deviceId))
        }
    }

    fun forgetPairing() {
        preferences.edit { remove(KEY_TOKEN) }
        storedDevice()?.let(::reconnect)
    }

    fun bindPlayer(player: Player, lyricParser: LrcxParser) {
        if (this.player === player) return
        unbindPlayer()
        this.player = player
        this.lyricParser = lyricParser
        player.addListener(playerListener)
        loadCurrentTrack(player.currentMediaItem)
        syncJob = scope.launch {
            while (isActive) {
                delay(SYNC_INTERVAL_MS)
                sendSync()
            }
        }
    }

    fun unbindPlayer() {
        player?.removeListener(playerListener)
        player = null
        lyricParser = null
        syncJob?.cancel()
        syncJob = null
        currentTrack = null
    }

    private fun loadCurrentTrack(mediaItem: MediaItem?) {
        if (mediaItem == null) {
            currentTrack = null
            return
        }
        val mediaId = mediaItem.mediaId
        val lyricUri = mediaItem.mediaMetadata.extras?.getString(MEDIA_EXTRA_LYRIC_URI)
        val parser = lyricParser ?: return
        scope.launch {
            val lyrics = parser.read(lyricUri).map { CastLyricLine(it.timeMs, it.text) }
            if (player?.currentMediaItem?.mediaId != mediaId) return@launch
            val activePlayer = player ?: return@launch
            val trackId = sha256(mediaId)
            currentTrack = TrackMessage(
                trackId = trackId,
                title = mediaItem.mediaMetadata.title?.toString().orEmpty(),
                artist = mediaItem.mediaMetadata.artist?.toString().orEmpty(),
                durationMs = activePlayer.safeDuration(),
                positionMs = activePlayer.currentPosition.coerceAtLeast(0),
                isPlaying = activePlayer.isPlaying,
                playbackSpeed = activePlayer.playbackParameters.speed,
                sentAtEpochMs = System.currentTimeMillis(),
                lyricsHash = sha256(lyrics.joinToString("\n") { "${it.timeMs}:${it.text}" }),
                lyrics = lyrics,
            )
            sendTrackSnapshot()
        }
    }

    private fun sendTrackSnapshot() {
        if (_state.value.connectionStatus != CastConnectionStatus.CONNECTED) return
        val track = currentTrack ?: return
        val activePlayer = player
        val snapshot = if (activePlayer == null) track else track.copy(
            durationMs = activePlayer.safeDuration(),
            positionMs = activePlayer.currentPosition.coerceAtLeast(0),
            isPlaying = activePlayer.isPlaying,
            playbackSpeed = activePlayer.playbackParameters.speed,
            sentAtEpochMs = System.currentTimeMillis(),
        )
        scope.launch { sendMessage(snapshot) }
    }

    private suspend fun sendSync() {
        if (_state.value.connectionStatus != CastConnectionStatus.CONNECTED) return
        val activePlayer = player ?: return
        val track = currentTrack ?: return
        sendMessage(
            SyncMessage(
                trackId = track.trackId,
                positionMs = activePlayer.currentPosition.coerceAtLeast(0),
                durationMs = activePlayer.safeDuration(),
                isPlaying = activePlayer.isPlaying,
                playbackSpeed = activePlayer.playbackParameters.speed,
                sentAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    private fun sendSyncNow() {
        scope.launch { sendSync() }
    }

    private fun reconnect(device: CastDevice) {
        val previous = connectionJob
        previous?.cancel()
        closeTransport()
        connectionJob = scope.launch {
            // A cancelled blocking socket operation must finish before a new
            // connection takes ownership of the shared transport.
            previous?.join()
            var retryDelay = 1_000L
            while (isActive && _state.value.enabled) {
                _state.update {
                    it.copy(
                        connectionStatus = CastConnectionStatus.CONNECTING,
                        pairingRequired = false,
                        message = null,
                    )
                }
                val failure = runCatching { withContext(Dispatchers.IO) { runConnection(device) } }.exceptionOrNull()
                if (!isActive || !_state.value.enabled) break
                _state.update {
                    it.copy(
                        connectionStatus = CastConnectionStatus.ERROR,
                        pairingRequired = false,
                        message = when (failure) {
                            is SocketTimeoutException -> "连接 ${device.host}:${device.port} 超时，请检查同一 Wi-Fi、Mac 本地网络权限及防火墙"
                            is ConnectException -> "Mac 拒绝连接，请确认桌面端已打开，且地址和端口与窗口显示一致"
                            is NoRouteToHostException -> "无法访问 Mac，请确认两台设备在同一局域网，网络未开启设备隔离"
                            else -> failure?.localizedMessage ?: "Mac 连接已断开，正在重试"
                        },
                    )
                }
                delay(retryDelay)
                retryDelay = min(retryDelay * 2, MAX_RETRY_DELAY_MS)
            }
        }
    }

    private suspend fun runConnection(device: CastDevice) {
        val connection = Socket()
        try {
            connection.connect(InetSocketAddress(device.host, device.port), CONNECT_TIMEOUT_MS)
            connection.tcpNoDelay = true
            connection.soTimeout = HANDSHAKE_TIMEOUT_MS
            val output = BufferedWriter(OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8))
            val input = BufferedReader(InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))
            val connectionContext = currentCoroutineContext()
            synchronized(this) {
                connectionContext.ensureActive()
                socket = connection
                writer = output
            }
            _state.update {
                it.copy(
                    connectionStatus = CastConnectionStatus.CONNECTING,
                    message = "已连接，等待 Mac 确认",
                )
            }

            sendMessage(
                HelloMessage(
                    deviceId = deviceId,
                    token = preferences.getString(KEY_TOKEN, null),
                ),
            )
            while (true) {
                val line = input.readLine() ?: break
                currentCoroutineContext().ensureActive()
                connection.soTimeout = 0
                handleIncoming(line)
            }
        } finally {
            runCatching { connection.close() }
            closeTransport(connection)
        }
    }

    private fun handleIncoming(line: String) {
        runCatching {
            val payload = json.parseToJsonElement(line).jsonObject
            when (payload["type"]?.jsonPrimitive?.content) {
                "pair_required" -> _state.update {
                    it.copy(
                        connectionStatus = CastConnectionStatus.PAIRING,
                        pairingRequired = true,
                        message = "输入 Mac 上显示的配对码",
                    )
                }

                "paired" -> {
                    payload["token"]?.jsonPrimitive?.content?.let { token ->
                        preferences.edit { putString(KEY_TOKEN, token) }
                    }
                    _state.update {
                        it.copy(
                            connectionStatus = CastConnectionStatus.CONNECTED,
                            pairingRequired = false,
                            message = "配对成功",
                        )
                    }
                    scope.launch { sendTrackSnapshot() }
                }

                "ready" -> {
                    _state.update {
                        it.copy(
                            connectionStatus = CastConnectionStatus.CONNECTED,
                            pairingRequired = false,
                            message = "歌词同步中",
                        )
                    }
                    scope.launch { sendTrackSnapshot() }
                }

                "error" -> _state.update {
                    it.copy(message = payload["message"]?.jsonPrimitive?.content ?: "Mac 返回错误")
                }
            }
        }.onFailure { error ->
            _state.update { it.copy(message = "无法解析 Mac 消息：${error.localizedMessage}") }
        }
    }

    private suspend inline fun <reified T> sendMessage(message: T) {
        val encoded = json.encodeToString(message)
        withContext(Dispatchers.IO) {
            var sendingSocket: Socket? = null
            val failure = runCatching {
                sendMutex.withLock {
                    val activeWriter = synchronized(this@LyricsCastManager) {
                        sendingSocket = socket
                        writer
                    } ?: return@withLock
                    activeWriter.write(encoded)
                    activeWriter.newLine()
                    activeWriter.flush()
                    _state.update { it.copy(lastSyncAt = System.currentTimeMillis()) }
                }
            }.exceptionOrNull()
            if (failure != null) {
                sendingSocket?.let(::closeTransport)
            }
        }
    }

    private fun persistEndpoint(device: CastDevice) {
        preferences.edit {
            putString(KEY_DEVICE_HOST, device.host)
            putInt(KEY_DEVICE_PORT, device.port)
        }
    }

    private fun storedDevice(): CastDevice? {
        val name = preferences.getString(KEY_DEVICE_NAME, null) ?: return null
        val host = preferences.getString(KEY_DEVICE_HOST, null) ?: return null
        val port = preferences.getInt(KEY_DEVICE_PORT, 0).takeIf { it > 0 } ?: return null
        return CastDevice("$name@$host:$port", name, host, port)
    }

    @Synchronized
    private fun closeTransport(expected: Socket? = null) {
        if (expected != null && socket !== expected) return
        runCatching { socket?.close() }
        runCatching { writer?.close() }
        writer = null
        socket = null
    }

    override fun close() {
        unbindPlayer()
        disconnect()
        scope.cancel()
    }

    private fun Player.safeDuration(): Long = duration.takeUnless { it == C.TIME_UNSET }?.coerceAtLeast(0) ?: 0

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }

    private companion object {
        const val PREFERENCES_NAME = "lyrics_cast"
        const val KEY_ENABLED = "enabled"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_DEVICE_NAME = "device_name"
        const val KEY_DEVICE_HOST = "device_host"
        const val KEY_DEVICE_PORT = "device_port"
        const val KEY_MANUAL_ADDRESS = "manual_address"
        const val KEY_TOKEN = "pair_token"
        const val SYNC_INTERVAL_MS = 750L
        const val CONNECT_TIMEOUT_MS = 5_000
        const val HANDSHAKE_TIMEOUT_MS = 10_000
        const val MAX_RETRY_DELAY_MS = 15_000L
    }
}
