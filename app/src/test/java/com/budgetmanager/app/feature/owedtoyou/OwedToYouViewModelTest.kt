package com.budgetmanager.app.feature.owedtoyou

import com.budgetmanager.app.core.model.Category
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.data.repository.FakeCategoryRepository
import com.budgetmanager.app.data.repository.FakeFriendRepository
import com.budgetmanager.app.data.repository.FakeTransactionRepository
import com.budgetmanager.app.data.repository.FakeTransactionSplitRepository
import com.budgetmanager.app.data.repository.TransactionSplitInput
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
class OwedToYouViewModelTest {

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
        val viewModel: OwedToYouViewModel,
        val transactions: FakeTransactionRepository,
        val friends: FakeFriendRepository,
        val splits: FakeTransactionSplitRepository
    )

    private fun setUpViewModel(): TestHarness {
        val categories = FakeCategoryRepository().apply {
            seed(listOf(Category(1, "Food & Dining", "🍔", 0, false)))
        }
        val transactions = FakeTransactionRepository()
        val friends = FakeFriendRepository()
        val splits = FakeTransactionSplitRepository(friends, transactions, categories)
        val viewModel = OwedToYouViewModel(splits, transactions)
        return TestHarness(viewModel, transactions, friends, splits)
    }

    @Test
    fun `onFiltersClicked opens the sheet, onFiltersDismissed closes it`() = runTest {
        val (viewModel, _, _, _) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        assertFalse(viewModel.uiState.value.showFiltersSheet)

        viewModel.onFiltersClicked()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showFiltersSheet)

        viewModel.onFiltersDismissed()
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showFiltersSheet)
        collector.cancel()
    }

    @Test
    fun `a transaction split with two friends is one row listing both`() = runTest {
        val (viewModel, transactions, friends, splits) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val transactionId = transactions.insert(
            amount = Money.ofRupees(300), occurredAt = Instant.now(),
            monthKey = MonthKey.of(2026, 9), categoryId = 1, note = "Dinner", owedTotal = Money.ofRupees(200)
        )
        val rajId = friends.getOrCreate("Raj")
        val priyaId = friends.getOrCreate("Priya")
        splits.replaceSplits(transactionId, listOf(TransactionSplitInput(rajId, Money.ofRupees(100)), TransactionSplitInput(priyaId, Money.ofRupees(100))))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.rows.size)
        val row = viewModel.uiState.value.rows.first()
        assertEquals(setOf("Raj", "Priya"), row.friendNames.toSet())
        assertEquals(Money.ofRupees(200).formatted(), row.amountText)
        assertEquals(Money.ofRupees(200).formatted(), viewModel.uiState.value.totalPendingText)
        collector.cancel()
    }

    @Test
    fun `two separate split transactions are two separate rows`() = runTest {
        val (viewModel, transactions, friends, splits) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val t1 = transactions.insert(
            amount = Money.ofRupees(300), occurredAt = Instant.now(),
            monthKey = MonthKey.of(2026, 9), categoryId = 1, note = "Dinner", owedTotal = Money.ofRupees(100)
        )
        val t2 = transactions.insert(
            amount = Money.ofRupees(500), occurredAt = Instant.now(),
            monthKey = MonthKey.of(2026, 9), categoryId = 1, note = "Groceries", owedTotal = Money.ofRupees(250)
        )
        val rajId = friends.getOrCreate("Raj")
        splits.replaceSplits(t1, listOf(TransactionSplitInput(rajId, Money.ofRupees(100))))
        splits.replaceSplits(t2, listOf(TransactionSplitInput(rajId, Money.ofRupees(250))))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.rows.size)
        collector.cancel()
    }

    @Test
    fun `reminding a transaction is transaction-level, not per friend`() = runTest {
        val (viewModel, transactions, friends, splits) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val transactionId = transactions.insert(
            amount = Money.ofRupees(300), occurredAt = Instant.now(),
            monthKey = MonthKey.of(2026, 9), categoryId = 1, note = "Dinner", owedTotal = Money.ofRupees(200)
        )
        val rajId = friends.getOrCreate("Raj")
        val priyaId = friends.getOrCreate("Priya")
        splits.replaceSplits(transactionId, listOf(TransactionSplitInput(rajId, Money.ofRupees(100)), TransactionSplitInput(priyaId, Money.ofRupees(100))))
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.rows.first().reminded)

        viewModel.onReminderToggled(transactionId, true)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.rows.first().reminded)
        // Nothing about the owed amount changed - reminding never touches the receivable.
        assertEquals(Money.ofRupees(200).formatted(), viewModel.uiState.value.totalPendingText)
        collector.cancel()
    }

    @Test
    fun `reminding then un-reminding returns to Pending`() = runTest {
        val (viewModel, transactions, friends, splits) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val transactionId = transactions.insert(
            amount = Money.ofRupees(300), occurredAt = Instant.now(),
            monthKey = MonthKey.of(2026, 9), categoryId = 1, note = "Dinner", owedTotal = Money.ofRupees(100)
        )
        val rajId = friends.getOrCreate("Raj")
        splits.replaceSplits(transactionId, listOf(TransactionSplitInput(rajId, Money.ofRupees(100))))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onReminderToggled(transactionId, true)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.rows.first().reminded)

        viewModel.onReminderToggled(transactionId, false)
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.rows.first().reminded)
        collector.cancel()
    }

    @Test
    fun `status filter narrows rows, and counts stay independent of the current filter`() = runTest {
        val (viewModel, transactions, friends, splits) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val t1 = transactions.insert(
            amount = Money.ofRupees(300), occurredAt = Instant.now(),
            monthKey = MonthKey.of(2026, 9), categoryId = 1, note = "Dinner", owedTotal = Money.ofRupees(100)
        )
        val t2 = transactions.insert(
            amount = Money.ofRupees(500), occurredAt = Instant.now(),
            monthKey = MonthKey.of(2026, 9), categoryId = 1, note = "Groceries", owedTotal = Money.ofRupees(250)
        )
        val rajId = friends.getOrCreate("Raj")
        splits.replaceSplits(t1, listOf(TransactionSplitInput(rajId, Money.ofRupees(100))))
        splits.replaceSplits(t2, listOf(TransactionSplitInput(rajId, Money.ofRupees(250))))
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onReminderToggled(t1, true)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onStatusFilterChanged(ReminderFilter.REMINDED)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.rows.size)
        assertEquals(t1, viewModel.uiState.value.rows.first().transactionId)
        // Counts on the chips themselves aren't narrowed by the currently selected filter.
        val options = viewModel.uiState.value.statusFilterOptions
        assertEquals(2, options.first { it.value == ReminderFilter.ALL }.count)
        assertEquals(1, options.first { it.value == ReminderFilter.PENDING }.count)
        assertEquals(1, options.first { it.value == ReminderFilter.REMINDED }.count)

        viewModel.onStatusFilterChanged(ReminderFilter.PENDING)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.rows.size)
        assertEquals(t2, viewModel.uiState.value.rows.first().transactionId)
        collector.cancel()
    }

    @Test
    fun `friend filter narrows rows to only transactions that friend is on`() = runTest {
        val (viewModel, transactions, friends, splits) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val t1 = transactions.insert(
            amount = Money.ofRupees(300), occurredAt = Instant.now(),
            monthKey = MonthKey.of(2026, 9), categoryId = 1, note = "Dinner", owedTotal = Money.ofRupees(200)
        )
        val t2 = transactions.insert(
            amount = Money.ofRupees(500), occurredAt = Instant.now(),
            monthKey = MonthKey.of(2026, 9), categoryId = 1, note = "Groceries", owedTotal = Money.ofRupees(250)
        )
        val rajId = friends.getOrCreate("Raj")
        val priyaId = friends.getOrCreate("Priya")
        splits.replaceSplits(t1, listOf(TransactionSplitInput(rajId, Money.ofRupees(100)), TransactionSplitInput(priyaId, Money.ofRupees(100))))
        splits.replaceSplits(t2, listOf(TransactionSplitInput(priyaId, Money.ofRupees(250))))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.rows.size)

        viewModel.onFriendFilterChanged(rajId)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.rows.size)
        assertEquals(t1, viewModel.uiState.value.rows.first().transactionId)

        viewModel.onFriendFilterChanged(null)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.rows.size)
        collector.cancel()
    }

    @Test
    fun `by friend view groups transactions per friend, that friend's tag first, collapsed by default`() = runTest {
        val (viewModel, transactions, friends, splits) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val transactionId = transactions.insert(
            amount = Money.ofRupees(300), occurredAt = Instant.now(),
            monthKey = MonthKey.of(2026, 9), categoryId = 1, note = "Dinner", owedTotal = Money.ofRupees(200)
        )
        val rajId = friends.getOrCreate("Raj")
        val priyaId = friends.getOrCreate("Priya")
        splits.replaceSplits(transactionId, listOf(TransactionSplitInput(rajId, Money.ofRupees(100)), TransactionSplitInput(priyaId, Money.ofRupees(100))))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onViewModeChanged(OwedToYouViewMode.BY_FRIEND)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(OwedToYouViewMode.BY_FRIEND, state.viewMode)
        assertEquals(2, state.friendSections.size)
        // Collapsed by default - nothing in expandedFriendIds yet.
        assertTrue(state.expandedFriendIds.isEmpty())

        val rajSection = state.friendSections.first { it.friendId == rajId }
        assertEquals(Money.ofRupees(100).formatted(), rajSection.pendingText)
        assertEquals("Raj", rajSection.rows.first().friendNames.first())
        // Raj's own row shows his own ₹100 share, not the ₹200 combined total shown in By
        // transaction view - with that combined total struck through alongside it.
        assertEquals(Money.ofRupees(100).formatted(), rajSection.rows.first().amountText)
        assertEquals(Money.ofRupees(200).formatted(), rajSection.rows.first().splitTotalText)

        val priyaSection = state.friendSections.first { it.friendId == priyaId }
        assertEquals("Priya", priyaSection.rows.first().friendNames.first())
        assertEquals(Money.ofRupees(100).formatted(), priyaSection.rows.first().amountText)
        assertEquals(Money.ofRupees(200).formatted(), priyaSection.rows.first().splitTotalText)

        viewModel.onFriendSectionToggled(rajId)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(rajId in viewModel.uiState.value.expandedFriendIds)

        viewModel.onFriendSectionToggled(rajId)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.expandedFriendIds.isEmpty())
        collector.cancel()
    }

    @Test
    fun `by friend view never shows a struck-through total for a single-friend split`() = runTest {
        val (viewModel, transactions, friends, splits) = setUpViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        val transactionId = transactions.insert(
            amount = Money.ofRupees(300), occurredAt = Instant.now(),
            monthKey = MonthKey.of(2026, 9), categoryId = 1, note = "Dinner", owedTotal = Money.ofRupees(100)
        )
        val rajId = friends.getOrCreate("Raj")
        splits.replaceSplits(transactionId, listOf(TransactionSplitInput(rajId, Money.ofRupees(100))))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onViewModeChanged(OwedToYouViewMode.BY_FRIEND)
        dispatcher.scheduler.advanceUntilIdle()

        val row = viewModel.uiState.value.friendSections.first { it.friendId == rajId }.rows.first()
        assertEquals(Money.ofRupees(100).formatted(), row.amountText)
        assertNull(row.splitTotalText)
        collector.cancel()
    }
}
