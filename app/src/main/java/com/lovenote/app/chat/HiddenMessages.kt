package com.lovenote.app.chat

import android.content.Context

/** "Delete for me": message ids hidden locally on this phone only. */
object HiddenMessages {
    private fun prefs(context: Context) =
        context.getSharedPreferences("hidden_messages", Context.MODE_PRIVATE)

    fun load(context: Context): Set<String> =
        prefs(context).getStringSet("ids", emptySet()) ?: emptySet()

    fun hide(context: Context, id: String): Set<String> {
        val updated = load(context) + id
        prefs(context).edit().putStringSet("ids", updated).apply()
        return updated
    }
}
