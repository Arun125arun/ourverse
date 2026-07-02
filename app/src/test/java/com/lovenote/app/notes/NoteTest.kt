package com.lovenote.app.notes

import com.google.firebase.Timestamp
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteTest {

    @Test
    fun `toMap and fromMap round-trip`() {
        val sent = Timestamp(Date(1_700_000_000_000L))
        val original = Note(
            id = "n1",
            senderUid = "alice",
            text = "thinking of you",
            style = "sky",
            sentAt = sent,
        )
        val restored = Note.fromMap("n1", original.toMap())
        assertEquals(original, restored)
    }

    @Test
    fun `fromMap defaults missing fields`() {
        val note = Note.fromMap("n2", emptyMap())
        assertEquals("n2", note.id)
        assertEquals("", note.senderUid)
        assertEquals("", note.text)
        assertEquals(Note.DEFAULT_STYLE, note.style)
        assertNull(note.sentAt)
    }

    @Test
    fun `unknown style falls back to default`() {
        val note = Note.fromMap("n3", mapOf("style" to "neon-zebra"))
        assertEquals("peach", note.style)
    }

    @Test
    fun `all known styles survive round-trip`() {
        Note.STYLES.forEach { style ->
            val note = Note.fromMap("n4", mapOf("style" to style))
            assertEquals(style, note.style)
        }
    }
}
