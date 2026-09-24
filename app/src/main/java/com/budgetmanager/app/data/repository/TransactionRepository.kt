package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.core.model.SmsMessage
import com.budgetmanager.app.core.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface TransactionRepository {
    fun observeForCategoryAndMonth(monthKey: MonthKey, categoryId: Long): Flow<List<Transaction>>
    fun observeSpentForCategoryAndMonth(monthKey: MonthKey, categoryId: Long): Flow<Money>
    suspend fun getById(id: Long): Transaction?

    /** A manual transaction - no linked message. */
    suspend fun insert(amount: Money, occurredAt: Instant, monthKey: MonthKey, categoryId: Long, note: String?): Long

    suspend fun update(transaction: Transaction)

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
        note: String?
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
