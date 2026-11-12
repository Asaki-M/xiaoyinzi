@file:Suppress("DEPRECATION")

package com.xiaoyinzi.player.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.xiaoyinzi.player.R
import com.xiaoyinzi.player.data.ThemeBackgroundSlot
import com.xiaoyinzi.player.data.ThemeImage
import com.xiaoyinzi.player.data.ThemeImageKind
import com.xiaoyinzi.player.data.ThemeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun ThemeArtwork(
    slot: ThemeBackgroundSlot,
    modifier: Modifier = Modifier,
    image: ThemeImage = LocalAppTheme.current.image(slot),
    blur: Float = LocalAppTheme.current.imageBlur,
    focus: Float = LocalAppTheme.current.imageFocus,
) {
    val resource =
        when (image.kind) {
            ThemeImageKind.PORTRAIT -> R.drawable.yinlin_portrait
            ThemeImageKind.MOUNTAIN -> R.drawable.yinlin_shanse
            ThemeImageKind.STAGE -> R.drawable.yinlin_sidebar
            else -> null
        }
    val alignment = BiasAlignment(focus * 2f - 1f, -.2f)
    val legacyBlur = Build.VERSION.SDK_INT < Build.VERSION_CODES.S && blur > 0f
    Box(modifier.background(MaterialTheme.colorScheme.background)) {
        val artworkModifier = Modifier.fillMaxSize().blur(blur.dp)
        if (resource != null && !legacyBlur) {
            Image(
                painter = painterResource(resource),
                contentDescription = null,
                modifier = artworkModifier,
                contentScale = ContentScale.Crop,
                alignment = alignment,
            )
        } else if (image.kind == ThemeImageKind.LOCAL || (resource != null && legacyBlur)) {
            val context = LocalContext.current
            val bitmap by
                produceState<Bitmap?>(null, image, if (legacyBlur) blur else 0f) {
                    value =
                        withContext(Dispatchers.IO) {
                            runCatching {
                                val file = ThemeRepository.imageFile(context, image)
                                val bounds =
                                    BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                if (resource != null)
                                    BitmapFactory.decodeResource(
                                        context.resources,
                                        resource,
                                        bounds,
                                    )
                                else if (file != null) BitmapFactory.decodeFile(file.path, bounds)
                                var sample = 1
                                val targetSize = if (legacyBlur) 512 else 1280
                                while (
                                    maxOf(bounds.outWidth, bounds.outHeight) / sample > targetSize
                                ) sample *= 2
                                val options =
                                    BitmapFactory.Options().apply {
                                        inSampleSize = sample
                                        inPreferredConfig = Bitmap.Config.ARGB_8888
                                    }
                                val decoded =
                                    if (resource != null)
                                        BitmapFactory.decodeResource(
                                            context.resources,
                                            resource,
                                            options,
                                        )
                                    else file?.let { BitmapFactory.decodeFile(it.path, options) }
                                if (decoded != null && legacyBlur)
                                    blurLegacyImage(context, decoded, blur)
                                else decoded
                            }
                                .getOrNull()
                        }
                }
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = artworkModifier,
                    contentScale = ContentScale.Crop,
                    alignment = alignment,
                )
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun blurLegacyImage(context: Context, source: Bitmap, radius: Float): Bitmap {
    return runCatching {
        val renderScript = RenderScript.create(context)
        try {
            val input = Allocation.createFromBitmap(renderScript, source)
            try {
                val output = Allocation.createTyped(renderScript, input.type)
                try {
                    val script =
                        ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
                    try {
                        script.setRadius(radius.coerceIn(.1f, 25f))
                        script.setInput(input)
                        script.forEach(output)
                        createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888).also {
                            output.copyTo(it)
                        }
                    } finally {
                        script.destroy()
                    }
                } finally {
                    output.destroy()
                }
            } finally {
                input.destroy()
            }
        } finally {
            renderScript.destroy()
        }
    }
        .getOrElse { source }
        .also { if (it !== source) source.recycle() }
}
