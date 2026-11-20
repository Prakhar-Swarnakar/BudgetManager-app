package com.budgetmanager.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.budgetmanager.app.core.model.TaxonomyType
import com.budgetmanager.app.data.database.entity.TransactionSplitEntity
import kotlinx.coroutines.flow.Flow

/** One row of [TransactionSplitDao.observeOwedToYou] - a friend's share on one transaction,
 *  joined with just enough to show a row without a per-item lookup. The repository groups these
 *  by [transactionId] into one [com.budgetmanager.app.core.model.OwedToYouEntry] per transaction -
 *  a transaction split with several friends produces several rows here, one per friend. */
data class OwedEntryRow(
    val splitId: Long,
    val friendId: Long,
    val friendName: String,
    val amountOwedPaise: Long,
    val settled: Boolean,
    val transactionId: Long,
    val title: String?,
    val note: String?,
    val occurredAt: Long,
    val categoryEmoji: String,
    val categoryName: String,
    val taxonomy: TaxonomyType?,
    val reminded: Boolean
)

@Dao
interface TransactionSplitDao {
    @Query("SELECT * FROM transaction_split WHERE transaction_id = :transactionId")
    fun observeByTransaction(transactionId: Long): Flow<List<TransactionSplitEntity>>

    /** Every split, newest-transaction-first, friends within one transaction alphabetical - the
     *  "Owed to you" page's whole list, one row per friend per transaction. The repository groups
     *  these by transaction into one [com.budgetmanager.app.core.model.OwedToYouEntry] each. */
    @Query(
        "SELECT ts.id as splitId, f.id as friendId, f.name as friendName, " +
            "ts.amount_owed_paise as amountOwedPaise, ts.settled as settled, " +
            "t.id as transactionId, t.title as title, t.note as note, " +
            "t.occurred_at as occurredAt, c.emoji as categoryEmoji, c.name as categoryName, " +
            "t.taxonomy as taxonomy, t.reminded as reminded " +
            "FROM transaction_split ts " +
            "JOIN friend f ON f.id = ts.friend_id " +
            "JOIN transactions t ON t.id = ts.transaction_id " +
            "JOIN category c ON c.id = t.category_id " +
            "ORDER BY t.occurred_at DESC, f.name ASC"
    )
    fun observeOwedToYou(): Flow<List<OwedEntryRow>>

    @Insert
    suspend fun insertAll(splits: List<TransactionSplitEntity>)

    /** Wholesale replace on edit - see TransactionSplitEntity's doc comment for why. */
    @Query("DELETE FROM transaction_split WHERE transaction_id = :transactionId")
    suspend fun deleteByTransaction(transactionId: Long)

    @Query("UPDATE transaction_split SET settled = 1, settled_at = :settledAt WHERE id = :id")
    suspend fun settle(id: Long, settledAt: Long)

    @Query("DELETE FROM transaction_split")
    suspend fun deleteAll()
}
