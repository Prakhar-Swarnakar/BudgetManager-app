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
 *
 * Deliberately excludes the sender, even though an earlier version included it: real inbox data
 * (confirmed on-device 2026-10-02) showed the same bank re-sending the identical debit SMS
 * through more than one registered sender header minutes apart - e.g. one notification arriving
 * as both "BG-XXXXXX-S" and "JD-XXXXXX-S" - which produced visible duplicate messages under a
 * sender-inclusive key. Body text already carries enough unique detail (amount, merchant, a
 * reference number) to distinguish two genuinely different same-day transactions without needing
 * the sender's help, so dropping it only catches more real duplicates and risks nothing.
 */
internal object DedupeKey {
    fun build(timestampMillis: Long, body: String): String {
        val day = Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        return "$day|${body.hashCode()}"
    }
}
