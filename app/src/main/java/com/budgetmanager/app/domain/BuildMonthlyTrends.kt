package com.budgetmanager.app.domain

import com.budgetmanager.app.core.model.BudgetMaths
import com.budgetmanager.app.core.model.BudgetStatus
import com.budgetmanager.app.core.model.Category
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey

/** One month's bar: totals across every category, whether it finished over/within budget, and
 *  whether it's the current, still-accumulating month - Historic's summary tiles and Previous
 *  month's "months over budget" both exclude an in-progress month, since it hasn't finished. */
data class MonthlyBar(
    val monthKey: MonthKey,
    val budget: Money,
    val spent: Money,
    val percentUsed: Double?,
    val status: BudgetStatus,
    val isInProgress: Boolean
)

data class CategoryComparisonRow(
    val categoryId: Long,
    val emoji: String,
    val name: String,
    val currentSpent: Money,
    val previousSpent: Money
) {
    /** Positive means spending went up (shown as a red "increase" arrow); zero or negative means
     *  it held steady or went down (a green "decrease" arrow). */
    val difference: Money get() = currentSpent - previousSpent
}

data class HistoricSummary(
    val averageSpentPerMonth: Money,
    val monthsOverBudget: Int,
    val monthsConsidered: Int
)

data class MonthlyTrends(
    /** Ascending, oldest first, the current (in-progress) month last. */
    val bars: List<MonthlyBar>,
    /** The current month so far vs the previous month, by category - only categories with
     *  spending in either month, in category sort order. */
    val comparisonRows: List<CategoryComparisonRow>,
    val historicSummary: HistoricSummary
)

/**
 * Pure calculation behind Trends' "Previous month" and "Historic" tabs (02-features.md F10,
 * 14-implementation-plan.md M11). [months] and the two by-month maps come from the caller already
 * scoped to the range setting (3/6/12 months) - this function only does the maths, so it's
 * testable without Compose or a real clock.
 */
object BuildMonthlyTrends {
    operator fun invoke(
        categories: List<Category>,
        months: List<MonthKey>,
        currentMonth: MonthKey,
        budgetsByMonth: Map<MonthKey, Map<Long, Money>>,
        spentByMonth: Map<MonthKey, Map<Long, Money>>
    ): MonthlyTrends {
        val bars = months.map { month ->
            val budgets = budgetsByMonth[month] ?: emptyMap()
            val spent = spentByMonth[month] ?: emptyMap()
            val totalBudget = budgets.values.fold(Money.Zero) { acc, m -> acc + m }
            val totalSpent = spent.values.fold(Money.Zero) { acc, m -> acc + m }
            val progress = BudgetMaths.evaluate(totalBudget, totalSpent)
            MonthlyBar(
                monthKey = month,
                budget = totalBudget,
                spent = totalSpent,
                percentUsed = progress.percentUsed,
                status = progress.status,
                isInProgress = month == currentMonth
            )
        }

        val previousMonth = currentMonth.previous()
        val currentSpentByCategory = spentByMonth[currentMonth] ?: emptyMap()
        val previousSpentByCategory = spentByMonth[previousMonth] ?: emptyMap()
        val comparisonRows = categories
            .filter { category ->
                (currentSpentByCategory[category.id] ?: Money.Zero).paise > 0 ||
                    (previousSpentByCategory[category.id] ?: Money.Zero).paise > 0
            }
            .map { category ->
                CategoryComparisonRow(
                    categoryId = category.id,
                    emoji = category.emoji,
                    name = category.name,
                    currentSpent = currentSpentByCategory[category.id] ?: Money.Zero,
                    previousSpent = previousSpentByCategory[category.id] ?: Money.Zero
                )
            }

        val completedBars = bars.filterNot { it.isInProgress }
        val averageSpent = if (completedBars.isEmpty()) {
            Money.Zero
        } else {
            Money(completedBars.sumOf { it.spent.paise } / completedBars.size)
        }

        return MonthlyTrends(
            bars = bars,
            comparisonRows = comparisonRows,
            historicSummary = HistoricSummary(
                averageSpentPerMonth = averageSpent,
                monthsOverBudget = completedBars.count { it.status == BudgetStatus.OVER_BUDGET },
                monthsConsidered = completedBars.size
            )
        )
    }
}
