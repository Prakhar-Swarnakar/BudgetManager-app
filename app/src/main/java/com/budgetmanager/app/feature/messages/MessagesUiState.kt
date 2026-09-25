package com.budgetmanager.app.feature.messages

import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.SmsMessage
import java.time.Instant

/** Order matches the filter chips in 04-messages-and-notifications.md: All, Not assigned, Accepted, Rejected. */
enum class MessageFilter { ALL, NOT_ASSIGNED, ACCEPTED, REJECTED }

data class MessageRowUi(
    val id: Long,
    val sender: String,
    val merchantOrBody: String,
    val amountText: String?,
    val paymentMethod: String?,
    val receivedAt: Instant,
    val status: MessageStatus,
    val isNew: Boolean,
    /** Set only for an Accepted row, from its transaction's category - shown so you don't have
     *  to open the message to see what a spend was filed under. */
    val categoryEmoji: String?,
    val categoryName: String?
)

data class MessagesUiState(
    val isLoading: Boolean = true,
    val filter: MessageFilter = MessageFilter.ALL,
    val rows: List<MessageRowUi> = emptyList(),
    val counts: Map<MessageFilter, Int> = emptyMap(),
    val selectedMessage: SmsMessage? = null,
    val undoRejectedMessageId: Long? = null,
    val navigateToAddTransactionForMessageId: Long? = null
)
