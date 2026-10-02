package com.budgetmanager.app.core.model

import java.time.Instant

/** [sourceMessageId] is set when this came from accepting an SMS, and links back to it.
 *  [taxonomy] is how it was paid for - independent of [categoryId], which is what it was spent
 *  on. */
data class Transaction(
    val id: Long,
    val amount: Money,
    val occurredAt: Instant,
    val monthKey: MonthKey,
    val categoryId: Long,
    val note: String?,
    val sourceMessageId: Long?,
    val taxonomy: TaxonomyType? = null
)
