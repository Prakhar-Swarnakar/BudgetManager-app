package com.budgetmanager.app.feature.categorydetail

import com.budgetmanager.app.core.model.Category
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun categories() = FakeCategoryRepository().apply {
        seed(listOf(Category(1, "Food", "🍔", 0, archived = false)))
    }

    private fun viewModel(
        categories: FakeCategoryRepository = categories(),
        budgets: FakeMonthlyBudgetRepository = FakeMonthlyBudgetRepository(),
        messages: FakeMessageRepository = FakeMessageRepository(),
        transactions: FakeTransactionRepository = FakeTransactionRepository(messages)
    ) = CategoryDetailViewModel(categories, budgets, transactions, messages)

    @Test
    fun `loading shows the category and its transactions for the month`() = runTest {
        val budgets = FakeMonthlyBudgetRepository()
        val transactions = FakeTransactionRepository()
        val viewModel = viewModel(budgets = budgets, transactions = transactions)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val month = MonthKey.current()
        budgets.setAmount(month, categoryId = 1, amount = Money.ofRupees(1000))
        transactions.insert(Money.ofRupees(300), Instant.now(), month, categoryId = 1, note = "Lunch")
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.load(1, month)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Food", state.categoryName)
        assertEquals("🍔", state.categoryEmoji)
        assertEquals(1, state.transactions.size)
        assertEquals("Lunch", state.transactions.first().noteOrPlaceholder)
        assertEquals("₹700", state.remainingText)
        collector.cancel()
    }

    @Test
    fun `clicking a transaction requests navigation, which then clears`() = runTest {
        val transactions = FakeTransactionRepository()
        val viewModel = viewModel(transactions = transactions)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val month = MonthKey.current()
        val id = transactions.insert(Money.ofRupees(100), Instant.now(), month, categoryId = 1, note = null)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.load(1, month)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onTransactionClicked(id)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(id, viewModel.uiState.value.editTransactionId)

        viewModel.onEditNavigationHandled()
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(viewModel.uiState.value.editTransactionId)
        collector.cancel()
    }

    @Test
    fun `deleting a manual transaction removes it, and undo restores it`() = runTest {
        val transactions = FakeTransactionRepository()
        val viewModel = viewModel(transactions = transactions)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val month = MonthKey.current()
        val id = transactions.insert(Money.ofRupees(250), Instant.now(), month, categoryId = 1, note = "Snacks")
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.load(1, month)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onDeleteSwiped(id)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.transactions.isEmpty())
        assertEquals(id, viewModel.uiState.value.undoDeletedTransactionId)

        viewModel.onUndoDelete()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.transactions.size)
        assertEquals("Snacks", viewModel.uiState.value.transactions.first().noteOrPlaceholder)
        collector.cancel()
    }

    @Test
    fun `deleting an SMS-linked transaction reverts the message, and undo re-accepts it`() = runTest {
        val messages = FakeMessageRepository()
        val transactions = FakeTransactionRepository(messages)
        val viewModel = viewModel(messages = messages, transactions = transactions)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val messageId = messages.ingest(
            SmsMessage(
                id = 0, sender = "BANK", body = "Rs 300 debited", receivedAt = Instant.now(),
                smsProviderId = null, dedupeKey = "k1", parsedAmount = Money.ofRupees(300),
                merchant = "Cafe", suggestedCategoryId = null, status = MessageStatus.NOT_ASSIGNED,
                isNew = true
            )
        )!!
        val month = MonthKey.current()
        val message = messages.getById(messageId)!!
        transactions.saveFromMessage(message, Money.ofRupees(300), Instant.now(), month, categoryId = 1, note = "Cafe")
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.load(1, month)
        dispatcher.scheduler.advanceUntilIdle()

        val transactionId = viewModel.uiState.value.transactions.first().id
        viewModel.onDeleteSwiped(transactionId)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(MessageStatus.NOT_ASSIGNED, messages.getById(messageId)!!.status)

        viewModel.onUndoDelete()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.transactions.size)
        assertEquals(MessageStatus.ACCEPTED, messages.getById(messageId)!!.status)
        collector.cancel()
    }

    @Test
    fun `month navigation moves forward and back`() = runTest {
        val viewModel = viewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val month = MonthKey.current()
        viewModel.load(1, month)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onNextMonth()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(month.next(), viewModel.uiState.value.monthKey)

        viewModel.onPreviousMonth()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(month, viewModel.uiState.value.monthKey)
        collector.cancel()
    }
}
