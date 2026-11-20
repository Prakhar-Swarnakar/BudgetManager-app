package com.budgetmanager.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.budgetmanager.app.core.model.MessageDirection
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.TaxonomyType
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

    /** Home's "Income" line: the total of every Accepted credit message received in
     *  [sinceMillis, untilMillisExclusive). One aggregate rather than Home observing the whole
     *  table and summing a month's worth of it in memory. A message with no parsed amount of its
     *  own contributes nothing, same as the in-memory version it replaced. */
    @Query(
        "SELECT COALESCE(SUM(parsed_amount_paise), 0) FROM sms_message " +
            "WHERE status = :status AND direction = :direction " +
            "AND received_at >= :sinceMillis AND received_at < :untilMillisExclusive"
    )
    fun observeAmountTotal(
        status: MessageStatus,
        direction: MessageDirection,
        sinceMillis: Long,
        untilMillisExclusive: Long
    ): Flow<Long>

    /** Every message, for backup export. */
    @Query("SELECT * FROM sms_message")
    suspend fun getAllOnce(): List<SmsMessageEntity>

    @Query("SELECT * FROM sms_message WHERE id = :id")
    suspend fun getById(id: Long): SmsMessageEntity?

    /** The messages behind a batch of transactions, in one query - Category detail needs each
     *  SMS-linked transaction's merchant and real received time, and used to fetch them one
     *  getById at a time while building its list. */
    @Query("SELECT * FROM sms_message WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<SmsMessageEntity>

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

    /** Same idea as [updateSuggestedCategory], for the separate taxonomy rule set. */
    @Query("UPDATE sms_message SET suggested_taxonomy = :taxonomy WHERE id = :id")
    suspend fun updateSuggestedTaxonomy(id: Long, taxonomy: TaxonomyType?)

    @Query("UPDATE sms_message SET is_new = 0")
    suspend fun markAllSeen()

    @Query("DELETE FROM sms_message")
    suspend fun deleteAll()

    /** Deletes every message received in [sinceMillis, untilMillisExclusive) - backs the
     *  Messages page's per-month "Clear month" button. A linked transaction is kept, only
     *  unlinked (source_message_id ON DELETE SET NULL on the transactions table). */
    @Query("DELETE FROM sms_message WHERE received_at >= :sinceMillis AND received_at < :untilMillisExclusive")
    suspend fun deleteByReceivedAtRange(sinceMillis: Long, untilMillisExclusive: Long)
}
