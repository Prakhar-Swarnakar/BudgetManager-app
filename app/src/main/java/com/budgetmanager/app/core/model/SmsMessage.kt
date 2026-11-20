package com.budgetmanager.app.core.model

import java.time.Instant

/** An SMS that looked like a spend (debit) or incoming money (credit). [parsedAmount] and
 *  [merchant] are null when the text could not be fully read - the message is still kept, with
 *  its raw [body], so nothing is lost. A credit message never has a [merchant] - it isn't
 *  classified into a type yet, so there's nothing to suggest a category/taxonomy against. */
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
    /** A keyword-rule guess at how this was paid for, same idea as [suggestedCategoryId] but
     *  against the separate taxonomy rule set - never auto-applied, only a pre-fill. */
    val suggestedTaxonomy: TaxonomyType? = null,
    val status: MessageStatus,
    val isNew: Boolean,
    /** Which way the money moved. Defaults to DEBIT so every existing call site (tests, fakes)
     *  that predates credit detection keeps compiling unchanged. */
    val direction: MessageDirection = MessageDirection.DEBIT
)
