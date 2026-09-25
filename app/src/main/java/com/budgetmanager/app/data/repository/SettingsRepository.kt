package com.budgetmanager.app.data.repository

import kotlinx.coroutines.flow.Flow

/** The three alert switches (08-pages-and-navigation.md, Settings). All default to on. */
interface SettingsRepository {
    fun observeNewSpendsAlertsEnabled(): Flow<Boolean>
    suspend fun setNewSpendsAlertsEnabled(enabled: Boolean)

    fun observeEightyPercentAlertsEnabled(): Flow<Boolean>
    suspend fun setEightyPercentAlertsEnabled(enabled: Boolean)

    fun observeOverBudgetAlertsEnabled(): Flow<Boolean>
    suspend fun setOverBudgetAlertsEnabled(enabled: Boolean)
}
