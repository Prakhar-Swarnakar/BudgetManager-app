package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.Transaction
import com.budgetmanager.app.core.model.TransactionSplit
import com.budgetmanager.app.data.database.dao.OwedEntryRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant

/** Pass the same [FakeFriendRepository] and [FakeTransactionRepository] a test uses, so
 *  [observeOwedToYou] can join against real friend names and transaction details, as it would
 *  with Room. */
class FakeTransactionSplitRepository(
    private val friends: FakeFriendRepository? = null,
    private val transactions: FakeTransactionRepository? = null,
    private val categories: FakeCategoryRepository? = null
) : TransactionSplitRepository {
    private val state = MutableStateFlow<List<TransactionSplit>>(emptyList())
    private var nextId = 1L

    override fun observeByTransaction(transactionId: Long) =
        state.map { list -> list.filter { it.transactionId == transactionId } }

    override fun observeOwedToYou() =
        if (transactions != null) {
            // A plain state.map { ... transactions.current ... } would only re-emit when the
            // splits themselves change - a transaction-only change (e.g. the reminded flag)
            // never touches a split row, so it would silently never reach the UI. Combining with
            // transactions' own flow is what makes this reactive to that, the same way Room's
            // real join naturally is.
            combine(state, transactions.currentFlow) { splits, _ -> splits.toOwedEntryRows() }
        } else {
            state.map { splits -> splits.toOwedEntryRows() }
        }.map { rows -> rows.toOwedToYouEntries() }

    private fun List<TransactionSplit>.toOwedEntryRows(): List<OwedEntryRow> = mapNotNull { split ->
        val friend = friends?.current?.firstOrNull { it.id == split.friendId } ?: return@mapNotNull null
        val transaction: Transaction = transactions?.current
            ?.firstOrNull { it.id == split.transactionId } ?: return@mapNotNull null
        val category = categories?.current
            ?.firstOrNull { it.id == transaction.categoryId } ?: return@mapNotNull null
        OwedEntryRow(
            splitId = split.id,
            friendId = friend.id,
            friendName = friend.name,
            amountOwedPaise = split.amountOwed.paise,
            settled = split.settled,
            transactionId = split.transactionId,
            title = transaction.title,
            note = transaction.note,
            occurredAt = transaction.occurredAt.toEpochMilli(),
            categoryEmoji = category.emoji,
            categoryName = category.name,
            taxonomy = transaction.taxonomy,
            reminded = transaction.reminded
        )
    }

    override suspend fun replaceSplits(transactionId: Long, splits: List<TransactionSplitInput>) {
        state.value = state.value.filterNot { it.transactionId == transactionId } +
            splits.map { input ->
                TransactionSplit(
                    id = nextId++,
                    transactionId = transactionId,
                    friendId = input.friendId,
                    amountOwed = input.amount,
                    settled = input.settled,
                    settledAt = input.settledAt
                )
            }
    }

    override suspend fun settle(splitId: Long) {
        state.value = state.value.map {
            if (it.id == splitId) it.copy(settled = true, settledAt = Instant.now()) else it
        }
    }

    /** Test helper: seed the fake directly, bypassing replaceSplits(). */
    fun seed(splits: List<TransactionSplit>) {
        state.value = splits
    }
}
