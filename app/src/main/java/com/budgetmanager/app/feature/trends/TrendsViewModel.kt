package com.budgetmanager.app.feature.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.core.model.BudgetStatus
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.data.repository.CategoryRepository
import com.budgetmanager.app.data.repository.MonthlyBudgetRepository
import com.budgetmanager.app.data.repository.TransactionRepository
import com.budgetmanager.app.domain.BuildThisMonthTrends
import com.budgetmanager.app.domain.TrendsCategoryRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class TrendsViewModel @Inject constructor(
    categoryRepository: CategoryRepository,
    monthlyBudgetRepository: MonthlyBudgetRepository,
    transactionRepository: TransactionRepository
) : ViewModel() {

    val uiState: StateFlow<TrendsUiState> = combine(
        categoryRepository.observeActive(),
        monthlyBudgetRepository.observeForMonth(MonthKey.current()),
        transactionRepository.observeSpentByCategoryForMonth(MonthKey.current())
    ) { categories, budgets, spent ->
        val trends = BuildThisMonthTrends(categories, budgets, spent)
        TrendsUiState(
            isLoading = false,
            monthLabel = MonthKey.current().monthName(),
            totalBudgetText = trends.totalBudget.formatted(),
            overallPercentText = trends.overallPercentUsed.asPercentText(),
            hasBudget = trends.totalBudget.paise > 0,
            slices = trends.slices,
            rows = trends.rows.map { it.toRowUi() }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrendsUiState())
}

private fun TrendsCategoryRow.toRowUi() = TrendsCategoryRowUi(
    categoryId = categoryId,
    emoji = emoji,
    name = name,
    allocatedText = "${budget.formatted()} allocated",
    usedText = "${spent.formatted()} used",
    statusText = when {
        isNotBudgeted -> "Not budgeted"
        status == BudgetStatus.OVER_BUDGET -> "Over by ${(spent - budget).formatted()}"
        else -> percentUsed.asPercentText() + " used"
    },
    status = status,
    isNotBudgeted = isNotBudgeted
)

private fun Double?.asPercentText(): String = "${((this ?: 0.0) * 100).roundToInt()}%"

private fun MonthKey.monthName(): String =
    YearMonth.of(year, month).month.getDisplayName(TextStyle.FULL, Locale.getDefault())
