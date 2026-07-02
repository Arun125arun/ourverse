package com.lovenote.app.us

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

data class Mood(val emoji: String, val dateKey: String)

data class CoupleEvent(
    val id: String,
    val title: String,
    val dateMillis: Long,
)

class UsRepository(
    private val coupleId: String,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    val myUid: String
        get() = auth.currentUser?.uid ?: error("Not signed in")

    private val coupleRef get() = db.collection("couples").document(coupleId)

    // --- Mood check-in (stored on the couple doc) ---

    suspend fun setMood(emoji: String) {
        coupleRef.update(
            "moods.$myUid",
            mapOf("emoji" to emoji, "dateKey" to Questions.dateKey()),
        ).await()
    }

    /** uid → mood; callers filter for today's dateKey. */
    fun moods(): Flow<Map<String, Mood>> =
        coupleRef.snapshots().map { doc ->
            (doc.get("moods") as? Map<*, *>).orEmpty()
                .entries
                .mapNotNull { (k, v) ->
                    val uid = k as? String ?: return@mapNotNull null
                    val m = v as? Map<*, *> ?: return@mapNotNull null
                    val emoji = m["emoji"] as? String ?: return@mapNotNull null
                    val dateKey = m["dateKey"] as? String ?: return@mapNotNull null
                    uid to Mood(emoji, dateKey)
                }
                .toMap()
        }

    // --- Daily question ---

    suspend fun submitAnswer(dateKey: String, answer: String) {
        val text = answer.trim()
        if (text.isEmpty()) return
        coupleRef.collection("daily").document(dateKey)
            .set(mapOf("answers" to mapOf(myUid to text)), SetOptions.merge())
            .await()
    }

    /** uid → answer for the given day. */
    fun answers(dateKey: String): Flow<Map<String, String>> =
        coupleRef.collection("daily").document(dateKey).snapshots()
            .map { doc ->
                (doc.get("answers") as? Map<*, *>).orEmpty()
                    .entries
                    .mapNotNull { (k, v) ->
                        val uid = k as? String ?: return@mapNotNull null
                        val text = v as? String ?: return@mapNotNull null
                        uid to text
                    }
                    .toMap()
            }

    // --- Special dates ---

    suspend fun addEvent(title: String, dateMillis: Long) {
        val name = title.trim()
        if (name.isEmpty()) return
        coupleRef.collection("events").add(
            mapOf(
                "title" to name,
                "dateMillis" to dateMillis,
                "createdBy" to myUid,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    suspend fun deleteEvent(id: String) {
        coupleRef.collection("events").document(id).delete().await()
    }

    fun events(): Flow<List<CoupleEvent>> =
        coupleRef.collection("events")
            .orderBy("dateMillis", Query.Direction.ASCENDING)
            .limit(50)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val millis = doc.getLong("dateMillis") ?: return@mapNotNull null
                    CoupleEvent(doc.id, title, millis)
                }
            }
}
