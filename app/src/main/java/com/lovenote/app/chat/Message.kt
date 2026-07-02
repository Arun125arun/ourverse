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
) {
    val seen: Boolean get() = seenAt != null

    val isPhoto: Boolean get() = type == "photo"

    fun isMine(uid: String): Boolean = senderUid == uid

    fun toMap(): Map<String, Any?> = mapOf(
        "senderUid" to senderUid,
        "type" to type,
        "body" to body,
        "sentAt" to sentAt,
        "seenAt" to seenAt,
        "reactions" to reactions,
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
        )
    }
}
