package com.budgetmanager.app.core.model

/**
 * The status band a category (or the whole month) is in. Thresholds: below 80% used is
 * UNDER_BUDGET, 80% up to and including 100% is WARNING, over 100% is OVER_BUDGET.
 * See 05-budget-rules.md.
 */
enum class BudgetStatus { UNDER_BUDGET, WARNING, OVER_BUDGET }

/**
 * The result of comparing spending against a budget. [percentUsed] is null when there is no
 * budget to measure against - the "Not budgeted" case - and callers must show no percentage
 * then, only the status and the raw amounts.
 */
data class BudgetProgress(
    val budget: Money,
    val spent: Money,
    val remaining: Money,
    val percentUsed: Double?,
    val status: BudgetStatus,
    val isNotBudgeted: Boolean
)

object BudgetMaths {

    /**
     * Computes remaining, percent used, and status for one category (or the month as a whole).
     * A budget of zero (or less) with any spending is always OVER_BUDGET with no percentage -
     * the "Not budgeted" rule in 05-budget-rules.md. Percent used is computed once, here, and
     * must be reused by the progress bar, the status colour, and the alert check rather than
     * recalculated in three places.
     */
    fun evaluate(budget: Money, spent: Money): BudgetProgress {
        val remaining = budget - spent
        if (budget.paise <= 0) {
            val hasSpending = spent.paise > 0
            return BudgetProgress(
                budget = budget,
                spent = spent,
                remaining = remaining,
                percentUsed = null,
                status = if (hasSpending) BudgetStatus.OVER_BUDGET else BudgetStatus.UNDER_BUDGET,
                isNotBudgeted = hasSpending
            )
        }
        val percent = spent.paise.toDouble() / budget.paise.toDouble()
        val status = when {
            percent > 1.0 -> BudgetStatus.OVER_BUDGET
            percent >= 0.8 -> BudgetStatus.WARNING
            else -> BudgetStatus.UNDER_BUDGET
        }
        return BudgetProgress(
            budget = budget,
            spent = spent,
            remaining = remaining,
            percentUsed = percent,
            status = status,
            isNotBudgeted = false
        )
    }
}
