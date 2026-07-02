package com.lovenote.app.pairing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InviteCodeTest {

    @Test
    fun `generate returns 6 characters from the unambiguous alphabet`() {
        repeat(100) {
            val code = InviteCode.generate()
            assertEquals(6, code.length)
            code.forEach { c ->
                assertTrue("unexpected char $c in $code", c in InviteCode.ALPHABET)
            }
        }
    }

    @Test
    fun `generate does not always return the same code`() {
        val codes = (1..20).map { InviteCode.generate() }.toSet()
        assertTrue(codes.size > 1)
    }

    @Test
    fun `normalize trims and uppercases`() {
        assertEquals("AB12KX", InviteCode.normalize(" ab12kx "))
    }

    @Test
    fun `isValid accepts a generated code`() {
        assertTrue(InviteCode.isValid(InviteCode.generate()))
    }

    @Test
    fun `isValid rejects wrong length`() {
        assertFalse(InviteCode.isValid("ABC"))
        assertFalse(InviteCode.isValid("ABCDEFG"))
        assertFalse(InviteCode.isValid(""))
    }

    @Test
    fun `isValid rejects ambiguous or invalid characters`() {
        assertFalse(InviteCode.isValid("ABC10X")) // 1 and 0 are excluded
        assertFalse(InviteCode.isValid("ABCIOL")) // I, O, L are excluded
        assertFalse(InviteCode.isValid("AB!2KX"))
    }
}
