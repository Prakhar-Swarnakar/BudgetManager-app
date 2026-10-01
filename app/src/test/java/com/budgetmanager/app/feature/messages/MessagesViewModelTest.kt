package com.budgetmanager.app.feature.messages

import com.budgetmanager.app.core.model.Category
import com.budgetmanager.app.core.model.KeywordRule
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.SmsMessage
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.data.repository.FakeCategoryRepository
import com.budgetmanager.app.data.repository.FakeKeywordRuleRepository
import com.budgetmanager.app.data.repository.FakeMessageRepository
import com.budgetmanager.app.data.repository.FakeTransactionRepository
import com.budgetmanager.app.sms.CategorySuggester
import com.budgetmanager.app.sms.FakeInboxScanner
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
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class MessagesViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun categoryRepository() = FakeCategoryRepository().apply {
        seed(listOf(Category(1, "Food & Dining", "🍔", 0, false)))
    }

    private fun viewModel(
        repo: FakeMessageRepository,
        transactions: FakeTransactionRepository = FakeTransactionRepository(repo),
        scanner: FakeInboxScanner = FakeInboxScanner(),
        categorySuggester: CategorySuggester = CategorySuggester(FakeKeywordRuleRepository())
    ) = MessagesViewModel(repo, transactions, scanner, categoryRepository(), categorySuggester)

    private fun testMessage(dedupeKey: String) = SmsMessage(
        id = 0, sender = "TESTBANK", body = "Rs 100 debited", receivedAt = Instant.now(),
        smsProviderId = null, dedupeKey = dedupeKey, parsedAmount = Money.ofRupees(100),
        merchant = "Test Store", suggestedCategoryId = null, status = MessageStatus.NOT_ASSIGNED,
        isNew = true
    )

    /** An Instant that falls inside [month], in the device's own zone - for tests that need
     *  messages placed in a specific month relative to [MonthKey.current]'s real one. */
    private fun instantIn(month: MonthKey, dayOfMonth: Int = 15): Instant =
        YearMonth.of(month.year, month.month).atDay(dayOfMonth).atStartOfDay(ZoneId.systemDefault()).toInstant()

    @Test
    fun `filter counts reflect message statuses`() = runTest {
        val repo = FakeMessageRepository()
        val viewModel = viewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        repo.ingest(testMessage("k1"))
        val id2 = repo.ingest(testMessage("k2"))!!
        repo.updateStatus(id2, MessageStatus.ACCEPTED)
        val id3 = repo.ingest(testMessage("k3"))!!
        repo.updateStatus(id3, MessageStatus.REJECTED)
        dispatcher.scheduler.advanceUntilIdle()

        val counts = viewModel.uiState.value.counts
        assertEquals(3, counts[MessageFilter.ALL])
        assertEquals(1, counts[MessageFilter.NOT_ASSIGNED])
        assertEquals(1, counts[MessageFilter.ACCEPTED])
        assertEquals(1, counts[MessageFilter.REJECTED])
        collector.cancel()
    }

    @Test
    fun `selecting a filter shows only matching rows`() = runTest {
        val repo = FakeMessageRepository()
        val viewModel = viewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        repo.ingest(testMessage("k1"))
        val id2 = repo.ingest(testMessage("k2"))!!
        repo.updateStatus(id2, MessageStatus.ACCEPTED)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onFilterSelected(MessageFilter.ACCEPTED)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.rows.size)
        assertEquals(id2, viewModel.uiState.value.rows.first().id)
        collector.cancel()
    }

    @Test
    fun `swipe reject rejects the message and can be undone`() = runTest {
        val repo = FakeMessageRepository()
        val viewModel = viewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val id = repo.ingest(testMessage("k1"))!!
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onSwipeEnd(id)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(MessageStatus.REJECTED, repo.getById(id)!!.status)
        assertEquals(id, viewModel.uiState.value.undoRejectedMessageId)

        viewModel.onUndoReject()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(MessageStatus.NOT_ASSIGNED, repo.getById(id)!!.status)
        assertNull(viewModel.uiState.value.undoRejectedMessageId)
        collector.cancel()
    }

    @Test
    fun `new-flag clears only after leaving the screen`() = runTest {
        val repo = FakeMessageRepository()
        val viewModel = viewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val id = repo.ingest(testMessage("k1"))!!
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(repo.getById(id)!!.isNew)

        // Still "on" the screen in this test's terms: isNew must not clear on its own.
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(repo.getById(id)!!.isNew)

        viewModel.onLeftScreen()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(false, repo.getById(id)!!.isNew)
        collector.cancel()
    }

    @Test
    fun `swipe start on Not assigned requests navigation to Add Transaction`() = runTest {
        val repo = FakeMessageRepository()
        val viewModel = viewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val id = repo.ingest(testMessage("k1"))!!
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onSwipeStart(id)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(id, viewModel.uiState.value.navigateToAddTransactionForMessageId)
        assertEquals(MessageStatus.NOT_ASSIGNED, repo.getById(id)!!.status)
        collector.cancel()
    }

    @Test
    fun `swipe start on Rejected reverts to Not assigned - only Not assigned can become Accepted`() = runTest {
        val repo = FakeMessageRepository()
        val viewModel = viewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val id = repo.ingest(testMessage("k1"))!!
        repo.updateStatus(id, MessageStatus.REJECTED)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onSwipeStart(id)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(MessageStatus.NOT_ASSIGNED, repo.getById(id)!!.status)
        assertNull(viewModel.uiState.value.navigateToAddTransactionForMessageId)
        collector.cancel()
    }

    @Test
    fun `swipe start on Accepted is a no-op`() = runTest {
        val repo = FakeMessageRepository()
        val viewModel = viewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val id = repo.ingest(testMessage("k1"))!!
        repo.updateStatus(id, MessageStatus.ACCEPTED)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onSwipeStart(id)
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.navigateToAddTransactionForMessageId)
        collector.cancel()
    }

    @Test
    fun `swipe end on Accepted deletes its transaction and reverts to Not assigned`() = runTest {
        val repo = FakeMessageRepository()
        val transactions = FakeTransactionRepository(repo)
        val viewModel = viewModel(repo, transactions)

        val id = repo.ingest(testMessage("k1"))!!
        val message = repo.getById(id)!!
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val transactionId = transactions.saveFromMessage(
            message = message, amount = Money.ofRupees(100), occurredAt = Instant.now(),
            monthKey = MonthKey.of(2026, 9), categoryId = 1, note = "Test"
        )
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(MessageStatus.ACCEPTED, repo.getById(id)!!.status)

        viewModel.onSwipeEnd(id)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(MessageStatus.NOT_ASSIGNED, repo.getById(id)!!.status)
        assertNull(transactions.getById(transactionId))
        collector.cancel()
    }

    @Test
    fun `an Accepted row shows the emoji and name of its transaction's category`() = runTest {
        val repo = FakeMessageRepository()
        val transactions = FakeTransactionRepository(repo)
        val viewModel = viewModel(repo, transactions)

        val id = repo.ingest(testMessage("k1"))!!
        val message = repo.getById(id)!!
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        transactions.saveFromMessage(
            message = message, amount = Money.ofRupees(100), occurredAt = Instant.now(),
            monthKey = MonthKey.of(2026, 9), categoryId = 1, note = "Test"
        )
        dispatcher.scheduler.advanceUntilIdle()

        val row = viewModel.uiState.value.rows.first { it.id == id }
        assertEquals(MessageStatus.ACCEPTED, row.status)
        assertEquals("🍔", row.categoryEmoji)
        assertEquals("Food & Dining", row.categoryName)
        collector.cancel()
    }

    @Test
    fun `a Not assigned row shows its keyword suggestion in the same spot an Accepted row shows its real category`() = runTest {
        val repo = FakeMessageRepository()
        val viewModel = viewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val id = repo.ingest(testMessage("k1").copy(suggestedCategoryId = 1))!!
        dispatcher.scheduler.advanceUntilIdle()

        val row = viewModel.uiState.value.rows.first { it.id == id }
        assertEquals(MessageStatus.NOT_ASSIGNED, row.status)
        assertEquals("🍔", row.categoryEmoji)
        assertEquals("Food & Dining", row.categoryName)
        collector.cancel()
    }

    @Test
    fun `a Not assigned row with no keyword match shows no category`() = runTest {
        val repo = FakeMessageRepository()
        val viewModel = viewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val id = repo.ingest(testMessage("k1"))!! // suggestedCategoryId is null by default
        dispatcher.scheduler.advanceUntilIdle()

        val row = viewModel.uiState.value.rows.first { it.id == id }
        assertNull(row.categoryEmoji)
        collector.cancel()
    }

    @Test
    fun `a Rejected row never shows a category, even if one was suggested`() = runTest {
        val repo = FakeMessageRepository()
        val viewModel = viewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val id = repo.ingest(testMessage("k1").copy(suggestedCategoryId = 1))!!
        repo.updateStatus(id, MessageStatus.REJECTED)
        dispatcher.scheduler.advanceUntilIdle()

        val row = viewModel.uiState.value.rows.first { it.id == id }
        assertNull(row.categoryEmoji)
        collector.cancel()
    }

    @Test
    fun `swipe end on Rejected is a no-op - only Not assigned can become Rejected`() = runTest {
        val repo = FakeMessageRepository()
        val viewModel = viewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val id = repo.ingest(testMessage("k1"))!!
        repo.updateStatus(id, MessageStatus.REJECTED)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onSwipeEnd(id)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(MessageStatus.REJECTED, repo.getById(id)!!.status)
        collector.cancel()
    }

    @Test
    fun `two Not assigned messages with the same amount, different senders, close in time are both flagged`() = runTest {
        val repo = FakeMessageRepository()
        val viewModel = viewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val now = Instant.now()
        val bankId = repo.ingest(
            testMessage("k1").copy(sender = "AX-SBICRD-S", receivedAt = now)
        )!!
        val upiAppId = repo.ingest(
            testMessage("k2").copy(sender = "VM-GOOGLEPAY", receivedAt = now.plusSeconds(60))
        )!!
        dispatcher.scheduler.advanceUntilIdle()

        val rows = viewModel.uiState.value.rows.associateBy { it.id }
        assertTrue(rows[bankId]!!.isPossibleDuplicate)
        assertTrue(rows[upiAppId]!!.isPossibleDuplicate)
        collector.cancel()
    }

    @Test
    fun `selecting a flagged message shows which other message it might duplicate`() = runTest {
        val repo = FakeMessageRepository()
        val viewModel = viewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val now = Instant.now()
        val bankId = repo.ingest(testMessage("k1").copy(sender = "AX-SBICRD-S", receivedAt = now))!!
        val upiAppId = repo.ingest(
            testMessage("k2").copy(sender = "VM-GOOGLEPAY", receivedAt = now.plusSeconds(60))
        )!!
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onRowClick(bankId)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(upiAppId, viewModel.uiState.value.selectedMessageDuplicateOf?.id)
        collector.cancel()
    }

    @Test
    fun `onFetchMonth scans the currently viewed month's full range`() = runTest {
        val repo = FakeMessageRepository()
        val scanner = FakeInboxScanner()
        val viewModel = viewModel(repo, scanner = scanner)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        val month = viewModel.uiState.value.monthKey

        viewModel.onPreviousMonth()
        viewModel.onFetchMonth()
        dispatcher.scheduler.advanceUntilIdle()

        val previousMonth = month.previous()
        val zone = ZoneId.systemDefault()
        val expectedStart = YearMonth.of(previousMonth.year, previousMonth.month)
            .atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val expectedEnd = YearMonth.of(previousMonth.year, previousMonth.month)
            .plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        assertEquals(expectedStart to expectedEnd, scanner.lastScanRange)
        collector.cancel()
    }

    @Test
    fun `onPreviousMonth and onNextMonth move the viewed month, never past the current one`() = runTest {
        val repo = FakeMessageRepository()
        val viewModel = viewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        val currentMonth = viewModel.uiState.value.monthKey

        assertEquals(false, viewModel.uiState.value.canGoNext)

        viewModel.onPreviousMonth()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(currentMonth.previous(), viewModel.uiState.value.monthKey)
        assertTrue(viewModel.uiState.value.canGoNext)

        viewModel.onNextMonth()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(currentMonth, viewModel.uiState.value.monthKey)

        // Already back at the current month - next is a no-op, never moves into the future.
        viewModel.onNextMonth()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(currentMonth, viewModel.uiState.value.monthKey)
        collector.cancel()
    }

    @Test
    fun `rows and counts are scoped to the viewed month only`() = runTest {
        val repo = FakeMessageRepository()
        val viewModel = viewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        val currentMonth = viewModel.uiState.value.monthKey

        val thisMonthId = repo.ingest(testMessage("k1").copy(receivedAt = instantIn(currentMonth)))!!
        repo.ingest(testMessage("k2").copy(receivedAt = instantIn(currentMonth.previous())))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.counts[MessageFilter.ALL])
        assertEquals(thisMonthId, viewModel.uiState.value.rows.single().id)

        viewModel.onPreviousMonth()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.counts[MessageFilter.ALL])
        assertEquals("k2", repo.getById(viewModel.uiState.value.rows.single().id)!!.dedupeKey)
        collector.cancel()
    }

    @Test
    fun `onRunRule updates suggestions only for Not assigned messages in the viewed month`() = runTest {
        val repo = FakeMessageRepository()
        val keywordRules = FakeKeywordRuleRepository().apply {
            seed(listOf(KeywordRule(keyword = "test store", categoryId = 1)))
        }
        val viewModel = viewModel(repo, categorySuggester = CategorySuggester(keywordRules))
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        val currentMonth = viewModel.uiState.value.monthKey

        val notAssignedId = repo.ingest(testMessage("k1").copy(receivedAt = instantIn(currentMonth)))!!
        val acceptedId = repo.ingest(testMessage("k2").copy(receivedAt = instantIn(currentMonth)))!!
        repo.updateStatus(acceptedId, MessageStatus.ACCEPTED)
        val rejectedId = repo.ingest(testMessage("k3").copy(receivedAt = instantIn(currentMonth)))!!
        repo.updateStatus(rejectedId, MessageStatus.REJECTED)
        val otherMonthId = repo.ingest(
            testMessage("k4").copy(receivedAt = instantIn(currentMonth.previous()))
        )!!
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onRunRule()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1L, repo.getById(notAssignedId)!!.suggestedCategoryId)
        assertNull(repo.getById(acceptedId)!!.suggestedCategoryId)
        assertEquals(MessageStatus.ACCEPTED, repo.getById(acceptedId)!!.status)
        assertNull(repo.getById(rejectedId)!!.suggestedCategoryId)
        assertEquals(MessageStatus.REJECTED, repo.getById(rejectedId)!!.status)
        assertNull(repo.getById(otherMonthId)!!.suggestedCategoryId)
        collector.cancel()
    }
}
