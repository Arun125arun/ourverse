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
            seenAt = Timestamp(Date(1_700_000_060_000L)),
            reactions = mapOf("bob" to "❤", "alice" to "😂"),
        )
        val restored = Message.fromMap("m1", original.toMap())
        assertEquals(original, restored)
    }

    @Test
    fun `photo message round-trips`() {
        val original = Message(
            id = "p1",
            senderUid = "bob",
            type = "photo",
            body = "aGVsbG8=",
        )
        val restored = Message.fromMap("p1", original.toMap())
        assertEquals("photo", restored.type)
        assertEquals("aGVsbG8=", restored.body)
    }

    @Test
    fun `seen is true only when seenAt is set`() {
        assertFalse(Message(id = "m5").seen)
        assertTrue(Message(id = "m6", seenAt = Timestamp(Date(0L))).seen)
    }

    @Test
    fun `reactions default to empty and ignore non-string entries`() {
        val message = Message.fromMap("m7", mapOf("reactions" to mapOf("alice" to 42)))
        assertTrue(message.reactions.isEmpty())
        assertTrue(Message.fromMap("m8", emptyMap()).reactions.isEmpty())
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
    fun `voice message round-trips with duration`() {
        val original = Message(
            id = "v1",
            senderUid = "alice",
            type = "voice",
            body = "aGVsbG8=",
            durationSec = 12,
        )
        val restored = Message.fromMap("v1", original.toMap())
        assertEquals(original, restored)
        assertTrue(restored.isVoice)
    }

    @Test
    fun `one-time photo flag round-trips and consumed state works`() {
        val fresh = Message(id = "o1", senderUid = "bob", type = "photo", body = "eA==", once = true)
        val restored = Message.fromMap("o1", fresh.toMap())
        assertTrue(restored.once)
        assertFalse(restored.onceConsumed)

        val consumed = Message.fromMap("o2", mapOf("type" to "photo", "once" to true, "body" to ""))
        assertTrue(consumed.onceConsumed)
    }

    @Test
    fun `reply fields round-trip`() {
        val reply = Message(
            id = "r1",
            senderUid = "alice",
            body = "yes!",
            replyToId = "m9",
            replyText = "movie tonight?",
            replySender = "bob",
        )
        val restored = Message.fromMap("r1", reply.toMap())
        assertEquals(reply, restored)
        assertNull(Message.fromMap("r2", emptyMap()).replyToId)
    }

    @Test
    fun `preview summarizes message types`() {
        assertEquals("hello", Message.preview(Message(body = "hello")))
        assertEquals("📷 Photo", Message.preview(Message(type = "photo", body = "x")))
        assertEquals("🎤 Voice note", Message.preview(Message(type = "voice", body = "x")))
    }

    @Test
    fun `isMine matches sender uid`() {
        val message = Message(id = "m3", senderUid = "alice", body = "hey")
        assertTrue(message.isMine("alice"))
        assertFalse(message.isMine("bob"))
    }
}
