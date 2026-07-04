package com.lovenote.app.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.lovenote.app.MainActivity
import com.lovenote.app.R
import com.lovenote.app.chat.Message
import com.lovenote.app.notes.Note
import com.lovenote.app.notes.NoteCache
import com.lovenote.app.widget.WidgetRefreshWorker

/**
 * Keeps a live Firestore connection while the app is closed so messages and
 * notes notify instantly (no paid push service needed). Started on app launch
 * and after boot; survives thanks to the battery-optimization exemption.
 */
class ListenerService : Service() {

    private val registrations = mutableListOf<ListenerRegistration>()
    private val coupleRegistrations = mutableListOf<ListenerRegistration>()
    private var coupleId: String? = null
    private var attachedUid: String? = null
    private var authListener: FirebaseAuth.AuthStateListener? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(FOREGROUND_ID, buildQuietNotification())
        // Auth restores asynchronously (especially after boot) — attach
        // whenever a user becomes available instead of checking once.
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val uid = auth.currentUser?.uid
            when {
                uid != null && uid != attachedUid -> {
                    detachAll()
                    attachedUid = uid
                    attach(uid)
                }
                uid == null -> {
                    detachAll()
                    attachedUid = null
                }
            }
        }
        authListener = listener
        FirebaseAuth.getInstance().addAuthStateListener(listener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        authListener?.let { FirebaseAuth.getInstance().removeAuthStateListener(it) }
        detachAll()
        super.onDestroy()
    }

    private fun detachAll() {
        registrations.forEach { it.remove() }
        registrations.clear()
        coupleRegistrations.forEach { it.remove() }
        coupleRegistrations.clear()
        coupleId = null
    }

    private fun attach(uid: String) {
        val db = FirebaseFirestore.getInstance()
        registrations += db.collection("users").document(uid)
            .addSnapshotListener { snap, _ ->
                val cid = snap?.getString("coupleId")
                if (cid != null && cid != coupleId) {
                    coupleId = cid
                    listenToCouple(db, cid, uid)
                }
            }
    }

    private fun listenToCouple(db: FirebaseFirestore, cid: String, uid: String) {
        coupleRegistrations.forEach { it.remove() }
        coupleRegistrations.clear()

        // Ring for incoming calls even when the app is closed.
        coupleRegistrations += db.collection("couples").document(cid)
            .collection("call").document("current")
            .addSnapshotListener { snap, _ ->
                val status = snap?.getString("status")
                val caller = snap?.getString("caller")
                val startedMillis = snap?.getTimestamp("startedAt")?.toDate()?.time ?: 0L
                val fresh = System.currentTimeMillis() - startedMillis < 60_000
                if (status == "ringing" && fresh && caller != null && caller != uid &&
                    !AppVisibility.appVisible
                ) {
                    Notifier.notifyIncomingCall(this, snap.getBoolean("video") ?: false)
                } else {
                    Notifier.cancelIncomingCall(this)
                }
            }

        coupleRegistrations += db.collection("couples").document(cid).collection("messages")
            .orderBy("sentAt", Query.Direction.DESCENDING).limit(1)
            .addSnapshotListener { snap, _ ->
                val doc = snap?.documents?.firstOrNull() ?: return@addSnapshotListener
                val message = Message.fromMap(doc.id, doc.data ?: emptyMap())
                val millis = message.sentAt?.toDate()?.time ?: return@addSnapshotListener
                if (!message.isMine(uid) && !message.seen && !AppVisibility.appVisible &&
                    millis > NotifyState.lastMessageMillis(this)
                ) {
                    NotifyState.setLastMessage(this, millis)
                    Notifier.notifyMessage(
                        this,
                        when {
                            message.isPhoto -> "📷 Photo"
                            message.isVoice -> "🎤 Voice note"
                            else -> message.body
                        },
                    )
                }
            }

        coupleRegistrations += db.collection("couples").document(cid).collection("notes")
            .orderBy("sentAt", Query.Direction.DESCENDING).limit(1)
            .addSnapshotListener { snap, _ ->
                val doc = snap?.documents?.firstOrNull() ?: return@addSnapshotListener
                val note = Note.fromMap(doc.id, doc.data ?: emptyMap())
                if (note.senderUid == uid) return@addSnapshotListener
                val millis = note.sentAt?.toDate()?.time ?: return@addSnapshotListener
                NoteCache.save(this, note)
                WidgetRefreshWorker.refreshNow(this)
                if (!AppVisibility.appVisible && millis > NotifyState.lastNoteMillis(this)) {
                    NotifyState.setLastNote(this, millis)
                    Notifier.notifyNote(this, note.text.ifBlank { "🎨 A doodle for you" })
                }
            }
    }

    private fun buildQuietNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                "Background connection",
                NotificationManager.IMPORTANCE_MIN,
            ).apply { description = "Keeps OurVerse connected for instant notifications" },
        )
        val open = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_heart)
            .setContentTitle("OurVerse is connected ❤")
            .setContentIntent(open)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    companion object {
        private const val FOREGROUND_ID = 3
        private const val CHANNEL = "background"

        fun start(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, ListenerService::class.java),
                )
            }
        }
    }
}
