package com.lovenote.app.settings

/** Release notes shown in the "What's new" dialog after each update. */
object Changelog {
    private val NOTES = mapOf(
        43L to listOf(
            "💌 Voice Letters — record and send voice notes with captions in the Us tab",
            "🎲 Question Roulette — spin the wheel and answer fun relationship questions",
            "⏰ Shared Countdown — set a countdown to your next special moment",
            "😊 Daily Pulse — add an emoji and one-word mood to your check-in",
            "🔥 Connection Streak — track how many days you've connected in a row",
            "📷 Memory Lane — a random memory from your story, shown daily",
        ),
        42L to listOf(
            "🎲 Classic Ludo — animated dice rolling with shake effect",
            "🏁 Realistic board — colored triangles, star markers, proper home columns",
            "🧭 Compact bottom nav — smaller, tighter, more screen space",
        ),
        41L to listOf(
            "🎨 Premium UI refresh — sleek black sign-in with red heart",
            "🧭 Bigger, cleaner bottom navigation bar",
            "⚙️ Game screens wired for online play with your partner",
        ),
        40L to listOf(
            "🎮 Online games — play with your partner even when you're apart",
            "💬 Game invites appear in chat — tap to join and play",
            "🎲 Full Ludo game with proper 4-home board",
            "✨ Sleek floating bottom nav — compact and modern",
            "💫 Floating reaction picker — bouncy emojis that pop in",
            "🧹 Couple Trivia removed — cleaner game selection",
        ),
        39L to listOf(
            "🎮 Couple Hub — brand-new hub for notes and games in one place",
            "❌ Tic Tac Toe — classic strategy game with score tracking",
            "🎲 Ludo — race-to-home dice game for two",
            "🧠 Couple Trivia — how well do you really know each other?",
            "🔥 Truth or Dare — spice things up with 18 truths and 18 dares",
            "🔤 Word Game — see how connected your minds are",
        ),
        38L to listOf(
            "💬 Fixed messages not showing after sending",
        ),
        37L to listOf(
            "📜 Scroll back to read older messages — chat now loads history on demand",
            "🗂 Cleaner code under the hood: extracted WebRTC helpers, removed dead code",
            "🎨 Pulsing heart empty states across chat, notes, and memories",
            "♿ Better accessibility — heart labels, semantic tags, proper button ripples",
            "⚡ Faster scrolling — photos decode off the main thread, cached formatters",
            "🧹 Cleaner chat UI — view-once messages use clean icons, no fire emojis",
        ),
        36L to listOf(
            "🔧 Fixed build issues for Compose/Material3 compatibility",
            "📦 Updated dependencies and resolved import issues",
            "⚙️ Internal improvements to app stability"
        ),
        35L to listOf(
            "📷 Camera photos are no longer sideways — rotation is now handled correctly",
            "⏱ Live call timer while you're connected",
            "📨 Waiting screen: tap the invite code to copy it, or share it in one tap",
            "🛡 The app no longer breaks if your partner deletes their account",
            "📞 Calls connect more reliably (old call leftovers are cleaned up)",
            "💖 Lots of little fixes: loading states, presence updates, smoother notes",
        ),
        34L to listOf(
            "❤ Hold the heart in the chat header to customize what it sends",
        ),
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
