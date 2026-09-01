package com.xiaoyinzi.player.library

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TrackArtworkLoader(context: Context) {
    private val contentResolver = context.applicationContext.contentResolver
    private val cache = object : LruCache<String, Bitmap>(cacheSizeKib()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount / 1024
    }

    suspend fun load(trackUri: String, targetSizePx: Int): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "$trackUri@$targetSizePx"
        cache.get(cacheKey) ?: readEmbeddedArtwork(trackUri, targetSizePx)?.also {
            cache.put(cacheKey, it)
        }
    }

    private fun readEmbeddedArtwork(trackUri: String, targetSizePx: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            contentResolver.openFileDescriptor(trackUri.toUri(), "r")?.use { descriptor ->
                retriever.setDataSource(descriptor.fileDescriptor)
                retriever.embeddedPicture?.decodeSampledBitmap(targetSizePx)
            }
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun ByteArray.decodeSampledBitmap(targetSizePx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(this, 0, size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (
            bounds.outWidth / (sampleSize * 2) >= targetSizePx &&
            bounds.outHeight / (sampleSize * 2) >= targetSizePx
        ) {
            sampleSize *= 2
        }
        return BitmapFactory.decodeByteArray(
            this,
            0,
            size,
            BitmapFactory.Options().apply { inSampleSize = sampleSize },
        )
    }

    private companion object {
        fun cacheSizeKib(): Int = (Runtime.getRuntime().maxMemory() / 1024 / 16)
            .coerceIn(4 * 1024L, 24 * 1024L)
            .toInt()
    }
}
