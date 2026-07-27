package com.lovenote.app.chat

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val senderUid: String = "",
    val type: String = "text",
    val body: String = "",
    val sentAt: Timestamp? = null,
    val seenAt: Timestamp? = null,
    val reactions: Map<String, String> = emptyMap(),
    val durationSec: Long? = null,
    val once: Boolean = false,
    val editedAt: Timestamp? = null,
    val replyToId: String? = null,
    val replyText: String? = null,
    val replySender: String? = null,
    val gameId: String = "",
    val gameType: String = "",
) {
    val seen: Boolean get() = seenAt != null

    val edited: Boolean get() = editedAt != null

    val isPhoto: Boolean get() = type == "photo"

    val isVoice: Boolean get() = type == "voice"

    val isGameInvite: Boolean get() = type == "game_invite"

    /** A view-once photo whose content has already been destroyed. */
    val onceConsumed: Boolean get() = once && body.isEmpty()

    fun isMine(uid: String): Boolean = senderUid == uid

    fun toMap(): Map<String, Any?> = mapOf(
        "senderUid" to senderUid,
        "type" to type,
        "body" to body,
        "sentAt" to sentAt,
        "seenAt" to seenAt,
        "reactions" to reactions,
        "durationSec" to durationSec,
        "once" to once,
        "editedAt" to editedAt,
        "replyToId" to replyToId,
        "replyText" to replyText,
        "replySender" to replySender,
        "gameId" to gameId,
        "gameType" to gameType,
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): Message = Message(
            id = id,
            senderUid = map["senderUid"] as? String ?: "",
            type = map["type"] as? String ?: "text",
            body = map["body"] as? String ?: "",
            sentAt = map["sentAt"] as? Timestamp,
            seenAt = map["seenAt"] as? Timestamp,
            reactions = (map["reactions"] as? Map<*, *>)
                .orEmpty()
                .entries
                .mapNotNull { (k, v) ->
                    val key = k as? String ?: return@mapNotNull null
                    val value = v as? String ?: return@mapNotNull null
                    key to value
                }
                .toMap(),
            durationSec = (map["durationSec"] as? Number)?.toLong(),
            once = map["once"] as? Boolean ?: false,
            editedAt = map["editedAt"] as? Timestamp,
            replyToId = map["replyToId"] as? String,
            replyText = map["replyText"] as? String,
            replySender = map["replySender"] as? String,
            gameId = map["gameId"] as? String ?: "",
            gameType = map["gameType"] as? String ?: "",
        )

        /** Short summary used when quoting a message in a reply. */
        fun preview(message: Message): String = when {
            message.isPhoto -> "📷 Photo"
            message.isVoice -> "🎤 Voice note"
            message.isGameInvite -> "🎮 ${message.gameType} game"
            else -> message.body.take(60)
        }
    }
}
