package com.lovenote.app.settings

/** Release notes shown in the "What's new" dialog after each update. */
object Changelog {
    private val NOTES = mapOf(
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
