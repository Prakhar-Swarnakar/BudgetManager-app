package com.budgetmanager.app.feature.trends

import com.budgetmanager.app.core.designsystem.components.GroupedBar
import com.budgetmanager.app.core.designsystem.components.PercentBar
import com.budgetmanager.app.core.model.BudgetStatus
import com.budgetmanager.app.domain.DonutSlice

data class TrendsUiState(
    val isLoading: Boolean = true,

    // This month
    val monthLabel: String = "",
    val totalBudgetText: String = "₹0",
    val overallPercentText: String = "0%",
    /** False when nothing has a budget yet this month - the donut has nothing meaningful to draw. */
    val hasBudget: Boolean = false,
    val slices: List<DonutSlice> = emptyList(),
    val rows: List<TrendsCategoryRowUi> = emptyList(),

    // Previous month
    val percentBars: List<PercentBar> = emptyList(),
    val comparisonTitle: String = "",
    val comparisonRows: List<CategoryComparisonRowUi> = emptyList(),

    // Historic
    val groupedBars: List<GroupedBar> = emptyList(),
    val averageSpentText: String = "₹0",
    val monthsOverBudgetText: String = "",
    val historicRangeLabel: String = "",

    // Shared range setting
    val rangeFooterText: String = ""
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

data class CategoryComparisonRowUi(
    val categoryId: Long,
    val emoji: String,
    val name: String,
    val currentText: String,
    val previousText: String,
    val differenceText: String,
    val isIncrease: Boolean
)
