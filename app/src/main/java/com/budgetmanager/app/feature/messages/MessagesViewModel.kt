package com.budgetmanager.app.feature.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.SmsMessage
import com.budgetmanager.app.data.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val messageRepository: MessageRepository
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

    fun onSwipeAccept(id: Long) {
        navigateToAddTransactionForMessageId.value = id
    }

    fun onNavigationHandled() {
        navigateToAddTransactionForMessageId.value = null
    }

    fun onSwipeReject(id: Long) {
        viewModelScope.launch {
            messageRepository.reject(id)
            undoRejectedMessageId.value = id
        }
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
        onSwipeReject(id)
        selectedMessageId.value = null
    }

    /** Called when the screen is left (not opened) - rows keep their new-flag highlight for as
     *  long as the user is looking at the page, per 04-messages-and-notifications.md. */
    fun onLeftScreen() {
        viewModelScope.launch { messageRepository.markAllSeen() }
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
