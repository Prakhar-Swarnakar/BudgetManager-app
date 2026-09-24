package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeMonthlyBudgetRepository : MonthlyBudgetRepository {
    private val state = MutableStateFlow<Map<MonthKey, Map<Long, Money>>>(emptyMap())

    override fun observeForMonth(monthKey: MonthKey): Flow<Map<Long, Money>> =
        state.map { it[monthKey] ?: emptyMap() }

    override suspend fun setAmount(monthKey: MonthKey, categoryId: Long, amount: Money) {
        val forMonth = (state.value[monthKey] ?: emptyMap()) + (categoryId to amount)
        state.value = state.value + (monthKey to forMonth)
    }
}
