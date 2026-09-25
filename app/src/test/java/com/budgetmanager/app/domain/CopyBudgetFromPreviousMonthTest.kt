package com.budgetmanager.app.domain

import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.data.repository.FakeMonthlyBudgetRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CopyBudgetFromPreviousMonthTest {

    private val september = MonthKey.of(2026, 9)
    private val october = MonthKey.of(2026, 10)

    @Test
    fun `copies the previous month's amounts when the target month is empty`() = runTest {
        val repo = FakeMonthlyBudgetRepository()
        repo.setAmount(september, categoryId = 1, amount = Money.ofRupees(5000))
        repo.setAmount(september, categoryId = 2, amount = Money.ofRupees(2000))
        val copyBudget = CopyBudgetFromPreviousMonth(repo)

        val copied = copyBudget(october)

        assertTrue(copied)
        val octoberAmounts = repo.observeForMonth(october).first()
        assertEquals(Money.ofRupees(5000), octoberAmounts[1])
        assertEquals(Money.ofRupees(2000), octoberAmounts[2])
    }

    @Test
    fun `does nothing when the target month already has amounts`() = runTest {
        val repo = FakeMonthlyBudgetRepository()
        repo.setAmount(september, categoryId = 1, amount = Money.ofRupees(5000))
        repo.setAmount(october, categoryId = 1, amount = Money.ofRupees(9999))
        val copyBudget = CopyBudgetFromPreviousMonth(repo)

        val copied = copyBudget(october)

        assertFalse(copied)
        assertEquals(Money.ofRupees(9999), repo.observeForMonth(october).first()[1])
    }

    @Test
    fun `does nothing when the previous month also has no budget`() = runTest {
        val repo = FakeMonthlyBudgetRepository()
        val copyBudget = CopyBudgetFromPreviousMonth(repo)

        val copied = copyBudget(october)

        assertFalse(copied)
        assertTrue(repo.observeForMonth(october).first().isEmpty())
    }
}
