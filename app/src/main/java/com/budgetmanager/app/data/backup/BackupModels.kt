package com.budgetmanager.app.data.backup

import com.budgetmanager.app.core.model.AlertType
import com.budgetmanager.app.core.model.MessageStatus
import kotlinx.serialization.Serializable

/**
 * The whole app's data, one file. Deliberately its own set of types rather than the Room
 * entities directly: a backup file must stay readable by future app versions whose database
 * schema has moved on, so its shape is decoupled from - and only changes when explicitly
 * versioned via [formatVersion], not - Room's own schema version. See 12-tech-architecture.md
 * section 7 and 07-open-questions.md row 24 (import replaces everything, after a summary and
 * confirmation - merging is in the backlog).
 */
@Serializable
data class BackupBundle(
    val formatVersion: Int,
    val exportedAtMillis: Long,
    val categories: List<CategoryBackup>,
    val monthlyBudgets: List<MonthlyBudgetBackup>,
    val transactions: List<TransactionBackup>,
    val smsMessages: List<SmsMessageBackup>,
    val alertLogs: List<AlertLogBackup>,
    val keywordRules: List<KeywordRuleBackup>
) {
    companion object {
        /** Bump this, and only this, when BackupBundle's shape changes - never reuse a number. */
        const val CURRENT_FORMAT_VERSION = 1
    }
}

@Serializable
data class CategoryBackup(
    val id: Long,
    val name: String,
    val emoji: String,
    val sortOrder: Int,
    val archived: Boolean
)

@Serializable
data class MonthlyBudgetBackup(
    val id: Long,
    val monthKey: String,
    val categoryId: Long,
    val amountPaise: Long
)

@Serializable
data class TransactionBackup(
    val id: Long,
    val amountPaise: Long,
    val occurredAt: Long,
    val monthKey: String,
    val categoryId: Long,
    val note: String?,
    val sourceMessageId: Long?
)

@Serializable
data class SmsMessageBackup(
    val id: Long,
    val sender: String,
    val body: String,
    val receivedAt: Long,
    val smsProviderId: String?,
    val dedupeKey: String,
    val parsedAmountPaise: Long?,
    val merchant: String?,
    val paymentMethod: String?,
    val suggestedCategoryId: Long?,
    val status: MessageStatus,
    val isNew: Boolean
)

@Serializable
data class AlertLogBackup(
    val id: Long,
    val monthKey: String,
    val categoryId: Long,
    val type: AlertType
)

@Serializable
data class KeywordRuleBackup(
    val keyword: String,
    val categoryId: Long
)
