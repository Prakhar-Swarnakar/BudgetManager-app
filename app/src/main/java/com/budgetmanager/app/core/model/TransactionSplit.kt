package com.budgetmanager.app.core.model

import java.time.Instant

/**
 * One friend's share of a transaction you fronted. [amountOwed] is fixed at creation (or edit)
 * time and permanently excluded from that transaction's budget-counted amount, regardless of
 * [settled] - settling only closes the receivable for your own bookkeeping, it never re-touches
 * the category (the split already excluded that portion when it was created).
 */
data class TransactionSplit(
    val id: Long,
    val transactionId: Long,
    val friendId: Long,
    val amountOwed: Money,
    val settled: Boolean,
    val settledAt: Instant?
)
