package com.budgetmanager.app.domain

import com.budgetmanager.app.core.model.BudgetStatus
import com.budgetmanager.app.core.model.Category
import com.budgetmanager.app.core.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildThisMonthTrendsTest {

    private val food = Category(1, "Food", "🍔", 0, archived = false)
    private val transport = Category(2, "Transport", "🚗", 1, archived = false)

    @Test
    fun `slice share is proportional to each category's budget`() {
        val result = BuildThisMonthTrends(
            categories = listOf(food, transport),
            budgets = mapOf(1L to Money.ofRupees(30), 2L to Money.ofRupees(70)),
            spent = emptyMap()
        )

        assertEquals(0.3f, result.slices.single { it.categoryId == 1L }.budgetShare, 0.001f)
        assertEquals(0.7f, result.slices.single { it.categoryId == 2L }.budgetShare, 0.001f)
    }

    @Test
    fun `used fraction is clamped to 1 even when overspent`() {
        val result = BuildThisMonthTrends(
            categories = listOf(food),
            budgets = mapOf(1L to Money.ofRupees(100)),
            spent = mapOf(1L to Money.ofRupees(150))
        )

        assertEquals(1f, result.slices.single().usedFraction, 0.001f)
        assertEquals(BudgetStatus.OVER_BUDGET, result.slices.single().status)
    }

    @Test
    fun `a zero-budget category with spending gets a row but no slice`() {
        val result = BuildThisMonthTrends(
            categories = listOf(food),
            budgets = emptyMap(),
            spent = mapOf(1L to Money.ofRupees(500))
        )

        assertTrue(result.slices.isEmpty())
        val row = result.rows.single()
        assertTrue(row.isNotBudgeted)
        assertEquals(Money.ofRupees(500), row.spent)
    }

    @Test
    fun `every active category gets a row, even with no budget or spending`() {
        val result = BuildThisMonthTrends(
            categories = listOf(food, transport),
            budgets = mapOf(1L to Money.ofRupees(100)),
            spent = mapOf(1L to Money.ofRupees(50))
        )

        assertEquals(2, result.rows.size)
        val transportRow = result.rows.single { it.categoryId == 2L }
        assertEquals(Money.Zero, transportRow.budget)
        assertEquals(Money.Zero, transportRow.spent)
        assertEquals(BudgetStatus.UNDER_BUDGET, transportRow.status)
        assertTrue(!transportRow.isNotBudgeted)
    }

    @Test
    fun `overall percent used is total spent over total budget`() {
        val result = BuildThisMonthTrends(
            categories = listOf(food, transport),
            budgets = mapOf(1L to Money.ofRupees(100), 2L to Money.ofRupees(100)),
            spent = mapOf(1L to Money.ofRupees(50), 2L to Money.ofRupees(98))
        )

        assertEquals(0.74, result.overallPercentUsed!!, 0.001)
    }

    @Test
    fun `no budget anywhere and no spending means no percentage and under-budget status`() {
        val result = BuildThisMonthTrends(
            categories = listOf(food),
            budgets = emptyMap(),
            spent = emptyMap()
        )

        assertNull(result.overallPercentUsed)
        assertEquals(BudgetStatus.UNDER_BUDGET, result.overallStatus)
        assertEquals(Money.Zero, result.totalBudget)
    }

    @Test
    fun `row order matches the category list passed in`() {
        val result = BuildThisMonthTrends(
            categories = listOf(transport, food),
            budgets = emptyMap(),
            spent = emptyMap()
        )

        assertEquals(listOf(2L, 1L), result.rows.map { it.categoryId })
    }
}
