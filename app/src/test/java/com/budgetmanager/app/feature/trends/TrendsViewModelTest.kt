package com.budgetmanager.app.feature.trends

import com.budgetmanager.app.core.model.BudgetStatus
import com.budgetmanager.app.core.model.Category
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.data.repository.FakeCategoryRepository
import com.budgetmanager.app.data.repository.FakeMonthlyBudgetRepository
import com.budgetmanager.app.data.repository.FakeSettingsRepository
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
import java.time.Instant

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

    private fun viewModel(
        categories: FakeCategoryRepository = FakeCategoryRepository(),
        budgets: FakeMonthlyBudgetRepository = FakeMonthlyBudgetRepository(),
        transactions: FakeTransactionRepository = FakeTransactionRepository(),
        settings: FakeSettingsRepository = FakeSettingsRepository()
    ) = TrendsViewModel(categories, budgets, transactions, settings)

    @Test
    fun `no budgets yet means hasBudget is false`() = runTest {
        val categories = FakeCategoryRepository().apply {
            seed(listOf(Category(1, "Food", "🍔", 0, archived = false)))
        }
        val vm = viewModel(categories)
        val collector = vm.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.hasBudget)
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
        transactions.insert(Money.ofRupees(81), Instant.now(), MonthKey.current(), 1, null)

        val vm = viewModel(categories, budgets, transactions)
        val collector = vm.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
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
        transactions.insert(Money.ofRupees(150), Instant.now(), MonthKey.current(), 1, null)

        val vm = viewModel(categories, budgets, transactions)
        val collector = vm.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        val row = vm.uiState.value.rows.single()
        assertEquals(BudgetStatus.OVER_BUDGET, row.status)
        assertEquals("Over by ₹50", row.statusText)
        collector.cancel()
    }

    @Test
    fun `defaults to 6 months and changing the setting changes the bar counts`() = runTest {
        val settings = FakeSettingsRepository()
        val vm = viewModel(settings = settings)
        val collector = vm.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(6, vm.uiState.value.percentBars.size)
        assertEquals(6, vm.uiState.value.groupedBars.size)
        assertEquals("Showing the last 6 months. Change this in Settings.", vm.uiState.value.rangeFooterText)

        settings.setTrendsMonthsShown(3)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, vm.uiState.value.percentBars.size)
        assertEquals(3, vm.uiState.value.groupedBars.size)
        collector.cancel()
    }

    @Test
    fun `the current month is in-progress and excluded from the historic summary`() = runTest {
        val budgets = FakeMonthlyBudgetRepository()
        val transactions = FakeTransactionRepository()
        val currentMonth = MonthKey.current()
        val previousMonth = currentMonth.previous()
        budgets.setAmount(previousMonth, 1, Money.ofRupees(100))
        transactions.insert(Money.ofRupees(150), Instant.now(), previousMonth, 1, null) // over budget, completed
        transactions.insert(Money.ofRupees(9999), Instant.now(), currentMonth, 1, null) // huge, but in progress

        val vm = viewModel(budgets = budgets, transactions = transactions)
        val collector = vm.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        val bars = vm.uiState.value.percentBars
        assertTrue(bars.last().isInProgress) // current month sorts last, ascending
        assertEquals("1 of 5", vm.uiState.value.monthsOverBudgetText) // 6-month range minus the in-progress month
        collector.cancel()
    }

    @Test
    fun `comparison rows compare current month so far with the previous month`() = runTest {
        val categories = FakeCategoryRepository().apply {
            seed(listOf(Category(1, "Food", "🍔", 0, archived = false)))
        }
        val transactions = FakeTransactionRepository()
        val currentMonth = MonthKey.current()
        val previousMonth = currentMonth.previous()
        transactions.insert(Money.ofRupees(100), Instant.now(), previousMonth, 1, null)
        transactions.insert(Money.ofRupees(120), Instant.now(), currentMonth, 1, null)

        val vm = viewModel(categories = categories, transactions = transactions)
        val collector = vm.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        val row = vm.uiState.value.comparisonRows.single()
        assertEquals("₹120", row.currentText)
        assertTrue(row.isIncrease)
        assertEquals("₹20", row.differenceText)
        collector.cancel()
    }
}
