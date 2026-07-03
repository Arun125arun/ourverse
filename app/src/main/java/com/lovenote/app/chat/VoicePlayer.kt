package com.lovenote.app.chat

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import java.io.File

/** Plays one voice note at a time from its Base64 content. */
object VoicePlayer {
    private var player: MediaPlayer? = null
    private var playingId: String? = null

    /**
     * Toggles playback of [messageId]. Returns true if now playing.
     * [onFinished] fires when playback reaches the end.
     */
    fun toggle(
        context: Context,
        messageId: String,
        base64Audio: String,
        onFinished: () -> Unit,
    ): Boolean {
        if (playingId == messageId) {
            stop()
            return false
        }
        stop()
        return runCatching {
            val temp = File.createTempFile("play", ".m4a", context.cacheDir)
            temp.writeBytes(Base64.decode(base64Audio, Base64.NO_WRAP))
            val p = MediaPlayer()
            p.setDataSource(temp.absolutePath)
            p.setOnCompletionListener {
                stop()
                temp.delete()
                onFinished()
            }
            p.prepare()
            p.start()
            player = p
            playingId = messageId
            true
        }.getOrDefault(false)
    }

    fun stop() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        playingId = null
    }
}
