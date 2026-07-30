package com.lovenote.app.us

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.lovenote.app.common.fallbackTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

data class Mood(val emoji: String, val dateKey: String, val statusWord: String? = null)

data class Profile(val name: String, val photoUrl: String)

data class QuizEntry(val answer: Int, val guess: Int)

data class CountdownEvent(
    val id: String,
    val title: String,
    val targetMillis: Long,
    val createdBy: String,
)

data class Todo(
    val id: String,
    val title: String,
    val done: Boolean,
    val mine: Boolean,
)

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

data class VoiceLetter(
    val id: String,
    val senderUid: String,
    val audioBase64: String,
    val durationSec: Long,
    val caption: String,
    val createdAtMillis: Long,
)

enum class PingType(val emoji: String, val label: String) {
    HEART("❤️", "I love you"),
    HUG("🤗", "I need a hug"),
    THINKING("💭", "Thinking of you"),
    MISS("🥺", "I miss you"),
    KISS("💋", "Blow a kiss"),
    STAR("⭐", "You're amazing"),
    FOOD("🍕", "Let's eat"),
    MOVIE("🎬", "Movie night"),
}

data class Ping(
    val id: String,
    val senderUid: String,
    val type: PingType,
    val sentAtMillis: Long,
    val opened: Boolean = false,
)

data class TimeCapsule(
    val id: String,
    val senderUid: String,
    val title: String,
    val message: String,
    val photoBase64: String?,
    val unlockAtMillis: Long,
    val createdAtMillis: Long,
    val opened: Boolean = false,
    val openedAtMillis: Long? = null,
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

    suspend fun setMood(emoji: String, statusWord: String? = null) {
        stateRef.set(
            mapOf("moods" to mapOf(myUid to mapOf(
                "emoji" to emoji,
                "dateKey" to Questions.dateKey(),
                "statusWord" to statusWord,
            ))),
            SetOptions.merge(),
        ).await()
    }

    // --- Shared Color Theme ---

    data class CoupleColor(
        val name: String,
        val primary: Long,
        val container: Long,
    )

    val colorOptions = listOf(
        CoupleColor("Rose", 0xFFE53935, 0xFF1C1214),
        CoupleColor("Lavender", 0xFF7E57C2, 0xFF1A1425),
        CoupleColor("Ocean", 0xFF1E88E5, 0xFF0D1B2A),
        CoupleColor("Mint", 0xFF26A69A, 0xFF0D1F1D),
        CoupleColor("Amber", 0xFFFFA000, 0xFF1F1A0D),
        CoupleColor("Coral", 0xFFFF7043, 0xFF1F120D),
    )

    suspend fun setCoupleColor(colorName: String) {
        stateRef.set(mapOf("coupleColor" to colorName), SetOptions.merge()).await()
    }

    fun coupleColorName(): Flow<String?> =
        stateRef.snapshots().map { doc ->
            doc.getString("coupleColor")
        }.fallbackTo(null)

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
                    val statusWord = m["statusWord"] as? String
                    uid to Mood(emoji, dateKey, statusWord)
                }
                .toMap()
        }.fallbackTo(emptyMap())

    // --- Connection Streak ---

    /** Returns the current consecutive-day streak of at least one partner interacting. */
    fun connectionStreak(): Flow<Int> =
        combine(
            stateRef.snapshots(),
            coupleRef.collection("daily").snapshots(),
        ) { state, _ ->
            val moods = (state.get("moods") as? Map<*, *>).orEmpty()
            var streak = 0
            val cal = java.util.Calendar.getInstance()
            for (i in 0..365) {
                val key = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(cal.time)
                val hasActivity = moods.values.any { mood ->
                    val m = mood as? Map<*, *>
                    m?.get("dateKey") == key
                }
                if (hasActivity) {
                    streak++
                } else {
                    break
                }
                cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            }
            streak
        }.fallbackTo(0)

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
            .fallbackTo(null)
            .distinctUntilChanged()
            .flatMapLatest { uid -> if (uid == null) flowOf(null) else profileOf(uid) }

    private fun profileOf(uid: String): Flow<Profile?> =
        db.collection("users").document(uid).snapshots().map { doc ->
            Profile(
                name = doc.getString("displayName").orEmpty(),
                photoUrl = doc.getString("photoUrl").orEmpty(),
            )
        }.fallbackTo(null)

    /** "Together since" date, shared with the chat header and settings. */
    fun anniversaryMillis(): Flow<Long?> =
        combine(stateRef.snapshots(), coupleRef.snapshots()) { state, couple ->
            // legacy fallback: early versions stored this on the couple doc
            state.getLong("anniversaryMillis") ?: couple.getLong("anniversaryMillis")
        }.fallbackTo(null)

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
            }.fallbackTo(emptyMap())

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
            }.fallbackTo(emptyMap())

    // --- Shared to-do list ---

    suspend fun addTodo(title: String) {
        val name = title.trim()
        if (name.isEmpty()) return
        coupleRef.collection("todos").add(
            mapOf(
                "title" to name,
                "done" to false,
                "createdBy" to myUid,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    suspend fun setTodoDone(id: String, done: Boolean) {
        coupleRef.collection("todos").document(id).update("done", done).await()
    }

    suspend fun deleteTodo(id: String) {
        coupleRef.collection("todos").document(id).delete().await()
    }

    fun todos(): Flow<List<Todo>> =
        coupleRef.collection("todos")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    Todo(
                        id = doc.id,
                        title = title,
                        done = doc.getBoolean("done") ?: false,
                        mine = doc.getString("createdBy") == myUid,
                    )
                }.sortedBy { it.done }
            }.fallbackTo(emptyList())

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
            }.fallbackTo(emptyList())

    // --- Memory Lane (random daily memory) ---

    /** Returns a random memory for today's "Memory Lane" feature. */
    suspend fun randomMemoryForToday(): Memory? {
        val snapshot = coupleRef.collection("memories")
            .orderBy("dateMillis", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()
        val memories = snapshot.documents.mapNotNull { doc ->
            val title = doc.getString("title") ?: return@mapNotNull null
            val millis = doc.getLong("dateMillis") ?: return@mapNotNull null
            Memory(doc.id, title, doc.getString("photo"), millis)
        }
        if (memories.isEmpty()) return null
        // Use today's date as seed so the same memory shows all day
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
        val seed = today.hashCode().toLong()
        return memories[(seed % memories.size).toInt().coerceAtLeast(0)]
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
            }.fallbackTo(emptyList())

    // --- Voice Letters ---

    suspend fun sendVoiceLetter(audioBase64: String, durationSec: Long, caption: String) {
        if (audioBase64.isEmpty()) return
        coupleRef.collection("voiceLetters").add(
            mapOf(
                "senderUid" to myUid,
                "audioBase64" to audioBase64,
                "durationSec" to durationSec,
                "caption" to caption.trim(),
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    fun voiceLetters(): Flow<List<VoiceLetter>> =
        coupleRef.collection("voiceLetters")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val sender = doc.getString("senderUid") ?: return@mapNotNull null
                    val audio = doc.getString("audioBase64") ?: return@mapNotNull null
                    val dur = (doc.getLong("durationSec")) ?: return@mapNotNull null
                    val caption = doc.getString("caption").orEmpty()
                    val created = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                    VoiceLetter(doc.id, sender, audio, dur, caption, created)
                }
            }.fallbackTo(emptyList())

    suspend fun deleteVoiceLetter(id: String) {
        coupleRef.collection("voiceLetters").document(id).delete().await()
    }

    // --- Question Roulette ---

    val rouletteQuestions: List<String> = listOf(
        "What's your favorite memory of us?",
        "If we could travel anywhere right now, where would we go?",
        "What's something I do that makes you smile?",
        "What song reminds you of our relationship?",
        "What's one thing you want us to try together?",
        "When did you first know you loved me?",
        "What's your favorite thing about our relationship?",
        "What's a goal you have for us this year?",
        "What's the most adventurous thing we've done?",
        "How do you feel when we're together?",
        "What's your favorite way to spend a lazy Sunday with me?",
        "What's something you admire about me?",
        "What's a small thing that means a lot to you?",
        "If we had 24 hours together with no plans, what would we do?",
        "What's your favorite thing I've cooked for you?",
        "What made you fall for me?",
        "What's a tradition you want us to keep?",
        "What's something that always makes us laugh together?",
        "What's your dream date night?",
        "What's one thing you'd change about our routine?",
        "What's the best surprise I've given you?",
        "How has our relationship changed your life?",
        "What's your favorite photo of us and why?",
        "What's a skill you'd love to learn together?",
        "What makes our relationship unique?",
    )

    suspend fun nextRouletteQuestion(): String? {
        val snapshot = coupleRef.collection("roulette").document("state").get().await()
        val used = (snapshot.get("usedIndices") as? List<*>)
            ?.filterIsInstance<Number>()?.map { it.toInt() }?.toMutableSet() ?: mutableSetOf()
        val total = rouletteQuestions.size
        val available = (0 until total) - used
        if (available.isEmpty()) {
            coupleRef.collection("roulette").document("state")
                .set(mapOf("usedIndices" to emptyList<Any>()), SetOptions.merge()).await()
            return rouletteQuestions.random()
        }
        val idx = available.random()
        used.add(idx)
        coupleRef.collection("roulette").document("state")
            .set(mapOf("usedIndices" to used.toList()), SetOptions.merge()).await()
        return rouletteQuestions[idx]
    }

    suspend fun submitRouletteAnswer(questionIndex: Int, answer: String) {
        val text = answer.trim()
        if (text.isEmpty()) return
        coupleRef.collection("roulette").document("state")
            .set(
                mapOf("answers" to mapOf(myUid to mapOf("questionIndex" to questionIndex, "answer" to text))),
                SetOptions.merge(),
            ).await()
    }

    fun rouletteState(): Flow<Map<String, Any?>> =
        coupleRef.collection("roulette").document("state").snapshots()
            .map { doc -> doc.data ?: emptyMap<String, Any?>() }
            .fallbackTo(emptyMap())

    // --- Shared Countdown ---

    suspend fun setCountdown(title: String, targetMillis: Long) {
        stateRef.set(
            mapOf("countdown" to mapOf(
                "title" to title.trim(),
                "targetMillis" to targetMillis,
                "createdBy" to myUid,
            )),
            SetOptions.merge(),
        ).await()
    }

    fun countdownEvent(): Flow<CountdownEvent?> =
        stateRef.snapshots().map { doc ->
            val cd = doc.get("countdown") as? Map<*, *> ?: return@map null
            val title = cd["title"] as? String ?: return@map null
            val target = (cd["targetMillis"] as? Number)?.toLong() ?: return@map null
            val creator = cd["createdBy"] as? String ?: ""
            CountdownEvent("countdown", title, target, creator)
        }.fallbackTo(null)

    suspend fun clearCountdown() {
        stateRef.set(mapOf("countdown" to FieldValue.delete()), SetOptions.merge()).await()
    }

    // --- One-Tap Pings ---

    suspend fun sendPing(type: PingType): String {
        val doc = coupleRef.collection("pings").add(
            mapOf(
                "senderUid" to myUid,
                "type" to type.name,
                "sentAt" to FieldValue.serverTimestamp(),
                "opened" to false,
            ),
        ).await()
        return doc.id
    }

    fun pings(): Flow<List<Ping>> =
        coupleRef.collection("pings")
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .limit(50)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val sender = doc.getString("senderUid") ?: return@mapNotNull null
                    val typeName = doc.getString("type") ?: return@mapNotNull null
                    val type = try { PingType.valueOf(typeName) } catch (_: Exception) { return@mapNotNull null }
                    val millis = doc.getTimestamp("sentAt")?.toDate()?.time ?: 0L
                    val opened = doc.getBoolean("opened") ?: false
                    Ping(doc.id, sender, type, millis, opened)
                }
            }.fallbackTo(emptyList())

    suspend fun openPing(id: String) {
        coupleRef.collection("pings").document(id).update("opened", true).await()
    }

    /** True if the partner has unread pings. */
    fun hasUnreadPings(): Flow<Boolean> =
        coupleRef.collection("pings")
            .whereNotEqualTo("senderUid", myUid)
            .whereEqualTo("opened", false)
            .snapshots()
            .map { it.size() > 0 }
            .fallbackTo(false)

    // --- Time Capsule Messages ---

    suspend fun sendTimeCapsule(
        title: String,
        message: String,
        photoBase64: String?,
        unlockAtMillis: Long,
    ): String {
        val t = title.trim().ifEmpty { "A capsule from me" }
        val doc = coupleRef.collection("timeCapsules").add(
            mapOf(
                "senderUid" to myUid,
                "title" to t,
                "message" to message.trim(),
                "photo" to photoBase64,
                "unlockAt" to com.google.firebase.Timestamp(java.util.Date(unlockAtMillis)),
                "createdAt" to FieldValue.serverTimestamp(),
                "opened" to false,
            ),
        ).await()
        return doc.id
    }

    fun timeCapsules(): Flow<List<TimeCapsule>> =
        coupleRef.collection("timeCapsules")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val sender = doc.getString("senderUid") ?: return@mapNotNull null
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val message = doc.getString("message").orEmpty()
                    val unlockAt = doc.getTimestamp("unlockAt")?.toDate()?.time ?: return@mapNotNull null
                    val created = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                    val opened = doc.getBoolean("opened") ?: false
                    val openedAt = doc.getTimestamp("openedAt")?.toDate()?.time
                    TimeCapsule(doc.id, sender, title, message, doc.getString("photo"), unlockAt, created, opened, openedAt)
                }
            }.fallbackTo(emptyList())

    suspend fun openTimeCapsule(id: String) {
        coupleRef.collection("timeCapsules").document(id).update(
            mapOf("opened" to true, "openedAt" to FieldValue.serverTimestamp()),
        ).await()
    }

    fun sealedCapsules(): Flow<List<TimeCapsule>> =
        timeCapsules().map { list -> list.filter { !it.opened } }

    fun openedCapsules(): Flow<List<TimeCapsule>> =
        timeCapsules().map { list -> list.filter { it.opened } }
}
