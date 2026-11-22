package com.budgetmanager.app.feature.home

import com.budgetmanager.app.core.model.BudgetProgress
import com.budgetmanager.app.core.model.BudgetStatus
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import java.time.LocalDate
import kotlin.math.roundToLong

/**
 * How far through the viewed month [today] is, as 0..1 (day 20 of 30 -> 0.667), or null for a
 * past month - nothing is "ahead of pace" once the month is over, so Home shows no today mark.
 */
fun paceFraction(month: MonthKey, today: LocalDate): Double? {
    if (month != MonthKey.of(today.year, today.monthValue)) return null
    return today.dayOfMonth.toDouble() / today.lengthOfMonth()
}

/**
 * Home's bar colour, which differs from [BudgetProgress.status] (that one drives alerts and
 * Category detail): blue at or below the today mark, amber past the mark but within budget,
 * red over budget. For a past month the mark counts as the end of the month, so anything
 * within budget is blue.
 */
fun paceStatus(progress: BudgetProgress, pace: Double?): BudgetStatus {
    if (progress.status == BudgetStatus.OVER_BUDGET) return BudgetStatus.OVER_BUDGET
    val percent = progress.percentUsed ?: return progress.status
    return if (percent > (pace ?: 1.0)) BudgetStatus.WARNING else BudgetStatus.UNDER_BUDGET
}

/** Spent minus the budget share expected by now, to the nearest rupee; positive = ahead of pace. */
fun paceDifference(progress: BudgetProgress, pace: Double): Money {
    val expectedPaise = progress.budget.paise * pace
    val diffPaise = progress.spent.paise - expectedPaise
    return Money.ofRupees((diffPaise / 100.0).roundToLong())
}
