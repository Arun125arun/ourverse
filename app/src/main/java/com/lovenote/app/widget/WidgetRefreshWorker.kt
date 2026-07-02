package com.lovenote.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lovenote.app.notes.NoteCache
import com.lovenote.app.notes.NoteRepository
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.tasks.await

/** Fetches the partner's newest note and redraws the widget. */
class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val coupleId = FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .get().await()
                    .getString("coupleId")
                if (coupleId != null) {
                    NoteRepository(coupleId).fetchLatestFromPartner()
                        ?.let { NoteCache.save(applicationContext, it) }
                }
            }
        } catch (_: Exception) {
            // Offline or Firebase not configured yet — show the cached note.
        }
        NoteWidget().updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        private const val PERIODIC_WORK = "widget-refresh-periodic"
        private const val ONESHOT_WORK = "widget-refresh-now"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun refreshNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONESHOT_WORK,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build(),
            )
        }
    }
}
