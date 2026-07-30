package com.lovenote.app.us

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * The daily-question deck. The question for a given day is derived from the
 * UTC date, so both partners always see the same one.
 */
object Questions {
    private val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    val ALL = listOf(
        "What's a small thing I do that makes you feel loved?",
        "What was your first impression of me, honestly?",
        "Which memory of us do you replay the most?",
        "What's something you've always wanted us to try together?",
        "What song makes you think of me?",
        "If we could teleport anywhere right now, where would you take us?",
        "What do you think is my hidden talent?",
        "What's one thing you'd love me to do more often?",
        "What's your favorite photo of us, and why?",
        "When did you first realize you liked me?",
        "What's a dream you've never told me about?",
        "What food reminds you of home?",
        "If we had a whole day with no responsibilities, how would we spend it?",
        "What's something I taught you without realizing it?",
        "Which of my habits secretly makes you smile?",
        "What's the best gift you've ever received — from anyone?",
        "What are you most proud of from this past year?",
        "What's one fear you'd like to conquer together?",
        "If our love story were a movie, what would it be called?",
        "What's your idea of a perfect lazy Sunday with me?",
        "What did you want to be when you were a kid?",
        "What's one place from your childhood you'd love to show me?",
        "What compliment do you wish you heard more often?",
        "What's something new you learned about me recently?",
        "If we could master one skill together, what should it be?",
        "What's your favorite way to be comforted after a hard day?",
        "Which fictional couple reminds you of us?",
        "What tiny everyday moment with me do you treasure?",
        "What would you cook for me if we had a fancy dinner tonight?",
        "What's a tradition you'd like to start, just for us two?",
        "What's the most spontaneous thing you've ever done?",
        "When do you feel closest to me?",
        "What's one thing you admire about how I handle problems?",
        "If we wrote a book together, what would it be about?",
        "What smell instantly brings back a memory for you?",
        "What's your favorite thing about waking up next to a message from me?",
        "What adventure is at the top of your bucket list for us?",
        "What's something silly that always makes you laugh?",
        "How do you like to celebrate small wins?",
        "What's one way I could support your dreams better?",
        "If you could relive one of our dates, which one?",
        "What's the kindest thing a stranger ever did for you?",
        "What's your comfort movie, and will you watch it with me?",
        "What are you looking forward to most in our future?",
        "What's a question you've always wanted to ask me?",
        "What does 'home' mean to you?",
        "If we had a pet together, what would we name it?",
        "What little thing did you notice about me today?",
    )

    /** UTC calendar date, e.g. "2026-07-03". */
    fun dateKey(millis: Long = System.currentTimeMillis()): String =
        formatter.format(Date(millis))

    fun indexForDate(dateKey: String): Int {
        val parsed = formatter.parse(dateKey) ?: return 0
        val epochDay = parsed.time / 86_400_000L
        return (epochDay % ALL.size).toInt()
    }

    fun forDate(dateKey: String): String = ALL[indexForDate(dateKey)]
}
