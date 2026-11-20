package com.budgetmanager.app.feature.wallet

import com.budgetmanager.app.core.model.CashSpend
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.data.repository.FakeCashRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class WalletViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun cashSpend(id: Long, rupees: Long, at: Instant) = CashSpend(
        transactionId = id, title = "Street food", amount = Money.ofRupees(rupees), occurredAt = at,
        categoryId = 1, categoryEmoji = "🍔", categoryName = "Food"
    )

    @Test
    fun `balance is top-ups minus cash spends`() = runTest {
        val repo = FakeCashRepository()
        repo.seedSpends(listOf(cashSpend(1, 150, Instant.now().minusSeconds(60)), cashSpend(2, 110, Instant.now())))
        repo.addTopUp(Money.ofRupees(2000), Instant.now().minusSeconds(3600), "ATM")
        val viewModel = WalletViewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("₹1,740", viewModel.uiState.value.balanceText)
        assertFalse(viewModel.uiState.value.isNegative)
        assertEquals(3, viewModel.uiState.value.entries.size)
        collector.cancel()
    }

    @Test
    fun `a balance below zero is flagged as negative`() = runTest {
        val repo = FakeCashRepository()
        repo.seedSpends(listOf(cashSpend(1, 260, Instant.now())))
        val viewModel = WalletViewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("-₹260", viewModel.uiState.value.balanceText)
        assertTrue(viewModel.uiState.value.isNegative)
        collector.cancel()
    }

    @Test
    fun `entries are newest first across top-ups and spends`() = runTest {
        val repo = FakeCashRepository()
        val now = Instant.now()
        repo.addTopUp(Money.ofRupees(500), now.minusSeconds(100), null)
        repo.seedSpends(listOf(cashSpend(7, 50, now)))
        val viewModel = WalletViewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        val entries = viewModel.uiState.value.entries
        assertTrue(entries[0] is WalletEntryUi.Spend)
        assertTrue(entries[1] is WalletEntryUi.TopUp)
        assertEquals("Cash added", (entries[1] as WalletEntryUi.TopUp).title)
        collector.cancel()
    }

    @Test
    fun `saving the sheet adds a top-up and closes it`() = runTest {
        val repo = FakeCashRepository()
        val viewModel = WalletViewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onAddClicked()
        viewModel.onSheetAmountChanged("2000")
        viewModel.onSheetNoteChanged("ATM withdrawal")
        viewModel.onSheetSaved()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.sheet)
        assertEquals("₹2,000", viewModel.uiState.value.balanceText)
        assertEquals("ATM withdrawal", (viewModel.uiState.value.entries.single() as WalletEntryUi.TopUp).title)
        collector.cancel()
    }

    @Test
    fun `an empty or zero amount keeps the sheet open with an error`() = runTest {
        val repo = FakeCashRepository()
        val viewModel = WalletViewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onAddClicked()
        viewModel.onSheetSaved()
        dispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.sheet?.amountError)

        viewModel.onSheetAmountChanged("0")
        viewModel.onSheetSaved()
        dispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.sheet?.amountError)
        assertTrue(viewModel.uiState.value.entries.isEmpty())
        collector.cancel()
    }

    @Test
    fun `deleting a top-up needs confirmation and then removes it`() = runTest {
        val repo = FakeCashRepository()
        repo.addTopUp(Money.ofRupees(500), Instant.now(), null)
        val viewModel = WalletViewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        val id = (viewModel.uiState.value.entries.single() as WalletEntryUi.TopUp).id
        viewModel.onDeleteTopUpRequested(id)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(id, viewModel.uiState.value.pendingDeleteTopUpId)
        assertEquals(1, viewModel.uiState.value.entries.size)

        viewModel.onDeleteTopUpConfirmed()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.entries.isEmpty())
        assertNull(viewModel.uiState.value.pendingDeleteTopUpId)
        collector.cancel()
    }

    @Test
    fun `cancelling the delete leaves the top-up in place`() = runTest {
        val repo = FakeCashRepository()
        repo.addTopUp(Money.ofRupees(500), Instant.now(), null)
        val viewModel = WalletViewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onDeleteTopUpRequested((viewModel.uiState.value.entries.single() as WalletEntryUi.TopUp).id)
        viewModel.onDeleteTopUpCancelled()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingDeleteTopUpId)
        assertEquals(1, viewModel.uiState.value.entries.size)
        collector.cancel()
    }
}
