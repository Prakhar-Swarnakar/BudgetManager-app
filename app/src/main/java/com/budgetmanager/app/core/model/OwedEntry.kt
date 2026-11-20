package com.budgetmanager.app.core.model

import java.time.Instant

/** One friend's share of one transaction - [settled] is unused by the Owed to You page's own UI
 *  right now (settling is deferred to a future round, done from Add Transaction's edit screen
 *  instead), but still carried through so [OwedToYouEntry.totalOwed] excludes anything already
 *  settled by some other means. */
data class OwedFriendShare(
    val friendId: Long,
    val friendName: String,
    val splitId: Long,
    val amountOwed: Money,
    val settled: Boolean
)

/** One row on the "Owed to you" page - a transaction split with one or more friends, with just
 *  enough about it to show without a per-row lookup. [totalOwed] is the sum of every listed
 *  friend's still-unsettled share - the actual receivable this row represents. */
data class OwedToYouEntry(
    val transactionId: Long,
    val title: String,
    val occurredAt: Instant,
    val categoryEmoji: String,
    val categoryName: String,
    val taxonomy: TaxonomyType?,
    val friends: List<OwedFriendShare>,
    val reminded: Boolean
) {
    val totalOwed: Money get() = Money(friends.filterNot { it.settled }.sumOf { it.amountOwed.paise })
}
