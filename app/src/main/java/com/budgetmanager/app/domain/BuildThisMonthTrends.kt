package com.budgetmanager.app.domain

import com.budgetmanager.app.core.model.BudgetMaths
import com.budgetmanager.app.core.model.BudgetStatus
import com.budgetmanager.app.core.model.Category
import com.budgetmanager.app.core.model.Money

/** One donut ring slice. [budgetShare] is this category's share of the total budget (0f for a
 *  category with no budget - it draws no arc). [usedFraction] is how much of the slice's own arc
 *  is the coloured "used" portion, clamped to [0,1] so overspending never draws past the slice. */
data class DonutSlice(
    val categoryId: Long,
    val emoji: String,
    val budgetShare: Float,
    val usedFraction: Float,
    val status: BudgetStatus
)

data class TrendsCategoryRow(
    val categoryId: Long,
    val emoji: String,
    val name: String,
    val budget: Money,
    val spent: Money,
    val percentUsed: Double?,
    val status: BudgetStatus,
    val isNotBudgeted: Boolean
)

data class ThisMonthTrends(
    val totalBudget: Money,
    val totalSpent: Money,
    val overallPercentUsed: Double?,
    val overallStatus: BudgetStatus,
    val slices: List<DonutSlice>,
    val rows: List<TrendsCategoryRow>
)

/**
 * Pure calculation behind Trends' "This month" tab: a donut slice and a row per active category,
 * sized by each category's share of the total budget (02-features.md F10, 05-budget-rules.md).
 * Every active category gets a row - including a ₹0-budget one with spending - matching what Home
 * shows, so the two pages' figures always agree. Only categories with a budget get a slice, since
 * a 0-share arc draws nothing.
 */
object BuildThisMonthTrends {
    operator fun invoke(
        categories: List<Category>,
        budgets: Map<Long, Money>,
        spent: Map<Long, Money>
    ): ThisMonthTrends {
        val totalBudget = budgets.values.fold(Money.Zero) { acc, m -> acc + m }
        val totalSpent = spent.values.fold(Money.Zero) { acc, m -> acc + m }
        val overall = BudgetMaths.evaluate(totalBudget, totalSpent)

        val rows = categories.map { category ->
            val budget = budgets[category.id] ?: Money.Zero
            val categorySpent = spent[category.id] ?: Money.Zero
            val progress = BudgetMaths.evaluate(budget, categorySpent)
            TrendsCategoryRow(
                categoryId = category.id,
                emoji = category.emoji,
                name = category.name,
                budget = budget,
                spent = categorySpent,
                percentUsed = progress.percentUsed,
                status = progress.status,
                isNotBudgeted = progress.isNotBudgeted
            )
        }

        val slices = rows
            .filter { it.budget.paise > 0 }
            .map { row ->
                DonutSlice(
                    categoryId = row.categoryId,
                    emoji = row.emoji,
                    budgetShare = if (totalBudget.paise > 0) {
                        row.budget.paise.toFloat() / totalBudget.paise.toFloat()
                    } else {
                        0f
                    },
                    usedFraction = (row.percentUsed ?: 0.0).toFloat().coerceIn(0f, 1f),
                    status = row.status
                )
            }

        return ThisMonthTrends(
            totalBudget = totalBudget,
            totalSpent = totalSpent,
            overallPercentUsed = overall.percentUsed,
            overallStatus = overall.status,
            slices = slices,
            rows = rows
        )
    }
}
