package com.budgetmanager.app.domain

import com.budgetmanager.app.core.model.BudgetStatus
import com.budgetmanager.app.core.model.Category
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildMonthlyTrendsTest {

    private val food = Category(1, "Food", "🍔", 0, archived = false)
    private val transport = Category(2, "Transport", "🚗", 1, archived = false)

    private val apr = MonthKey.of(2026, 4)
    private val may = MonthKey.of(2026, 5)
    private val jun = MonthKey.of(2026, 6)

    @Test
    fun `each bar is compared with its own month's budget, not the current month's`() {
        val result = BuildMonthlyTrends(
            categories = listOf(food),
            months = listOf(apr, may, jun),
            currentMonth = jun,
            budgetsByMonth = mapOf(
                apr to mapOf(1L to Money.ofRupees(100)),
                may to mapOf(1L to Money.ofRupees(200)),
                jun to mapOf(1L to Money.ofRupees(100))
            ),
            spentByMonth = mapOf(
                apr to mapOf(1L to Money.ofRupees(90)),
                may to mapOf(1L to Money.ofRupees(90)),
                jun to mapOf(1L to Money.ofRupees(50))
            )
        )

        assertEquals(0.9, result.bars[0].percentUsed!!, 0.001) // Apr: 90/100
        assertEquals(0.45, result.bars[1].percentUsed!!, 0.001) // May: 90/200 - different budget
    }

    @Test
    fun `the current month is marked in-progress and excluded from the historic summary`() {
        val result = BuildMonthlyTrends(
            categories = listOf(food),
            months = listOf(apr, may, jun),
            currentMonth = jun,
            budgetsByMonth = mapOf(
                apr to mapOf(1L to Money.ofRupees(100)),
                may to mapOf(1L to Money.ofRupees(100)),
                jun to mapOf(1L to Money.ofRupees(100))
            ),
            spentByMonth = mapOf(
                apr to mapOf(1L to Money.ofRupees(150)), // over budget, completed
                may to mapOf(1L to Money.ofRupees(50)),  // within budget, completed
                jun to mapOf(1L to Money.ofRupees(500))  // way over, but in progress - must not count
            )
        )

        assertFalse(result.bars[0].isInProgress)
        assertFalse(result.bars[1].isInProgress)
        assertTrue(result.bars[2].isInProgress)
        assertEquals(2, result.historicSummary.monthsConsidered)
        assertEquals(1, result.historicSummary.monthsOverBudget) // only Apr
        assertEquals(Money.ofRupees(100), result.historicSummary.averageSpentPerMonth) // (150+50)/2
    }

    @Test
    fun `comparison rows are current month so far vs the previous month`() {
        val result = BuildMonthlyTrends(
            categories = listOf(food, transport),
            months = listOf(may, jun),
            currentMonth = jun,
            budgetsByMonth = emptyMap(),
            spentByMonth = mapOf(
                may to mapOf(1L to Money.ofRupees(100), 2L to Money.ofRupees(50)),
                jun to mapOf(1L to Money.ofRupees(120))
            )
        )

        val foodRow = result.comparisonRows.single { it.categoryId == 1L }
        assertEquals(Money.ofRupees(120), foodRow.currentSpent)
        assertEquals(Money.ofRupees(100), foodRow.previousSpent)
        assertEquals(Money.ofRupees(20), foodRow.difference) // up

        val transportRow = result.comparisonRows.single { it.categoryId == 2L }
        assertEquals(Money.Zero, transportRow.currentSpent)
        assertEquals(Money.ofRupees(50), transportRow.previousSpent)
        assertEquals(Money.ofRupees(-50), transportRow.difference) // down
    }

    @Test
    fun `a category with no spending in either month is left out of the comparison`() {
        val result = BuildMonthlyTrends(
            categories = listOf(food, transport),
            months = listOf(may, jun),
            currentMonth = jun,
            budgetsByMonth = emptyMap(),
            spentByMonth = mapOf(may to mapOf(1L to Money.ofRupees(100)))
        )

        assertEquals(listOf(1L), result.comparisonRows.map { it.categoryId })
    }

    @Test
    fun `no completed months yet means a zero average and zero over-budget count, not a crash`() {
        val result = BuildMonthlyTrends(
            categories = listOf(food),
            months = listOf(jun),
            currentMonth = jun,
            budgetsByMonth = emptyMap(),
            spentByMonth = emptyMap()
        )

        assertEquals(0, result.historicSummary.monthsConsidered)
        assertEquals(0, result.historicSummary.monthsOverBudget)
        assertEquals(Money.Zero, result.historicSummary.averageSpentPerMonth)
    }
}
