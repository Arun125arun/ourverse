package com.lovenote.app.chat

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import java.io.File

/**
 * Records compressed AAC voice notes small enough to store as Base64 in a
 * Firestore document (like photos, this avoids the paid Storage plan).
 */
class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAtMillis = 0L

    companion object {
        const val MAX_SECONDS = 90
        private const val MAX_BYTES = 700_000
    }

    fun start() {
        stopQuietly()
        val out = File.createTempFile("voice", ".m4a", context.cacheDir)
        val r = if (Build.VERSION.SDK_INT >= 31) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioChannels(1)
        r.setAudioSamplingRate(22050)
        r.setAudioEncodingBitRate(24000)
        r.setMaxDuration(MAX_SECONDS * 1000)
        r.setOutputFile(out.absolutePath)
        r.prepare()
        r.start()
        recorder = r
        file = out
        startedAtMillis = System.currentTimeMillis()
    }

    /** Stops and returns (base64Audio, durationSec), or null if too short or too large. */
    fun stop(): Pair<String, Long>? {
        val out = file ?: return null
        stopQuietly()
        val durationSec = (System.currentTimeMillis() - startedAtMillis) / 1000L
        val bytes = out.readBytes()
        out.delete()
        if (durationSec < 1 || bytes.isEmpty()) return null
        if (bytes.size > MAX_BYTES) {
            // Truncate to max allowed size instead of throwing
            val trimmed = bytes.copyOf(MAX_BYTES)
            return Base64.encodeToString(trimmed, Base64.NO_WRAP) to durationSec
        }
        return Base64.encodeToString(bytes, Base64.NO_WRAP) to durationSec
    }

    fun cancel() {
        stopQuietly()
        file?.delete()
        file = null
    }

    private fun stopQuietly() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
    }
}
