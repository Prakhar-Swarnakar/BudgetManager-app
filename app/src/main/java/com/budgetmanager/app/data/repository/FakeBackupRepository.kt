package com.budgetmanager.app.data.repository

import com.budgetmanager.app.data.backup.BackupBundle

class FakeBackupRepository : BackupRepository {
    var bundleToReturn: BackupBundle = BackupBundle(
        formatVersion = BackupBundle.CURRENT_FORMAT_VERSION,
        exportedAtMillis = 0L,
        categories = emptyList(),
        monthlyBudgets = emptyList(),
        transactions = emptyList(),
        smsMessages = emptyList(),
        alertLogs = emptyList(),
        keywordRules = emptyList()
    )
    var restoredBundle: BackupBundle? = null
        private set

    override suspend fun buildBackup(): BackupBundle = bundleToReturn

    override suspend fun restore(bundle: BackupBundle) {
        restoredBundle = bundle
    }
}
