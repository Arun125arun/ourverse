package com.lovenote.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.lovenote.app.MainActivity
import com.lovenote.app.R

object Notifier {
    private const val CHANNEL_MESSAGES = "messages"
    private const val CHANNEL_NOTES = "notes"
    private const val CHANNEL_CALLS = "calls"
    private const val CALL_NOTIFICATION_ID = 5
    private val VIBRATION = longArrayOf(0, 250, 150, 250)

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "New chat messages from your partner"
                enableVibration(true)
                vibrationPattern = VIBRATION
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_NOTES,
                "Notes",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "New notes from your partner"
                enableVibration(true)
                vibrationPattern = VIBRATION
            },
        )
    }

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

    fun notifyMessage(context: Context, preview: String) =
        notify(context, CHANNEL_MESSAGES, 1, "New message ❤", preview)

    /** Full-screen incoming-call alert for when the app is closed. */
    fun notifyIncomingCall(context: Context, video: Boolean) {
        if (!canNotify(context)) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CALLS,
                "Calls",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 400, 600, 400, 600)
            },
        )
        val fullScreen = PendingIntent.getActivity(
            context,
            2,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_CALLS)
            .setSmallIcon(R.drawable.ic_heart)
            .setContentTitle(if (video) "Incoming video call ❤" else "Incoming voice call ❤")
            .setContentText("Tap to answer")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .setAutoCancel(true)
            .build()
        manager.notify(CALL_NOTIFICATION_ID, notification)
    }

    fun cancelIncomingCall(context: Context) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(CALL_NOTIFICATION_ID)
    }

    fun notifyNote(context: Context, preview: String) =
        notify(context, CHANNEL_NOTES, 2, "A note for you ❤", preview)

    private fun notify(
        context: Context,
        channel: String,
        id: Int,
        title: String,
        text: String,
    ) {
        if (!canNotify(context)) return
        val intent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_heart)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(intent)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(id, notification)
    }

    /** Short in-app buzz for when the user is already looking at the chat. */
    fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}

/** Whether the app/chat is currently on screen (no notification needed then). */
object AppVisibility {
    @Volatile var appVisible = false
    @Volatile var chatVisible = false
}

/** Tracks the newest message/note we've already notified (or shown), in prefs. */
object NotifyState {
    private fun prefs(context: Context) =
        context.getSharedPreferences("notify_state", Context.MODE_PRIVATE)

    fun lastMessageMillis(context: Context) = prefs(context).getLong("msg", 0L)
    fun lastNoteMillis(context: Context) = prefs(context).getLong("note", 0L)

    fun setLastMessage(context: Context, millis: Long) {
        if (millis > lastMessageMillis(context)) {
            prefs(context).edit().putLong("msg", millis).apply()
        }
    }

    fun setLastNote(context: Context, millis: Long) {
        if (millis > lastNoteMillis(context)) {
            prefs(context).edit().putLong("note", millis).apply()
        }
    }
}
