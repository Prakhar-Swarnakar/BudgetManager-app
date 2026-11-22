package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.core.model.SmsMessage
import com.budgetmanager.app.core.model.Transaction
import com.budgetmanager.app.core.model.TaxonomyType
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface TransactionRepository {
    fun observeForCategoryAndMonth(monthKey: MonthKey, categoryId: Long): Flow<List<Transaction>>
    fun observeSpentForCategoryAndMonth(monthKey: MonthKey, categoryId: Long): Flow<Money>

    /** Every category's total for the month, keyed by category id. A category with no
     *  transactions that month has no entry - callers treat a missing key as ₹0, same as
     *  MonthlyBudgetRepository.observeForMonth. */
    fun observeSpentByCategoryForMonth(monthKey: MonthKey): Flow<Map<Long, Money>>

    /** Your share of every transaction marked avoidable in the month (Home's Avoidable figure). */
    fun observeAvoidableForMonth(monthKey: MonthKey): Flow<Money>

    /** Which category an Accepted message's transaction is filed under, keyed by message id -
     *  lets the Messages list show a row's category without a per-row lookup. */
    fun observeCategoryIdsBySourceMessage(): Flow<Map<Long, Long>>

    /** Same idea as [observeCategoryIdsBySourceMessage], for the separate taxonomy value. */
    fun observeTaxonomyBySourceMessage(): Flow<Map<Long, TaxonomyType>>

    /** Which Accepted messages are linked to a split transaction, keyed by message id, with how
     *  much of it is owed back to friends - a message whose transaction was never split has no
     *  entry here at all (never ₹0). */
    fun observeOwedTotalBySourceMessage(): Flow<Map<Long, Money>>

    /** Which Accepted messages are linked to a transaction with its own saved title, keyed by
     *  message id - lets the Messages list show your renamed title instead of the SMS's own
     *  merchant. A transaction with no title saved yet has no entry here. */
    fun observeTitleBySourceMessage(): Flow<Map<Long, String>>

    /** Which Accepted messages are linked to a transaction, keyed by message id, with that
     *  transaction's real saved amount - used as a fallback once a message with no parsed amount
     *  of its own (06-backlog.md, "Richer inclusion gate") gets accepted with a real amount. */
    fun observeAmountBySourceMessage(): Flow<Map<Long, Money>>

    /** Ids of the messages whose linked transaction is marked Avoidable. */
    fun observeAvoidableSourceMessageIds(): Flow<Set<Long>>

    suspend fun getById(id: Long): Transaction?

    /** A manual transaction - no linked message. [owedTotal] is the sum of any friends' shares -
     *  see [Transaction]'s doc comment for how it affects the budget-counted amount. [title] is
     *  Add Transaction's "Title" field, saved as-is (null if left blank). */
    suspend fun insert(
        amount: Money,
        occurredAt: Instant,
        monthKey: MonthKey,
        categoryId: Long,
        note: String?,
        taxonomy: TaxonomyType? = null,
        owedTotal: Money = Money(0),
        title: String? = null,
        avoidable: Boolean = false
    ): Long

    suspend fun update(transaction: Transaction)

    /** Owed to You's "I've nudged about this" toggle - transaction-level, see [Transaction]'s
     *  doc comment. Never touches settled state or the budget-counted amount. */
    suspend fun setReminded(transactionId: Long, reminded: Boolean)

    /**
     * Inserts the transaction and marks [message] Accepted, in one database transaction - either
     * both happen or neither does. This is what makes "a message turns green only after the
     * transaction is saved" true. See the walkthrough in 12-tech-architecture.md.
     */
    suspend fun saveFromMessage(
        message: SmsMessage,
        amount: Money,
        occurredAt: Instant,
        monthKey: MonthKey,
        categoryId: Long,
        note: String?,
        taxonomy: TaxonomyType? = null,
        owedTotal: Money = Money(0),
        title: String? = null,
        avoidable: Boolean = false
    ): Long

    /**
     * Deletes the transaction and, if it came from an SMS, reverts that message to Not Assigned,
     * in one database transaction. Editing or deleting never sends budget alerts.
     */
    suspend fun delete(id: Long)

    /** Deletes whichever transaction is linked to [messageId] (if any) and reverts the message
     *  to Not Assigned - the un-accept path from the Messages screen. No-ops if the message has
     *  no linked transaction. */
    suspend fun deleteBySourceMessage(messageId: Long)
}
