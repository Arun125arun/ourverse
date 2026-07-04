package com.lovenote.app.settings

/** Release notes shown in the "What's new" dialog after each update. */
object Changelog {
    private val NOTES = mapOf(
        33L to listOf(
            "❤ Hold the heart in the chat header to customize what it sends",
        ),
        32L to listOf(
            "🔒 Security hardening — invite codes can no longer be discovered by others",
            "📞 Incoming calls now ring even when the app is closed",
            "🧹 Under-the-hood cleanups: voice playback, listeners, dead code",
        ),
        31L to listOf(
            "↩ Reply to messages — swipe any message right, just like WhatsApp",
            "💬 Replies show a quoted preview of the original",
        ),
        30L to listOf(
            "💬 Beautiful new message menu — bouncy reactions, clean actions",
            "🗑 Delete partner's messages for yourself",
            "📋 Copy messages to clipboard",
            "🎨 Draw-a-note button moved below the note",
        ),
        29L to listOf(
            "✏ Edit sent messages — long-press your message → Edit",
            "Edited messages show a small “edited” label",
        ),
        28L to listOf(
            "📷 Camera flip in video calls now toggles front ↔ back correctly",
            "🪞 Selfie preview only mirrors on the front camera",
        ),
        27L to listOf(
            "📞 Proper call buttons — real icons for mute, speaker, flip & end",
            "📺 Screen sharing during video calls",
            "✉ Tap the developer email in About to write directly",
        ),
        26L to listOf(
            "📞 Voice & video calls — talk live, free, just you two",
            "⚡ Instant notifications — no more 15-minute delays",
            "📋 Shared to-do list with partner reminders (Us tab)",
        ),
        25L to listOf(
            "🎨 Draw notes! Sketch with your finger and it appears on their widget",
            "🖼 Doodles show on the widget, wallpaper, and note history",
        ),
        24L to listOf(
            "🛡 Crash reporting — problems get found and fixed faster",
            "🔔 New in Settings: one tap to make notifications reliable",
            "🌐 Web version now works offline too",
        ),
        23L to listOf(
            "✨ This screen! After every update you'll see what changed",
            "💬 Smaller, tidier chat bubbles and header",
        ),
        22L to listOf(
            "⚡ Much smoother chat scrolling",
            "📦 App size cut from 16 MB to under 4 MB",
        ),
    )

    fun notesFor(versionCode: Long): List<String> =
        NOTES[versionCode] ?: listOf("💖 Bug fixes and little improvements")
}
