package com.lovenote.app.chat

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val senderUid: String = "",
    val type: String = "text",
    val body: String = "",
    val sentAt: Timestamp? = null,
) {
    fun isMine(uid: String): Boolean = senderUid == uid

    fun toMap(): Map<String, Any?> = mapOf(
        "senderUid" to senderUid,
        "type" to type,
        "body" to body,
        "sentAt" to sentAt,
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): Message = Message(
            id = id,
            senderUid = map["senderUid"] as? String ?: "",
            type = map["type"] as? String ?: "text",
            body = map["body"] as? String ?: "",
            sentAt = map["sentAt"] as? Timestamp,
        )
    }
}
