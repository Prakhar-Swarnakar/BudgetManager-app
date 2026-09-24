package com.budgetmanager.app.sms

interface InboxScanner {
    /** Catches up on messages missed while the app was closed. Runs on app open. */
    suspend fun scan()

    /** Debug/testing helper: scans from [sinceMillis] regardless of the stored marker, and does
     *  not move the marker. Not called by production code. */
    suspend fun scanFrom(sinceMillis: Long)
}
