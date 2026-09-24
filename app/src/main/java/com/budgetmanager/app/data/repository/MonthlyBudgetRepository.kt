package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import kotlinx.coroutines.flow.Flow

interface MonthlyBudgetRepository {
    /** Amounts for a month, keyed by category id. A category with no entry has no budget yet - ₹0. */
    fun observeForMonth(monthKey: MonthKey): Flow<Map<Long, Money>>
    suspend fun setAmount(monthKey: MonthKey, categoryId: Long, amount: Money)
}
