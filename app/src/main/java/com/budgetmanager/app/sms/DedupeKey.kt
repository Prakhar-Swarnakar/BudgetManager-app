package com.budgetmanager.app.sms

/** Shared by the live receiver and the inbox catch-up scan, so the same SMS always produces
 *  the same key no matter which path caught it first (R11). */
internal object DedupeKey {
    fun build(sender: String, timestampMillis: Long, body: String): String =
        "$sender|$timestampMillis|${body.hashCode()}"
}
