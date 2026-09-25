package com.budgetmanager.app.feature.categorydetail

import com.budgetmanager.app.core.model.BudgetStatus
import com.budgetmanager.app.core.model.MonthKey
import java.time.Instant

data class CategoryDetailUiState(
    val isLoading: Boolean = true,
    val monthKey: MonthKey = MonthKey.current(),
    val categoryId: Long = 0,
    val categoryEmoji: String = "",
    val categoryName: String = "",
    val spentText: String = "₹0",
    val budgetText: String = "₹0",
    val remainingText: String = "₹0",
    val percentUsed: Double? = null,
    val status: BudgetStatus = BudgetStatus.UNDER_BUDGET,
    val isNotBudgeted: Boolean = false,
    val transactions: List<TransactionRowUi> = emptyList(),
    val pendingDeleteTransactionId: Long? = null,
    val editTransactionId: Long? = null
)

data class TransactionRowUi(
    val id: Long,
    val noteOrPlaceholder: String,
    val occurredAt: Instant,
    val amountText: String,
    val fromSms: Boolean
)
