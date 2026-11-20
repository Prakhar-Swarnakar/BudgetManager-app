package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.OwedFriendShare
import com.budgetmanager.app.core.model.OwedToYouEntry
import com.budgetmanager.app.data.database.dao.OwedEntryRow
import java.time.Instant

/** Groups one-row-per-friend-per-transaction into one [OwedToYouEntry] per transaction, each
 *  listing every friend on it - shared by [RoomTransactionSplitRepository] (fed real query rows)
 *  and [FakeTransactionSplitRepository] (fed the same shape built from its own in-memory state),
 *  so both repositories group identically rather than risking two implementations drifting
 *  apart. Preserves the incoming rows' first-seen transaction order (already
 *  newest-transaction-first from the real query). */
internal fun List<OwedEntryRow>.toOwedToYouEntries(): List<OwedToYouEntry> {
    val transactionOrder = distinctBy { it.transactionId }.map { it.transactionId }
    val rowsByTransaction = groupBy { it.transactionId }
    return transactionOrder.map { transactionId ->
        val rows = rowsByTransaction.getValue(transactionId)
        val first = rows.first()
        OwedToYouEntry(
            transactionId = transactionId,
            title = first.title?.takeIf { it.isNotBlank() } ?: first.note?.takeIf { it.isNotBlank() } ?: "No note",
            occurredAt = Instant.ofEpochMilli(first.occurredAt),
            categoryEmoji = first.categoryEmoji,
            categoryName = first.categoryName,
            taxonomy = first.taxonomy,
            reminded = first.reminded,
            friends = rows.map {
                OwedFriendShare(
                    friendId = it.friendId,
                    friendName = it.friendName,
                    splitId = it.splitId,
                    amountOwed = Money(it.amountOwedPaise),
                    settled = it.settled
                )
            }
        )
    }
}
