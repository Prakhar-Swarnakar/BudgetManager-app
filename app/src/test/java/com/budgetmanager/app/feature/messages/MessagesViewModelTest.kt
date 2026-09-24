package com.budgetmanager.app.feature.messages

import com.budgetmanager.app.core.model.Category
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.SmsMessage
import com.budgetmanager.app.data.repository.FakeCategoryRepository
import com.budgetmanager.app.data.repository.FakeMessageRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

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

    private fun viewModel(repo: FakeMessageRepository, scanner: FakeInboxScanner = FakeInboxScanner()) =
        MessagesViewModel(repo, scanner, categoryRepository())

    private fun testMessage(dedupeKey: String) = SmsMessage(
        id = 0, sender = "TESTBANK", body = "Rs 100 debited", receivedAt = Instant.now(),
        smsProviderId = null, dedupeKey = dedupeKey, parsedAmount = Money.ofRupees(100),
        merchant = "Test Store", suggestedCategoryId = null, status = MessageStatus.NOT_ASSIGNED,
        isNew = true
    )

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
    fun `swipe start on Rejected also requests navigation - can still be accepted later`() = runTest {
        val repo = FakeMessageRepository()
        val viewModel = viewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val id = repo.ingest(testMessage("k1"))!!
        repo.updateStatus(id, MessageStatus.REJECTED)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onSwipeStart(id)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(id, viewModel.uiState.value.navigateToAddTransactionForMessageId)
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
    fun `swipe end on Accepted or Rejected is a no-op - un-accepting needs deleting the transaction`() = runTest {
        val repo = FakeMessageRepository()
        val viewModel = viewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val acceptedId = repo.ingest(testMessage("k1"))!!
        repo.updateStatus(acceptedId, MessageStatus.ACCEPTED)
        val rejectedId = repo.ingest(testMessage("k2"))!!
        repo.updateStatus(rejectedId, MessageStatus.REJECTED)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onSwipeEnd(acceptedId)
        viewModel.onSwipeEnd(rejectedId)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(MessageStatus.ACCEPTED, repo.getById(acceptedId)!!.status)
        assertEquals(MessageStatus.REJECTED, repo.getById(rejectedId)!!.status)
        collector.cancel()
    }

    @Test
    fun `onAddTestMessage inserts one Not assigned message with an amount and suggested category`() = runTest {
        val repo = FakeMessageRepository()
        val viewModel = viewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        viewModel.onAddTestMessage()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.counts[MessageFilter.ALL])
        val row = viewModel.uiState.value.rows.first()
        assertEquals(MessageStatus.NOT_ASSIGNED, row.status)
        // Pre-fill needs a real amount and category on the message - this is the only way to
        // exercise Add Transaction's pre-fill before M2b's real parser exists.
        assertNotNull(row.amountText)
        assertNotNull(repo.getById(row.id)!!.suggestedCategoryId)
        collector.cancel()
    }

    @Test
    fun `onImportTodaySms delegates to the inbox scanner from start of today`() = runTest {
        val repo = FakeMessageRepository()
        val scanner = FakeInboxScanner()
        val viewModel = viewModel(repo, scanner)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        viewModel.onImportTodaySms()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(scanner.lastScanFromMillis != null)
        collector.cancel()
    }
}
