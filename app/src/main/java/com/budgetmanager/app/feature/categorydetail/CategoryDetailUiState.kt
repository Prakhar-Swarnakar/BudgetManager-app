package com.budgetmanager.app.feature.categorydetail

import com.budgetmanager.app.core.model.BudgetStatus
import com.budgetmanager.app.core.model.MonthKey
import java.time.Instant

data class CategoryDetailUiState(
    val isLoading: Boolean = true,
    val monthKey: MonthKey = MonthKey.current(),
    /** The month selector's next arrow is disabled once this is false - a category's own
     *  spending is tied to dates that have already happened, same as Home and Messages. */
    val canGoNext: Boolean = false,
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
    /** "Your share" when this transaction is split with friends, otherwise the full amount. */
    val amountText: String,
    /** The original full amount, struck through - only set once this transaction is actually
     *  split with friends. Null for every non-split transaction. */
    val splitTotalText: String? = null,
    val fromSms: Boolean
)
