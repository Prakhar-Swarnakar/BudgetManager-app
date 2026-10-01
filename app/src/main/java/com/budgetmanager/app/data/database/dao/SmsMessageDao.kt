package com.budgetmanager.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.data.database.entity.SmsMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsMessageDao {
    @Query("SELECT * FROM sms_message ORDER BY received_at DESC")
    fun observeAll(): Flow<List<SmsMessageEntity>>

    @Query("SELECT * FROM sms_message WHERE status = :status ORDER BY received_at DESC")
    fun observeByStatus(status: MessageStatus): Flow<List<SmsMessageEntity>>

    @Query("SELECT COUNT(*) FROM sms_message WHERE status = :status")
    fun observeCountByStatus(status: MessageStatus): Flow<Int>

    @Query("SELECT COUNT(*) FROM sms_message WHERE is_new = 1")
    fun observeNewCount(): Flow<Int>

    /** Every message, for backup export. */
    @Query("SELECT * FROM sms_message")
    suspend fun getAllOnce(): List<SmsMessageEntity>

    @Query("SELECT * FROM sms_message WHERE id = :id")
    suspend fun getById(id: Long): SmsMessageEntity?

    @Query("SELECT * FROM sms_message WHERE dedupe_key = :dedupeKey LIMIT 1")
    suspend fun getByDedupeKey(dedupeKey: String): SmsMessageEntity?

    @Insert
    suspend fun insert(message: SmsMessageEntity): Long

    @Insert
    suspend fun insertAll(messages: List<SmsMessageEntity>)

    @Update
    suspend fun update(message: SmsMessageEntity)

    @Query("UPDATE sms_message SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: MessageStatus)

    /** Updates only the keyword suggestion - used to re-run category rules against existing
     *  messages without touching status or anything else. */
    @Query("UPDATE sms_message SET suggested_category_id = :categoryId WHERE id = :id")
    suspend fun updateSuggestedCategory(id: Long, categoryId: Long?)

    @Query("UPDATE sms_message SET is_new = 0")
    suspend fun markAllSeen()

    @Query("DELETE FROM sms_message")
    suspend fun deleteAll()
}
