package com.budgetmanager.app.domain

import com.budgetmanager.app.core.model.AlertType
import com.budgetmanager.app.core.model.Category
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.data.repository.FakeAlertLogRepository
import com.budgetmanager.app.data.repository.FakeCategoryRepository
import com.budgetmanager.app.data.repository.FakeMonthlyBudgetRepository
import com.budgetmanager.app.data.repository.FakeSettingsRepository
import com.budgetmanager.app.data.repository.FakeTransactionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/** The table of alert cases from 14-implementation-plan.md M8: 79% to 81%, 79% straight to
 *  101%, repeated crossing, the ₹0-budget rule, and deletion-then-re-adding. */
class EvaluateBudgetAlertsTest {

    private val month = MonthKey.of(2026, 9)
    private lateinit var budgets: FakeMonthlyBudgetRepository
    private lateinit var transactions: FakeTransactionRepository
    private lateinit var alertLog: FakeAlertLogRepository
    private lateinit var notifier: FakeBudgetAlertNotifier
    private lateinit var evaluate: EvaluateBudgetAlerts

    @Before
    fun setUp() {
        budgets = FakeMonthlyBudgetRepository()
        transactions = FakeTransactionRepository()
        alertLog = FakeAlertLogRepository()
        notifier = FakeBudgetAlertNotifier()
        val categories = FakeCategoryRepository().apply {
            seed(listOf(Category(1, "Food", "🍔", 0, archived = false)))
        }
        evaluate = EvaluateBudgetAlerts(
            budgets, transactions, alertLog, categories, FakeSettingsRepository(), notifier
        )
    }

    private suspend fun spend(rupees: Long) {
        transactions.insert(Money.ofRupees(rupees), Instant.now(), month, categoryId = 1, note = null)
    }

    @Test
    fun `below 80 percent fires nothing`() = runTest {
        budgets.setAmount(month, categoryId = 1, amount = Money.ofRupees(1000))
        spend(790) // 79%

        evaluate(month, categoryId = 1)

        assertTrue(notifier.calls.isEmpty())
    }

    @Test
    fun `crossing 80 percent fires the eighty-percent alert exactly once`() = runTest {
        budgets.setAmount(month, categoryId = 1, amount = Money.ofRupees(1000))
        spend(810) // 81%

        evaluate(month, categoryId = 1)

        assertEquals(1, notifier.calls.size)
        assertEquals(AlertType.EIGHTY_PERCENT, notifier.calls.single().type)
    }

    @Test
    fun `jumping straight from under 80 to over budget fires only the over-budget alert`() = runTest {
        budgets.setAmount(month, categoryId = 1, amount = Money.ofRupees(1000))
        spend(1010) // 79 -> 101% in one transaction

        evaluate(month, categoryId = 1)

        assertEquals(1, notifier.calls.size)
        assertEquals(AlertType.OVER_BUDGET, notifier.calls.single().type)
    }

    @Test
    fun `crossing 80 then later going over budget fires both alerts, once each`() = runTest {
        budgets.setAmount(month, categoryId = 1, amount = Money.ofRupees(1000))

        spend(850) // 85% - crosses 80
        evaluate(month, categoryId = 1)
        spend(200) // now 105% - crosses over budget
        evaluate(month, categoryId = 1)

        assertEquals(2, notifier.calls.size)
        assertEquals(AlertType.EIGHTY_PERCENT, notifier.calls[0].type)
        assertEquals(AlertType.OVER_BUDGET, notifier.calls[1].type)
    }

    @Test
    fun `repeated crossings of the same threshold only alert once`() = runTest {
        budgets.setAmount(month, categoryId = 1, amount = Money.ofRupees(1000))
        spend(900) // 90% - crosses 80, fires once
        evaluate(month, categoryId = 1)
        spend(50) // 95% - still just past 80%, must not re-fire
        evaluate(month, categoryId = 1)
        evaluate(month, categoryId = 1) // called again with no new spend at all

        assertEquals(1, notifier.calls.size)
    }

    @Test
    fun `a zero-budget category with spending fires the over-budget alert, not eighty-percent`() = runTest {
        // No budget set at all for this category this month.
        spend(500)

        evaluate(month, categoryId = 1)

        assertEquals(1, notifier.calls.size)
        assertEquals(AlertType.OVER_BUDGET, notifier.calls.single().type)
    }

    @Test
    fun `deleting the transaction that crossed a threshold does not un-fire it`() = runTest {
        budgets.setAmount(month, categoryId = 1, amount = Money.ofRupees(1000))
        val id = transactions.insert(Money.ofRupees(900), Instant.now(), month, categoryId = 1, note = null)
        evaluate(month, categoryId = 1)
        assertEquals(1, notifier.calls.size)

        // Deleting a transaction never calls evaluate (05-budget-rules.md: editing/deleting
        // never sends alerts) - the alert log is untouched, so even spending drops back down.
        transactions.delete(id)

        assertTrue(alertLog.hasFired(month, categoryId = 1, AlertType.EIGHTY_PERCENT))
    }

    @Test
    fun `re-adding spending after a delete does not re-fire an already-fired alert`() = runTest {
        budgets.setAmount(month, categoryId = 1, amount = Money.ofRupees(1000))
        val id = transactions.insert(Money.ofRupees(900), Instant.now(), month, categoryId = 1, note = null)
        evaluate(month, categoryId = 1)
        transactions.delete(id)

        spend(850) // crosses 80% again, in the same month
        evaluate(month, categoryId = 1)

        assertEquals(1, notifier.calls.size) // still just the first one
    }

    @Test
    fun `a disabled switch still records the alert but does not notify`() = runTest {
        val settings = FakeSettingsRepository().apply { setEightyPercentAlertsEnabled(false) }
        val evaluateWithSwitchOff = EvaluateBudgetAlerts(
            budgets, transactions, alertLog,
            FakeCategoryRepository().apply { seed(listOf(Category(1, "Food", "🍔", 0, archived = false))) },
            settings, notifier
        )
        budgets.setAmount(month, categoryId = 1, amount = Money.ofRupees(1000))
        spend(900)

        evaluateWithSwitchOff(month, categoryId = 1)

        assertTrue(notifier.calls.isEmpty())
        assertTrue(alertLog.hasFired(month, categoryId = 1, AlertType.EIGHTY_PERCENT))
    }
}
