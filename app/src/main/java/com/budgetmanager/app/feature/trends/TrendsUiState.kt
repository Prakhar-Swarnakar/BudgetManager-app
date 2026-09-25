package com.budgetmanager.app.feature.trends

import com.budgetmanager.app.core.model.BudgetStatus
import com.budgetmanager.app.domain.DonutSlice

data class TrendsUiState(
    val isLoading: Boolean = true,
    val monthLabel: String = "",
    val totalBudgetText: String = "₹0",
    val overallPercentText: String = "0%",
    /** False when nothing has a budget yet this month - the donut has nothing meaningful to draw. */
    val hasBudget: Boolean = false,
    val slices: List<DonutSlice> = emptyList(),
    val rows: List<TrendsCategoryRowUi> = emptyList()
)

data class TrendsCategoryRowUi(
    val categoryId: Long,
    val emoji: String,
    val name: String,
    val allocatedText: String,
    val usedText: String,
    val statusText: String,
    val status: BudgetStatus,
    val isNotBudgeted: Boolean
)
