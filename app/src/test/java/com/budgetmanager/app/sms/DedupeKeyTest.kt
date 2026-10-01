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
        val fromLiveReceiver = DedupeKey.build("AX-SBICRD-S", noon, "Rs.1,499.00 spent at NYKAA")
        val fromCatchUpScan = DedupeKey.build("AX-SBICRD-S", noon + 180_000, "Rs.1,499.00 spent at NYKAA")

        assertEquals(fromLiveReceiver, fromCatchUpScan)
    }

    @Test
    fun `the same wording on a different day is not treated as a duplicate`() {
        val today = DedupeKey.build("AX-SBICRD-S", noon, "Rs 500 debited")
        val tomorrow = DedupeKey.build("AX-SBICRD-S", noon + 24 * 60 * 60 * 1000, "Rs 500 debited")

        assertNotEquals(today, tomorrow)
    }

    @Test
    fun `a different sender is not treated as a duplicate, even with identical text and time`() {
        val bankA = DedupeKey.build("AX-SBICRD-S", noon, "Rs 500 debited")
        val bankB = DedupeKey.build("VM-ICICIB-S", noon, "Rs 500 debited")

        assertNotEquals(bankA, bankB)
    }

    @Test
    fun `different wording on the same day is not treated as a duplicate`() {
        val first = DedupeKey.build("AX-SBICRD-S", noon, "Rs.1,499.00 spent at NYKAA")
        val second = DedupeKey.build("AX-SBICRD-S", noon, "Rs.650.00 spent at ZOMATO")

        assertNotEquals(first, second)
    }
}
