package com.lovenote.app.us

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

data class Mood(val emoji: String, val dateKey: String)

data class Profile(val name: String, val photoUrl: String)

data class QuizEntry(val answer: Int, val guess: Int)

data class Memory(
    val id: String,
    val title: String,
    val photoBase64: String?,
    val dateMillis: Long,
)

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

    // Members-only doc shared with ChatRepository (typing/anniversary/moods).
    private val stateRef get() = coupleRef.collection("state").document("shared")

    // --- Mood check-in ---

    suspend fun setMood(emoji: String) {
        stateRef.set(
            mapOf("moods" to mapOf(myUid to mapOf("emoji" to emoji, "dateKey" to Questions.dateKey()))),
            SetOptions.merge(),
        ).await()
    }

    /** uid → mood; callers filter for today's dateKey. */
    fun moods(): Flow<Map<String, Mood>> =
        stateRef.snapshots().map { doc ->
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

    // --- Profiles for the hero header ---

    fun myProfile(): Flow<Profile?> = profileOf(myUid)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun partnerProfile(): Flow<Profile?> =
        coupleRef.snapshots()
            .map { doc ->
                (doc.get("members") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.firstOrNull { it != myUid }
            }
            .distinctUntilChanged()
            .flatMapLatest { uid -> if (uid == null) flowOf(null) else profileOf(uid) }

    private fun profileOf(uid: String): Flow<Profile?> =
        db.collection("users").document(uid).snapshots().map { doc ->
            Profile(
                name = doc.getString("displayName").orEmpty(),
                photoUrl = doc.getString("photoUrl").orEmpty(),
            )
        }

    /** "Together since" date, shared with the chat header and settings. */
    fun anniversaryMillis(): Flow<Long?> =
        stateRef.snapshots().map { it.getLong("anniversaryMillis") }

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

    // --- Couple quiz ---

    /** My own pick plus my guess at the partner's pick, both option indexes. */
    suspend fun submitQuiz(dateKey: String, myAnswer: Int, guessForPartner: Int) {
        coupleRef.collection("quiz").document(dateKey)
            .set(
                mapOf(myUid to mapOf("answer" to myAnswer, "guess" to guessForPartner)),
                SetOptions.merge(),
            )
            .await()
    }

    /** uid → (answer, guess) for the given day. */
    fun quizEntries(dateKey: String): Flow<Map<String, QuizEntry>> =
        coupleRef.collection("quiz").document(dateKey).snapshots()
            .map { doc ->
                (doc.data ?: emptyMap())
                    .entries
                    .mapNotNull { (k, v) ->
                        val m = v as? Map<*, *> ?: return@mapNotNull null
                        val answer = (m["answer"] as? Number)?.toInt() ?: return@mapNotNull null
                        val guess = (m["guess"] as? Number)?.toInt() ?: return@mapNotNull null
                        k to QuizEntry(answer, guess)
                    }
                    .toMap()
            }

    // --- Memories ("Our story") ---

    suspend fun addMemory(title: String, dateMillis: Long, photoBase64: String?) {
        val name = title.trim()
        if (name.isEmpty()) return
        coupleRef.collection("memories").add(
            mapOf(
                "title" to name,
                "photo" to photoBase64,
                "dateMillis" to dateMillis,
                "createdBy" to myUid,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    suspend fun deleteMemory(id: String) {
        coupleRef.collection("memories").document(id).delete().await()
    }

    /** Newest moment first. */
    fun memories(): Flow<List<Memory>> =
        coupleRef.collection("memories")
            .orderBy("dateMillis", Query.Direction.DESCENDING)
            .limit(100)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val millis = doc.getLong("dateMillis") ?: return@mapNotNull null
                    Memory(doc.id, title, doc.getString("photo"), millis)
                }
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
