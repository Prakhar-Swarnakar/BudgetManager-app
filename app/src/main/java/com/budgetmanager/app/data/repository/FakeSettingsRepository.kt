package com.budgetmanager.app.data.repository

import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository : SettingsRepository {
    private val newSpendsAlertsEnabled = MutableStateFlow(true)
    private val eightyPercentAlertsEnabled = MutableStateFlow(true)
    private val overBudgetAlertsEnabled = MutableStateFlow(true)

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
}
