package com.budgetmanager.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

/** Small persisted settings that aren't part of the main schema: the SMS catch-up marker, the
 *  three alert switches, and later trends range and last export date as those milestones need
 *  them. */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val lastProcessedSmsAtKey = longPreferencesKey("last_processed_sms_at")
    private val newSpendsAlertsEnabledKey = booleanPreferencesKey("new_spends_alerts_enabled")
    private val eightyPercentAlertsEnabledKey = booleanPreferencesKey("eighty_percent_alerts_enabled")
    private val overBudgetAlertsEnabledKey = booleanPreferencesKey("over_budget_alerts_enabled")
    private val lastExportAtKey = longPreferencesKey("last_export_at")
    private val trendsMonthsShownKey = intPreferencesKey("trends_months_shown")
    private val taxonomyKeywordsSeededV1Key = booleanPreferencesKey("taxonomy_keywords_seeded_v1")

    /** Null means the catch-up scan has never run - the caller must not treat that as epoch 0,
     *  or the very first scan would bulk-import the phone's entire SMS history. */
    suspend fun getLastProcessedSmsAt(): Long? =
        context.dataStore.data.first()[lastProcessedSmsAtKey]

    suspend fun setLastProcessedSmsAt(timestampMillis: Long) {
        context.dataStore.edit { it[lastProcessedSmsAtKey] = timestampMillis }
    }

    fun observeNewSpendsAlertsEnabled(): Flow<Boolean> =
        context.dataStore.data.map { it[newSpendsAlertsEnabledKey] ?: true }

    suspend fun setNewSpendsAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[newSpendsAlertsEnabledKey] = enabled }
    }

    fun observeEightyPercentAlertsEnabled(): Flow<Boolean> =
        context.dataStore.data.map { it[eightyPercentAlertsEnabledKey] ?: true }

    suspend fun setEightyPercentAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[eightyPercentAlertsEnabledKey] = enabled }
    }

    fun observeOverBudgetAlertsEnabled(): Flow<Boolean> =
        context.dataStore.data.map { it[overBudgetAlertsEnabledKey] ?: true }

    suspend fun setOverBudgetAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[overBudgetAlertsEnabledKey] = enabled }
    }

    /** Null means no export has ever been made. */
    fun observeLastExportAt(): Flow<Long?> =
        context.dataStore.data.map { it[lastExportAtKey] }

    suspend fun setLastExportAt(timestampMillis: Long) {
        context.dataStore.edit { it[lastExportAtKey] = timestampMillis }
    }

    /** How many months Trends' Previous month and Historic charts show - 3, 6, or 12; 6 by default. */
    fun observeTrendsMonthsShown(): Flow<Int> =
        context.dataStore.data.map { it[trendsMonthsShownKey] ?: 6 }

    suspend fun setTrendsMonthsShown(months: Int) {
        context.dataStore.edit { it[trendsMonthsShownKey] = months }
    }

    /** One-shot, checked once at app startup - not observed reactively like the flags above, so
     *  a plain suspend get/set rather than a Flow (same shape as getLastProcessedSmsAt). */
    suspend fun isTaxonomyKeywordsSeeded(): Boolean =
        context.dataStore.data.first()[taxonomyKeywordsSeededV1Key] ?: false

    suspend fun setTaxonomyKeywordsSeeded(seeded: Boolean) {
        context.dataStore.edit { it[taxonomyKeywordsSeededV1Key] = seeded }
    }
}
