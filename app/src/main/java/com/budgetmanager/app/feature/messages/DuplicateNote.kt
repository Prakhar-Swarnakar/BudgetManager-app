package com.budgetmanager.app.feature.messages

import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.SmsMessage

/** The words the app uses for each stored status. */
fun MessageStatus.displayName(): String = when (this) {
    MessageStatus.NOT_ASSIGNED -> "To review"
    MessageStatus.ACCEPTED -> "Confirmed"
    MessageStatus.REJECTED -> "Rejected"
}

/** What the detail sheet says about a possible duplicate. [warn] false means a calm note: nothing
 *  can be counted twice any more. */
data class DuplicateNote(val heading: String, val body: String?, val warn: Boolean)

/** The wording for [message] matching [other], from what has been decided about each of them. */
fun duplicateNote(message: SmsMessage, other: SmsMessage): DuplicateNote {
    val amount = message.parsedAmount?.formatted() ?: "the amount"
    val from = other.sender
    return when {
        message.status == MessageStatus.REJECTED ->
            DuplicateNote("Matches a message from $from (${other.status.displayName()})", null, warn = false)
        other.status == MessageStatus.REJECTED -> DuplicateNote(
            "The matching message from $from was rejected",
            if (message.status == MessageStatus.NOT_ASSIGNED) "Safe to confirm this one." else null,
            warn = false
        )
        message.status == MessageStatus.NOT_ASSIGNED && other.status == MessageStatus.ACCEPTED -> DuplicateNote(
            "Already confirmed as the message from $from",
            "Confirming this one too would count $amount twice.",
            warn = true
        )
        message.status == MessageStatus.ACCEPTED && other.status == MessageStatus.ACCEPTED -> DuplicateNote(
            "Both are confirmed, so $amount may be counted twice",
            "Undo this one if it is the same payment.",
            warn = true
        )
        message.status == MessageStatus.ACCEPTED -> DuplicateNote(
            "Looks like the same payment as a message from $from still to review",
            "Reject that one if it is the same payment.",
            warn = true
        )
        else -> DuplicateNote(
            "These two look like the same payment",
            "Confirm one and reject the other.",
            warn = true
        )
    }
}
