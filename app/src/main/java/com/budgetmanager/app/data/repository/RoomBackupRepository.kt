package com.budgetmanager.app.data.repository

import androidx.room.withTransaction
import com.budgetmanager.app.data.backup.AlertLogBackup
import com.budgetmanager.app.data.backup.BackupBundle
import com.budgetmanager.app.data.backup.CategoryBackup
import com.budgetmanager.app.data.backup.KeywordRuleBackup
import com.budgetmanager.app.data.backup.MonthlyBudgetBackup
import com.budgetmanager.app.data.backup.SmsMessageBackup
import com.budgetmanager.app.data.backup.TransactionBackup
import com.budgetmanager.app.data.database.AppDatabase
import com.budgetmanager.app.data.database.dao.AlertLogDao
import com.budgetmanager.app.data.database.dao.CategoryDao
import com.budgetmanager.app.data.database.dao.KeywordRuleDao
import com.budgetmanager.app.data.database.dao.MonthlyBudgetDao
import com.budgetmanager.app.data.database.dao.SmsMessageDao
import com.budgetmanager.app.data.database.dao.TransactionDao
import com.budgetmanager.app.data.database.entity.AlertLogEntity
import com.budgetmanager.app.data.database.entity.CategoryEntity
import com.budgetmanager.app.data.database.entity.KeywordRuleEntity
import com.budgetmanager.app.data.database.entity.MonthlyBudgetEntity
import com.budgetmanager.app.data.database.entity.SmsMessageEntity
import com.budgetmanager.app.data.database.entity.TransactionEntity
import javax.inject.Inject

class RoomBackupRepository @Inject constructor(
    private val database: AppDatabase,
    private val categoryDao: CategoryDao,
    private val monthlyBudgetDao: MonthlyBudgetDao,
    private val transactionDao: TransactionDao,
    private val smsMessageDao: SmsMessageDao,
    private val alertLogDao: AlertLogDao,
    private val keywordRuleDao: KeywordRuleDao
) : BackupRepository {

    override suspend fun buildBackup(): BackupBundle = BackupBundle(
        formatVersion = BackupBundle.CURRENT_FORMAT_VERSION,
        exportedAtMillis = System.currentTimeMillis(),
        categories = categoryDao.getAllOnce().map { it.toBackup() },
        monthlyBudgets = monthlyBudgetDao.getAllOnce().map { it.toBackup() },
        transactions = transactionDao.getAllOnce().map { it.toBackup() },
        smsMessages = smsMessageDao.getAllOnce().map { it.toBackup() },
        alertLogs = alertLogDao.getAllOnce().map { it.toBackup() },
        keywordRules = keywordRuleDao.getAll().map { it.toBackup() }
    )

    override suspend fun restore(bundle: BackupBundle) {
        database.withTransaction {
            // Children before parents, so a foreign key never briefly points at a row that's
            // already gone.
            alertLogDao.deleteAll()
            transactionDao.deleteAll()
            smsMessageDao.deleteAll()
            monthlyBudgetDao.deleteAll()
            keywordRuleDao.deleteAll()
            categoryDao.deleteAll()

            // Parents before children. Every id is inserted exactly as backed up (not
            // auto-generated) so every foreign key relationship between tables survives -
            // deleteAll leaves each table empty, so there's nothing for an explicit id to
            // conflict with.
            categoryDao.insertAll(bundle.categories.map { it.toEntity() })
            keywordRuleDao.insertAll(bundle.keywordRules.map { it.toEntity() })
            monthlyBudgetDao.upsertAll(bundle.monthlyBudgets.map { it.toEntity() })
            smsMessageDao.insertAll(bundle.smsMessages.map { it.toEntity() })
            transactionDao.insertAll(bundle.transactions.map { it.toEntity() })
            alertLogDao.insertAll(bundle.alertLogs.map { it.toEntity() })
        }
    }
}

private fun CategoryEntity.toBackup() = CategoryBackup(id, name, emoji, sortOrder, archived)
private fun CategoryBackup.toEntity() = CategoryEntity(id, name, emoji, sortOrder, archived)

private fun MonthlyBudgetEntity.toBackup() = MonthlyBudgetBackup(id, monthKey, categoryId, amountPaise)
private fun MonthlyBudgetBackup.toEntity() = MonthlyBudgetEntity(id, monthKey, categoryId, amountPaise)

private fun TransactionEntity.toBackup() =
    TransactionBackup(id, amountPaise, occurredAt, monthKey, categoryId, note, sourceMessageId)
private fun TransactionBackup.toEntity() =
    TransactionEntity(id, amountPaise, occurredAt, monthKey, categoryId, note, sourceMessageId)

private fun SmsMessageEntity.toBackup() = SmsMessageBackup(
    id, sender, body, receivedAt, smsProviderId, dedupeKey, parsedAmountPaise, merchant,
    paymentMethod, suggestedCategoryId, status, isNew
)
private fun SmsMessageBackup.toEntity() = SmsMessageEntity(
    id = id, sender = sender, body = body, receivedAt = receivedAt, smsProviderId = smsProviderId,
    dedupeKey = dedupeKey, parsedAmountPaise = parsedAmountPaise, merchant = merchant,
    paymentMethod = paymentMethod, suggestedCategoryId = suggestedCategoryId,
    status = status, isNew = isNew
)

private fun AlertLogEntity.toBackup() = AlertLogBackup(id, monthKey, categoryId, type)
private fun AlertLogBackup.toEntity() = AlertLogEntity(id, monthKey, categoryId, type)

private fun KeywordRuleEntity.toBackup() = KeywordRuleBackup(keyword, categoryId)
private fun KeywordRuleBackup.toEntity() = KeywordRuleEntity(keyword, categoryId)
