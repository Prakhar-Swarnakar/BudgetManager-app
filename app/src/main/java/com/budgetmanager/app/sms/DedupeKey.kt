package com.budgetmanager.app.sms

import java.time.Instant
import java.time.ZoneId

/**
 * Shared by the live receiver and the inbox catch-up scan, so the same SMS always produces
 * the same key no matter which path caught it first (R11).
 *
 * Rounds the timestamp to the day rather than using it exactly: SmsReceiver's broadcast
 * timestamp (often the SMSC's own timestamp) and DefaultInboxScanner's Telephony.Sms.DATE (the
 * device's local receipt time) can genuinely differ by seconds to a few minutes for the very
 * same physical SMS, which let the same real spend through twice under the old exact-millis key
 * (confirmed on-device 2026-10-01: duplicate NYKAA/Flipkart rows, identical sender, body, and
 * embedded bank Ref No., different stored timestamps). Day granularity absorbs that skew. It
 * does not use the parsed amount or merchant - not every bank SMS has enough distinguishing text
 * for that to help, and the raw body usually carries more unique detail (a reference number, an
 * account suffix) than the parsed fields alone.
 */
internal object DedupeKey {
    fun build(sender: String, timestampMillis: Long, body: String): String {
        val day = Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        return "$sender|$day|${body.hashCode()}"
    }
}
