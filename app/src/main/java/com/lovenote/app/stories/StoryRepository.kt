package com.lovenote.app.stories

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import com.lovenote.app.common.fallbackTo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date

data class Story(
    val id: String,
    val senderUid: String,
    val senderName: String,
    val photoBase64: String,
    val caption: String,
    val createdAtMillis: Long,
    val expiresAtMillis: Long,
    val myReaction: String?,
    val partnerReaction: String?,
)

class StoryRepository(
    private val coupleId: String,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    val myUid: String
        get() = auth.currentUser?.uid ?: error("Not signed in")

    private val storiesRef get() = db.collection("couples").document(coupleId).collection("stories")

    suspend fun addStory(photoBase64: String, caption: String, expiryMillis: Long): String {
        val doc = storiesRef.add(
            mapOf(
                "senderUid" to myUid,
                "photo" to photoBase64,
                "caption" to caption.trim(),
                "createdAt" to FieldValue.serverTimestamp(),
                "expiresAt" to Timestamp(Date(expiryMillis)),
            ),
        ).await()
        return doc.id
    }

    /** All non-expired stories, newest first. */
    fun activeStories(): Flow<List<Story>> =
        storiesRef
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .snapshots()
            .map { snapshot ->
                val now = System.currentTimeMillis()
                snapshot.documents.mapNotNull { doc ->
                    val sender = doc.getString("senderUid") ?: return@mapNotNull null
                    val photo = doc.getString("photo") ?: return@mapNotNull null
                    val caption = doc.getString("caption").orEmpty()
                    val created = doc.getTimestamp("createdAt")?.toDate()?.time ?: return@mapNotNull null
                    val expires = doc.getTimestamp("expiresAt")?.toDate()?.time ?: (created + 24 * 3600 * 1000L)
                    if (expires < now) return@mapNotNull null
                    val reactions = doc.get("reactions") as? Map<*, *> ?: emptyMap<String, Any>()
                    val myReaction = reactions[myUid] as? String
                    val partnerReaction = reactions.entries.firstOrNull { it.key != myUid }?.value as? String
                    Story(doc.id, sender, "", photo, caption, created, expires, myReaction, partnerReaction)
                }
            }.fallbackTo(emptyList())

    /** Stories from partner only (for notifications). */
    fun partnerStories(): Flow<List<Story>> =
        activeStories().map { list -> list.filter { it.senderUid != myUid } }

    suspend fun reactToStory(id: String, emoji: String) {
        storiesRef.document(id).update("reactions.$myUid", emoji).await()
    }

    suspend fun deleteStory(id: String) {
        storiesRef.document(id).delete().await()
    }
}
