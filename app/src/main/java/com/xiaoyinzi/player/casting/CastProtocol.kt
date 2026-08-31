package com.xiaoyinzi.player.casting

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val CAST_PROTOCOL_VERSION = 1
const val CAST_SERVICE_TYPE = "_xiaoyinzi-lyrics._tcp."
const val MEDIA_EXTRA_LYRIC_URI = "com.xiaoyinzi.player.LYRIC_URI"

val CastJson = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

data class CastDevice(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
)

enum class CastConnectionStatus {
    OFF,
    SEARCHING,
    CONNECTING,
    PAIRING,
    CONNECTED,
    ERROR,
}

data class CastUiState(
    val enabled: Boolean = false,
    val discovering: Boolean = false,
    val devices: List<CastDevice> = emptyList(),
    val selectedDeviceName: String? = null,
    val connectionStatus: CastConnectionStatus = CastConnectionStatus.OFF,
    val pairingRequired: Boolean = false,
    val message: String? = null,
    val lastSyncAt: Long? = null,
)

@Serializable
data class CastLyricLine(
    val timeMs: Long,
    val text: String,
)

@Serializable
data class HelloMessage(
    val type: String = "hello",
    val protocolVersion: Int = CAST_PROTOCOL_VERSION,
    val deviceId: String,
    val deviceName: String = "小银子 Android",
    val token: String? = null,
)

@Serializable
data class PairMessage(
    val type: String = "pair",
    val code: String,
    val deviceId: String,
)

@Serializable
data class TrackMessage(
    val type: String = "track",
    val protocolVersion: Int = CAST_PROTOCOL_VERSION,
    val trackId: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val positionMs: Long,
    val isPlaying: Boolean,
    val playbackSpeed: Float,
    val sentAtEpochMs: Long,
    val lyricsHash: String,
    val lyrics: List<CastLyricLine>,
)

@Serializable
data class SyncMessage(
    val type: String = "sync",
    val trackId: String,
    val positionMs: Long,
    val durationMs: Long,
    val isPlaying: Boolean,
    val playbackSpeed: Float,
    val sentAtEpochMs: Long,
)
