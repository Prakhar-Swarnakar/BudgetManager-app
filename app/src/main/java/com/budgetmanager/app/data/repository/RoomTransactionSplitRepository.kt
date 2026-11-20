package com.budgetmanager.app.data.repository

import androidx.room.withTransaction
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.OwedToYouEntry
import com.budgetmanager.app.core.model.TransactionSplit
import com.budgetmanager.app.data.database.AppDatabase
import com.budgetmanager.app.data.database.dao.TransactionSplitDao
import com.budgetmanager.app.data.database.entity.TransactionSplitEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class RoomTransactionSplitRepository @Inject constructor(
    private val database: AppDatabase,
    private val transactionSplitDao: TransactionSplitDao
) : TransactionSplitRepository {

    override fun observeByTransaction(transactionId: Long): Flow<List<TransactionSplit>> =
        transactionSplitDao.observeByTransaction(transactionId).map { entities -> entities.map { it.toDomain() } }

    override fun observeOwedToYou(): Flow<List<OwedToYouEntry>> =
        transactionSplitDao.observeOwedToYou().map { rows -> rows.toOwedToYouEntries() }

    override suspend fun replaceSplits(transactionId: Long, splits: List<TransactionSplitInput>) {
        database.withTransaction {
            transactionSplitDao.deleteByTransaction(transactionId)
            if (splits.isNotEmpty()) {
                transactionSplitDao.insertAll(
                    splits.map { input ->
                        TransactionSplitEntity(
                            transactionId = transactionId,
                            friendId = input.friendId,
                            amountOwedPaise = input.amount.paise,
                            settled = input.settled,
                            settledAt = input.settledAt?.toEpochMilli()
                        )
                    }
                )
            }
        }
    }

    override suspend fun settle(splitId: Long) {
        transactionSplitDao.settle(splitId, Instant.now().toEpochMilli())
    }
}

private fun TransactionSplitEntity.toDomain() = TransactionSplit(
    id = id,
    transactionId = transactionId,
    friendId = friendId,
    amountOwed = Money(amountOwedPaise),
    settled = settled,
    settledAt = settledAt?.let { Instant.ofEpochMilli(it) }
)
