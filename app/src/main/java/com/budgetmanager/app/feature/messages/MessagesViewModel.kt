package com.budgetmanager.app.feature.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.SmsMessage
import com.budgetmanager.app.data.repository.CategoryRepository
import com.budgetmanager.app.data.repository.MessageRepository
import com.budgetmanager.app.sms.InboxScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val inboxScanner: InboxScanner,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val filter = MutableStateFlow(MessageFilter.ALL)
    private val selectedMessageId = MutableStateFlow<Long?>(null)
    private val undoRejectedMessageId = MutableStateFlow<Long?>(null)
    private val navigateToAddTransactionForMessageId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<MessagesUiState> = combine(
        messageRepository.observeAll(),
        filter,
        selectedMessageId,
        undoRejectedMessageId,
        navigateToAddTransactionForMessageId
    ) { messages, selectedFilter, selectedId, undoId, navId ->
        MessagesUiState(
            isLoading = false,
            filter = selectedFilter,
            rows = messages.filter { matchesFilter(it, selectedFilter) }.map { it.toRowUi() },
            counts = MessageFilter.entries.associateWith { f -> messages.count { matchesFilter(it, f) } },
            selectedMessage = messages.firstOrNull { it.id == selectedId },
            undoRejectedMessageId = undoId,
            navigateToAddTransactionForMessageId = navId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MessagesUiState())

    fun onFilterSelected(newFilter: MessageFilter) {
        filter.value = newFilter
    }

    /**
     * Swipe right (StartToEnd): requests navigation to Add Transaction, for Not assigned and
     * for Rejected alike - "a rejected message can still be accepted later, by swiping right"
     * (04-messages-and-notifications.md). Accepted is a no-op; only saving a transaction there
     * (M4) actually turns a message green.
     */
    fun onSwipeStart(id: Long) {
        viewModelScope.launch {
            when (messageRepository.getById(id)?.status) {
                MessageStatus.NOT_ASSIGNED, MessageStatus.REJECTED -> navigateToAddTransactionForMessageId.value = id
                else -> Unit
            }
        }
    }

    /** Swipe left (EndToStart): Not assigned -> Rejected, with undo. Accepted and Rejected are
     *  no-ops here - un-accepting means deleting the transaction (Category detail, M7), not a
     *  simple status flip. */
    fun onSwipeEnd(id: Long) {
        viewModelScope.launch {
            when (messageRepository.getById(id)?.status) {
                MessageStatus.NOT_ASSIGNED -> {
                    messageRepository.reject(id)
                    undoRejectedMessageId.value = id
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
        navigateToAddTransactionForMessageId.value = id
        selectedMessageId.value = null
    }

    fun onRejectFromDetail(id: Long) {
        viewModelScope.launch { messageRepository.reject(id) }
        selectedMessageId.value = null
    }

    /** Called when the screen is left (not opened) - rows keep their new-flag highlight for as
     *  long as the user is looking at the page, per 04-messages-and-notifications.md. */
    fun onLeftScreen() {
        viewModelScope.launch { messageRepository.markAllSeen() }
    }

    /** Debug-only (see MessagesContent's BuildConfig.DEBUG gate): backfills today's real spend
     *  SMS as Not assigned, for quick testing without waiting on the live receiver. */
    fun onImportTodaySms() {
        viewModelScope.launch {
            val startOfToday = LocalDate.now(ZoneId.systemDefault())
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            inboxScanner.scanFrom(startOfToday)
        }
    }

    /** Debug-only: inserts one fabricated Not assigned message so a "new" row shows up instantly.
     *  Includes a parsed amount and a suggested category (picks the first active one) - real
     *  received SMS won't have these until M2b's per-bank parser exists, so this is the only way
     *  to exercise Add Transaction's pre-fill before then. */
    fun onAddTestMessage() {
        viewModelScope.launch {
            val now = Instant.now()
            val amountRupees = (50..999).random()
            val firstCategoryId = categoryRepository.observeActive().first().firstOrNull()?.id
            val message = SmsMessage(
                id = 0,
                sender = "TESTBANK",
                body = "Rs $amountRupees.00 debited from a/c XX1234 at TEST MERCHANT. Avl bal Rs 5000.",
                receivedAt = now,
                smsProviderId = null,
                dedupeKey = "debug-${now.toEpochMilli()}-${(0..999_999).random()}",
                parsedAmount = Money.ofRupees(amountRupees.toLong()),
                merchant = "TEST MERCHANT",
                suggestedCategoryId = firstCategoryId,
                status = MessageStatus.NOT_ASSIGNED,
                isNew = true
            )
            messageRepository.ingest(message)
        }
    }

    /** Debug-only: resets a message back to Not assigned regardless of its current status, for
     *  quickly re-testing the accept/reject flow without waiting for a fresh SMS. Does NOT touch
     *  any transaction that might already be linked - unlike the real revert path (deleting the
     *  transaction, M7), this is purely for exercising the Messages screen's states. */
    fun onDebugResetToNotAssigned(id: Long) {
        viewModelScope.launch { messageRepository.setStatus(id, MessageStatus.NOT_ASSIGNED) }
        selectedMessageId.value = null
    }

    private fun matchesFilter(message: SmsMessage, f: MessageFilter): Boolean = when (f) {
        MessageFilter.ALL -> true
        MessageFilter.NOT_ASSIGNED -> message.status == MessageStatus.NOT_ASSIGNED
        MessageFilter.ACCEPTED -> message.status == MessageStatus.ACCEPTED
        MessageFilter.REJECTED -> message.status == MessageStatus.REJECTED
    }

    private fun SmsMessage.toRowUi() = MessageRowUi(
        id = id,
        sender = sender,
        merchantOrBody = merchant ?: body.take(60),
        amountText = parsedAmount?.formatted(),
        receivedAt = receivedAt,
        status = status,
        isNew = isNew
    )
}
