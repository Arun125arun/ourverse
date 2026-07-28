package com.lovenote.app.chat

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
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

data class PartnerProfile(
    val name: String,
    val photoUrl: String,
    val lastActiveMillis: Long?,
)

class ChatRepository(
    private val coupleId: String,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    companion object {
        private const val PAGE_SIZE = 50L
    }
    val myUid: String
        get() = auth.currentUser?.uid ?: error("Not signed in")

    private val coupleRef
        get() = db.collection("couples").document(coupleId)

    private val messagesRef
        get() = coupleRef.collection("messages")

    private var oldestCursor: DocumentSnapshot? = null
    private var hasMore = true

    /** Reset pagination cursor (call when the chat screen opens fresh). */
    fun resetPagination() {
        oldestCursor = null
        hasMore = true
    }

    /** Whether more older messages may exist. */
    fun canLoadMore(): Boolean = hasMore

    /**
     * Fetches the next batch of older messages (oldest first in Firestore, but
     * the caller prepends them so the newest-first list grows at the tail).
     * Returns the loaded messages (empty list when there are no more).
     */
    suspend fun loadOlderMessages(): List<Message> {
        val cursor = oldestCursor
        val baseQuery = messagesRef
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE)
        val query = if (cursor != null) baseQuery.startAfter(cursor) else baseQuery
        val snapshot = query.get().await()
        oldestCursor = snapshot.documents.lastOrNull()
        hasMore = snapshot.documents.size.toLong() == PAGE_SIZE
        return snapshot.documents.map { doc ->
            Message.fromMap(doc.id, doc.data ?: emptyMap())
        }
    }

    // Members-only doc for typing/mood/anniversary (the couple doc itself is
    // readable during pairing, so private state lives here instead).
    private val stateRef
        get() = coupleRef.collection("state").document("shared")

    suspend fun send(text: String, replyTo: Message? = null) {
        val body = text.trim()
        if (body.isEmpty()) return
        messagesRef.add(
            mapOf(
                "senderUid" to myUid,
                "type" to "text",
                "body" to body,
                "replyToId" to replyTo?.id,
                "replyText" to replyTo?.let { Message.preview(it) },
                "replySender" to replyTo?.senderUid,
                "sentAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    suspend fun sendPhoto(base64Jpeg: String, once: Boolean = false) {
        messagesRef.add(
            mapOf(
                "senderUid" to myUid,
                "type" to "photo",
                "body" to base64Jpeg,
                "once" to once,
                "sentAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    suspend fun sendVoice(base64Audio: String, durationSec: Long) {
        messagesRef.add(
            mapOf(
                "senderUid" to myUid,
                "type" to "voice",
                "body" to base64Audio,
                "durationSec" to durationSec,
                "sentAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    private fun gameLabel(gameType: String): String = when (gameType) {
        "tictactoe" -> "Tic Tac Toe"
        "ludo" -> "Ludo"
        "truthdare" -> "Truth or Dare"
        "wordgame" -> "Word Game"
        else -> "Game"
    }

    suspend fun sendGameInvite(gameId: String, gameType: String) {
        val label = gameLabel(gameType)
        messagesRef.add(
            mapOf(
                "senderUid" to myUid,
                "type" to "game_invite",
                "body" to "\uD83C\uDFAE Let's play $label!",
                "gameId" to gameId,
                "gameType" to gameType,
                "sentAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    suspend fun sendGameEnd(gameId: String, gameType: String, result: String) {
        val label = gameLabel(gameType)
        messagesRef.add(
            mapOf(
                "senderUid" to myUid,
                "type" to "text",
                "body" to "\uD83C\uDFAE $label ended! $result",
                "sentAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    /** Destroys a view-once photo's content after the partner has seen it. */
    suspend fun consumeOncePhoto(messageId: String) {
        messagesRef.document(messageId).update("body", "").await()
    }

    /** Stamps seenAt on every unseen partner message (read receipts). */
    suspend fun markPartnerMessagesSeen(messages: List<Message>) {
        val unseen = messages.filter { !it.isMine(myUid) && !it.seen }.take(500)
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

    /** One-shot fetch of the partner's newest message (for background checks). */
    suspend fun fetchLatestFromPartner(): Message? =
        messagesRef
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .await()
            .documents
            .map { Message.fromMap(it.id, it.data ?: emptyMap()) }
            .firstOrNull { !it.isMine(myUid) }

    suspend fun delete(messageId: String) {
        messagesRef.document(messageId).delete().await()
    }

    suspend fun edit(messageId: String, newText: String) {
        val body = newText.trim()
        if (body.isEmpty()) return
        messagesRef.document(messageId).update(
            mapOf("body" to body, "editedAt" to FieldValue.serverTimestamp()),
        ).await()
    }

    suspend fun setAnniversary(millis: Long) {
        stateRef.set(mapOf("anniversaryMillis" to millis), SetOptions.merge()).await()
    }

    /** Epoch millis of the couple's "together since" date, or null if unset. */
    fun anniversaryMillis(): Flow<Long?> =
        combine(stateRef.snapshots(), coupleRef.snapshots()) { state, couple ->
            // legacy fallback: early versions stored this on the couple doc
            state.getLong("anniversaryMillis") ?: couple.getLong("anniversaryMillis")
        }.fallbackTo(null)

    /** Firestore uid of the partner, or null while unpaired. */
    fun partnerUid(): Flow<String?> =
        coupleRef.snapshots()
            .map { doc ->
                (doc.get("members") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.firstOrNull { it != myUid }
            }
            .fallbackTo(null)
            .distinctUntilChanged()

    /** Live name/photo/presence of the partner. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun partnerProfile(): Flow<PartnerProfile?> =
        partnerUid().flatMapLatest { uid ->
            if (uid == null) {
                flowOf(null)
            } else {
                db.collection("users").document(uid).snapshots().map { doc ->
                    PartnerProfile(
                        name = doc.getString("displayName").orEmpty(),
                        photoUrl = doc.getString("photoUrl").orEmpty(),
                        lastActiveMillis = doc.getTimestamp("lastActiveAt")?.toDate()?.time,
                    )
                }
            }
        }.fallbackTo(null)

    fun myProfile(): Flow<PartnerProfile?> =
        db.collection("users").document(myUid).snapshots().map { doc ->
            PartnerProfile(
                name = doc.getString("displayName").orEmpty(),
                photoUrl = doc.getString("photoUrl").orEmpty(),
                lastActiveMillis = doc.getTimestamp("lastActiveAt")?.toDate()?.time,
            )
        }.fallbackTo(null)

    /** Called periodically while the app is open so the partner sees presence. */
    suspend fun heartbeatPresence() {
        db.collection("users").document(myUid)
            .set(mapOf("lastActiveAt" to FieldValue.serverTimestamp()), SetOptions.merge())
            .await()
    }

    /** Sets or clears (emoji == null) my reaction on a message. */
    suspend fun react(messageId: String, emoji: String?) {
        messagesRef.document(messageId)
            .update("reactions.$myUid", emoji ?: FieldValue.delete())
            .await()
    }

    /** Heartbeat written while I'm typing; partner shows it briefly. */
    suspend fun setTyping() {
        stateRef.set(
            mapOf("typing" to mapOf(myUid to FieldValue.serverTimestamp())),
            SetOptions.merge(),
        ).await()
    }

    /** Epoch millis of the partner's latest typing heartbeat, or null. */
    fun partnerTypingAt(): Flow<Long?> =
        stateRef.snapshots()
            .map { doc ->
                val typing = doc.get("typing") as? Map<*, *> ?: return@map null
                typing.entries
                    .firstOrNull { it.key != myUid }
                    ?.let { (it.value as? com.google.firebase.Timestamp)?.toDate()?.time }
            }
            .fallbackTo(null)

    /** Newest first (matches a reversed LazyColumn). */
    fun messages(): Flow<List<Message>> =
        messagesRef
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    Message.fromMap(doc.id, doc.data ?: emptyMap())
                }
            }
            .fallbackTo(emptyList())
}
