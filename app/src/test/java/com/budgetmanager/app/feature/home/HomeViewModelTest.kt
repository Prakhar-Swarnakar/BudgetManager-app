package com.budgetmanager.app.feature.home

import com.budgetmanager.app.core.model.Category
import com.budgetmanager.app.core.model.BudgetStatus
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.core.model.SmsMessage
import com.budgetmanager.app.data.repository.FakeCategoryRepository
import com.budgetmanager.app.data.repository.FakeMessageRepository
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
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

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
        messages: FakeMessageRepository = FakeMessageRepository()
    ) = HomeViewModel(categories, budgets, transactions, messages)

    @Test
    fun `a category under budget shows its percent used and remaining`() = runTest {
        val categories = FakeCategoryRepository().apply {
            seed(listOf(Category(1, "Food", "🍔", 0, archived = false)))
        }
        val budgets = FakeMonthlyBudgetRepository()
        val transactions = FakeTransactionRepository()
        val viewModel = viewModel(categories, budgets, transactions)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        val month = viewModel.uiState.value.monthKey
        budgets.setAmount(month, categoryId = 1, amount = Money.ofRupees(1000))
        transactions.insert(Money.ofRupees(400), Instant.now(), month, categoryId = 1, note = null)
        dispatcher.scheduler.advanceUntilIdle()

        val card = viewModel.uiState.value.cards.first()
        assertEquals(BudgetStatus.UNDER_BUDGET, card.status)
        assertFalse(card.isNotBudgeted)
        assertEquals("₹600", card.remainingText)
        collector.cancel()
    }

    @Test
    fun `a zero-budget category with spending is Not budgeted and shows a negative remaining`() = runTest {
        val categories = FakeCategoryRepository().apply {
            seed(listOf(Category(1, "Food", "🍔", 0, archived = false)))
        }
        val transactions = FakeTransactionRepository()
        val viewModel = viewModel(categories = categories, transactions = transactions)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        val month = viewModel.uiState.value.monthKey
        transactions.insert(Money.ofRupees(500), Instant.now(), month, categoryId = 1, note = null)
        dispatcher.scheduler.advanceUntilIdle()

        val card = viewModel.uiState.value.cards.first()
        assertEquals(BudgetStatus.OVER_BUDGET, card.status)
        assertTrue(card.isNotBudgeted)
        assertEquals("-₹500", card.remainingText)
        collector.cancel()
    }

    @Test
    fun `totals sum every category's budget and spend`() = runTest {
        val categories = FakeCategoryRepository().apply {
            seed(
                listOf(
                    Category(1, "Food", "🍔", 0, archived = false),
                    Category(2, "Rent", "🏠", 1, archived = false)
                )
            )
        }
        val budgets = FakeMonthlyBudgetRepository()
        val transactions = FakeTransactionRepository()
        val viewModel = viewModel(categories, budgets, transactions)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        val month = viewModel.uiState.value.monthKey
        budgets.setAmount(month, categoryId = 1, amount = Money.ofRupees(1000))
        budgets.setAmount(month, categoryId = 2, amount = Money.ofRupees(20000))
        transactions.insert(Money.ofRupees(400), Instant.now(), month, categoryId = 1, note = null)
        transactions.insert(Money.ofRupees(15000), Instant.now(), month, categoryId = 2, note = null)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("₹21,000", viewModel.uiState.value.totalBudgetText)
        assertEquals("₹15,400", viewModel.uiState.value.totalSpentText)
        assertEquals("₹5,600", viewModel.uiState.value.totalRemainingText)
        collector.cancel()
    }

    @Test
    fun `the review count is the Not assigned message count`() = runTest {
        val messages = FakeMessageRepository()
        val viewModel = viewModel(messages = messages)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        messages.ingest(testMessage("k1"))
        messages.ingest(testMessage("k2"))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.reviewCount)
        collector.cancel()
    }

    @Test
    fun `next month is disabled at the current month and does nothing`() = runTest {
        val viewModel = viewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        val currentMonth = viewModel.uiState.value.monthKey
        assertFalse(viewModel.uiState.value.canGoNext)

        viewModel.onNextMonth()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(currentMonth, viewModel.uiState.value.monthKey)
        collector.cancel()
    }

    @Test
    fun `previous month is always allowed and re-enables next`() = runTest {
        val viewModel = viewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        val currentMonth = viewModel.uiState.value.monthKey
        viewModel.onPreviousMonth()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(currentMonth.previous(), viewModel.uiState.value.monthKey)
        assertTrue(viewModel.uiState.value.canGoNext)
        collector.cancel()
    }

    private fun testMessage(dedupeKey: String) = SmsMessage(
        id = 0, sender = "TESTBANK", body = "Rs 100 debited", receivedAt = Instant.now(),
        smsProviderId = null, dedupeKey = dedupeKey, parsedAmount = Money.ofRupees(100),
        merchant = "Test Store", suggestedCategoryId = null, status = MessageStatus.NOT_ASSIGNED,
        isNew = true
    )
}
