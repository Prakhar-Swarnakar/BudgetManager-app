package com.budgetmanager.app.data.repository

import com.budgetmanager.app.data.backup.BackupBundle

interface BackupRepository {
    suspend fun buildBackup(): BackupBundle

    /** Replaces every table's contents with [bundle]'s, in one database transaction - either
     *  all of it lands or none of it does. Caller must validate first (BackupSerializer). */
    suspend fun restore(bundle: BackupBundle)
}
