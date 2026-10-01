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
    /** An Accepted row's actual category (from its transaction), or a Not assigned row's keyword
     *  suggestion - shown the same way either side, so you can see what a spend was (or would be)
     *  filed under without opening the message. Null for a Rejected row, or a Not assigned row
     *  with no keyword match. */
    val categoryEmoji: String?,
    val categoryName: String?,
    /** True when another Not assigned message looks like it could be the same real payment -
     *  same amount, different sender, within a few minutes (DetectPossibleDuplicates). */
    val isPossibleDuplicate: Boolean = false
)

data class MessagesUiState(
    val isLoading: Boolean = true,
    val filter: MessageFilter = MessageFilter.ALL,
    val rows: List<MessageRowUi> = emptyList(),
    val counts: Map<MessageFilter, Int> = emptyMap(),
    val selectedMessage: SmsMessage? = null,
    /** The other message [selectedMessage] looks like a possible duplicate of, for the detail
     *  sheet to show side by side - null when there's no match. */
    val selectedMessageDuplicateOf: SmsMessage? = null,
    val undoRejectedMessageId: Long? = null,
    val navigateToAddTransactionForMessageId: Long? = null
)
