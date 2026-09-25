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

    /** Null means no export has ever been made. */
    fun observeLastExportAt(): Flow<Long?>
    suspend fun setLastExportAt(timestampMillis: Long)

    /** How many months Trends' Previous month and Historic charts show - 3, 6, or 12; 6 by default. */
    fun observeTrendsMonthsShown(): Flow<Int>
    suspend fun setTrendsMonthsShown(months: Int)
}
