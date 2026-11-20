package com.budgetmanager.app.core.model

import java.time.Instant

/** Cash coming in - see [com.budgetmanager.app.data.database.entity.CashTopUpEntity]. */
data class CashTopUp(val id: Long, val amount: Money, val occurredAt: Instant, val note: String?)

/** One cash spend, for the Wallet ledger. [amount] is the full transaction amount. */
data class CashSpend(
    val transactionId: Long,
    val title: String,
    val amount: Money,
    val occurredAt: Instant,
    val categoryId: Long,
    val categoryEmoji: String,
    val categoryName: String
)
