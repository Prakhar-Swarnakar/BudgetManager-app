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

    override fun observeLastExportAt() = settingsDataStore.observeLastExportAt()
    override suspend fun setLastExportAt(timestampMillis: Long) =
        settingsDataStore.setLastExportAt(timestampMillis)

    override fun observeTrendsMonthsShown() = settingsDataStore.observeTrendsMonthsShown()
    override suspend fun setTrendsMonthsShown(months: Int) =
        settingsDataStore.setTrendsMonthsShown(months)

    override suspend fun isTaxonomyKeywordsSeeded() = settingsDataStore.isTaxonomyKeywordsSeeded()
    override suspend fun setTaxonomyKeywordsSeeded(seeded: Boolean) =
        settingsDataStore.setTaxonomyKeywordsSeeded(seeded)
}
