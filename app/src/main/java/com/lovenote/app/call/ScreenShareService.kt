package com.lovenote.app.call

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.lovenote.app.R

/**
 * Android requires an active mediaProjection foreground service before the
 * screen can be captured; this service exists only for that.
 */
class ScreenShareService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                "screenshare",
                "Screen sharing",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        val notification = NotificationCompat.Builder(this, "screenshare")
            .setSmallIcon(R.drawable.ic_screen_share)
            .setContentTitle("Sharing your screen with your partner 📺")
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(
            this,
            4,
            notification,
            if (Build.VERSION.SDK_INT >= 29) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            } else {
                0
            },
        )
        CallManager.onProjectionServiceReady(this)
        return START_NOT_STICKY
    }

    companion object {
        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ScreenShareService::class.java),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenShareService::class.java))
        }
    }
}
