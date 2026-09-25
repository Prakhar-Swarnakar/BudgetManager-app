package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.SmsMessage
import com.budgetmanager.app.data.database.dao.SmsMessageDao
import com.budgetmanager.app.data.database.entity.SmsMessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class RoomMessageRepository @Inject constructor(
    private val smsMessageDao: SmsMessageDao
) : MessageRepository {

    override fun observeAll(): Flow<List<SmsMessage>> =
        smsMessageDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeByStatus(status: MessageStatus): Flow<List<SmsMessage>> =
        smsMessageDao.observeByStatus(status).map { entities -> entities.map { it.toDomain() } }

    override fun observeCountByStatus(status: MessageStatus): Flow<Int> =
        smsMessageDao.observeCountByStatus(status)

    override fun observeNewCount(): Flow<Int> = smsMessageDao.observeNewCount()

    override suspend fun getById(id: Long): SmsMessage? = smsMessageDao.getById(id)?.toDomain()

    override suspend fun ingest(message: SmsMessage): Long? {
        if (smsMessageDao.getByDedupeKey(message.dedupeKey) != null) return null
        return smsMessageDao.insert(message.toEntity())
    }

    override suspend fun reject(id: Long) {
        setStatus(id, MessageStatus.REJECTED)
    }

    override suspend fun setStatus(id: Long, status: MessageStatus) {
        smsMessageDao.updateStatus(id, status)
    }

    override suspend fun markAllSeen() {
        smsMessageDao.markAllSeen()
    }
}

private fun SmsMessageEntity.toDomain() = SmsMessage(
    id = id,
    sender = sender,
    body = body,
    receivedAt = Instant.ofEpochMilli(receivedAt),
    smsProviderId = smsProviderId,
    dedupeKey = dedupeKey,
    parsedAmount = parsedAmountPaise?.let { Money(it) },
    merchant = merchant,
    paymentMethod = paymentMethod,
    suggestedCategoryId = suggestedCategoryId,
    status = status,
    isNew = isNew
)

private fun SmsMessage.toEntity() = SmsMessageEntity(
    id = id,
    sender = sender,
    body = body,
    receivedAt = receivedAt.toEpochMilli(),
    smsProviderId = smsProviderId,
    dedupeKey = dedupeKey,
    parsedAmountPaise = parsedAmount?.paise,
    merchant = merchant,
    paymentMethod = paymentMethod,
    suggestedCategoryId = suggestedCategoryId,
    status = status,
    isNew = isNew
)
