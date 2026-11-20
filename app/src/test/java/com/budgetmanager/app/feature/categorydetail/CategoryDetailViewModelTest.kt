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
import org.junit.Assert.assertFalse
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
        transactions: FakeTransactionRepository = FakeTransactionRepository(),
        messages: FakeMessageRepository = FakeMessageRepository()
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
    fun `swiping asks for confirmation but does not delete yet`() = runTest {
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

        assertEquals(id, viewModel.uiState.value.pendingDeleteTransactionId)
        assertEquals(1, viewModel.uiState.value.transactions.size)
        collector.cancel()
    }

    @Test
    fun `cancelling the confirmation leaves the transaction in place`() = runTest {
        val transactions = FakeTransactionRepository()
        val viewModel = viewModel(transactions = transactions)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val month = MonthKey.current()
        val id = transactions.insert(Money.ofRupees(250), Instant.now(), month, categoryId = 1, note = "Snacks")
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.load(1, month)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onDeleteSwiped(id)
        viewModel.onCancelDelete()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingDeleteTransactionId)
        assertEquals(1, viewModel.uiState.value.transactions.size)
        collector.cancel()
    }

    @Test
    fun `confirming deletes a manual transaction`() = runTest {
        val transactions = FakeTransactionRepository()
        val viewModel = viewModel(transactions = transactions)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val month = MonthKey.current()
        val id = transactions.insert(Money.ofRupees(250), Instant.now(), month, categoryId = 1, note = "Snacks")
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.load(1, month)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onDeleteSwiped(id)
        viewModel.onConfirmDelete()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingDeleteTransactionId)
        assertTrue(viewModel.uiState.value.transactions.isEmpty())
        collector.cancel()
    }

    @Test
    fun `confirming deletes an SMS-linked transaction and reverts the message`() = runTest {
        val messages = FakeMessageRepository()
        val transactions = FakeTransactionRepository(messages)
        val viewModel = viewModel(transactions = transactions, messages = messages)
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
        viewModel.onConfirmDelete()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.transactions.isEmpty())
        assertEquals(MessageStatus.NOT_ASSIGNED, messages.getById(messageId)!!.status)
        collector.cancel()
    }

    @Test
    fun `month navigation moves back and forward again`() = runTest {
        val viewModel = viewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val month = MonthKey.current()
        viewModel.load(1, month)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onPreviousMonth()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(month.previous(), viewModel.uiState.value.monthKey)
        assertTrue(viewModel.uiState.value.canGoNext)

        viewModel.onNextMonth()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(month, viewModel.uiState.value.monthKey)
        collector.cancel()
    }

    @Test
    fun `next month is disabled at the current month and does nothing`() = runTest {
        val viewModel = viewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val month = MonthKey.current()
        viewModel.load(1, month)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canGoNext)

        viewModel.onNextMonth()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(month, viewModel.uiState.value.monthKey)
        collector.cancel()
    }

    @Test
    fun `money split with friends is left out of the spent total, matching Home's card`() = runTest {
        val transactions = FakeTransactionRepository()
        val viewModel = viewModel(transactions = transactions)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val month = MonthKey.current()
        transactions.insert(
            Money.ofRupees(900), Instant.now(), month, categoryId = 1, note = "Dinner",
            owedTotal = Money.ofRupees(550)
        )
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.load(1, month)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(Money.ofRupees(350).formatted(), viewModel.uiState.value.spentText)
        collector.cancel()
    }

    @Test
    fun `a saved title takes priority over the linked message's merchant and the note`() = runTest {
        val messages = FakeMessageRepository()
        val transactions = FakeTransactionRepository(messages)
        val viewModel = viewModel(transactions = transactions, messages = messages)
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
        transactions.saveFromMessage(
            message, Money.ofRupees(300), Instant.now(), month, categoryId = 1,
            note = "Original note", title = "Dinner with the team"
        )
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.load(1, month)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Dinner with the team", viewModel.uiState.value.transactions.first().noteOrPlaceholder)
        collector.cancel()
    }

    @Test
    fun `a split transaction's row shows your share, with the full amount struck through`() = runTest {
        val transactions = FakeTransactionRepository()
        val viewModel = viewModel(transactions = transactions)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val month = MonthKey.current()
        transactions.insert(
            Money.ofRupees(900), Instant.now(), month, categoryId = 1, note = "Dinner",
            owedTotal = Money.ofRupees(550)
        )
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.load(1, month)
        dispatcher.scheduler.advanceUntilIdle()

        val row = viewModel.uiState.value.transactions.first()
        assertEquals(Money.ofRupees(350).formatted(), row.amountText)
        assertEquals(Money.ofRupees(900).formatted(), row.splitTotalText)
        collector.cancel()
    }

    @Test
    fun `a non-split transaction's row never shows a struck-through total`() = runTest {
        val transactions = FakeTransactionRepository()
        val viewModel = viewModel(transactions = transactions)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val month = MonthKey.current()
        transactions.insert(Money.ofRupees(250), Instant.now(), month, categoryId = 1, note = "Snacks")
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.load(1, month)
        dispatcher.scheduler.advanceUntilIdle()

        val row = viewModel.uiState.value.transactions.first()
        assertEquals(Money.ofRupees(250).formatted(), row.amountText)
        assertNull(row.splitTotalText)
        collector.cancel()
    }

    @Test
    fun `an SMS-linked transaction shows the message's merchant as its title and the message's real time, not the transaction's midnight-truncated one`() = runTest {
        val messages = FakeMessageRepository()
        val transactions = FakeTransactionRepository(messages)
        val viewModel = viewModel(transactions = transactions, messages = messages)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val realReceivedAt = Instant.now().minusSeconds(3600) // a real, non-midnight timestamp
        val messageId = messages.ingest(
            SmsMessage(
                id = 0, sender = "BANK", body = "Rs 300 debited", receivedAt = realReceivedAt,
                smsProviderId = null, dedupeKey = "k1", parsedAmount = Money.ofRupees(300),
                merchant = "Cafe", suggestedCategoryId = null, status = MessageStatus.NOT_ASSIGNED,
                isNew = true
            )
        )!!
        val month = MonthKey.current()
        val message = messages.getById(messageId)!!
        // saveFromMessage stores whatever occurredAt it's given - here a midnight-truncated one,
        // matching what AddTransactionViewModel.onSave() actually produces.
        val midnightOccurredAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS)
        transactions.saveFromMessage(message, Money.ofRupees(300), midnightOccurredAt, month, categoryId = 1, note = null)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.load(1, month)
        dispatcher.scheduler.advanceUntilIdle()

        val row = viewModel.uiState.value.transactions.first()
        assertEquals("Cafe", row.noteOrPlaceholder)
        assertEquals(realReceivedAt, row.occurredAt)
        collector.cancel()
    }

    @Test
    fun `a manual transaction with no note falls back to the placeholder, unaffected by the message lookup`() = runTest {
        val transactions = FakeTransactionRepository()
        val viewModel = viewModel(transactions = transactions)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val month = MonthKey.current()
        transactions.insert(Money.ofRupees(100), Instant.now(), month, categoryId = 1, note = null)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.load(1, month)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("No note", viewModel.uiState.value.transactions.first().noteOrPlaceholder)
        collector.cancel()
    }
}
