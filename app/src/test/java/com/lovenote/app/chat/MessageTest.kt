package com.lovenote.app.chat

import com.google.firebase.Timestamp
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageTest {

    @Test
    fun `toMap and fromMap round-trip`() {
        val sent = Timestamp(Date(1_700_000_000_000L))
        val original = Message(
            id = "m1",
            senderUid = "alice",
            type = "text",
            body = "hi love",
            sentAt = sent,
        )
        val restored = Message.fromMap("m1", original.toMap())
        assertEquals(original, restored)
    }

    @Test
    fun `fromMap defaults missing fields`() {
        val message = Message.fromMap("m2", emptyMap())
        assertEquals("m2", message.id)
        assertEquals("", message.senderUid)
        assertEquals("text", message.type)
        assertEquals("", message.body)
        assertNull(message.sentAt)
    }

    @Test
    fun `isMine matches sender uid`() {
        val message = Message(id = "m3", senderUid = "alice", body = "hey")
        assertTrue(message.isMine("alice"))
        assertFalse(message.isMine("bob"))
    }
}
