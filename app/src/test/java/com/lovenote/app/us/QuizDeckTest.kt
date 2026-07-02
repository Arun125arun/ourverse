package com.lovenote.app.us

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizDeckTest {

    @Test
    fun `deck has enough distinct questions with 4 options each`() {
        assertTrue("need at least 20 quiz questions", QuizDeck.ALL.size >= 20)
        assertEquals(QuizDeck.ALL.size, QuizDeck.ALL.map { it.prompt }.toSet().size)
        QuizDeck.ALL.forEach { q ->
            assertEquals("options for '${q.prompt}'", 4, q.options.size)
            assertEquals(4, q.options.toSet().size)
        }
    }

    @Test
    fun `same date key always gives the same quiz question`() {
        assertEquals(QuizDeck.forDate("2026-07-03"), QuizDeck.forDate("2026-07-03"))
    }

    @Test
    fun `quiz question differs from a plain daily rotation`() {
        // decorrelated from Questions so both features don't feel in lockstep
        val indexes = (1..10).map { day ->
            QuizDeck.indexForDate("2026-07-%02d".format(day))
        }
        assertTrue(indexes.toSet().size > 1)
        indexes.forEach { assertTrue(it in QuizDeck.ALL.indices) }
    }
}
