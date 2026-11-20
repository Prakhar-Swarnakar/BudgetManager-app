package com.budgetmanager.app.feature.owedtoyou

import com.budgetmanager.app.core.designsystem.components.FilterChipItem
import com.budgetmanager.app.core.model.Money
import java.time.Instant

enum class OwedToYouViewMode { BY_TRANSACTION, BY_FRIEND }

enum class ReminderFilter { ALL, PENDING, REMINDED }

/** One row on the "Owed to you" page - a transaction split with one or more friends. [reminded]
 *  is transaction-level, not per friend (06-backlog.md: a single GPay/Splitwise request commonly
 *  covers the whole group expense). Settling is deferred to a later round (done from Add
 *  Transaction's edit screen instead), so there's no settled state surfaced here yet. */
data class OwedToYouRowUi(
    val transactionId: Long,
    val title: String,
    val occurredAt: Instant,
    /** Every friend on this transaction. Alphabetical order in By transaction view; in By friend
     *  view, the section's own friend is moved to the front so the row reads naturally wherever
     *  it's shown - [com.budgetmanager.app.core.designsystem.components.ActivityRow] truncates
     *  this to a couple of visible tags plus a "+N" overflow regardless. */
    val friendNames: List<String>,
    val reminded: Boolean,
    /** In By transaction view, every friend's combined share (the whole receivable this row
     *  represents). In By friend view, the section's own friend's individual share instead - see
     *  [splitTotalText] for the combined total alongside it in that case. */
    val amountText: String,
    /** The combined total owed by every friend on this transaction, struck through - only set in
     *  By friend view, and only when more than one friend is on the split (otherwise it would
     *  just repeat [amountText]). Null in By transaction view and for a single-friend split. */
    val splitTotalText: String? = null,
    val categoryEmoji: String,
    val categoryName: String,
    val taxonomyLabel: String?
)

/** By friend view's one collapsible section - every transaction that friend has a share in,
 *  newest first (same order [OwedToYouRowUi] already comes in). Collapsed by default. */
data class OwedToYouFriendSectionUi(
    val friendId: Long,
    val friendName: String,
    val pendingText: String,
    val rows: List<OwedToYouRowUi>
)

data class OwedToYouUiState(
    val isLoading: Boolean = true,
    val viewMode: OwedToYouViewMode = OwedToYouViewMode.BY_TRANSACTION,
    val statusFilter: ReminderFilter = ReminderFilter.ALL,
    val statusFilterOptions: List<FilterChipItem<ReminderFilter>> = emptyList(),
    /** null = every friend. */
    val friendFilter: Long? = null,
    val friendFilterOptions: List<FilterChipItem<Long?>> = emptyList(),
    /** By transaction view's list, already filtered. */
    val rows: List<OwedToYouRowUi> = emptyList(),
    /** By friend view's list, already filtered. */
    val friendSections: List<OwedToYouFriendSectionUi> = emptyList(),
    val expandedFriendIds: Set<Long> = emptySet(),
    val totalPendingText: String = Money.Zero.formatted(),
    /** True while the Friend filter sheet is open - same idea as Messages' Filters sheet: Status
     *  stays the one chip row always visible, Friend lives behind this entry point instead of a
     *  second permanent chip row. */
    val showFiltersSheet: Boolean = false
)
