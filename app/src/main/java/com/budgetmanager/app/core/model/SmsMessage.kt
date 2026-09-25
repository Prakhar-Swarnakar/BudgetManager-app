package com.budgetmanager.app.core.model

import java.time.Instant

/** An SMS that looked like a spend. [parsedAmount] and [merchant] are null when the text
 *  could not be fully read - the message is still kept, with its raw [body], so nothing is lost. */
data class SmsMessage(
    val id: Long,
    val sender: String,
    val body: String,
    val receivedAt: Instant,
    val smsProviderId: String?,
    val dedupeKey: String,
    val parsedAmount: Money?,
    val merchant: String?,
    /** e.g. "UPI", "NEFT" - null when the SMS text didn't say or the format isn't recognised. */
    val paymentMethod: String? = null,
    val suggestedCategoryId: Long?,
    val status: MessageStatus,
    val isNew: Boolean
)
