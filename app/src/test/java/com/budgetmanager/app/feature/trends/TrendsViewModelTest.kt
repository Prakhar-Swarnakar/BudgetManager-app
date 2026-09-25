package com.budgetmanager.app.feature.trends

import com.budgetmanager.app.core.model.BudgetStatus
import com.budgetmanager.app.core.model.Category
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.data.repository.FakeCategoryRepository
import com.budgetmanager.app.data.repository.FakeMonthlyBudgetRepository
import com.budgetmanager.app.data.repository.FakeTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrendsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `no budgets yet means hasBudget is false`() = runTest {
        val categories = FakeCategoryRepository().apply {
            seed(listOf(Category(1, "Food", "🍔", 0, archived = false)))
        }
        val viewModel = TrendsViewModel(categories, FakeMonthlyBudgetRepository(), FakeTransactionRepository())
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasBudget)
        collector.cancel()
    }

    @Test
    fun `figures agree with the same maths Home uses`() = runTest {
        val categories = FakeCategoryRepository().apply {
            seed(
                listOf(
                    Category(1, "Food", "🍔", 0, archived = false),
                    Category(2, "Transport", "🚗", 1, archived = false)
                )
            )
        }
        val budgets = FakeMonthlyBudgetRepository()
        budgets.setAmount(MonthKey.current(), 1, Money.ofRupees(100))
        budgets.setAmount(MonthKey.current(), 2, Money.ofRupees(100))
        val transactions = FakeTransactionRepository()
        transactions.insert(Money.ofRupees(81), java.time.Instant.now(), MonthKey.current(), 1, null)

        val viewModel = TrendsViewModel(categories, budgets, transactions)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasBudget)
        assertEquals("₹200", state.totalBudgetText)
        assertEquals("41%", state.overallPercentText) // 81 of 200

        val foodRow = state.rows.single { it.categoryId == 1L }
        assertEquals(BudgetStatus.WARNING, foodRow.status) // 81% is >= 80%
        assertEquals("81% used", foodRow.statusText)

        val transportRow = state.rows.single { it.categoryId == 2L }
        assertEquals("0% used", transportRow.statusText)
        collector.cancel()
    }

    @Test
    fun `an over-budget category shows how much over`() = runTest {
        val categories = FakeCategoryRepository().apply {
            seed(listOf(Category(1, "Food", "🍔", 0, archived = false)))
        }
        val budgets = FakeMonthlyBudgetRepository()
        budgets.setAmount(MonthKey.current(), 1, Money.ofRupees(100))
        val transactions = FakeTransactionRepository()
        transactions.insert(Money.ofRupees(150), java.time.Instant.now(), MonthKey.current(), 1, null)

        val viewModel = TrendsViewModel(categories, budgets, transactions)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        val row = viewModel.uiState.value.rows.single()
        assertEquals(BudgetStatus.OVER_BUDGET, row.status)
        assertEquals("Over by ₹50", row.statusText)
        collector.cancel()
    }
}
