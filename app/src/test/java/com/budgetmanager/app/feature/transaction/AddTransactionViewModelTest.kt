package com.budgetmanager.app.feature.transaction

import com.budgetmanager.app.core.model.Category
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.core.model.SmsMessage
import com.budgetmanager.app.data.repository.FakeAlertLogRepository
import com.budgetmanager.app.data.repository.FakeCategoryRepository
import com.budgetmanager.app.data.repository.FakeMessageRepository
import com.budgetmanager.app.data.repository.FakeMonthlyBudgetRepository
import com.budgetmanager.app.data.repository.FakeSettingsRepository
import com.budgetmanager.app.data.repository.FakeTransactionRepository
import com.budgetmanager.app.domain.EvaluateBudgetAlerts
import com.budgetmanager.app.domain.FakeBudgetAlertNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AddTransactionViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private data class TestHarness(
        val viewModel: AddTransactionViewModel,
        val messages: FakeMessageRepository,
        val transactions: FakeTransactionRepository,
        val budgets: FakeMonthlyBudgetRepository,
        val notifier: FakeBudgetAlertNotifier
    )

    private fun setUpViewModel(): TestHarness {
        val categories = FakeCategoryRepository().apply {
            seed(listOf(Category(1, "Food & Dining", "🍔", 0, false), Category(2, "Transport", "🚗", 1, false)))
        }
        val messages = FakeMessageRepository()
        val transactions = FakeTransactionRepository(messages)
        val budgets = FakeMonthlyBudgetRepository()
        val notifier = FakeBudgetAlertNotifier()
        val evaluateBudgetAlerts = EvaluateBudgetAlerts(
            budgets, transactions, FakeAlertLogRepository(), categories, FakeSettingsRepository(), notifier
        )
        val viewModel = AddTransactionViewModel(transactions, messages, evaluateBudgetAlerts, categories)
        return TestHarness(viewModel, messages, transactions, budgets, notifier)
    }

    @Test
    fun `manual add saves a transaction with the entered amount and category`() = runTest {
        val (viewModel, _, transactions) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        viewModel.load(messageId = null, transactionId = null)
        dispatcher.scheduler.advanceUntilIdle()

        val date = viewModel.uiState.value.date
        viewModel.onAmountChanged("250")
        viewModel.onCategorySelected(1)
        viewModel.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saved)
        val monthKey = com.budgetmanager.app.core.model.MonthKey.of(date.year, date.monthValue)
        val saved = transactions.observeForCategoryAndMonth(monthKey, 1).first()
        assertEquals(1, saved.size)
        assertEquals(Money.ofRupees(250), saved.first().amount)
        collector.cancel()
    }

    @Test
    fun `saving without an amount shows an error and does not save`() = runTest {
        val (viewModel, _, _) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        viewModel.load(messageId = null, transactionId = null)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onCategorySelected(1)
        viewModel.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.amountError)
        assertFalse(viewModel.uiState.value.saved)
        collector.cancel()
    }

    @Test
    fun `saving without a category shows an error and does not save`() = runTest {
        val (viewModel, _, _) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        viewModel.load(messageId = null, transactionId = null)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onAmountChanged("100")
        viewModel.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.categoryError)
        assertFalse(viewModel.uiState.value.saved)
        collector.cancel()
    }

    @Test
    fun `accepting a message pre-fills from it and saving marks the message Accepted`() = runTest {
        val (viewModel, messages, transactions) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val message = SmsMessage(
            id = 0, sender = "HDFCBK", body = "Rs 500 debited", receivedAt = Instant.now(),
            smsProviderId = null, dedupeKey = "k1", parsedAmount = Money.ofRupees(500),
            merchant = "Swiggy", suggestedCategoryId = 1, status = MessageStatus.NOT_ASSIGNED, isNew = true
        )
        val messageId = messages.ingest(message)!!

        viewModel.load(messageId = messageId, transactionId = null)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("500", viewModel.uiState.value.amountInput)
        assertEquals("Swiggy", viewModel.uiState.value.note)
        assertEquals(1L, viewModel.uiState.value.suggestedCategoryId)
        assertEquals(1L, viewModel.uiState.value.selectedCategoryId)
        assertNotNull(viewModel.uiState.value.smsBannerText)

        viewModel.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saved)
        assertEquals(MessageStatus.ACCEPTED, messages.getById(messageId)!!.status)
        collector.cancel()
    }

    @Test
    fun `editing an existing transaction pre-fills and updates it, not inserts a new one`() = runTest {
        val (viewModel, _, transactions) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val transactionId = transactions.insert(
            amount = Money.ofRupees(300), occurredAt = Instant.now(),
            monthKey = com.budgetmanager.app.core.model.MonthKey.of(2026, 9), categoryId = 1, note = "Original"
        )

        viewModel.load(messageId = null, transactionId = transactionId)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("300", viewModel.uiState.value.amountInput)
        assertTrue(viewModel.uiState.value.isEditMode)

        viewModel.onAmountChanged("450")
        viewModel.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        val updated = transactions.getById(transactionId)!!
        assertEquals(Money.ofRupees(450), updated.amount)
        collector.cancel()
    }

    @Test
    fun `saving a manual transaction that crosses 80 percent fires a budget alert`() = runTest {
        val (viewModel, _, _, budgets, notifier) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        viewModel.load(messageId = null, transactionId = null)
        dispatcher.scheduler.advanceUntilIdle()

        val date = viewModel.uiState.value.date
        val monthKey = MonthKey.of(date.year, date.monthValue)
        budgets.setAmount(monthKey, categoryId = 1, amount = Money.ofRupees(1000))

        viewModel.onAmountChanged("900")
        viewModel.onCategorySelected(1)
        viewModel.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, notifier.calls.size)
        collector.cancel()
    }

    @Test
    fun `editing a transaction never fires a budget alert, even if it crosses over budget`() = runTest {
        val (viewModel, _, transactions, budgets, notifier) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val monthKey = MonthKey.of(2026, 9)
        budgets.setAmount(monthKey, categoryId = 1, amount = Money.ofRupees(1000))
        val transactionId = transactions.insert(
            amount = Money.ofRupees(300), occurredAt = Instant.now(),
            monthKey = monthKey, categoryId = 1, note = "Original"
        )

        viewModel.load(messageId = null, transactionId = transactionId)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onAmountChanged("1200") // now over budget
        viewModel.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(notifier.calls.isEmpty())
        collector.cancel()
    }
}
