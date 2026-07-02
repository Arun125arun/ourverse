package com.lovenote.app.pairing

object InviteCode {
    // Excludes I, L, O, 0, 1 so codes are easy to read aloud and retype.
    const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    const val LENGTH = 6

    fun generate(): String =
        (1..LENGTH).map { ALPHABET.random() }.joinToString("")

    fun normalize(input: String): String = input.trim().uppercase()

    fun isValid(code: String): Boolean =
        code.length == LENGTH && code.all { it in ALPHABET }
}
