package com.lovenote.app.us

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lovenote.app.notify.AppVisibility
import com.lovenote.app.notify.Notifier
import com.lovenote.app.notify.NotifyState
import kotlinx.coroutines.tasks.await

/**
 * Checks for time capsules that have just unlocked and sends notifications.
 * Scheduled by WorkManager when a capsule is created, with a delay equal
 * to the unlock time.
 */
class TimeCapsuleUnlockWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure()
            val coupleId = FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .get().await()
                .getString("coupleId") ?: return Result.failure()

            val snapshot = FirebaseFirestore.getInstance()
                .collection("couples").document(coupleId)
                .collection("timeCapsules")
                .whereEqualTo("opened", false)
                .get()
                .await()

            val now = System.currentTimeMillis()
            for (doc in snapshot.documents) {
                val unlockAt = doc.getTimestamp("unlockAt")?.toDate()?.time ?: continue
                if (unlockAt <= now) {
                    val title = doc.getString("title") ?: "A capsule"
                    if (!AppVisibility.appVisible) {
                        Notifier.notifyTimeCapsuleReady(context, title)
                    }
                }
            }
        } catch (_: Exception) {
            // Best effort — will be retried by WorkManager
        }
        return Result.success()
    }
}
