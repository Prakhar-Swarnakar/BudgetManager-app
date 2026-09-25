package com.budgetmanager.app.feature.home

import com.budgetmanager.app.core.model.BudgetStatus
import com.budgetmanager.app.core.model.MonthKey

data class HomeUiState(
    val isLoading: Boolean = true,
    val monthKey: MonthKey = MonthKey.current(),
    /** The month selector's next arrow is disabled once this is false - Home never shows a
     *  future month, unlike Monthly budget (08-pages-and-navigation.md). */
    val canGoNext: Boolean = false,
    val totalSpentText: String = "₹0",
    val totalBudgetText: String = "₹0",
    val totalRemainingText: String = "₹0",
    val totalPercentUsed: Double? = null,
    val totalStatus: BudgetStatus = BudgetStatus.UNDER_BUDGET,
    /** Messages still Not assigned - the "N messages to review" line. Distinct from the "new"
     *  flag used for the Messages tab's red badge. */
    val reviewCount: Int = 0,
    val cards: List<HomeCategoryCardUi> = emptyList()
)

data class HomeCategoryCardUi(
    val categoryId: Long,
    val emoji: String,
    val name: String,
    val spentText: String,
    /** Formatted remaining amount - already carries its own "-" when negative (Money.formatted()). */
    val remainingText: String,
    val percentUsed: Double?,
    val status: BudgetStatus,
    /** True for a ₹0-budget category that has spending - shown as "Not budgeted" instead of a
     *  percentage, per 05-budget-rules.md. */
    val isNotBudgeted: Boolean
)
