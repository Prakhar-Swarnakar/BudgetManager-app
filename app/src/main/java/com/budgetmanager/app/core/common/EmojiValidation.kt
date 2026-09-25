package com.budgetmanager.app.core.common

import java.text.BreakIterator

/**
 * A pragmatic single-emoji check for the category icon field, since the app has no emoji
 * picker of its own and relies on the phone's keyboard (03-user-flows.md, flow 11; R27 in
 * 09-risks-and-phases.md). Uses grapheme-cluster segmentation so a multi-codepoint emoji
 * (a skin tone, a ZWJ family, a flag) still counts as one character, then rejects plain text
 * by requiring the cluster to contain at least one codepoint outside the plain
 * ASCII/Latin-punctuation range.
 */
object EmojiValidation {
    private const val PLAIN_TEXT_CEILING = 0x2000

    fun isSingleEmoji(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false

        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(trimmed)
        val firstClusterEnd = iterator.next()
        if (firstClusterEnd != trimmed.length) return false // more than one grapheme cluster

        return trimmed.codePoints().anyMatch { codePoint -> codePoint >= PLAIN_TEXT_CEILING }
    }
}
