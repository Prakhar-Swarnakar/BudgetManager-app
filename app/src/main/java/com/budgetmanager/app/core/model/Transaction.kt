package com.budgetmanager.app.core.model

import java.time.Instant

/** [sourceMessageId] is set when this came from accepting an SMS, and links back to it. */
data class Transaction(
    val id: Long,
    val amount: Money,
    val occurredAt: Instant,
    val monthKey: MonthKey,
    val categoryId: Long,
    val note: String?,
    val sourceMessageId: Long?
)
