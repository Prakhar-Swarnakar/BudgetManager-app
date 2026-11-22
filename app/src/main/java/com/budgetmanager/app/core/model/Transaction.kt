package com.budgetmanager.app.core.model

import java.time.Instant

/** [sourceMessageId] is set when this came from accepting an SMS, and links back to it.
 *  [taxonomy] is how it was paid for - independent of [categoryId], which is what it was spent
 *  on. [amount] is always the full amount your bank actually shows, even when split with
 *  friends - [owedTotal] is the sum of their shares, kept separately so [amount] never has to be
 *  reconstructed from splits. Only [amount] minus [owedTotal] ever counts toward the category's
 *  budget (see TransactionDao's spent queries) - this never changes once a split is created,
 *  regardless of whether a friend later settles their share. [title] is what Add Transaction's
 *  "Title" field saves - null for a transaction never saved since this field existed, or where it
 *  was left blank; callers display it with a fallback chain (linked message's merchant, then
 *  [note], then a placeholder) rather than ever showing nothing. Deliberately separate from the
 *  linked message's own `merchant`, which is never written to - renaming a title never alters
 *  what the SMS actually said. */
data class Transaction(
    val id: Long,
    val amount: Money,
    val occurredAt: Instant,
    val monthKey: MonthKey,
    val categoryId: Long,
    val note: String?,
    val sourceMessageId: Long?,
    val taxonomy: TaxonomyType? = null,
    val owedTotal: Money = Money(0),
    val title: String? = null,
    /** Owed to You's "I've nudged about this" marker - see [TransactionEntity]'s doc comment. */
    val reminded: Boolean = false,
    /** Spend marked as avoidable at Add Transaction - see [TransactionEntity]'s doc comment. */
    val avoidable: Boolean = false
)
