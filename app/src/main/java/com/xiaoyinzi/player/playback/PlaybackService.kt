package com.xiaoyinzi.player.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.xiaoyinzi.player.MainActivity
import com.xiaoyinzi.player.XiaoYinZiApplication

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    private val playbackModeListener = object : Player.Listener {
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = persistPlaybackMode()

        override fun onRepeatModeChanged(repeatMode: Int) = persistPlaybackMode()
    }

    @UnstableApi
    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setDeviceVolumeControlEnabled(true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        applyPlaybackMode(
            player,
            PlaybackMode.fromStoredValue(
                getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).getString(KEY_PLAYBACK_MODE, null),
            ),
        )
        player.addListener(playbackModeListener)
        val app = application as XiaoYinZiApplication
        app.lyricsCastManager.bindPlayer(player, app.lyricParser)
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        (application as XiaoYinZiApplication).lyricsCastManager.unbindPlayer()
        mediaSession?.run {
            player.removeListener(playbackModeListener)
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private fun persistPlaybackMode() {
        val player = mediaSession?.player ?: return
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
            .edit()
            .putString(
                KEY_PLAYBACK_MODE,
                PlaybackMode.from(player.shuffleModeEnabled, player.repeatMode).name,
            )
            .apply()
    }

    private fun applyPlaybackMode(player: Player, mode: PlaybackMode) {
        player.shuffleModeEnabled = mode == PlaybackMode.SHUFFLE
        player.repeatMode = when (mode) {
            PlaybackMode.SEQUENTIAL -> Player.REPEAT_MODE_OFF
            PlaybackMode.REPEAT_ALL, PlaybackMode.SHUFFLE -> Player.REPEAT_MODE_ALL
            PlaybackMode.REPEAT_ONE -> Player.REPEAT_MODE_ONE
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "playback"
        const val KEY_PLAYBACK_MODE = "playback_mode"
    }
}
