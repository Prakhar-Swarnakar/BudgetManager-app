package com.budgetmanager.app.feature.settings

import com.budgetmanager.app.data.backup.BackupBundle

data class SettingsUiState(
    val isLoading: Boolean = true,
    val newSpendsAlertsEnabled: Boolean = true,
    val eightyPercentAlertsEnabled: Boolean = true,
    val overBudgetAlertsEnabled: Boolean = true,
    val trendsMonthsShown: Int = 6,
    val lastExportAtMillis: Long? = null,
    val pendingImport: PendingImportUi? = null,
    val importError: String? = null,
    val importSuccessMessage: String? = null,
    val exportError: String? = null
)

/** A validated backup file, waiting on the user to confirm before it replaces all current data. */
data class PendingImportUi(
    val bundle: BackupBundle,
    val categoryCount: Int,
    val transactionCount: Int,
    val smsMessageCount: Int
)
