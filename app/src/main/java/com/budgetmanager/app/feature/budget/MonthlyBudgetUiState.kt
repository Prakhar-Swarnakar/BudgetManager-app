package com.budgetmanager.app.feature.budget

import com.budgetmanager.app.core.model.MonthKey

data class MonthlyBudgetUiState(
    val isLoading: Boolean = true,
    val monthKey: MonthKey = MonthKey.current(),
    val rows: List<BudgetRowUi> = emptyList(),
    val totalText: String = "₹0",
    val copiedFromPreviousMonth: Boolean = false,
    val sheet: BudgetSheetUiState? = null
)

data class BudgetRowUi(
    val categoryId: Long,
    val emoji: String,
    val name: String,
    val amountText: String,
    /** False when this category has no row for the month yet - [amountText] is then "₹0"
     *  shown in a muted style, per 08-pages-and-navigation.md. */
    val hasAmount: Boolean
)

sealed interface BudgetSheetMode {
    data class Edit(val categoryId: Long) : BudgetSheetMode
    data object New : BudgetSheetMode
}

/** One sheet shape for both jobs: creating a new category (name, icon, and its first amount)
 *  and editing an existing one (rename, change icon, and change its amount for this month) -
 *  category management lives on this page, there is no separate Categories page. */
data class BudgetSheetUiState(
    val mode: BudgetSheetMode,
    val name: String = "",
    val emoji: String = "",
    val amountInput: String = "",
    val nameError: String? = null,
    val emojiError: String? = null,
    val amountError: String? = null
)
