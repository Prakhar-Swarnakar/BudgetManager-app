package com.budgetmanager.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

/** Small persisted settings that aren't part of the main schema: the SMS catch-up marker here,
 *  and later the alert switches, trends range, and last export date as those milestones need them. */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val lastProcessedSmsAtKey = longPreferencesKey("last_processed_sms_at")

    suspend fun getLastProcessedSmsAt(): Long =
        context.dataStore.data.first()[lastProcessedSmsAtKey] ?: 0L

    suspend fun setLastProcessedSmsAt(timestampMillis: Long) {
        context.dataStore.edit { it[lastProcessedSmsAtKey] = timestampMillis }
    }
}
