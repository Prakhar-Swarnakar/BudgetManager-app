package com.budgetmanager.app.core.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiValidationTest {

    @Test
    fun `a single simple emoji is valid`() {
        assertTrue(EmojiValidation.isSingleEmoji("🏠"))
    }

    @Test
    fun `a surrogate-pair emoji is valid`() {
        assertTrue(EmojiValidation.isSingleEmoji("🍔"))
    }

    @Test
    fun `a ZWJ sequence emoji counts as one`() {
        assertTrue(EmojiValidation.isSingleEmoji("👨‍👩‍👧"))
    }

    @Test
    fun `a flag emoji (regional indicator pair) counts as one`() {
        assertTrue(EmojiValidation.isSingleEmoji("🇮🇳"))
    }

    @Test
    fun `leading and trailing whitespace is trimmed`() {
        assertTrue(EmojiValidation.isSingleEmoji("  🎉 "))
    }

    @Test
    fun `two emoji is rejected`() {
        assertFalse(EmojiValidation.isSingleEmoji("😀😀"))
    }

    @Test
    fun `plain text is rejected`() {
        assertFalse(EmojiValidation.isSingleEmoji("abc"))
    }

    @Test
    fun `a single plain letter is rejected`() {
        assertFalse(EmojiValidation.isSingleEmoji("a"))
    }

    @Test
    fun `empty input is rejected`() {
        assertFalse(EmojiValidation.isSingleEmoji(""))
    }

    @Test
    fun `whitespace-only input is rejected`() {
        assertFalse(EmojiValidation.isSingleEmoji("   "))
    }
}
