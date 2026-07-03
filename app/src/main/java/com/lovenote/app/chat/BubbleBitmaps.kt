package com.lovenote.app.chat

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Off-main-thread photo decoding with an in-memory cache, so scrolling past
 * photos never re-decodes them on the UI thread (the main source of jank).
 */
object BubbleBitmaps {
    private val cache = LruCache<String, ImageBitmap>(48)

    fun peek(id: String): ImageBitmap? = cache.get(id)

    /** Bubble-sized decode (half resolution is plenty for a 260dp bubble). */
    suspend fun bubble(id: String, base64: String): ImageBitmap? =
        cache.get(id) ?: decode(base64, sample = 2)?.also { cache.put(id, it) }

    /** Full-resolution decode for the fullscreen viewer. */
    suspend fun full(base64: String): ImageBitmap? = decode(base64, sample = 1)

    private suspend fun decode(base64: String, sample: Int): ImageBitmap? =
        withContext(Dispatchers.Default) {
            runCatching {
                val bytes = Base64.decode(base64, Base64.NO_WRAP)
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
            }.getOrNull()
        }
}
