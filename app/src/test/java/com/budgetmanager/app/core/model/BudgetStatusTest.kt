package com.budgetmanager.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetStatusTest {

    @Test
    fun `79 percent is UnderBudget`() {
        val result = BudgetMaths.evaluate(budget = Money.ofRupees(1000), spent = Money.ofRupees(790))
        assertEquals(BudgetStatus.UNDER_BUDGET, result.status)
        assertEquals(0.79, result.percentUsed!!, 0.0001)
    }

    @Test
    fun `exactly 80 percent is Warning`() {
        val result = BudgetMaths.evaluate(budget = Money.ofRupees(1000), spent = Money.ofRupees(800))
        assertEquals(BudgetStatus.WARNING, result.status)
    }

    @Test
    fun `exactly 100 percent is still Warning, not OverBudget`() {
        val result = BudgetMaths.evaluate(budget = Money.ofRupees(1000), spent = Money.ofRupees(1000))
        assertEquals(BudgetStatus.WARNING, result.status)
    }

    @Test
    fun `101 percent is OverBudget`() {
        val result = BudgetMaths.evaluate(budget = Money.ofRupees(1000), spent = Money.ofRupees(1010))
        assertEquals(BudgetStatus.OVER_BUDGET, result.status)
    }

    @Test
    fun `remaining can be negative when overspent, never clamped to zero`() {
        val result = BudgetMaths.evaluate(budget = Money.ofRupees(5000), spent = Money.ofRupees(7000))
        assertEquals(Money.ofRupees(-2000), result.remaining)
    }

    @Test
    fun `a zero budget with spending is Not budgeted - OverBudget with no percentage`() {
        val result = BudgetMaths.evaluate(budget = Money.Zero, spent = Money.ofRupees(1200))
        assertEquals(BudgetStatus.OVER_BUDGET, result.status)
        assertTrue(result.isNotBudgeted)
        assertNull(result.percentUsed)
        assertEquals(Money.ofRupees(-1200), result.remaining)
    }

    @Test
    fun `a zero budget with no spending is not flagged as Not budgeted`() {
        val result = BudgetMaths.evaluate(budget = Money.Zero, spent = Money.Zero)
        assertEquals(BudgetStatus.UNDER_BUDGET, result.status)
        assertFalse(result.isNotBudgeted)
        assertNull(result.percentUsed)
    }
}
