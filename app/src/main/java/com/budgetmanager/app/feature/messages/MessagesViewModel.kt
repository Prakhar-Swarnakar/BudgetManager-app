package com.budgetmanager.app.feature.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.core.model.Category
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
import java.time.YearMonth
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

private data class RawMessagesState(
    val messages: List<SmsMessage>,
    val selection: SelectionState
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

    private val selectionState = combine(
        filter, selectedMessageId, undoRejectedMessageId, navigateToAddTransactionForMessageId, monthKey
    ) { f, selectedId, undoId, navId, month -> SelectionState(f, selectedId, undoId, navId, month) }

    val uiState: StateFlow<MessagesUiState> = combine(
        messageRepository.observeAll(),
        selectionState
    ) { messages, selection ->
        RawMessagesState(messages, selection)
    }.combine(categoryRepository.observeAll()) { raw, categories ->
        raw to categories
    }.combine(transactionRepository.observeCategoryIdsBySourceMessage()) { (raw, categories), categoryIdsByMessage ->
        Triple(raw, categories, categoryIdsByMessage)
    }.combine(transactionRepository.observeTaxonomyBySourceMessage()) { (raw, categories, categoryIdsByMessage), taxonomyByMessage ->
        val categoryById = categories.associateBy { it.id }
        val messageById = raw.messages.associateBy { it.id }
        val zone = ZoneId.systemDefault()
        // Computed against every message, not just the viewed month's - a bank alert just before
        // midnight and a UPI app's confirmation just after it are still the same real payment.
        val duplicateOfId = DetectPossibleDuplicates(raw.messages)
        val monthMessages = raw.messages.filter { MonthKey.from(it.receivedAt, zone) == raw.selection.monthKey }
        val selectedMessage = messageById[raw.selection.selectedId]
        MessagesUiState(
            isLoading = false,
            filter = raw.selection.filter,
            monthKey = raw.selection.monthKey,
            canGoNext = raw.selection.monthKey < MonthKey.current(),
            rows = monthMessages.filter { matchesFilter(it, raw.selection.filter) }
                .map { it.toRowUi(categoryIdsByMessage, categoryById, taxonomyByMessage, duplicateOfId.containsKey(it.id)) },
            counts = MessageFilter.entries.associateWith { f -> monthMessages.count { matchesFilter(it, f) } },
            selectedMessage = selectedMessage,
            selectedMessageDuplicateOf = duplicateOfId[selectedMessage?.id]?.let { messageById[it] },
            undoRejectedMessageId = raw.selection.undoId,
            navigateToAddTransactionForMessageId = raw.selection.navId
        )
    }.combine(showClearMonthConfirm) { state, showConfirm ->
        state.copy(showClearMonthConfirm = showConfirm)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MessagesUiState())

    fun onFilterSelected(newFilter: MessageFilter) {
        filter.value = newFilter
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
     * Swipe right (StartToEnd). Only Not assigned can move into Accepted (via Add Transaction -
     * that's the only thing that actually makes a message Accepted). Rejected reverts to Not
     * assigned. Accepted is a no-op - only Not assigned can become Accepted or Rejected.
     */
    fun onSwipeStart(id: Long) {
        viewModelScope.launch {
            when (messageRepository.getById(id)?.status) {
                MessageStatus.NOT_ASSIGNED -> navigateToAddTransactionForMessageId.value = id
                MessageStatus.REJECTED -> messageRepository.setStatus(id, MessageStatus.NOT_ASSIGNED)
                else -> Unit
            }
        }
    }

    /**
     * Swipe left (EndToStart). Only Not assigned can move into Rejected, with undo. Accepted
     * reverts to Not assigned by deleting its linked transaction (not just flipping the status -
     * that would leave the transaction orphaned and re-crash the accept path). Rejected is a
     * no-op - only Not assigned can become Accepted or Rejected.
     */
    fun onSwipeEnd(id: Long) {
        viewModelScope.launch {
            when (messageRepository.getById(id)?.status) {
                MessageStatus.NOT_ASSIGNED -> {
                    messageRepository.reject(id)
                    undoRejectedMessageId.value = id
                }
                MessageStatus.ACCEPTED -> transactionRepository.deleteBySourceMessage(id)
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
            if (messageRepository.getById(id)?.status == MessageStatus.NOT_ASSIGNED) {
                navigateToAddTransactionForMessageId.value = id
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

    /** Re-runs category AND taxonomy rules against every Not assigned message in the currently
     *  viewed month, using whatever keyword rules exist right now - lets a rule added today fix
     *  an old suggestion without re-deciding anything. Only ever touches
     *  [SmsMessage.suggestedCategoryId]/[SmsMessage.suggestedTaxonomy]: never status, never an
     *  Accepted message's real transaction category/taxonomy, and Accepted/Rejected messages
     *  aren't considered at all. */
    fun onRunRule() {
        viewModelScope.launch {
            val month = monthKey.value
            val zone = ZoneId.systemDefault()
            val notAssignedThisMonth = messageRepository.observeByStatus(MessageStatus.NOT_ASSIGNED).first()
                .filter { MonthKey.from(it.receivedAt, zone) == month }
            notAssignedThisMonth.forEach { message ->
                val text = message.merchant ?: message.body
                val newCategorySuggestion = categorySuggester.suggest(text)
                if (newCategorySuggestion != message.suggestedCategoryId) {
                    messageRepository.updateSuggestedCategory(message.id, newCategorySuggestion)
                }
                val newTaxonomySuggestion = taxonomySuggester.suggest(text)
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

    private fun matchesFilter(message: SmsMessage, f: MessageFilter): Boolean = when (f) {
        MessageFilter.ALL -> true
        MessageFilter.NOT_ASSIGNED -> message.status == MessageStatus.NOT_ASSIGNED
        MessageFilter.ACCEPTED -> message.status == MessageStatus.ACCEPTED
        MessageFilter.REJECTED -> message.status == MessageStatus.REJECTED
    }

    private fun SmsMessage.toRowUi(
        categoryIdsByMessage: Map<Long, Long>,
        categoryById: Map<Long, Category>,
        taxonomyByMessage: Map<Long, TaxonomyType>,
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
        return MessageRowUi(
            id = id,
            sender = sender,
            merchantOrBody = merchant ?: body.take(60),
            amountText = parsedAmount?.formatted(),
            paymentMethod = paymentMethod,
            receivedAt = receivedAt,
            status = status,
            isNew = isNew,
            categoryEmoji = category?.emoji,
            categoryName = category?.name,
            taxonomyLabel = displayTaxonomy?.label,
            isPossibleDuplicate = isPossibleDuplicate
        )
    }
}

/** [MonthKey]'s [start, end) as device-local epoch millis, for an SMS Provider date-range query. */
private fun MonthKey.toMillisRange(zone: ZoneId): Pair<Long, Long> {
    val start = YearMonth.of(year, month).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val end = YearMonth.of(year, month).plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return start to end
}
