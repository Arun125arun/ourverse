package com.lovenote.app.notes

import com.google.firebase.Timestamp

data class Note(
    val id: String = "",
    val senderUid: String = "",
    val text: String = "",
    val style: String = DEFAULT_STYLE,
    val sentAt: Timestamp? = null,
    val doodle: String? = null,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "senderUid" to senderUid,
        "text" to text,
        "style" to style,
        "sentAt" to sentAt,
        "doodle" to doodle,
    )

    companion object {
        val STYLES = listOf("peach", "rose", "sky", "mint", "lavender")
        const val DEFAULT_STYLE = "peach"
        const val MAX_LENGTH = 140

        fun fromMap(id: String, map: Map<String, Any?>): Note {
            val rawStyle = map["style"] as? String
            return Note(
                id = id,
                senderUid = map["senderUid"] as? String ?: "",
                text = map["text"] as? String ?: "",
                style = if (rawStyle in STYLES) rawStyle!! else DEFAULT_STYLE,
                sentAt = map["sentAt"] as? Timestamp,
                doodle = map["doodle"] as? String,
            )
        }
    }
}
