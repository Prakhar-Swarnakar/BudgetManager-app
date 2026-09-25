package com.budgetmanager.app.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.budgetmanager.app.data.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/** One row of [TransactionDao.observeSpentByCategoryForMonth]'s grouped total. */
data class CategorySpent(val categoryId: Long, val total: Long)

/** One row of [TransactionDao.observeCategoryBySourceMessage]. */
data class MessageCategory(val messageId: Long, val categoryId: Long)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE month_key = :monthKey AND category_id = :categoryId ORDER BY occurred_at DESC")
    fun observeForCategoryAndMonth(monthKey: String, categoryId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(amount_paise), 0) FROM transactions WHERE month_key = :monthKey AND category_id = :categoryId")
    fun observeSpentForCategoryAndMonth(monthKey: String, categoryId: Long): Flow<Long>

    /** Every category's total for the month in one query, for Home - avoids one query per
     *  category card. A category with no transactions that month has no row here at all. */
    @Query(
        "SELECT category_id as categoryId, SUM(amount_paise) as total FROM transactions " +
            "WHERE month_key = :monthKey GROUP BY category_id"
    )
    fun observeSpentByCategoryForMonth(monthKey: String): Flow<List<CategorySpent>>

    /** Which category an Accepted message's transaction is filed under, for every message that
     *  has one - lets the Messages list show a row's category without a per-row lookup. */
    @Query(
        "SELECT source_message_id as messageId, category_id as categoryId FROM transactions " +
            "WHERE source_message_id IS NOT NULL"
    )
    fun observeCategoryBySourceMessage(): Flow<List<MessageCategory>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE source_message_id = :messageId LIMIT 1")
    suspend fun getBySourceMessageId(messageId: Long): TransactionEntity?

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)
}
