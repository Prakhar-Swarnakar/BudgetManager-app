package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.SmsMessage
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun observeAll(): Flow<List<SmsMessage>>
    fun observeByStatus(status: MessageStatus): Flow<List<SmsMessage>>
    fun observeCountByStatus(status: MessageStatus): Flow<Int>
    fun observeNewCount(): Flow<Int>
    suspend fun getById(id: Long): SmsMessage?

    /** Saves a newly-received SMS, skipping it if its dedupeKey already exists (R11).
     *  Returns null when skipped as a duplicate. */
    suspend fun ingest(message: SmsMessage): Long?

    suspend fun reject(id: Long)

    /** Used to undo a reject, and generally wherever a status change doesn't need its own
     *  named method. */
    suspend fun setStatus(id: Long, status: MessageStatus)

    suspend fun markAllSeen()
}
