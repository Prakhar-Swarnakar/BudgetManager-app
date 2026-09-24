package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.data.database.dao.MonthlyBudgetDao
import com.budgetmanager.app.data.database.entity.MonthlyBudgetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomMonthlyBudgetRepository @Inject constructor(
    private val monthlyBudgetDao: MonthlyBudgetDao
) : MonthlyBudgetRepository {

    override fun observeForMonth(monthKey: MonthKey): Flow<Map<Long, Money>> =
        monthlyBudgetDao.observeForMonth(monthKey.value).map { rows ->
            rows.associate { it.categoryId to Money(it.amountPaise) }
        }

    override suspend fun setAmount(monthKey: MonthKey, categoryId: Long, amount: Money) {
        monthlyBudgetDao.upsert(
            MonthlyBudgetEntity(monthKey = monthKey.value, categoryId = categoryId, amountPaise = amount.paise)
        )
    }
}
