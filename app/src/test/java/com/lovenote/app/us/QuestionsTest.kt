package com.lovenote.app.us

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionsTest {

    @Test
    fun `question list is reasonably large and has no duplicates`() {
        assertTrue("need at least 40 questions", Questions.ALL.size >= 40)
        assertEquals(Questions.ALL.size, Questions.ALL.toSet().size)
    }

    @Test
    fun `same date key always gives the same question`() {
        assertEquals(
            Questions.forDate("2026-07-03"),
            Questions.forDate("2026-07-03"),
        )
    }

    @Test
    fun `consecutive days give consecutive questions and wrap around`() {
        val a = Questions.indexForDate("2026-07-03")
        val b = Questions.indexForDate("2026-07-04")
        assertEquals((a + 1) % Questions.ALL.size, b)
    }

    @Test
    fun `index is always in bounds across many days`() {
        for (day in 1..28) {
            val key = "2026-02-%02d".format(day)
            val index = Questions.indexForDate(key)
            assertTrue(index in Questions.ALL.indices)
        }
    }

    @Test
    fun `date key formats as UTC yyyy-MM-dd`() {
        // 2026-07-03 00:30 UTC
        assertEquals("2026-07-03", Questions.dateKey(1_783_038_600_000L))
    }
}
