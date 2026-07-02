package com.lovenote.app.chat

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ChatRepository(
    private val coupleId: String,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    val myUid: String
        get() = auth.currentUser?.uid ?: error("Not signed in")

    private val messagesRef
        get() = db.collection("couples").document(coupleId).collection("messages")

    suspend fun send(text: String) {
        val body = text.trim()
        if (body.isEmpty()) return
        messagesRef.add(
            mapOf(
                "senderUid" to myUid,
                "type" to "text",
                "body" to body,
                "sentAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    suspend fun sendPhoto(base64Jpeg: String) {
        messagesRef.add(
            mapOf(
                "senderUid" to myUid,
                "type" to "photo",
                "body" to base64Jpeg,
                "sentAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    /** Stamps seenAt on every unseen partner message (read receipts). */
    suspend fun markPartnerMessagesSeen(messages: List<Message>) {
        val unseen = messages.filter { !it.isMine(myUid) && !it.seen }
        if (unseen.isEmpty()) return
        val batch = db.batch()
        unseen.forEach { message ->
            batch.update(
                messagesRef.document(message.id),
                "seenAt",
                FieldValue.serverTimestamp(),
            )
        }
        batch.commit().await()
    }

    /** Sets or clears (emoji == null) my reaction on a message. */
    suspend fun react(messageId: String, emoji: String?) {
        messagesRef.document(messageId)
            .update("reactions.$myUid", emoji ?: FieldValue.delete())
            .await()
    }

    /** Heartbeat written while I'm typing; partner shows it briefly. */
    suspend fun setTyping() {
        db.collection("couples").document(coupleId)
            .update("typing.$myUid", FieldValue.serverTimestamp())
            .await()
    }

    /** Epoch millis of the partner's latest typing heartbeat, or null. */
    fun partnerTypingAt(): Flow<Long?> =
        db.collection("couples").document(coupleId).snapshots()
            .map { doc ->
                val typing = doc.get("typing") as? Map<*, *> ?: return@map null
                typing.entries
                    .firstOrNull { it.key != myUid }
                    ?.let { (it.value as? com.google.firebase.Timestamp)?.toDate()?.time }
            }

    /** Newest first (matches a reversed LazyColumn). */
    fun messages(): Flow<List<Message>> =
        messagesRef
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .limit(100)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    Message.fromMap(doc.id, doc.data ?: emptyMap())
                }
            }
}
