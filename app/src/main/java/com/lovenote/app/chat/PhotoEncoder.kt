package com.lovenote.app.chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Turns a picked photo into a Base64 JPEG small enough to live inside a
 * Firestore document (1MB limit), so no paid Storage plan is needed.
 */
object PhotoEncoder {
    private const val MAX_DIMENSION = 1280
    private const val MAX_BYTES = 700_000

    suspend fun encode(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val source = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: error("Couldn't read that photo")

        val scale = MAX_DIMENSION.toFloat() / maxOf(source.width, source.height)
        val bitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                source,
                (source.width * scale).toInt().coerceAtLeast(1),
                (source.height * scale).toInt().coerceAtLeast(1),
                true,
            )
        } else {
            source
        }

        var quality = 70
        var bytes = compress(bitmap, quality)
        while (bytes.size > MAX_BYTES && quality > 30) {
            quality -= 10
            bytes = compress(bitmap, quality)
        }
        check(bytes.size <= MAX_BYTES) { "That photo is too large to send" }
        Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun compress(bitmap: Bitmap, quality: Int): ByteArray =
        ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }
}
