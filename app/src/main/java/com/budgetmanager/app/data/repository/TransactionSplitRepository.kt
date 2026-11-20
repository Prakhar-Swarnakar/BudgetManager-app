package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.OwedToYouEntry
import com.budgetmanager.app.core.model.TransactionSplit
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** One friend's share, ready to persist via [TransactionSplitRepository.replaceSplits]. [settled]/
 *  [settledAt] are supplied by the caller (Add Transaction's edit screen) rather than defaulted
 *  here - carrying forward a still-present friend's existing settled status is the caller's job
 *  (it already has that friend's current value in hand from when the split was loaded), not
 *  something this layer infers on its own. */
data class TransactionSplitInput(
    val friendId: Long,
    val amount: Money,
    val settled: Boolean = false,
    val settledAt: Instant? = null
)

interface TransactionSplitRepository {
    fun observeByTransaction(transactionId: Long): Flow<List<TransactionSplit>>

    /** Every split transaction, one entry per transaction (listing every friend on it), for the
     *  "Owed to you" page. */
    fun observeOwedToYou(): Flow<List<OwedToYouEntry>>

    /** Replaces every split on [transactionId] wholesale - used on both initial save and edit,
     *  so an edit can never leave a stale split behind under a friend who was removed. Each
     *  [TransactionSplitInput] carries whatever settled state the caller wants that row to have,
     *  so a friend still present after an edit keeps their settled status by the caller passing
     *  it through unchanged - this layer just writes what it's told. */
    suspend fun replaceSplits(transactionId: Long, splits: List<TransactionSplitInput>)

    suspend fun settle(splitId: Long)
}
