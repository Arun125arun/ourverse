package com.lovenote.app.vibe

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import com.lovenote.app.common.fallbackTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

data class SharedSong(
    val id: String,
    val uri: String,
    val source: String,
    val title: String,
    val artist: String,
    val albumArtUrl: String?,
    val audioUrl: String?,
    val sharedBy: String,
    val sharedAtMillis: Long,
    val reaction: String?,
)

data class Ritual(
    val id: String,
    val name: String,
    val description: String,
    val frequency: String,
    val actionType: String,
    val reminderTime: String,
    val reminderDays: List<Int>,
    val customPrompt: String?,
    val createdBy: String,
    val active: Boolean,
    val streak: Int,
    val longestStreak: Int,
    val lastCompletedAtMillis: Long?,
)

data class RitualLog(
    val id: String,
    val ritualId: String,
    val completedBy: String,
    val completedAtMillis: Long,
    val note: String?,
)

data class Profile(val name: String, val photoUrl: String)

class VibeRepository(
    private val coupleId: String,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    val myUid: String
        get() = auth.currentUser?.uid ?: error("Not signed in")

    private val coupleRef get() = db.collection("couples").document(coupleId)

    // ── Soundtrack ──

    suspend fun shareSong(
        uri: String,
        source: String,
        title: String,
        artist: String,
        albumArtUrl: String?,
        audioUrl: String? = null,
    ): String {
        val doc = coupleRef.collection("soundtrack").add(
            mapOf(
                "uri" to uri,
                "source" to source,
                "title" to title.trim(),
                "artist" to artist.trim(),
                "albumArtUrl" to albumArtUrl,
                "audioUrl" to audioUrl?.trim()?.ifBlank { null },
                "sharedBy" to myUid,
                "sharedAt" to FieldValue.serverTimestamp(),
                "reaction" to null,
            ),
        ).await()
        return doc.id
    }

    suspend fun reactToSong(songId: String, emoji: String?) {
        coupleRef.collection("soundtrack").document(songId)
            .update("reaction", emoji).await()
    }

    suspend fun deleteSong(songId: String) {
        coupleRef.collection("soundtrack").document(songId).delete().await()
    }

    fun songs(): Flow<List<SharedSong>> =
        coupleRef.collection("soundtrack")
            .orderBy("sharedAt", Query.Direction.DESCENDING)
            .limit(100)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val uri = doc.getString("uri") ?: return@mapNotNull null
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val artist = doc.getString("artist") ?: return@mapNotNull null
                    SharedSong(
                        id = doc.id,
                        uri = uri,
                        source = doc.getString("source") ?: "manual",
                        title = title,
                        artist = artist,
                        albumArtUrl = doc.getString("albumArtUrl"),
                        audioUrl = doc.getString("audioUrl"),
                        sharedBy = doc.getString("sharedBy") ?: "",
                        sharedAtMillis = doc.getTimestamp("sharedAt")?.toDate()?.time ?: 0L,
                        reaction = doc.getString("reaction"),
                    )
                }
            }.fallbackTo(emptyList())

    // ── Rituals ──

    suspend fun createRitual(
        name: String,
        description: String,
        frequency: String,
        actionType: String,
        reminderTime: String,
        reminderDays: List<Int>,
        customPrompt: String?,
    ): String {
        val doc = coupleRef.collection("rituals").add(
            mapOf(
                "name" to name.trim(),
                "description" to description.trim(),
                "frequency" to frequency,
                "actionType" to actionType,
                "reminderTime" to reminderTime,
                "reminderDays" to reminderDays,
                "customPrompt" to customPrompt?.trim(),
                "createdBy" to myUid,
                "createdAt" to FieldValue.serverTimestamp(),
                "active" to true,
                "streak" to 0,
                "longestStreak" to 0,
                "lastCompletedAt" to null,
            ),
        ).await()
        return doc.id
    }

    suspend fun toggleRitualActive(id: String, active: Boolean) {
        coupleRef.collection("rituals").document(id)
            .update("active", active).await()
    }

    suspend fun deleteRitual(id: String) {
        coupleRef.collection("rituals").document(id).delete().await()
    }

    suspend fun completeRitual(ritualId: String, note: String?) {
        val now = FieldValue.serverTimestamp()
        coupleRef.collection("rituals").document(ritualId)
            .update(
                mapOf(
                    "streak" to FieldValue.increment(1),
                    "lastCompletedAt" to now,
                ),
            ).await()
        coupleRef.collection("rituals").document(ritualId)
            .collection("logs").add(
                mapOf(
                    "completedBy" to myUid,
                    "completedAt" to now,
                    "note" to note?.trim(),
                ),
            ).await()
    }

    fun rituals(): Flow<List<Ritual>> =
        coupleRef.collection("rituals")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    Ritual(
                        id = doc.id,
                        name = name,
                        description = doc.getString("description").orEmpty(),
                        frequency = doc.getString("frequency") ?: return@mapNotNull null,
                        actionType = doc.getString("actionType") ?: return@mapNotNull null,
                        reminderTime = doc.getString("reminderTime") ?: return@mapNotNull null,
                        reminderDays = (doc.get("reminderDays") as? List<*>)?.filterIsInstance<Number>()?.map { it.toInt() } ?: emptyList(),
                        customPrompt = doc.getString("customPrompt"),
                        createdBy = doc.getString("createdBy") ?: "",
                        active = doc.getBoolean("active") ?: true,
                        streak = (doc.getLong("streak") ?: 0L).toInt(),
                        longestStreak = (doc.getLong("longestStreak") ?: 0L).toInt(),
                        lastCompletedAtMillis = doc.getTimestamp("lastCompletedAt")?.toDate()?.time,
                    )
                }
            }.fallbackTo(emptyList())

    fun ritualLogs(ritualId: String): Flow<List<RitualLog>> =
        coupleRef.collection("rituals").document(ritualId)
            .collection("logs")
            .orderBy("completedAt", Query.Direction.DESCENDING)
            .limit(50)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val completedBy = doc.getString("completedBy") ?: return@mapNotNull null
                    val millis = doc.getTimestamp("completedAt")?.toDate()?.time ?: 0L
                    RitualLog(
                        id = doc.id,
                        ritualId = ritualId,
                        completedBy = completedBy,
                        completedAtMillis = millis,
                        note = doc.getString("note"),
                    )
                }
            }.fallbackTo(emptyList())

    // ── Profiles ──

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
}
