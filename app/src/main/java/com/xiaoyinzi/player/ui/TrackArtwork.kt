package com.xiaoyinzi.player.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xiaoyinzi.player.library.TrackArtworkLoader

@Composable
internal fun TrackArtwork(
    trackUri: String?,
    loader: TrackArtworkLoader,
    modifier: Modifier = Modifier,
) {
    val artwork = rememberTrackArtwork(trackUri, loader, 56.dp)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (artwork != null) {
            Image(
                bitmap = artwork.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                Icons.Rounded.Album,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

    }
}

@Composable
internal fun rememberTrackArtwork(
    trackUri: String?,
    loader: TrackArtworkLoader,
    targetSize: Dp,
): android.graphics.Bitmap? {
    val targetSizePx = with(LocalDensity.current) { targetSize.roundToPx() }
    val artwork by produceState<android.graphics.Bitmap?>(
        initialValue = null,
        key1 = trackUri,
        key2 = targetSizePx,
    ) {
        value = trackUri?.let { loader.load(it, targetSizePx) }
    }
    return artwork
}
