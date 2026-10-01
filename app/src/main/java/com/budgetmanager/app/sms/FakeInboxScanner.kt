package com.budgetmanager.app.sms

/** In-memory fake for ViewModel tests - records calls rather than touching the SMS Provider. */
class FakeInboxScanner : InboxScanner {
    var scanCallCount = 0
        private set
    var lastScanRange: Pair<Long, Long>? = null
        private set

    override suspend fun scan() {
        scanCallCount++
    }

    override suspend fun scanRange(sinceMillis: Long, untilMillisExclusive: Long) {
        lastScanRange = sinceMillis to untilMillisExclusive
    }
}
