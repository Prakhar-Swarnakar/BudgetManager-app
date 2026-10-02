package com.budgetmanager.app.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class DedupeKeyTest {

    private val noon = ZonedDateTime.of(2026, 10, 1, 12, 0, 0, 0, ZoneId.systemDefault())
        .toInstant().toEpochMilli()

    @Test
    fun `the same SMS produces the same key even with a few minutes' timestamp skew`() {
        // SmsReceiver's broadcast timestamp and DefaultInboxScanner's Telephony.Sms.DATE can
        // genuinely differ by seconds to a few minutes for the same physical SMS (confirmed
        // on-device 2026-10-01) - this is exactly the gap that caused a real spend to be
        // counted twice.
        val fromLiveReceiver = DedupeKey.build(noon, "Rs.1,499.00 spent at NYKAA")
        val fromCatchUpScan = DedupeKey.build(noon + 180_000, "Rs.1,499.00 spent at NYKAA")

        assertEquals(fromLiveReceiver, fromCatchUpScan)
    }

    @Test
    fun `the same wording on a different day is not treated as a duplicate`() {
        val today = DedupeKey.build(noon, "Rs 500 debited")
        val tomorrow = DedupeKey.build(noon + 24 * 60 * 60 * 1000, "Rs 500 debited")

        assertNotEquals(today, tomorrow)
    }

    @Test
    fun `identical text on the same day is a duplicate even when it came through a different sender header`() {
        // Confirmed on-device 2026-10-02: the same bank re-sent the identical debit SMS through
        // more than one registered sender header minutes apart - a real duplicate that a
        // sender-inclusive key let through, since the key never saw the sender at all.
        val first = DedupeKey.build(noon, "Rs 500 debited")
        val secondHeader = DedupeKey.build(noon + 120_000, "Rs 500 debited")

        assertEquals(first, secondHeader)
    }

    @Test
    fun `different wording on the same day is not treated as a duplicate`() {
        val first = DedupeKey.build(noon, "Rs.1,499.00 spent at NYKAA")
        val second = DedupeKey.build(noon, "Rs.650.00 spent at ZOMATO")

        assertNotEquals(first, second)
    }
}
