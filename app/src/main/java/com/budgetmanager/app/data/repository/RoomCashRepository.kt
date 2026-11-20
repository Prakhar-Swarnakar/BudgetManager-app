package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.CashSpend
import com.budgetmanager.app.core.model.CashTopUp
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.data.database.dao.CashTopUpDao
import com.budgetmanager.app.data.database.dao.TransactionDao
import com.budgetmanager.app.data.database.entity.CashTopUpEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class RoomCashRepository @Inject constructor(
    private val cashTopUpDao: CashTopUpDao,
    private val transactionDao: TransactionDao
) : CashRepository {

    override fun observeTopUps(): Flow<List<CashTopUp>> = cashTopUpDao.observeAll().map { rows ->
        rows.map { CashTopUp(it.id, Money(it.amountPaise), Instant.ofEpochMilli(it.occurredAt), it.note) }
    }

    override fun observeSpends(): Flow<List<CashSpend>> = transactionDao.observeCashSpends().map { rows ->
        rows.map {
            CashSpend(
                transactionId = it.transactionId,
                // Same fallback order the rest of the app uses: saved title, then note.
                title = it.title?.takeIf { t -> t.isNotBlank() }
                    ?: it.note?.takeIf { n -> n.isNotBlank() }
                    ?: "Cash spend",
                amount = Money(it.amountPaise),
                occurredAt = Instant.ofEpochMilli(it.occurredAt),
                categoryId = it.categoryId,
                categoryEmoji = it.categoryEmoji,
                categoryName = it.categoryName
            )
        }
    }

    override suspend fun addTopUp(amount: Money, occurredAt: Instant, note: String?) {
        cashTopUpDao.insert(
            CashTopUpEntity(amountPaise = amount.paise, occurredAt = occurredAt.toEpochMilli(), note = note)
        )
    }

    override suspend fun deleteTopUp(id: Long) = cashTopUpDao.delete(id)
}
