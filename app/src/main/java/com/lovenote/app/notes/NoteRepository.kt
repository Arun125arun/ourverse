package com.lovenote.app.notes

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class NoteRepository(
    private val coupleId: String,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val myUid: String
        get() = auth.currentUser?.uid ?: error("Not signed in")

    private val notesRef
        get() = db.collection("couples").document(coupleId).collection("notes")

    suspend fun send(text: String, style: String) {
        val body = text.trim().take(Note.MAX_LENGTH)
        if (body.isEmpty()) return
        notesRef.add(
            mapOf(
                "senderUid" to myUid,
                "text" to body,
                "style" to if (style in Note.STYLES) style else Note.DEFAULT_STYLE,
                "sentAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    /**
     * Newest note written by the partner. Fetches the last few notes and
     * filters client-side, which avoids a composite Firestore index.
     */
    fun latestFromPartner(): Flow<Note?> =
        notesRef
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .limit(10)
            .snapshots()
            .map { snapshot ->
                snapshot.documents
                    .map { Note.fromMap(it.id, it.data ?: emptyMap()) }
                    .firstOrNull { it.senderUid != myUid }
            }

    /** One-shot variant of [latestFromPartner] for background widget refresh. */
    suspend fun fetchLatestFromPartner(): Note? =
        notesRef
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .await()
            .documents
            .map { Note.fromMap(it.id, it.data ?: emptyMap()) }
            .firstOrNull { it.senderUid != myUid }
}
