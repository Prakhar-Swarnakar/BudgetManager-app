package com.budgetmanager.app.feature.statementvalidator

import com.budgetmanager.app.core.model.MessageDirection
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.SmsMessage
import com.budgetmanager.app.data.repository.FakeMessageRepository
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
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class StatementValidatorViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun testMessage(dedupeKey: String, amount: Money, dayOfSeptember: Int) = SmsMessage(
        id = 0, sender = "TESTBANK", body = "Rs ${amount.paise / 100} debited",
        receivedAt = Instant.parse(String.format("2026-09-%02dT10:00:00Z", dayOfSeptember)),
        smsProviderId = null, dedupeKey = dedupeKey, parsedAmount = amount, merchant = "Test Store",
        suggestedCategoryId = null, status = MessageStatus.NOT_ASSIGNED, isNew = false,
        direction = MessageDirection.DEBIT
    )

    @Test
    fun `a matching CSV row and message land in needsReview, and the summary adds up`() = runTest {
        val repo = FakeMessageRepository()
        repo.ingest(testMessage("k1", Money.ofRupees(3000), dayOfSeptember = 5))
        val viewModel = StatementValidatorViewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        val csv = """
            Date,Narration,Debit,Credit,Balance
            05/09/2026,INVESCO SIP,3000.00,,50000.00
        """.trimIndent()
        viewModel.onFileSelected("statement.csv", csv.toByteArray())
        dispatcher.scheduler.advanceUntilIdle()

        val report = viewModel.uiState.value.step as StatementValidatorStep.Report
        assertEquals(100, report.coveragePercent)
        assertEquals(1, report.needsReview.size)
        assertTrue(report.missing.isEmpty())
        collector.cancel()
    }

    @Test
    fun `a CSV row with no matching message lands in missing, ready to Add`() = runTest {
        val repo = FakeMessageRepository()
        val viewModel = StatementValidatorViewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        val csv = """
            Date,Narration,Debit,Credit,Balance
            03/09/2026,Systematic Investment Purchase,2999.85,,50000.00
        """.trimIndent()
        viewModel.onFileSelected("statement.csv", csv.toByteArray())
        dispatcher.scheduler.advanceUntilIdle()

        val report = viewModel.uiState.value.step as StatementValidatorStep.Report
        assertEquals(1, report.missing.size)
        assertEquals(0, report.coveragePercent)

        viewModel.onAddRequested(report.missing.first())
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(report.missing.first(), viewModel.uiState.value.navigateToAddTransactionForLine)

        viewModel.onNavigationHandled()
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(viewModel.uiState.value.navigateToAddTransactionForLine)
        collector.cancel()
    }

    @Test
    fun `a file with no readable Date or Amount header surfaces a clear error, not a crash`() = runTest {
        val repo = FakeMessageRepository()
        val viewModel = StatementValidatorViewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onFileSelected("not-a-statement.csv", "Name,Address\nTest,Bangalore".toByteArray())
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.errorMessage != null)
        assertEquals(StatementValidatorStep.PickFile, viewModel.uiState.value.step)

        viewModel.onDismissError()
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(viewModel.uiState.value.errorMessage)
        collector.cancel()
    }

    @Test
    fun `onReset returns to the file-picking step`() = runTest {
        val repo = FakeMessageRepository()
        val viewModel = StatementValidatorViewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onFileSelected(
            "statement.csv",
            "Date,Narration,Debit,Credit,Balance\n05/09/2026,Test,100.00,,900.00".toByteArray()
        )
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.step is StatementValidatorStep.Report)

        viewModel.onReset()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(StatementValidatorStep.PickFile, viewModel.uiState.value.step)
        collector.cancel()
    }
}
