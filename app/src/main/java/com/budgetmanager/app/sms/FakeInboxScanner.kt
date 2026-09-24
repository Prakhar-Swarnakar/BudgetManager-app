package com.budgetmanager.app.sms

/** In-memory fake for ViewModel tests - records calls rather than touching the SMS Provider. */
class FakeInboxScanner : InboxScanner {
    var scanCallCount = 0
        private set
    var lastScanFromMillis: Long? = null
        private set

    override suspend fun scan() {
        scanCallCount++
    }

    override suspend fun scanFrom(sinceMillis: Long) {
        lastScanFromMillis = sinceMillis
    }
}
