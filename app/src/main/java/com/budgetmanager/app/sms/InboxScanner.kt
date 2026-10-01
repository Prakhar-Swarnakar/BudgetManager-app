package com.budgetmanager.app.sms

interface InboxScanner {
    /** Catches up on messages missed while the app was closed. Runs on app open. */
    suspend fun scan()

    /** Scans [sinceMillis, untilMillisExclusive) regardless of the stored catch-up marker, and
     *  never moves that marker - an independent, repeatable action. Backs the Messages page's
     *  per-month "Fetch SMS" button. */
    suspend fun scanRange(sinceMillis: Long, untilMillisExclusive: Long)
}
