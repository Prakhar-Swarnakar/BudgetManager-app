package com.budgetmanager.app.feature.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.core.designsystem.components.GroupedBar
import com.budgetmanager.app.core.designsystem.components.PercentBar
import com.budgetmanager.app.core.model.BudgetStatus
import com.budgetmanager.app.core.model.Category
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.data.repository.CategoryRepository
import com.budgetmanager.app.data.repository.MonthlyBudgetRepository
import com.budgetmanager.app.data.repository.SettingsRepository
import com.budgetmanager.app.data.repository.TransactionRepository
import com.budgetmanager.app.domain.BuildMonthlyTrends
import com.budgetmanager.app.domain.BuildThisMonthTrends
import com.budgetmanager.app.domain.CategoryComparisonRow
import com.budgetmanager.app.domain.MonthlyBar
import com.budgetmanager.app.domain.MonthlyTrends
import com.budgetmanager.app.domain.TrendsCategoryRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlin.math.floor
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TrendsViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val monthlyBudgetRepository: MonthlyBudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val currentMonth = MonthKey.current()

    val uiState: StateFlow<TrendsUiState> = settingsRepository.observeTrendsMonthsShown()
        .distinctUntilChanged()
        .flatMapLatest { monthsShown ->
            val months = monthsBackFrom(currentMonth, monthsShown)
            val spentMonths = (months + currentMonth.previous()).distinct()
            val budgetFlows = months.map { monthlyBudgetRepository.observeForMonth(it) }
            val spentFlows = spentMonths.map { transactionRepository.observeSpentByCategoryForMonth(it) }

            combine(
                categoryRepository.observeActive(),
                combine(budgetFlows) { it.toList() },
                combine(spentFlows) { it.toList() }
            ) { categories, budgetsList, spentList ->
                val budgetsByMonth = months.zip(budgetsList).toMap()
                val spentByMonth = spentMonths.zip(spentList).toMap()
                buildUiState(monthsShown, months, categories, budgetsByMonth, spentByMonth)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrendsUiState())

    private fun buildUiState(
        monthsShown: Int,
        months: List<MonthKey>,
        categories: List<Category>,
        budgetsByMonth: Map<MonthKey, Map<Long, Money>>,
        spentByMonth: Map<MonthKey, Map<Long, Money>>
    ): TrendsUiState {
        val thisMonth = BuildThisMonthTrends(
            categories = categories,
            budgets = budgetsByMonth[currentMonth] ?: emptyMap(),
            spent = spentByMonth[currentMonth] ?: emptyMap()
        )
        val monthly = BuildMonthlyTrends(
            categories = categories,
            months = months,
            currentMonth = currentMonth,
            budgetsByMonth = budgetsByMonth,
            spentByMonth = spentByMonth
        )

        return TrendsUiState(
            isLoading = false,
            monthLabel = currentMonth.monthName(),
            totalBudgetText = thisMonth.totalBudget.formatted(),
            overallPercentText = thisMonth.overallPercentUsed.asPercentText(),
            hasBudget = thisMonth.totalBudget.paise > 0,
            slices = thisMonth.slices,
            rows = thisMonth.rows.map { it.toRowUi() },
            percentBars = monthly.bars.map { it.toPercentBar() },
            comparisonTitle = "${currentMonth.monthName()} so far vs ${currentMonth.previous().monthName()}, by category",
            comparisonRows = monthly.comparisonRows.map { it.toRowUi(currentMonth.previous()) },
            groupedBars = monthly.bars.map { it.toGroupedBar() },
            averageSpentText = monthly.historicSummary.averageSpentPerMonth.formatted(),
            monthsOverBudgetText = "${monthly.historicSummary.monthsOverBudget} of ${monthly.historicSummary.monthsConsidered}",
            historicRangeLabel = monthly.historicRangeLabel(),
            rangeFooterText = "Showing the last $monthsShown months. Change this in Settings."
        )
    }

    private fun MonthlyTrends.historicRangeLabel(): String {
        val completed = bars.filterNot { it.isInProgress }
        if (completed.isEmpty()) return "No completed months yet"
        return "${completed.first().monthKey.monthAbbrev()} to ${completed.last().monthKey.monthAbbrev()}"
    }
}

private fun monthsBackFrom(current: MonthKey, count: Int): List<MonthKey> {
    val months = ArrayDeque<MonthKey>()
    var cursor = current
    repeat(count) {
        months.addFirst(cursor)
        cursor = cursor.previous()
    }
    return months.toList()
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

private fun MonthlyBar.toPercentBar() = PercentBar(
    label = monthKey.monthAbbrev(),
    percent = (percentUsed ?: 0.0).toFloat(),
    status = status,
    isInProgress = isInProgress,
    valueLabel = if (isInProgress || status == BudgetStatus.OVER_BUDGET) percentUsed.asPercentText() else null
)

private fun MonthlyBar.toGroupedBar() = GroupedBar(
    label = monthKey.monthAbbrev(),
    budget = budget.paise / 100,
    spent = spent.paise / 100,
    status = status,
    isInProgress = isInProgress,
    valueLabel = if (status == BudgetStatus.OVER_BUDGET) (spent.paise / 100).asThousandsLabel() else null
)

private fun CategoryComparisonRow.toRowUi(previousMonth: MonthKey) = CategoryComparisonRowUi(
    categoryId = categoryId,
    emoji = emoji,
    name = name,
    currentText = currentSpent.formatted(),
    previousText = "was ${previousSpent.formatted()} in ${previousMonth.monthAbbrev()}",
    differenceText = Money(kotlin.math.abs(difference.paise)).formatted(),
    isIncrease = difference.paise > 0
)

private fun Double?.asPercentText(): String = "${((this ?: 0.0) * 100).roundToInt()}%"

private fun Long.asThousandsLabel(): String {
    val thousands = this / 1000.0
    return if (thousands == floor(thousands)) "${thousands.toLong()}k" else "%.1fk".format(thousands)
}

private fun MonthKey.monthName(): String =
    YearMonth.of(year, month).month.getDisplayName(TextStyle.FULL, Locale.getDefault())

private fun MonthKey.monthAbbrev(): String =
    YearMonth.of(year, month).month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
