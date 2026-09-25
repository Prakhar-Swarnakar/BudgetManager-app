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
    data class EditAmount(val categoryId: Long) : BudgetSheetMode
    data object NewCategory : BudgetSheetMode
}

/** [name]/[emoji] only matter in [BudgetSheetMode.NewCategory] - editing an existing category's
 *  amount only ever touches [amountInput]. */
data class BudgetSheetUiState(
    val mode: BudgetSheetMode,
    val amountInput: String = "",
    val amountError: String? = null,
    val name: String = "",
    val emoji: String = "",
    val nameError: String? = null,
    val emojiError: String? = null
)
