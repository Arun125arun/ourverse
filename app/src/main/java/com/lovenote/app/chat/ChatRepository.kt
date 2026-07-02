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
