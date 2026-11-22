package com.budgetmanager.app.feature.messages

import com.budgetmanager.app.core.model.Category
import com.budgetmanager.app.core.model.MessageDirection
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.core.model.SmsMessage
import com.budgetmanager.app.core.model.TaxonomyType
import java.time.Instant

/** Chip order on screen: To review, Confirmed, Rejected, All. The names are the stored statuses;
 *  the on-screen words are in MessagesContent (NOT_ASSIGNED = "To review", ACCEPTED = "Confirmed"). */
enum class MessageFilter { NOT_ASSIGNED, ACCEPTED, REJECTED, ALL }

/** Category filter, independent of (stacks with) the status filter above - 06-backlog.md,
 *  "Richer Messages filters". A plain nullable id can't tell "any category is fine" apart from
 *  "specifically no category at all" - the latter is directly useful for finding messages that
 *  still need a new keyword rule - so this gets its own small sealed type instead. */
sealed interface CategoryFilter {
    data object Any : CategoryFilter
    data object None : CategoryFilter
    data class Specific(val categoryId: Long) : CategoryFilter
}

/** Same idea as [CategoryFilter], for the separate taxonomy (payment method) dimension. */
sealed interface TaxonomyFilter {
    data object Any : TaxonomyFilter
    data object None : TaxonomyFilter
    data class Specific(val taxonomy: TaxonomyType) : TaxonomyFilter
}

data class MessageRowUi(
    val id: Long,
    val sender: String,
    val merchantOrBody: String,
    /** "Your share" when the linked transaction is split with friends, otherwise the SMS's own
     *  full amount - see [splitTotalText]. */
    val amountText: String?,
    /** The original full amount, shown struck through next to [amountText] - only set once the
     *  linked transaction is actually split with friends (06-backlog.md, "Split with friends").
     *  Null otherwise, including for every Not assigned/Rejected row (nothing to split yet). */
    val splitTotalText: String? = null,
    val paymentMethod: String?,
    val receivedAt: Instant,
    val status: MessageStatus,
    val isNew: Boolean,
    val direction: MessageDirection,
    /** An Accepted row's actual category (from its transaction), or a Not assigned row's keyword
     *  suggestion - shown the same way either side, so you can see what a spend was (or would be)
     *  filed under without opening the message. Null for a Rejected row, or a Not assigned row
     *  with no keyword match. */
    val categoryEmoji: String?,
    val categoryName: String?,
    /** Same idea as [categoryEmoji]/[categoryName] but for the separate taxonomy (payment
     *  method) value - an Accepted row's real transaction taxonomy, or a Not assigned row's
     *  keyword suggestion. Null for Rejected, or when nothing is set/suggested. */
    val taxonomyLabel: String?,
    /** True when another Not assigned message looks like it could be the same real payment -
     *  same amount, different sender, within a few minutes (DetectPossibleDuplicates). */
    val isPossibleDuplicate: Boolean = false,
    /** What the row's amber line says it matches, e.g. "VM-PAYTMB, 12:06 · Confirmed". Null when
     *  the message matches nothing. Set whatever has been decided about either message. */
    val duplicateOfLabel: String? = null,
    /** The status of the message this one matches. */
    val duplicateOfStatus: MessageStatus? = null,
    /** The resolved value behind [categoryEmoji]/[categoryName] - an Accepted row's real
     *  transaction category id, or a Not assigned row's keyword suggestion, same status gating.
     *  Filtering needs this id directly rather than re-deriving it from the raw message a second
     *  time (06-backlog.md, "Richer Messages filters"). */
    val categoryId: Long? = null,
    /** Same idea as [categoryId], for [taxonomyLabel]. */
    val taxonomy: TaxonomyType? = null,
    /** How much friends owe you for this spend, once Confirmed and split - "₹2,200". */
    val owedTotalText: String? = null,
    /** True once Confirmed, when the linked transaction is marked Avoidable. */
    val isAvoidable: Boolean = false,
    /** Your share of this spend in paise - set only for a Confirmed debit, 0 otherwise. The day
     *  header adds these up, so a split bill counts only what you actually paid. */
    val spendPaise: Long = 0
)

/** The "Undo confirm?" question for a Confirmed money-out message. Undoing deletes its transaction
 *  (and the friend split with it), so the dialog names exactly what goes. */
data class UndoConfirmPrompt(val title: String, val owedText: String?) {
    fun message(): String =
        "This deletes the transaction “$title”" +
            (if (owedText != null) " and the $owedText friends owe you for it" else "") +
            ". The message goes back to To review."
}

data class MessagesUiState(
    val isLoading: Boolean = true,
    val filter: MessageFilter = MessageFilter.NOT_ASSIGNED,
    /** Null means both directions - stacks with [filter] (AND across dimensions), not a
     *  replacement for it (06-backlog.md, "Richer Messages filters"). */
    val directionFilter: MessageDirection? = null,
    val categoryFilter: CategoryFilter = CategoryFilter.Any,
    val taxonomyFilter: TaxonomyFilter = TaxonomyFilter.Any,
    /** True while the Category/Taxonomy filter sheet is open - those two dimensions have 9+ and
     *  8 values each, so they live behind this entry point rather than two more permanent chip
     *  rows alongside status and direction. */
    val showFiltersSheet: Boolean = false,
    /** For the filter sheet's Category chips (emoji + name) - the same list [MessageRowUi]'s
     *  resolved category values point into. */
    val categories: List<Category> = emptyList(),
    val monthKey: MonthKey = MonthKey.current(),
    /** The month selector's next arrow is disabled once this is false - messages are tied to
     *  real calendar dates that have already happened, so there's never a future month to see. */
    val canGoNext: Boolean = false,
    val rows: List<MessageRowUi> = emptyList(),
    val counts: Map<MessageFilter, Int> = emptyMap(),
    /** Each dimension's counts are independent - how many of the viewed month's messages fall
     *  into that one bucket overall, not cross-filtered against whatever else is currently
     *  selected in another dimension. Simpler to compute and reason about than a fully
     *  cross-filtered count (06-backlog.md, "Richer Messages filters"). */
    val directionCounts: Map<MessageDirection?, Int> = emptyMap(),
    val categoryCounts: Map<CategoryFilter, Int> = emptyMap(),
    val taxonomyCounts: Map<TaxonomyFilter, Int> = emptyMap(),
    val selectedMessage: SmsMessage? = null,
    /** The other message [selectedMessage] looks like a possible duplicate of, for the detail
     *  sheet to show side by side - null when there's no match. */
    val selectedMessageDuplicateOf: SmsMessage? = null,
    val undoRejectedMessageId: Long? = null,
    val navigateToAddTransactionForMessageId: Long? = null,
    /** True while the "Clear month" confirmation dialog is open. */
    val showClearMonthConfirm: Boolean = false,
    /** Set while the "Undo confirm?" dialog is open. */
    val undoConfirm: UndoConfirmPrompt? = null
)
