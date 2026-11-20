package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.CashSpend
import com.budgetmanager.app.core.model.CashTopUp
import com.budgetmanager.app.core.model.Money
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** The Wallet page's data: top-ups (cash in) and cash spends (transactions paid with Cash).
 *  The balance itself is never stored - callers derive it from these two lists. */
interface CashRepository {
    fun observeTopUps(): Flow<List<CashTopUp>>
    fun observeSpends(): Flow<List<CashSpend>>
    suspend fun addTopUp(amount: Money, occurredAt: Instant, note: String?)
    suspend fun deleteTopUp(id: Long)
}
