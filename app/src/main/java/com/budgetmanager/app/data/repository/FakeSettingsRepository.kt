package com.budgetmanager.app.data.repository

import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository : SettingsRepository {
    private val newSpendsAlertsEnabled = MutableStateFlow(true)
    private val eightyPercentAlertsEnabled = MutableStateFlow(true)
    private val overBudgetAlertsEnabled = MutableStateFlow(true)
    private val lastExportAt = MutableStateFlow<Long?>(null)
    private val trendsMonthsShown = MutableStateFlow(6)

    override fun observeNewSpendsAlertsEnabled() = newSpendsAlertsEnabled
    override suspend fun setNewSpendsAlertsEnabled(enabled: Boolean) {
        newSpendsAlertsEnabled.value = enabled
    }

    override fun observeEightyPercentAlertsEnabled() = eightyPercentAlertsEnabled
    override suspend fun setEightyPercentAlertsEnabled(enabled: Boolean) {
        eightyPercentAlertsEnabled.value = enabled
    }

    override fun observeOverBudgetAlertsEnabled() = overBudgetAlertsEnabled
    override suspend fun setOverBudgetAlertsEnabled(enabled: Boolean) {
        overBudgetAlertsEnabled.value = enabled
    }

    override fun observeLastExportAt() = lastExportAt
    override suspend fun setLastExportAt(timestampMillis: Long) {
        lastExportAt.value = timestampMillis
    }

    override fun observeTrendsMonthsShown() = trendsMonthsShown
    override suspend fun setTrendsMonthsShown(months: Int) {
        trendsMonthsShown.value = months
    }
}
