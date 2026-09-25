package com.budgetmanager.app.data.repository

import androidx.room.withTransaction
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.core.model.SmsMessage
import com.budgetmanager.app.core.model.Transaction
import com.budgetmanager.app.data.database.AppDatabase
import com.budgetmanager.app.data.database.dao.SmsMessageDao
import com.budgetmanager.app.data.database.dao.TransactionDao
import com.budgetmanager.app.data.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class RoomTransactionRepository @Inject constructor(
    private val database: AppDatabase,
    private val transactionDao: TransactionDao,
    private val smsMessageDao: SmsMessageDao
) : TransactionRepository {

    override fun observeForCategoryAndMonth(monthKey: MonthKey, categoryId: Long): Flow<List<Transaction>> =
        transactionDao.observeForCategoryAndMonth(monthKey.value, categoryId)
            .map { entities -> entities.map { it.toDomain() } }

    override fun observeSpentForCategoryAndMonth(monthKey: MonthKey, categoryId: Long): Flow<Money> =
        transactionDao.observeSpentForCategoryAndMonth(monthKey.value, categoryId).map { Money(it) }

    override fun observeSpentByCategoryForMonth(monthKey: MonthKey): Flow<Map<Long, Money>> =
        transactionDao.observeSpentByCategoryForMonth(monthKey.value)
            .map { rows -> rows.associate { it.categoryId to Money(it.total) } }

    override fun observeCategoryIdsBySourceMessage(): Flow<Map<Long, Long>> =
        transactionDao.observeCategoryBySourceMessage()
            .map { rows -> rows.associate { it.messageId to it.categoryId } }

    override suspend fun getById(id: Long): Transaction? = transactionDao.getById(id)?.toDomain()

    override suspend fun insert(
        amount: Money,
        occurredAt: Instant,
        monthKey: MonthKey,
        categoryId: Long,
        note: String?
    ): Long = transactionDao.insert(
        TransactionEntity(
            amountPaise = amount.paise,
            occurredAt = occurredAt.toEpochMilli(),
            monthKey = monthKey.value,
            categoryId = categoryId,
            note = note,
            sourceMessageId = null
        )
    )

    override suspend fun update(transaction: Transaction) {
        transactionDao.update(transaction.toEntity())
    }

    override suspend fun saveFromMessage(
        message: SmsMessage,
        amount: Money,
        occurredAt: Instant,
        monthKey: MonthKey,
        categoryId: Long,
        note: String?
    ): Long = database.withTransaction {
        // source_message_id is unique - if a transaction is already linked to this message
        // (e.g. accepted once, then its status got out of sync some other way), update that one
        // instead of inserting a second row, which would violate the constraint and crash.
        val existing = transactionDao.getBySourceMessageId(message.id)
        val transactionId = if (existing != null) {
            transactionDao.update(
                existing.copy(
                    amountPaise = amount.paise,
                    occurredAt = occurredAt.toEpochMilli(),
                    monthKey = monthKey.value,
                    categoryId = categoryId,
                    note = note
                )
            )
            existing.id
        } else {
            transactionDao.insert(
                TransactionEntity(
                    amountPaise = amount.paise,
                    occurredAt = occurredAt.toEpochMilli(),
                    monthKey = monthKey.value,
                    categoryId = categoryId,
                    note = note,
                    sourceMessageId = message.id
                )
            )
        }
        smsMessageDao.updateStatus(message.id, MessageStatus.ACCEPTED)
        transactionId
    }

    override suspend fun delete(id: Long) {
        database.withTransaction {
            val entity = transactionDao.getById(id) ?: return@withTransaction
            transactionDao.delete(entity)
            entity.sourceMessageId?.let { messageId ->
                smsMessageDao.updateStatus(messageId, MessageStatus.NOT_ASSIGNED)
            }
        }
    }

    override suspend fun deleteBySourceMessage(messageId: Long) {
        database.withTransaction {
            val entity = transactionDao.getBySourceMessageId(messageId) ?: return@withTransaction
            transactionDao.delete(entity)
            smsMessageDao.updateStatus(messageId, MessageStatus.NOT_ASSIGNED)
        }
    }
}

private fun TransactionEntity.toDomain() = Transaction(
    id = id,
    amount = Money(amountPaise),
    occurredAt = Instant.ofEpochMilli(occurredAt),
    monthKey = MonthKey(monthKey),
    categoryId = categoryId,
    note = note,
    sourceMessageId = sourceMessageId
)

private fun Transaction.toEntity() = TransactionEntity(
    id = id,
    amountPaise = amount.paise,
    occurredAt = occurredAt.toEpochMilli(),
    monthKey = monthKey.value,
    categoryId = categoryId,
    note = note,
    sourceMessageId = sourceMessageId
)
