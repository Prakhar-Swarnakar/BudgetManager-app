package com.budgetmanager.app.feature.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.core.model.Category
import com.budgetmanager.app.core.model.MessageDirection
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.core.model.SmsMessage
import com.budgetmanager.app.core.model.TaxonomyType
import com.budgetmanager.app.data.repository.CategoryRepository
import com.budgetmanager.app.data.repository.MessageRepository
import com.budgetmanager.app.data.repository.TransactionRepository
import com.budgetmanager.app.domain.DetectPossibleDuplicates
import com.budgetmanager.app.sms.CategorySuggester
import com.budgetmanager.app.sms.InboxScanner
import com.budgetmanager.app.sms.TaxonomySuggester
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

/** The 5 things the message-list combine needs besides the messages themselves, bundled so that
 *  step can stay a plain 2-flow combine instead of needing a 6-arg one. */
private data class SelectionState(
    val filter: MessageFilter,
    val selectedId: Long?,
    val undoId: Long?,
    val navId: Long?,
    val monthKey: MonthKey
)

/** The 3 extra filter dimensions (06-backlog.md, "Richer Messages filters") bundled separately
 *  from [SelectionState] rather than growing it to an 8-arg combine - direction is nullable
 *  (null = both); category/taxonomy each need their own 3-state sealed type (Any/None/Specific)
 *  since a nullable id can't tell "any" apart from "none". */
private data class FilterDimensions(
    val direction: MessageDirection?,
    val category: CategoryFilter,
    val taxonomy: TaxonomyFilter
)

private data class RawMessagesState(
    val messages: List<SmsMessage>,
    val selection: SelectionState,
    val dimensions: FilterDimensions
)

/** The per-message lookups the row mapping needs besides the messages themselves, bundled for
 *  the same reason as [SelectionState] - a plain 4-arg combine rather than nesting Triples. */
private data class MessageLookupsBase(
    val categories: List<Category>,
    val categoryIdsByMessage: Map<Long, Long>,
    val taxonomyByMessage: Map<Long, TaxonomyType>,
    val owedTotalByMessage: Map<Long, Money>,
    val titleByMessage: Map<Long, String>
)

/** [MessageLookupsBase] plus the amount fallback, combined as its own step rather than growing
 *  the base combine to a 6th direct argument - see [FilterDimensions] for the same reasoning. */
private data class MessageLookups(
    val categories: List<Category>,
    val categoryIdsByMessage: Map<Long, Long>,
    val taxonomyByMessage: Map<Long, TaxonomyType>,
    val owedTotalByMessage: Map<Long, Money>,
    val titleByMessage: Map<Long, String>,
    val amountByMessage: Map<Long, Money>
)

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val transactionRepository: TransactionRepository,
    private val inboxScanner: InboxScanner,
    private val categoryRepository: CategoryRepository,
    private val categorySuggester: CategorySuggester,
    private val taxonomySuggester: TaxonomySuggester
) : ViewModel() {

    private val filter = MutableStateFlow(MessageFilter.ALL)
    private val selectedMessageId = MutableStateFlow<Long?>(null)
    private val undoRejectedMessageId = MutableStateFlow<Long?>(null)
    private val navigateToAddTransactionForMessageId = MutableStateFlow<Long?>(null)
    private val monthKey = MutableStateFlow(MonthKey.current())
    private val showClearMonthConfirm = MutableStateFlow(false)
    private val directionFilter = MutableStateFlow<MessageDirection?>(null)
    private val categoryFilter = MutableStateFlow<CategoryFilter>(CategoryFilter.Any)
    private val taxonomyFilter = MutableStateFlow<TaxonomyFilter>(TaxonomyFilter.Any)
    private val showFiltersSheet = MutableStateFlow(false)

    private val selectionState = combine(
        filter, selectedMessageId, undoRejectedMessageId, navigateToAddTransactionForMessageId, monthKey
    ) { f, selectedId, undoId, navId, month -> SelectionState(f, selectedId, undoId, navId, month) }

    private val filterDimensions = combine(
        directionFilter, categoryFilter, taxonomyFilter
    ) { direction, category, taxonomy -> FilterDimensions(direction, category, taxonomy) }

    private val messageLookups = combine(
        categoryRepository.observeAll(),
        transactionRepository.observeCategoryIdsBySourceMessage(),
        transactionRepository.observeTaxonomyBySourceMessage(),
        transactionRepository.observeOwedTotalBySourceMessage(),
        transactionRepository.observeTitleBySourceMessage()
    ) { categories, categoryIdsByMessage, taxonomyByMessage, owedTotalByMessage, titleByMessage ->
        MessageLookupsBase(categories, categoryIdsByMessage, taxonomyByMessage, owedTotalByMessage, titleByMessage)
    }.combine(transactionRepository.observeAmountBySourceMessage()) { base, amountByMessage ->
        MessageLookups(
            base.categories, base.categoryIdsByMessage, base.taxonomyByMessage,
            base.owedTotalByMessage, base.titleByMessage, amountByMessage
        )
    }

    val uiState: StateFlow<MessagesUiState> = combine(
        messageRepository.observeAll(),
        selectionState,
        filterDimensions
    ) { messages, selection, dimensions ->
        RawMessagesState(messages, selection, dimensions)
    }.combine(messageLookups) { raw, lookups ->
        val categoryById = lookups.categories.associateBy { it.id }
        val messageById = raw.messages.associateBy { it.id }
        val zone = ZoneId.systemDefault()
        // Computed against every message, not just the viewed month's - a bank alert just before
        // midnight and a UPI app's confirmation just after it are still the same real payment.
        val duplicateOfId = DetectPossibleDuplicates(raw.messages)
        val monthMessages = raw.messages.filter { MonthKey.from(it.receivedAt, zone) == raw.selection.monthKey }
        val selectedMessage = messageById[raw.selection.selectedId]
        // Resolved for every one of this month's messages, unfiltered - both the per-dimension
        // counts and the final filtered list below are derived from this, rather than each
        // re-deriving category/taxonomy from the raw message a second time.
        val allRows = monthMessages.map {
            it.toRowUi(
                lookups.categoryIdsByMessage, categoryById, lookups.taxonomyByMessage,
                lookups.owedTotalByMessage, lookups.titleByMessage, lookups.amountByMessage,
                duplicateOfId.containsKey(it.id)
            )
        }
        MessagesUiState(
            isLoading = false,
            filter = raw.selection.filter,
            directionFilter = raw.dimensions.direction,
            categoryFilter = raw.dimensions.category,
            taxonomyFilter = raw.dimensions.taxonomy,
            categories = lookups.categories,
            monthKey = raw.selection.monthKey,
            canGoNext = raw.selection.monthKey < MonthKey.current(),
            rows = allRows.filter { matchesAllFilters(it, raw.selection.filter, raw.dimensions) },
            counts = MessageFilter.entries.associateWith { f -> monthMessages.count { matchesFilter(it.status, f) } },
            directionCounts = mapOf(
                null to monthMessages.size,
                MessageDirection.DEBIT to monthMessages.count { it.direction == MessageDirection.DEBIT },
                MessageDirection.CREDIT to monthMessages.count { it.direction == MessageDirection.CREDIT }
            ),
            categoryCounts = buildMap {
                put(CategoryFilter.Any, allRows.size)
                put(CategoryFilter.None, allRows.count { it.categoryId == null })
                lookups.categories.forEach { category ->
                    val value = CategoryFilter.Specific(category.id)
                    put(value, allRows.count { it.categoryId == category.id })
                }
            },
            taxonomyCounts = buildMap {
                put(TaxonomyFilter.Any, allRows.size)
                put(TaxonomyFilter.None, allRows.count { it.taxonomy == null })
                TaxonomyType.entries.forEach { taxonomy ->
                    val value = TaxonomyFilter.Specific(taxonomy)
                    put(value, allRows.count { it.taxonomy == taxonomy })
                }
            },
            selectedMessage = selectedMessage,
            selectedMessageDuplicateOf = duplicateOfId[selectedMessage?.id]?.let { messageById[it] },
            undoRejectedMessageId = raw.selection.undoId,
            navigateToAddTransactionForMessageId = raw.selection.navId
        )
    }.combine(showClearMonthConfirm) { state, showConfirm ->
        state.copy(showClearMonthConfirm = showConfirm)
    }.combine(showFiltersSheet) { state, showFilters ->
        state.copy(showFiltersSheet = showFilters)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MessagesUiState())

    fun onFilterSelected(newFilter: MessageFilter) {
        filter.value = newFilter
    }

    fun onDirectionFilterSelected(direction: MessageDirection?) {
        directionFilter.value = direction
    }

    fun onCategoryFilterSelected(newFilter: CategoryFilter) {
        categoryFilter.value = newFilter
    }

    fun onTaxonomyFilterSelected(newFilter: TaxonomyFilter) {
        taxonomyFilter.value = newFilter
    }

    fun onFiltersClicked() {
        showFiltersSheet.value = true
    }

    fun onFiltersDismissed() {
        showFiltersSheet.value = false
    }

    fun onPreviousMonth() {
        monthKey.value = monthKey.value.previous()
    }

    /** Messages are tied to real calendar dates that have already happened - same restriction
     *  as Home, never Monthly budget's "set next month's budget ahead of time" case. */
    fun onNextMonth() {
        if (monthKey.value < MonthKey.current()) {
            monthKey.value = monthKey.value.next()
        }
    }

    /**
     * Swipe right (StartToEnd). On a debit message, only Not assigned can move into Accepted
     * (via Add Transaction - that's the only thing that actually makes a debit message
     * Accepted). A credit message has no transaction to create, so its Not assigned -> Accepted
     * is a direct status toggle instead - "yes, this is real incoming money, keep it logged"
     * (06-backlog.md, "Credit handling"). Rejected reverts to Not assigned either way. Accepted
     * is a no-op - only Not assigned can become Accepted or Rejected.
     */
    fun onSwipeStart(id: Long) {
        viewModelScope.launch {
            val message = messageRepository.getById(id) ?: return@launch
            when (message.status) {
                MessageStatus.NOT_ASSIGNED -> when (message.direction) {
                    MessageDirection.DEBIT -> navigateToAddTransactionForMessageId.value = id
                    MessageDirection.CREDIT -> messageRepository.setStatus(id, MessageStatus.ACCEPTED)
                }
                MessageStatus.REJECTED -> messageRepository.setStatus(id, MessageStatus.NOT_ASSIGNED)
                else -> Unit
            }
        }
    }

    /**
     * Swipe left (EndToStart). Only Not assigned can move into Rejected, with undo. An Accepted
     * debit message reverts to Not assigned by deleting its linked transaction (not just
     * flipping the status - that would leave the transaction orphaned and re-crash the accept
     * path). An Accepted credit message has no transaction at all, so it reverts with a direct
     * status flip instead - deleteBySourceMessage would silently delete nothing and leave it
     * permanently stranded as Accepted. Rejected is a no-op - only Not assigned can become
     * Accepted or Rejected.
     */
    fun onSwipeEnd(id: Long) {
        viewModelScope.launch {
            val message = messageRepository.getById(id) ?: return@launch
            when (message.status) {
                MessageStatus.NOT_ASSIGNED -> {
                    messageRepository.reject(id)
                    undoRejectedMessageId.value = id
                }
                MessageStatus.ACCEPTED -> when (message.direction) {
                    MessageDirection.DEBIT -> transactionRepository.deleteBySourceMessage(id)
                    MessageDirection.CREDIT -> messageRepository.setStatus(id, MessageStatus.NOT_ASSIGNED)
                }
                else -> Unit
            }
        }
    }

    fun onNavigationHandled() {
        navigateToAddTransactionForMessageId.value = null
    }

    fun onUndoReject() {
        val id = undoRejectedMessageId.value ?: return
        viewModelScope.launch {
            messageRepository.setStatus(id, MessageStatus.NOT_ASSIGNED)
            undoRejectedMessageId.value = null
        }
    }

    fun onUndoDismissed() {
        undoRejectedMessageId.value = null
    }

    fun onRowClick(id: Long) {
        selectedMessageId.value = id
    }

    fun onDetailDismissed() {
        selectedMessageId.value = null
    }

    fun onAcceptFromDetail(id: Long) {
        viewModelScope.launch {
            val message = messageRepository.getById(id)
            if (message?.status == MessageStatus.NOT_ASSIGNED) {
                when (message.direction) {
                    MessageDirection.DEBIT -> navigateToAddTransactionForMessageId.value = id
                    MessageDirection.CREDIT -> messageRepository.setStatus(id, MessageStatus.ACCEPTED)
                }
            }
        }
        selectedMessageId.value = null
    }

    fun onRejectFromDetail(id: Long) {
        viewModelScope.launch {
            if (messageRepository.getById(id)?.status == MessageStatus.NOT_ASSIGNED) {
                messageRepository.reject(id)
            }
        }
        selectedMessageId.value = null
    }

    /** Called when the screen is left (not opened) - rows keep their new-flag highlight for as
     *  long as the user is looking at the page, per 04-messages-and-notifications.md. */
    fun onLeftScreen() {
        viewModelScope.launch { messageRepository.markAllSeen() }
    }

    /** Fetches SMS for the currently viewed month straight from the SMS Provider, independent of
     *  the automatic catch-up marker - for backfilling a month the live receiver missed, or one
     *  from before the app was set up. Safe to run more than once: already-seen messages are
     *  skipped by their dedupe key (see sms/DedupeKey.kt), same as any other ingest path. */
    fun onFetchMonth() {
        viewModelScope.launch {
            val (start, end) = monthKey.value.toMillisRange(ZoneId.systemDefault())
            inboxScanner.scanRange(start, end)
        }
    }

    /** Re-runs category AND taxonomy rules against every Not assigned debit message in the
     *  currently viewed month, using whatever keyword rules exist right now - lets a rule added
     *  today fix an old suggestion without re-deciding anything. Only ever touches
     *  [SmsMessage.suggestedCategoryId]/[SmsMessage.suggestedTaxonomy]: never status, never an
     *  Accepted message's real transaction category/taxonomy, and Accepted/Rejected messages
     *  aren't considered at all. Credit messages are skipped entirely - they have no merchant,
     *  so keyword-matching their raw body text could spuriously match an unrelated rule and
     *  write a nonsensical suggestion onto a message type that should never have one. */
    fun onRunRule() {
        viewModelScope.launch {
            val month = monthKey.value
            val zone = ZoneId.systemDefault()
            val notAssignedThisMonth = messageRepository.observeByStatus(MessageStatus.NOT_ASSIGNED).first()
                .filter { MonthKey.from(it.receivedAt, zone) == month && it.direction == MessageDirection.DEBIT }
            notAssignedThisMonth.forEach { message ->
                val newCategorySuggestion = categorySuggester.suggest(message.merchant ?: message.body)
                if (newCategorySuggestion != message.suggestedCategoryId) {
                    messageRepository.updateSuggestedCategory(message.id, newCategorySuggestion)
                }
                // Taxonomy keywords live in the SMS body's boilerplate, never in a merchant name -
                // always check the full body, not merchant-or-body (see SmsReceiver/DefaultInboxScanner).
                val newTaxonomySuggestion = taxonomySuggester.suggest(message.body)
                if (newTaxonomySuggestion != message.suggestedTaxonomy) {
                    messageRepository.updateSuggestedTaxonomy(message.id, newTaxonomySuggestion)
                }
            }
        }
    }

    fun onClearMonthClicked() {
        showClearMonthConfirm.value = true
    }

    fun onClearMonthDismissed() {
        showClearMonthConfirm.value = false
    }

    /** Deletes every message in the currently viewed month, regardless of status. A linked
     *  transaction is kept, only unlinked from its source message (ON DELETE SET NULL) - its
     *  amount and category spend are untouched. */
    fun onClearMonthConfirmed() {
        viewModelScope.launch {
            val (start, end) = monthKey.value.toMillisRange(ZoneId.systemDefault())
            messageRepository.deleteByReceivedAtRange(start, end)
            showClearMonthConfirm.value = false
        }
    }

    private fun matchesFilter(status: MessageStatus, f: MessageFilter): Boolean = when (f) {
        MessageFilter.ALL -> true
        MessageFilter.NOT_ASSIGNED -> status == MessageStatus.NOT_ASSIGNED
        MessageFilter.ACCEPTED -> status == MessageStatus.ACCEPTED
        MessageFilter.REJECTED -> status == MessageStatus.REJECTED
    }

    /** AND across every dimension - status, direction, category, taxonomy - per the design in
     *  06-backlog.md, "Richer Messages filters" (stackable, not mutually exclusive). */
    private fun matchesAllFilters(row: MessageRowUi, statusFilter: MessageFilter, dims: FilterDimensions): Boolean {
        if (!matchesFilter(row.status, statusFilter)) return false
        if (dims.direction != null && row.direction != dims.direction) return false
        if (!matchesCategoryFilter(row.categoryId, dims.category)) return false
        if (!matchesTaxonomyFilter(row.taxonomy, dims.taxonomy)) return false
        return true
    }

    private fun matchesCategoryFilter(categoryId: Long?, f: CategoryFilter): Boolean = when (f) {
        CategoryFilter.Any -> true
        CategoryFilter.None -> categoryId == null
        is CategoryFilter.Specific -> categoryId == f.categoryId
    }

    private fun matchesTaxonomyFilter(taxonomy: TaxonomyType?, f: TaxonomyFilter): Boolean = when (f) {
        TaxonomyFilter.Any -> true
        TaxonomyFilter.None -> taxonomy == null
        is TaxonomyFilter.Specific -> taxonomy == f.taxonomy
    }

    private fun SmsMessage.toRowUi(
        categoryIdsByMessage: Map<Long, Long>,
        categoryById: Map<Long, Category>,
        taxonomyByMessage: Map<Long, TaxonomyType>,
        owedTotalByMessage: Map<Long, Money>,
        titleByMessage: Map<Long, String>,
        amountByMessage: Map<Long, Money>,
        isPossibleDuplicate: Boolean
    ): MessageRowUi {
        // Accepted shows its transaction's real category; Not assigned shows the keyword
        // suggestion (if any) in the same spot, so you can see what it'll be filed under before
        // you've even opened it - Rejected never shows one, since it isn't a real spend.
        val displayCategoryId = when (status) {
            MessageStatus.ACCEPTED -> categoryIdsByMessage[id]
            MessageStatus.NOT_ASSIGNED -> suggestedCategoryId
            MessageStatus.REJECTED -> null
        }
        val category = displayCategoryId?.let { categoryById[it] }
        // Same idea as category, for the separate taxonomy value.
        val displayTaxonomy = when (status) {
            MessageStatus.ACCEPTED -> taxonomyByMessage[id]
            MessageStatus.NOT_ASSIGNED -> suggestedTaxonomy
            MessageStatus.REJECTED -> null
        }
        // A split transaction's share is only ever meaningful once Accepted - a Not assigned or
        // Rejected message can never be in owedTotalByMessage at all (it only has entries for
        // messages with a real linked transaction), but status is checked explicitly anyway to
        // match how category/taxonomy are gated above, rather than relying on that incidentally.
        // A message with no parsed amount of its own (06-backlog.md / real gap found
        // 2026-10-07: some genuine transaction SMS never state an amount inline, e.g. a mutual
        // fund SIP purchase confirmation that only points to a separate statement) falls back to
        // its linked transaction's real saved amount once Accepted, rather than showing nothing
        // forever even after you've actually told it what the amount was.
        val effectiveAmount = parsedAmount ?: amountByMessage[id]?.takeIf { status == MessageStatus.ACCEPTED }
        val owedTotal = owedTotalByMessage[id]?.takeIf { status == MessageStatus.ACCEPTED }
        val amountText: String?
        val splitTotalText: String?
        if (owedTotal != null && effectiveAmount != null) {
            amountText = Money(effectiveAmount.paise - owedTotal.paise).formatted()
            splitTotalText = effectiveAmount.formatted()
        } else {
            amountText = effectiveAmount?.formatted()
            splitTotalText = null
        }
        // A renamed title only ever applies once Accepted (same gating as category/taxonomy/
        // split above) - the message's own merchant is never touched by a title edit, so a Not
        // assigned or Rejected row always shows exactly what the SMS said, unchanged.
        val displayTitle = if (status == MessageStatus.ACCEPTED) titleByMessage[id] else null
        return MessageRowUi(
            id = id,
            sender = sender,
            merchantOrBody = displayTitle ?: merchant ?: body.take(60),
            amountText = amountText,
            splitTotalText = splitTotalText,
            paymentMethod = paymentMethod,
            receivedAt = receivedAt,
            status = status,
            isNew = isNew,
            direction = direction,
            categoryEmoji = category?.emoji,
            categoryName = category?.name,
            taxonomyLabel = displayTaxonomy?.label,
            isPossibleDuplicate = isPossibleDuplicate,
            categoryId = displayCategoryId,
            taxonomy = displayTaxonomy
        )
    }
}
