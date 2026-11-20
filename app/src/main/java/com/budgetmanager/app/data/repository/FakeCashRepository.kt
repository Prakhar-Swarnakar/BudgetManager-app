package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.CashSpend
import com.budgetmanager.app.core.model.CashTopUp
import com.budgetmanager.app.core.model.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

class FakeCashRepository : CashRepository {
    private val topUps = MutableStateFlow<List<CashTopUp>>(emptyList())
    private val spends = MutableStateFlow<List<CashSpend>>(emptyList())
    private var nextId = 1L

    override fun observeTopUps() = topUps.map { list -> list.sortedByDescending { it.occurredAt } }
    override fun observeSpends() = spends.map { list -> list.sortedByDescending { it.occurredAt } }

    override suspend fun addTopUp(amount: Money, occurredAt: Instant, note: String?) {
        topUps.value = topUps.value + CashTopUp(nextId++, amount, occurredAt, note)
    }

    override suspend fun deleteTopUp(id: Long) {
        topUps.value = topUps.value.filterNot { it.id == id }
    }

    fun seedSpends(list: List<CashSpend>) {
        spends.value = list
    }
}
