package com.xiaoyinzi.player

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaoyinzi.player.ui.PlayerApp
import com.xiaoyinzi.player.ui.XiaoYinZiTheme
import com.xiaoyinzi.player.ui.readableText

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        setContent {
            val themeSettings by viewModel.themeSettings.collectAsStateWithLifecycle()
            XiaoYinZiTheme(themeSettings.activeTheme) {
                SideEffect {
                    val background = Color(themeSettings.activeTheme.background)
                    val lightBars = readableText(background).luminance() < .5f
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = lightBars
                        isAppearanceLightNavigationBars = lightBars
                    }
                    @Suppress("DEPRECATION")
                    window.statusBarColor = background.toArgb()
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = background.toArgb()
                }
                val archivePicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument(),
                    onResult = { uri -> uri?.let(viewModel::importArchive) },
                )
                val notificationPermission = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = {},
                )
                LaunchedEffect(Unit) {
                    if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                PlayerApp(
                    viewModel = viewModel,
                    onImportArchive = {
                        archivePicker.launch(
                            arrayOf(
                                "application/zip",
                                "application/x-zip-compressed",
                                "application/octet-stream",
                            ),
                        )
                    },
                )
            }
        }
    }
}
