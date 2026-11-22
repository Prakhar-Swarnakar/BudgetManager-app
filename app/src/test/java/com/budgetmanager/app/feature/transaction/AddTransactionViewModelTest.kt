package com.budgetmanager.app.feature.transaction

import com.budgetmanager.app.core.model.Category
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.core.model.SmsMessage
import com.budgetmanager.app.core.model.TaxonomyType
import com.budgetmanager.app.data.repository.FakeAlertLogRepository
import com.budgetmanager.app.data.repository.FakeCategoryRepository
import com.budgetmanager.app.data.repository.FakeFriendRepository
import com.budgetmanager.app.data.repository.FakeKeywordRuleRepository
import com.budgetmanager.app.data.repository.FakeMessageRepository
import com.budgetmanager.app.data.repository.FakeMonthlyBudgetRepository
import com.budgetmanager.app.data.repository.FakeSettingsRepository
import com.budgetmanager.app.data.repository.FakeTaxonomyKeywordRuleRepository
import com.budgetmanager.app.data.repository.FakeTransactionRepository
import com.budgetmanager.app.data.repository.FakeTransactionSplitRepository
import com.budgetmanager.app.data.repository.TransactionSplitInput
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
        val notifier: FakeBudgetAlertNotifier,
        val keywordRules: FakeKeywordRuleRepository,
        val taxonomyRules: FakeTaxonomyKeywordRuleRepository,
        val friends: FakeFriendRepository,
        val splits: FakeTransactionSplitRepository
    )

    private fun setUpViewModel(): TestHarness {
        val categories = FakeCategoryRepository().apply {
            seed(listOf(Category(1, "Food & Dining", "🍔", 0, false), Category(2, "Transport", "🚗", 1, false)))
        }
        val messages = FakeMessageRepository()
        val transactions = FakeTransactionRepository(messages)
        val budgets = FakeMonthlyBudgetRepository()
        val notifier = FakeBudgetAlertNotifier()
        val keywordRules = FakeKeywordRuleRepository()
        val taxonomyRules = FakeTaxonomyKeywordRuleRepository()
        val friends = FakeFriendRepository()
        val splits = FakeTransactionSplitRepository(friends, transactions, categories)
        val evaluateBudgetAlerts = EvaluateBudgetAlerts(
            budgets, transactions, FakeAlertLogRepository(), categories, FakeSettingsRepository(), notifier
        )
        val viewModel = AddTransactionViewModel(
            transactions, messages, evaluateBudgetAlerts, keywordRules, taxonomyRules, friends, splits, categories
        )
        return TestHarness(viewModel, messages, transactions, budgets, notifier, keywordRules, taxonomyRules, friends, splits)
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
        assertEquals("Swiggy", viewModel.uiState.value.merchant)
        assertEquals("", viewModel.uiState.value.note)
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
    fun `accepting a message learns its merchant maps to the category the user chose`() = runTest {
        val (viewModel, messages, _, _, _, keywordRules) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val message = SmsMessage(
            id = 0, sender = "HDFCBK", body = "Rs 500 debited", receivedAt = Instant.now(),
            smsProviderId = null, dedupeKey = "k1", parsedAmount = Money.ofRupees(500),
            merchant = "Swiggy", suggestedCategoryId = 1, status = MessageStatus.NOT_ASSIGNED, isNew = true
        )
        val messageId = messages.ingest(message)!!
        viewModel.load(messageId = messageId, transactionId = null)
        dispatcher.scheduler.advanceUntilIdle()

        // Add to rule always starts unchecked - the user opts in deliberately.
        assertFalse(viewModel.uiState.value.addToRule)

        viewModel.onAddToRuleToggled(true)
        viewModel.onCategorySelected(2) // overrides the suggested category - the user's actual choice
        viewModel.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        val learned = keywordRules.getAll().single { it.keyword == "swiggy" }
        assertEquals(2L, learned.categoryId)
        collector.cancel()
    }

    @Test
    fun `leaving Add to rule unchecked on a message-linked transaction learns nothing`() = runTest {
        val (viewModel, messages, _, _, _, keywordRules) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val message = SmsMessage(
            id = 0, sender = "HDFCBK", body = "Rs 500 debited", receivedAt = Instant.now(),
            smsProviderId = null, dedupeKey = "k1", parsedAmount = Money.ofRupees(500),
            merchant = "Swiggy", suggestedCategoryId = 1, status = MessageStatus.NOT_ASSIGNED, isNew = true
        )
        val messageId = messages.ingest(message)!!
        viewModel.load(messageId = messageId, transactionId = null)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saved)
        assertTrue(keywordRules.getAll().isEmpty())
        collector.cancel()
    }

    @Test
    fun `a very short merchant is never learned - too likely to misfire against unrelated messages`() = runTest {
        val (viewModel, messages, _, _, _, keywordRules) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val message = SmsMessage(
            id = 0, sender = "HDFCBK", body = "Rs 500 debited", receivedAt = Instant.now(),
            smsProviderId = null, dedupeKey = "k1", parsedAmount = Money.ofRupees(500),
            merchant = "SB", suggestedCategoryId = null, status = MessageStatus.NOT_ASSIGNED, isNew = true
        )
        val messageId = messages.ingest(message)!!
        viewModel.load(messageId = messageId, transactionId = null)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onCategorySelected(1)
        viewModel.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(keywordRules.getAll().isEmpty())
        collector.cancel()
    }

    @Test
    fun `a manual transaction with Add to rule unchecked learns nothing`() = runTest {
        val (viewModel, _, _, _, _, keywordRules) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        viewModel.load(messageId = null, transactionId = null)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.merchant)
        assertFalse(viewModel.uiState.value.addToRule)

        viewModel.onAmountChanged("250")
        viewModel.onCategorySelected(1)
        viewModel.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(keywordRules.getAll().isEmpty())
        collector.cancel()
    }

    @Test
    fun `a manual transaction has no SMS merchant, so checking Add to rule still learns nothing`() = runTest {
        val (viewModel, _, _, _, _, keywordRules) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        viewModel.load(messageId = null, transactionId = null)
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.smsMerchant)

        // The UI disables this checkbox for a manual transaction - this simulates bypassing
        // that, to prove the real guard is in onSave (smsMerchant == null), not just the
        // checkbox's disabled state.
        viewModel.onAddToRuleToggled(true)
        viewModel.onCategoryKeywordChanged("Swiggy")
        viewModel.onAmountChanged("250")
        viewModel.onCategorySelected(1)
        viewModel.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saved)
        assertTrue(keywordRules.getAll().isEmpty())
        collector.cancel()
    }

    @Test
    fun `a very short merchant is never learned even with Add to rule checked`() = runTest {
        val (viewModel, _, _, _, _, keywordRules) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        viewModel.load(messageId = null, transactionId = null)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onMerchantChanged("SB")
        viewModel.onAddToRuleToggled(true)
        viewModel.onAmountChanged("250")
        viewModel.onCategorySelected(1)
        viewModel.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(keywordRules.getAll().isEmpty())
        collector.cancel()
    }

    @Test
    fun `editing Title never touches Add to rule or its keyword - they're seeded from the SMS merchant, not Title`() = runTest {
        val (viewModel, messages, _) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val message = SmsMessage(
            id = 0, sender = "HDFCBK", body = "Rs 500 debited", receivedAt = Instant.now(),
            smsProviderId = null, dedupeKey = "k1", parsedAmount = Money.ofRupees(500),
            merchant = "Swiggy", suggestedCategoryId = 1, status = MessageStatus.NOT_ASSIGNED, isNew = true
        )
        val messageId = messages.ingest(message)!!
        viewModel.load(messageId = messageId, transactionId = null)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onAddToRuleToggled(true)
        // Renaming Title for readability must not change what the rule would learn from.
        viewModel.onMerchantChanged("Dinner with the team")
        viewModel.onMerchantChanged("")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.addToRule)
        assertEquals("Swiggy", viewModel.uiState.value.categoryKeyword)
        collector.cancel()
    }

    @Test
    fun `editing a message-linked transaction prefills merchant from the original message`() = runTest {
        val (viewModel, messages, transactions) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val message = SmsMessage(
            id = 0, sender = "HDFCBK", body = "Rs 500 debited", receivedAt = Instant.now(),
            smsProviderId = null, dedupeKey = "k1", parsedAmount = Money.ofRupees(500),
            merchant = "Swiggy", suggestedCategoryId = 1, status = MessageStatus.NOT_ASSIGNED, isNew = true
        )
        val fetchedMessage = messages.ingest(message)!!.let { messages.getById(it)!! }
        val transactionId = transactions.saveFromMessage(
            message = fetchedMessage, amount = Money.ofRupees(500), occurredAt = Instant.now(),
            monthKey = MonthKey.of(2026, 9), categoryId = 1, note = null
        )

        viewModel.load(messageId = null, transactionId = transactionId)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Swiggy", viewModel.uiState.value.merchant)
        assertFalse(viewModel.uiState.value.addToRule)
        // The SMS banner shows in edit mode too now, the same as the accept-a-message path.
        assertEquals("Rs 500 debited", viewModel.uiState.value.smsBannerText)
        collector.cancel()
    }

    @Test
    fun `saving a renamed Title persists it on the transaction, and the linked message's own merchant is never touched`() = runTest {
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

        viewModel.onMerchantChanged("Dinner with the team")
        viewModel.onCategorySelected(1)
        viewModel.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        val saved = transactions.getById(transactions.current.first().id)!!
        assertEquals("Dinner with the team", saved.title)
        assertEquals("Swiggy", messages.getById(messageId)!!.merchant)
        collector.cancel()
    }

    @Test
    fun `reopening a renamed transaction shows the saved title, not the SMS merchant`() = runTest {
        val (viewModel, messages, transactions) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val message = SmsMessage(
            id = 0, sender = "HDFCBK", body = "Rs 500 debited", receivedAt = Instant.now(),
            smsProviderId = null, dedupeKey = "k1", parsedAmount = Money.ofRupees(500),
            merchant = "Swiggy", suggestedCategoryId = 1, status = MessageStatus.NOT_ASSIGNED, isNew = true
        )
        val fetchedMessage = messages.ingest(message)!!.let { messages.getById(it)!! }
        val transactionId = transactions.saveFromMessage(
            message = fetchedMessage, amount = Money.ofRupees(500), occurredAt = Instant.now(),
            monthKey = MonthKey.of(2026, 9), categoryId = 1, note = null, title = "Dinner with the team"
        )

        viewModel.load(messageId = null, transactionId = transactionId)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Dinner with the team", viewModel.uiState.value.merchant)
        collector.cancel()
    }

    @Test
    fun `editing a manual transaction with no linked message has a blank merchant field`() = runTest {
        val (viewModel, _, transactions) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val transactionId = transactions.insert(
            amount = Money.ofRupees(300), occurredAt = Instant.now(),
            monthKey = MonthKey.of(2026, 9), categoryId = 1, note = "Original"
        )

        viewModel.load(messageId = null, transactionId = transactionId)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.merchant)
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

    @Test
    fun `accepting a message pre-fills the taxonomy suggestion, unchecked by default`() = runTest {
        val (viewModel, messages, _) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val message = SmsMessage(
            id = 0, sender = "HDFCBK", body = "Rs 500 debited", receivedAt = Instant.now(),
            smsProviderId = null, dedupeKey = "k1", parsedAmount = Money.ofRupees(500),
            merchant = "Swiggy", suggestedCategoryId = 1, suggestedTaxonomy = TaxonomyType.UPI,
            status = MessageStatus.NOT_ASSIGNED, isNew = true
        )
        val messageId = messages.ingest(message)!!

        viewModel.load(messageId = messageId, transactionId = null)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(TaxonomyType.UPI, viewModel.uiState.value.selectedTaxonomy)
        assertEquals(TaxonomyType.UPI, viewModel.uiState.value.suggestedTaxonomy)
        assertFalse(viewModel.uiState.value.addTaxonomyToRule)
        collector.cancel()
    }

    @Test
    fun `a manual transaction has no SMS merchant, so checking Add to rule for taxonomy still learns nothing`() = runTest {
        val (viewModel, _, _, _, _, _, taxonomyRules) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        viewModel.load(messageId = null, transactionId = null)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onTaxonomySelected(TaxonomyType.UPI)
        viewModel.onAddTaxonomyToRuleToggled(true)
        viewModel.onTaxonomyKeywordChanged("Swiggy")
        viewModel.onAmountChanged("250")
        viewModel.onCategorySelected(1)
        viewModel.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saved)
        assertTrue(taxonomyRules.getAll().isEmpty())
        collector.cancel()
    }

    @Test
    fun `leaving Add to rule for taxonomy unchecked learns nothing, even with a taxonomy selected`() = runTest {
        val (viewModel, _, _, _, _, _, taxonomyRules) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        viewModel.load(messageId = null, transactionId = null)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onMerchantChanged("Swiggy")
        viewModel.onTaxonomySelected(TaxonomyType.UPI)
        viewModel.onAmountChanged("250")
        viewModel.onCategorySelected(1)
        viewModel.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(taxonomyRules.getAll().isEmpty())
        collector.cancel()
    }

    @Test
    fun `a very short merchant is never learned into the taxonomy rule either, even when checked`() = runTest {
        val (viewModel, _, _, _, _, _, taxonomyRules) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        viewModel.load(messageId = null, transactionId = null)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onMerchantChanged("SB")
        viewModel.onTaxonomySelected(TaxonomyType.UPI)
        viewModel.onAddTaxonomyToRuleToggled(true)
        viewModel.onAmountChanged("250")
        viewModel.onCategorySelected(1)
        viewModel.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(taxonomyRules.getAll().isEmpty())
        collector.cancel()
    }

    @Test
    fun `editing Title never touches Add to rule for taxonomy or its keyword`() = runTest {
        val (viewModel, messages, _) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val message = SmsMessage(
            id = 0, sender = "HDFCBK", body = "Rs 500 debited", receivedAt = Instant.now(),
            smsProviderId = null, dedupeKey = "k1", parsedAmount = Money.ofRupees(500),
            merchant = "Swiggy", suggestedCategoryId = 1, status = MessageStatus.NOT_ASSIGNED, isNew = true
        )
        val messageId = messages.ingest(message)!!
        viewModel.load(messageId = messageId, transactionId = null)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onAddTaxonomyToRuleToggled(true)
        viewModel.onMerchantChanged("Dinner with the team")
        viewModel.onMerchantChanged("")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.addTaxonomyToRule)
        assertEquals("Swiggy", viewModel.uiState.value.taxonomyKeyword)
        collector.cancel()
    }

    @Test
    fun `editing a transaction pre-fills its taxonomy directly, with Add to rule unchecked`() = runTest {
        val (viewModel, _, transactions) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val transactionId = transactions.insert(
            amount = Money.ofRupees(300), occurredAt = Instant.now(),
            monthKey = MonthKey.of(2026, 9), categoryId = 1, note = "Original", taxonomy = TaxonomyType.CASH
        )

        viewModel.load(messageId = null, transactionId = transactionId)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(TaxonomyType.CASH, viewModel.uiState.value.selectedTaxonomy)
        assertFalse(viewModel.uiState.value.addTaxonomyToRule)
        collector.cancel()
    }

    @Test
    fun `saving a manual transaction persists the selected taxonomy`() = runTest {
        val (viewModel, _, transactions) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        viewModel.load(messageId = null, transactionId = null)
        dispatcher.scheduler.advanceUntilIdle()

        val date = viewModel.uiState.value.date
        viewModel.onAmountChanged("250")
        viewModel.onCategorySelected(1)
        viewModel.onTaxonomySelected(TaxonomyType.CREDIT_CARD)
        viewModel.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        val monthKey = MonthKey.of(date.year, date.monthValue)
        val saved = transactions.observeForCategoryAndMonth(monthKey, 1).first()
        assertEquals(TaxonomyType.CREDIT_CARD, saved.first().taxonomy)
        collector.cancel()
    }

    @Test
    fun `an equal split among friends computes your share live as entries are added`() = runTest {
        val (viewModel, _, _, _, _, _, _, _, _) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        viewModel.load(messageId = null, transactionId = null)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onAmountChanged("300")
        viewModel.onAddSplitFriend("Raj")
        viewModel.onAddSplitFriend("Priya")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(Money.ofRupees(100), viewModel.uiState.value.yourShare)
        assertNull(viewModel.uiState.value.splitError)
        collector.cancel()
    }

    @Test
    fun `saving a split transaction stores the owed total and creates friends and splits`() = runTest {
        val (viewModel, _, transactions, _, _, _, _, friends, splits) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        viewModel.load(messageId = null, transactionId = null)
        dispatcher.scheduler.advanceUntilIdle()

        val date = viewModel.uiState.value.date
        viewModel.onAmountChanged("300")
        viewModel.onCategorySelected(1)
        viewModel.onAddSplitFriend("Raj")
        viewModel.onAddSplitFriend("Priya")
        viewModel.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saved)
        val monthKey = MonthKey.of(date.year, date.monthValue)
        val saved = transactions.observeForCategoryAndMonth(monthKey, 1).first().first()
        assertEquals(Money.ofRupees(200), saved.owedTotal)
        assertEquals(2, friends.current.size)
        assertEquals(2, splits.observeByTransaction(saved.id).first().size)
        collector.cancel()
    }

    @Test
    fun `custom amounts exceeding the total block saving with a split error`() = runTest {
        val (viewModel, _, _, _, _, _, _, _, _) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        viewModel.load(messageId = null, transactionId = null)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onAmountChanged("100")
        viewModel.onCategorySelected(1)
        viewModel.onAddSplitFriend("Raj")
        viewModel.onSplitFriendAmountChanged(0, "150")
        viewModel.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.splitError)
        assertFalse(viewModel.uiState.value.saved)
        collector.cancel()
    }

    @Test
    fun `editing a split transaction pre-fills its friends' amounts, split id, and settled status`() = runTest {
        val (viewModel, _, transactions, _, _, _, _, friends, splits) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val transactionId = transactions.insert(
            amount = Money.ofRupees(300), occurredAt = Instant.now(),
            monthKey = MonthKey.of(2026, 9), categoryId = 1, note = "Dinner", owedTotal = Money.ofRupees(200)
        )
        val rajId = friends.getOrCreate("Raj")
        splits.replaceSplits(transactionId, listOf(TransactionSplitInput(rajId, Money.ofRupees(200))))
        val splitId = splits.observeByTransaction(transactionId).first().first().id
        splits.settle(splitId)

        viewModel.load(messageId = null, transactionId = transactionId)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.splitEntries.size)
        val entry = viewModel.uiState.value.splitEntries.first()
        assertEquals("Raj", entry.name)
        assertEquals(splitId, entry.splitId)
        assertTrue(entry.settled)
        collector.cancel()
    }

    @Test
    fun `a friend added this session has no split id and starts unsettled`() = runTest {
        val (viewModel, _, _, _, _, _, _, _, _) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        viewModel.load(messageId = null, transactionId = null)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onAddSplitFriend("Raj")
        dispatcher.scheduler.advanceUntilIdle()

        val entry = viewModel.uiState.value.splitEntries.first()
        assertNull(entry.splitId)
        assertFalse(entry.settled)
        collector.cancel()
    }

    @Test
    fun `settled status survives an edit instead of being wiped by the wholesale split replace`() = runTest {
        val (viewModel, _, transactions, _, _, _, _, friends, splits) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val transactionId = transactions.insert(
            amount = Money.ofRupees(300), occurredAt = Instant.now(),
            monthKey = MonthKey.of(2026, 9), categoryId = 1, note = "Dinner", owedTotal = Money.ofRupees(200)
        )
        val rajId = friends.getOrCreate("Raj")
        splits.replaceSplits(transactionId, listOf(TransactionSplitInput(rajId, Money.ofRupees(200))))
        val originalSplitId = splits.observeByTransaction(transactionId).first().first().id
        splits.settle(originalSplitId)

        viewModel.load(messageId = null, transactionId = transactionId)
        dispatcher.scheduler.advanceUntilIdle()

        // Edit something unrelated to Raj's settled status (the note), then save - Raj himself
        // is never touched via onSplitFriendSettledToggled.
        viewModel.onNoteChanged("Dinner, updated")
        viewModel.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        val afterEdit = splits.observeByTransaction(transactionId).first().first()
        assertTrue(afterEdit.settled)
        collector.cancel()
    }

    @Test
    fun `toggling a friend unsettled during an edit persists as unsettled after save`() = runTest {
        val (viewModel, _, transactions, _, _, _, _, friends, splits) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val transactionId = transactions.insert(
            amount = Money.ofRupees(300), occurredAt = Instant.now(),
            monthKey = MonthKey.of(2026, 9), categoryId = 1, note = "Dinner", owedTotal = Money.ofRupees(200)
        )
        val rajId = friends.getOrCreate("Raj")
        splits.replaceSplits(transactionId, listOf(TransactionSplitInput(rajId, Money.ofRupees(200))))
        splits.settle(splits.observeByTransaction(transactionId).first().first().id)

        viewModel.load(messageId = null, transactionId = transactionId)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onSplitFriendSettledToggled(0, false)
        viewModel.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        val afterEdit = splits.observeByTransaction(transactionId).first().first()
        assertFalse(afterEdit.settled)
        assertNull(afterEdit.settledAt)
        collector.cancel()
    }

    @Test
    fun `removing all friends while editing clears the splits on save`() = runTest {
        val (viewModel, _, transactions, _, _, _, _, friends, splits) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val transactionId = transactions.insert(
            amount = Money.ofRupees(300), occurredAt = Instant.now(),
            monthKey = MonthKey.of(2026, 9), categoryId = 1, note = "Dinner", owedTotal = Money.ofRupees(200)
        )
        val rajId = friends.getOrCreate("Raj")
        splits.replaceSplits(transactionId, listOf(TransactionSplitInput(rajId, Money.ofRupees(200))))

        viewModel.load(messageId = null, transactionId = transactionId)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onRemoveSplitFriend(0)
        viewModel.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(splits.observeByTransaction(transactionId).first().isEmpty())
        assertEquals(Money(0), transactions.getById(transactionId)!!.owedTotal)
        collector.cancel()
    }

    @Test
    fun `the category keyword is seeded from the SMS merchant, not from Title, and survives unchecking then rechecking Add to rule`() = runTest {
        val (viewModel, messages, _) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val message = SmsMessage(
            id = 0, sender = "HDFCBK", body = "Rs 500 debited", receivedAt = Instant.now(),
            smsProviderId = null, dedupeKey = "k1", parsedAmount = Money.ofRupees(500),
            merchant = "Swiggy", suggestedCategoryId = 1, status = MessageStatus.NOT_ASSIGNED, isNew = true
        )
        val messageId = messages.ingest(message)!!
        viewModel.load(messageId = messageId, transactionId = null)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Swiggy", viewModel.uiState.value.categoryKeyword)

        viewModel.onAddToRuleToggled(true)
        viewModel.onCategoryKeywordChanged("swiggy food")
        viewModel.onAddToRuleToggled(false)
        viewModel.onAddToRuleToggled(true)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("swiggy food", viewModel.uiState.value.categoryKeyword)
        collector.cancel()
    }

    @Test
    fun `saving with Add to rule checked and a blank keyword blocks with an inline error, nothing saved`() = runTest {
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

        viewModel.onCategorySelected(1)
        viewModel.onAddToRuleToggled(true)
        viewModel.onCategoryKeywordChanged("sw")
        viewModel.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.saved)
        assertNotNull(viewModel.uiState.value.categoryKeywordError)
        assertTrue(transactions.observeForCategoryAndMonth(MonthKey.current(), 1).first().isEmpty())
        collector.cancel()
    }

    @Test
    fun `editing a message-linked transaction seeds the keyword fields from the linked message, independent of Title`() = runTest {
        val (viewModel, messages, transactions) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val message = SmsMessage(
            id = 0, sender = "HDFCBK", body = "Rs 500 debited", receivedAt = Instant.now(),
            smsProviderId = null, dedupeKey = "k1", parsedAmount = Money.ofRupees(500),
            merchant = "Swiggy", suggestedCategoryId = 1, status = MessageStatus.NOT_ASSIGNED, isNew = true
        )
        val fetchedMessage = messages.ingest(message)!!.let { messages.getById(it)!! }
        val transactionId = transactions.saveFromMessage(
            message = fetchedMessage, amount = Money.ofRupees(500), occurredAt = Instant.now(),
            monthKey = MonthKey.of(2026, 9), categoryId = 1, note = null
        )

        viewModel.load(messageId = null, transactionId = transactionId)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Swiggy", viewModel.uiState.value.categoryKeyword)
        assertEquals("Swiggy", viewModel.uiState.value.taxonomyKeyword)
        assertEquals("Swiggy", viewModel.uiState.value.smsMerchant)
        collector.cancel()
    }

    @Test
    fun `a new manual transaction can be saved as avoidable, and reopening it shows the flag`() = runTest {
        val (viewModel, _, transactions, _, _, _, _, _, _) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        viewModel.load(messageId = null, transactionId = null)
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.avoidable)

        viewModel.onAmountChanged("250")
        viewModel.onCategorySelected(1)
        viewModel.onAvoidableChanged(true)
        viewModel.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        val saved = transactions.current.single()
        assertTrue(saved.avoidable)

        viewModel.load(messageId = null, transactionId = saved.id)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.avoidable)
        collector.cancel()
    }

    @Test
    fun `unchecking avoidable on an edit clears the flag`() = runTest {
        val (viewModel, _, transactions, _, _, _, _, _, _) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        val id = transactions.insert(
            amount = Money.ofRupees(100), occurredAt = Instant.now(),
            monthKey = MonthKey.of(2026, 9), categoryId = 1, note = null, avoidable = true
        )
        viewModel.load(messageId = null, transactionId = id)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onAvoidableChanged(false)
        viewModel.onSave()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(transactions.getById(id)!!.avoidable)
        collector.cancel()
    }
}
