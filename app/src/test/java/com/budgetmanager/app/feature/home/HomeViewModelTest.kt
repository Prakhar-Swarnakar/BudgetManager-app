package com.budgetmanager.app.feature.home

import com.budgetmanager.app.core.model.Category
import com.budgetmanager.app.core.model.Friend
import com.budgetmanager.app.core.model.TransactionSplit
import com.budgetmanager.app.data.repository.FakeFriendRepository
import com.budgetmanager.app.core.model.BudgetStatus
import com.budgetmanager.app.core.model.MessageDirection
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.core.model.SmsMessage
import com.budgetmanager.app.data.repository.FakeCashRepository
import com.budgetmanager.app.data.repository.FakeCategoryRepository
import com.budgetmanager.app.data.repository.FakeMessageRepository
import com.budgetmanager.app.data.repository.FakeMonthlyBudgetRepository
import com.budgetmanager.app.data.repository.FakeTransactionRepository
import com.budgetmanager.app.data.repository.FakeTransactionSplitRepository
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
import java.time.LocalDate

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
        messages: FakeMessageRepository = FakeMessageRepository(),
        splits: FakeTransactionSplitRepository = FakeTransactionSplitRepository(),
        cash: FakeCashRepository = FakeCashRepository(),
        today: LocalDate = LocalDate.now().withDayOfMonth(15)
    ) = HomeViewModel(categories, budgets, transactions, messages, splits, cash).also { it.today = { today } }

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
    fun `income is the sum of Accepted credit messages this month, and saved is income minus spend`() = runTest {
        val categories = FakeCategoryRepository().apply {
            seed(listOf(Category(1, "Food", "🍔", 0, archived = false)))
        }
        val transactions = FakeTransactionRepository()
        val messages = FakeMessageRepository()
        val viewModel = viewModel(categories = categories, transactions = transactions, messages = messages)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        val month = viewModel.uiState.value.monthKey
        transactions.insert(Money.ofRupees(500), Instant.now(), month, categoryId = 1, note = null)
        val acceptedCreditId = messages.ingest(
            testMessage("k1").copy(direction = MessageDirection.CREDIT, parsedAmount = Money.ofRupees(2000))
        )!!
        messages.updateStatus(acceptedCreditId, MessageStatus.ACCEPTED)
        // Not assigned and Rejected credits, and an Accepted debit, must never count as income.
        messages.ingest(testMessage("k2").copy(direction = MessageDirection.CREDIT, parsedAmount = Money.ofRupees(9000)))
        val rejectedCreditId = messages.ingest(
            testMessage("k3").copy(direction = MessageDirection.CREDIT, parsedAmount = Money.ofRupees(9000))
        )!!
        messages.updateStatus(rejectedCreditId, MessageStatus.REJECTED)
        val acceptedDebitId = messages.ingest(testMessage("k4"))!!
        messages.updateStatus(acceptedDebitId, MessageStatus.ACCEPTED)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("₹2,000", viewModel.uiState.value.totalIncomeText)
        assertEquals("₹1,500", viewModel.uiState.value.totalSavedText)
        collector.cancel()
    }

    @Test
    fun `saved goes negative when spend exceeds income, shown with the usual minus sign`() = runTest {
        val categories = FakeCategoryRepository().apply {
            seed(listOf(Category(1, "Food", "🍔", 0, archived = false)))
        }
        val transactions = FakeTransactionRepository()
        val messages = FakeMessageRepository()
        val viewModel = viewModel(categories = categories, transactions = transactions, messages = messages)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        val month = viewModel.uiState.value.monthKey
        transactions.insert(Money.ofRupees(5000), Instant.now(), month, categoryId = 1, note = null)
        val acceptedCreditId = messages.ingest(
            testMessage("k1").copy(direction = MessageDirection.CREDIT, parsedAmount = Money.ofRupees(2000))
        )!!
        messages.updateStatus(acceptedCreditId, MessageStatus.ACCEPTED)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("-₹3,000", viewModel.uiState.value.totalSavedText)
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

    @Test
    fun `owed to you sums only still-pending shares on transactions dated in the viewed month`() = runTest {
        val categories = FakeCategoryRepository().apply {
            seed(listOf(Category(1, "Food", "🍔", 0, archived = false)))
        }
        val friends = FakeFriendRepository().apply { seed(listOf(Friend(1, "Raj"), Friend(2, "Priya"))) }
        val transactions = FakeTransactionRepository()
        val splits = FakeTransactionSplitRepository(friends, transactions, categories)
        val viewModel = viewModel(categories = categories, transactions = transactions, splits = splits)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        val month = viewModel.uiState.value.monthKey
        val thisMonth = month.toMillisRange(java.time.ZoneId.systemDefault()).first.let(Instant::ofEpochMilli)
            .plusSeconds(3600)
        val lastMonth = month.previous().toMillisRange(java.time.ZoneId.systemDefault()).first
            .let(Instant::ofEpochMilli).plusSeconds(3600)
        val dinner = transactions.insert(Money.ofRupees(900), thisMonth, month, 1, null)
        val oldDinner = transactions.insert(Money.ofRupees(600), lastMonth, month.previous(), 1, null)
        splits.seed(
            listOf(
                TransactionSplit(1, dinner, 1, Money.ofRupees(300), settled = false, settledAt = null),
                TransactionSplit(2, dinner, 2, Money.ofRupees(200), settled = true, settledAt = Instant.now()),
                TransactionSplit(3, oldDinner, 1, Money.ofRupees(250), settled = false, settledAt = null)
            )
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("₹300", viewModel.uiState.value.totalOwedText)
        collector.cancel()
    }

    private fun testMessage(dedupeKey: String) = SmsMessage(
        id = 0, sender = "TESTBANK", body = "Rs 100 debited", receivedAt = Instant.now(),
        smsProviderId = null, dedupeKey = dedupeKey, parsedAmount = Money.ofRupees(100),
        merchant = "Test Store", suggestedCategoryId = null, status = MessageStatus.NOT_ASSIGNED,
        isNew = true
    )

    @Test
    fun `bar colour follows the today mark - blue below it, amber past it, red over budget`() = runTest {
        val categories = FakeCategoryRepository().apply {
            seed(listOf(
                Category(1, "Food", "🍔", 0, archived = false),
                Category(2, "Rent", "🏠", 1, archived = false),
                Category(3, "Fun", "🎬", 2, archived = false)
            ))
        }
        val budgets = FakeMonthlyBudgetRepository()
        val transactions = FakeTransactionRepository()
        val day = LocalDate.now().withDayOfMonth(15)
        val viewModel = viewModel(categories, budgets, transactions, today = day)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        val month = viewModel.uiState.value.monthKey
        // Day 15 puts the mark at 15/length of the month, about 0.5.
        listOf(1L to 200L, 2L to 800L, 3L to 1200L).forEach { (id, spent) ->
            budgets.setAmount(month, categoryId = id, amount = Money.ofRupees(1000))
            transactions.insert(Money.ofRupees(spent), Instant.now(), month, categoryId = id, note = null)
        }
        dispatcher.scheduler.advanceUntilIdle()

        val byName = viewModel.uiState.value.cards.associateBy { it.name }
        assertEquals(BudgetStatus.UNDER_BUDGET, byName.getValue("Food").status)
        assertEquals(BudgetStatus.WARNING, byName.getValue("Rent").status)
        assertEquals("₹200 left", byName.getValue("Rent").leftLabel)
        assertEquals(BudgetStatus.OVER_BUDGET, byName.getValue("Fun").status)
        assertEquals("₹200 over", byName.getValue("Fun").leftLabel)
        collector.cancel()
    }

    @Test
    fun `avoidable is your share of this month's avoidable transactions only`() = runTest {
        val categories = FakeCategoryRepository().apply {
            seed(listOf(Category(1, "Food", "🍔", 0, archived = false)))
        }
        val transactions = FakeTransactionRepository()
        val viewModel = viewModel(categories, transactions = transactions)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        val month = viewModel.uiState.value.monthKey
        // 1,000 avoidable but 400 owed back by friends -> only your 600 counts.
        transactions.insert(
            Money.ofRupees(1000), Instant.now(), month, categoryId = 1, note = null,
            owedTotal = Money.ofRupees(400), avoidable = true
        )
        transactions.insert(Money.ofRupees(500), Instant.now(), month, categoryId = 1, note = null)
        // Avoidable but in another month - must not count.
        transactions.insert(
            Money.ofRupees(900), Instant.now(), month.previous(), categoryId = 1, note = null, avoidable = true
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("₹600", viewModel.uiState.value.avoidableText)
        // 600 of your 1,100 spent = 54%
        assertEquals("54% of spent", viewModel.uiState.value.avoidableShareText)
        collector.cancel()
    }

    @Test
    fun `a past month has no today mark or pace sentence`() = runTest {
        val viewModel = viewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onPreviousMonth()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.paceFraction)
        assertEquals(null, viewModel.uiState.value.paceText)
        collector.cancel()
    }

    @Test
    fun `cash on hand is top-ups minus cash spends`() = runTest {
        val cash = FakeCashRepository()
        val viewModel = viewModel(cash = cash)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()
        cash.addTopUp(Money.ofRupees(2000), Instant.now(), null)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("₹2,000", viewModel.uiState.value.cashOnHandText)
        collector.cancel()
    }
}

class HomePaceTest {
    private fun progress(budget: Long, spent: Long) =
        com.budgetmanager.app.core.model.BudgetMaths.evaluate(Money.ofRupees(budget), Money.ofRupees(spent))

    @Test
    fun `day 20 of a 30-day month is two thirds`() {
        val fraction = paceFraction(MonthKey.of(2026, 11), LocalDate.of(2026, 11, 20))
        assertEquals(20.0 / 30.0, fraction!!, 1e-9)
    }

    @Test
    fun `a month other than today's has no pace`() {
        assertEquals(null, paceFraction(MonthKey.of(2026, 10), LocalDate.of(2026, 11, 20)))
    }

    @Test
    fun `spent exactly at the mark is still blue`() {
        assertEquals(BudgetStatus.UNDER_BUDGET, paceStatus(progress(1000, 500), 0.5))
    }

    @Test
    fun `past a past month's end mark anything within budget is blue`() {
        assertEquals(BudgetStatus.UNDER_BUDGET, paceStatus(progress(1000, 1000), null))
        assertEquals(BudgetStatus.OVER_BUDGET, paceStatus(progress(1000, 1001), null))
    }

    @Test
    fun `pace difference is spent minus the share expected by now`() {
        // 45,000 budget, day 20 of 30 -> 30,000 expected; 38,420 spent -> 8,420 ahead.
        val diff = paceDifference(progress(45000, 38420), 20.0 / 30.0)
        assertEquals(Money.ofRupees(8420), diff)
    }
}
