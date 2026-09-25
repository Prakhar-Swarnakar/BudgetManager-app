package com.budgetmanager.app.feature.budget

import com.budgetmanager.app.core.model.Category
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.data.repository.FakeCategoryRepository
import com.budgetmanager.app.data.repository.FakeMonthlyBudgetRepository
import com.budgetmanager.app.domain.CopyBudgetFromPreviousMonth
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
class MonthlyBudgetViewModelTest {

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
        budgets: FakeMonthlyBudgetRepository = FakeMonthlyBudgetRepository()
    ) = MonthlyBudgetViewModel(categories, budgets, CopyBudgetFromPreviousMonth(budgets))

    @Test
    fun `a category with no budget shows a muted zero row`() = runTest {
        val categories = FakeCategoryRepository().apply {
            seed(listOf(Category(1, "Food", "🍔", 0, archived = false)))
        }
        val viewModel = viewModel(categories)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        val row = viewModel.uiState.value.rows.first()
        assertEquals("₹0", row.amountText)
        assertFalse(row.hasAmount)
        collector.cancel()
    }

    @Test
    fun `editing an amount from the row sheet persists it`() = runTest {
        val categories = FakeCategoryRepository().apply {
            seed(listOf(Category(1, "Food", "🍔", 0, archived = false)))
        }
        val viewModel = viewModel(categories)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onRowClicked(1)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onSheetAmountChanged("5000")
        viewModel.onSheetSaved()
        dispatcher.scheduler.advanceUntilIdle()

        val row = viewModel.uiState.value.rows.first()
        assertEquals("₹5,000", row.amountText)
        assertTrue(row.hasAmount)
        assertEquals(null, viewModel.uiState.value.sheet)
        collector.cancel()
    }

    @Test
    fun `an invalid amount keeps the sheet open with an error`() = runTest {
        val categories = FakeCategoryRepository().apply {
            seed(listOf(Category(1, "Food", "🍔", 0, archived = false)))
        }
        val viewModel = viewModel(categories)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onRowClicked(1)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onSheetAmountChanged("0")
        viewModel.onSheetSaved()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.sheet != null)
        assertTrue(viewModel.uiState.value.sheet!!.amountError != null)
        collector.cancel()
    }

    @Test
    fun `the plus button creates a new category with its own budget, leaving others untouched`() = runTest {
        val categories = FakeCategoryRepository().apply {
            seed(listOf(Category(1, "Food", "🍔", 0, archived = false)))
        }
        val budgets = FakeMonthlyBudgetRepository()
        val viewModel = viewModel(categories, budgets)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onAddClicked()
        viewModel.onSheetNameChanged("Pets")
        viewModel.onSheetEmojiChanged("🐶")
        viewModel.onSheetAmountChanged("1500")
        viewModel.onSheetSaved()
        dispatcher.scheduler.advanceUntilIdle()

        val rows = viewModel.uiState.value.rows
        assertEquals(2, rows.size)
        val newRow = rows.first { it.name == "Pets" }
        assertEquals("₹1,500", newRow.amountText)
        val foodRow = rows.first { it.name == "Food" }
        assertFalse(foodRow.hasAmount) // untouched, still ₹0
        collector.cancel()
    }

    @Test
    fun `the total sums every category's amount for the month`() = runTest {
        val categories = FakeCategoryRepository().apply {
            seed(
                listOf(
                    Category(1, "Food", "🍔", 0, archived = false),
                    Category(2, "Rent", "🏠", 1, archived = false)
                )
            )
        }
        val budgets = FakeMonthlyBudgetRepository()
        val viewModel = viewModel(categories, budgets)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        budgets.setAmount(viewModel.uiState.value.monthKey, categoryId = 1, amount = Money.ofRupees(500))
        budgets.setAmount(viewModel.uiState.value.monthKey, categoryId = 2, amount = Money.ofRupees(10000))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("₹10,500", viewModel.uiState.value.totalText)
        collector.cancel()
    }

    @Test
    fun `opening a month with nothing set copies the previous month and shows the note`() = runTest {
        val categories = FakeCategoryRepository().apply {
            seed(listOf(Category(1, "Food", "🍔", 0, archived = false)))
        }
        val budgets = FakeMonthlyBudgetRepository()
        val viewModel = viewModel(categories, budgets)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        val currentMonth = viewModel.uiState.value.monthKey
        budgets.setAmount(currentMonth, categoryId = 1, amount = Money.ofRupees(3000))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onNextMonth()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(currentMonth.next(), viewModel.uiState.value.monthKey)
        assertTrue(viewModel.uiState.value.copiedFromPreviousMonth)
        assertEquals("₹3,000", viewModel.uiState.value.rows.first().amountText)
        collector.cancel()
    }

    @Test
    fun `a brand-new month with nothing to copy shows no note`() = runTest {
        val categories = FakeCategoryRepository().apply {
            seed(listOf(Category(1, "Food", "🍔", 0, archived = false)))
        }
        val viewModel = viewModel(categories)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.copiedFromPreviousMonth)
        collector.cancel()
    }
}
