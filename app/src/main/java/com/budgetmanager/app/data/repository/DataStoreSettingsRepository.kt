package com.budgetmanager.app.data.repository

import com.budgetmanager.app.data.datastore.SettingsDataStore
import javax.inject.Inject

class DataStoreSettingsRepository @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    override fun observeNewSpendsAlertsEnabled() = settingsDataStore.observeNewSpendsAlertsEnabled()
    override suspend fun setNewSpendsAlertsEnabled(enabled: Boolean) =
        settingsDataStore.setNewSpendsAlertsEnabled(enabled)

    override fun observeEightyPercentAlertsEnabled() = settingsDataStore.observeEightyPercentAlertsEnabled()
    override suspend fun setEightyPercentAlertsEnabled(enabled: Boolean) =
        settingsDataStore.setEightyPercentAlertsEnabled(enabled)

    override fun observeOverBudgetAlertsEnabled() = settingsDataStore.observeOverBudgetAlertsEnabled()
    override suspend fun setOverBudgetAlertsEnabled(enabled: Boolean) =
        settingsDataStore.setOverBudgetAlertsEnabled(enabled)
}
