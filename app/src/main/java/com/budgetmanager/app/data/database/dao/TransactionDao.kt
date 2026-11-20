package com.budgetmanager.app.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.budgetmanager.app.core.model.TaxonomyType
import com.budgetmanager.app.data.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/** One row of [TransactionDao.observeSpentByCategoryForMonth]'s grouped total. */
data class CategorySpent(val categoryId: Long, val total: Long)

/** One row of [TransactionDao.observeCategoryBySourceMessage]. */
data class MessageCategory(val messageId: Long, val categoryId: Long)

/** One row of [TransactionDao.observeTaxonomyBySourceMessage]. */
data class MessageTaxonomy(val messageId: Long, val taxonomy: TaxonomyType)

/** One row of [TransactionDao.observeOwedTotalBySourceMessage]. */
data class MessageOwedTotal(val messageId: Long, val owedTotalPaise: Long)

/** One row of [TransactionDao.observeTitleBySourceMessage]. */
data class MessageTitle(val messageId: Long, val title: String)

/** One row of [TransactionDao.observeAmountBySourceMessage]. */
data class MessageAmount(val messageId: Long, val amountPaise: Long)

/** One cash spend on the Wallet page - a transaction whose taxonomy is Cash, with its category's
 *  look joined in so the row needs no per-item lookup. [amountPaise] is the FULL amount, not the
 *  budget-counted share: a split bill paid in cash leaves your hand in full. */
data class CashSpendRow(
    val transactionId: Long,
    val title: String?,
    val note: String?,
    val amountPaise: Long,
    val occurredAt: Long,
    val categoryId: Long,
    val categoryEmoji: String,
    val categoryName: String
)

@Dao
interface TransactionDao {
    @Query(
        "SELECT t.id as transactionId, t.title as title, t.note as note, " +
            "t.amount_paise as amountPaise, t.occurred_at as occurredAt, c.id as categoryId, " +
            "c.emoji as categoryEmoji, c.name as categoryName " +
            "FROM transactions t JOIN category c ON c.id = t.category_id " +
            "WHERE t.taxonomy = 'CASH' ORDER BY t.occurred_at DESC, t.id DESC"
    )
    fun observeCashSpends(): Flow<List<CashSpendRow>>

    @Query("SELECT * FROM transactions WHERE month_key = :monthKey AND category_id = :categoryId ORDER BY occurred_at DESC")
    fun observeForCategoryAndMonth(monthKey: String, categoryId: Long): Flow<List<TransactionEntity>>

    /** Each transaction's full amount minus any friends' shares, so money split with friends
     *  never counts toward your budget (06-backlog.md, "Split with friends") - owed_total_paise
     *  is 0 for every transaction that's never been split, so this is unchanged for them. */
    @Query(
        "SELECT COALESCE(SUM(amount_paise - owed_total_paise), 0) FROM transactions " +
            "WHERE month_key = :monthKey AND category_id = :categoryId"
    )
    fun observeSpentForCategoryAndMonth(monthKey: String, categoryId: Long): Flow<Long>

    /** Every category's total for the month in one query, for Home - avoids one query per
     *  category card. A category with no transactions that month has no row here at all. */
    @Query(
        "SELECT category_id as categoryId, SUM(amount_paise - owed_total_paise) as total FROM transactions " +
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

    /** Same idea as [observeCategoryBySourceMessage], for the separate taxonomy value - a
     *  message's row has no taxonomy of its own to show, only its linked transaction's. */
    @Query(
        "SELECT source_message_id as messageId, taxonomy FROM transactions " +
            "WHERE source_message_id IS NOT NULL AND taxonomy IS NOT NULL"
    )
    fun observeTaxonomyBySourceMessage(): Flow<List<MessageTaxonomy>>

    /** Which Accepted messages are linked to a split transaction, and how much of it is owed
     *  back to friends - lets the Messages list show "your share" plus the struck-through full
     *  amount without a per-row lookup. A message whose transaction was never split (the common
     *  case) has no row here at all. */
    @Query(
        "SELECT source_message_id as messageId, owed_total_paise as owedTotalPaise FROM transactions " +
            "WHERE source_message_id IS NOT NULL AND owed_total_paise > 0"
    )
    fun observeOwedTotalBySourceMessage(): Flow<List<MessageOwedTotal>>

    /** Which Accepted messages are linked to a transaction with its own saved title - lets the
     *  Messages list show your renamed title instead of the SMS's own merchant, without ever
     *  touching the message itself. A transaction with no title saved yet (including every one
     *  saved before this column existed) has no row here, so the message's own merchant is still
     *  what shows until that transaction is opened and saved again. */
    @Query(
        "SELECT source_message_id as messageId, title FROM transactions " +
            "WHERE source_message_id IS NOT NULL AND title IS NOT NULL AND title != ''"
    )
    fun observeTitleBySourceMessage(): Flow<List<MessageTitle>>

    /** Which Accepted messages are linked to a transaction, keyed by message id, with that
     *  transaction's real saved amount - the inclusion gate now keeps some messages with no
     *  parsed amount of their own (06-backlog.md / real gap found 2026-10-07: a genuine SIP
     *  purchase confirmation never states an amount inline), so once one of those is accepted
     *  with a real amount typed in, the Messages list needs this to show it instead of nothing. */
    @Query(
        "SELECT source_message_id as messageId, amount_paise as amountPaise FROM transactions " +
            "WHERE source_message_id IS NOT NULL"
    )
    fun observeAmountBySourceMessage(): Flow<List<MessageAmount>>

    /** Every transaction, for backup export. */
    @Query("SELECT * FROM transactions")
    suspend fun getAllOnce(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE source_message_id = :messageId LIMIT 1")
    suspend fun getBySourceMessageId(messageId: Long): TransactionEntity?

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    /** Owed to You's reminder toggle - a direct update rather than a full entity write, same
     *  idiom as other single-field changes in this app. */
    @Query("UPDATE transactions SET reminded = :reminded WHERE id = :id")
    suspend fun setReminded(id: Long, reminded: Boolean)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
