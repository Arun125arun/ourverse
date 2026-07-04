package com.lovenote.app.notes

import android.content.Context
import com.google.firebase.Timestamp
import java.util.Date

/**
 * Latest partner note, cached in SharedPreferences. The home-screen widget
 * reads from here because widgets can't hold live Firestore listeners.
 */
object NoteCache {
    private const val PREFS = "note_widget_cache"

    fun save(context: Context, note: Note) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("text", note.text)
            .putString("style", note.style)
            .putString("doodle", note.doodle)
            .putLong("sentAtMillis", note.sentAt?.toDate()?.time ?: 0L)
            .apply()
    }

    fun load(context: Context): Note? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val text = prefs.getString("text", null) ?: return null
        val sentAtMillis = prefs.getLong("sentAtMillis", 0L)
        return Note(
            text = text,
            style = prefs.getString("style", Note.DEFAULT_STYLE) ?: Note.DEFAULT_STYLE,
            doodle = prefs.getString("doodle", null),
            sentAt = if (sentAtMillis > 0) Timestamp(Date(sentAtMillis)) else null,
        )
    }
}
