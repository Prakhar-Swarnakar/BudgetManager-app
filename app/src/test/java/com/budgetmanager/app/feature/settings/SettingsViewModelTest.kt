package com.budgetmanager.app.feature.settings

import com.budgetmanager.app.data.backup.BackupBundle
import com.budgetmanager.app.data.backup.BackupSerializer
import com.budgetmanager.app.data.backup.CategoryBackup
import com.budgetmanager.app.data.repository.FakeBackupRepository
import com.budgetmanager.app.data.repository.FakeSettingsRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val emptyBundle = BackupBundle(
        formatVersion = BackupBundle.CURRENT_FORMAT_VERSION,
        exportedAtMillis = 1L,
        categories = listOf(CategoryBackup(1, "Food", "🍔", 0, false)),
        monthlyBudgets = emptyList(),
        transactions = emptyList(),
        smsMessages = emptyList(),
        alertLogs = emptyList(),
        keywordRules = emptyList()
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(
        settingsRepository: FakeSettingsRepository = FakeSettingsRepository(),
        backupRepository: FakeBackupRepository = FakeBackupRepository()
    ) = SettingsViewModel(settingsRepository, backupRepository)

    @Test
    fun `switches default to on`() = runTest {
        val viewModel = buildViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.newSpendsAlertsEnabled)
        assertTrue(state.eightyPercentAlertsEnabled)
        assertTrue(state.overBudgetAlertsEnabled)
        collector.cancel()
    }

    @Test
    fun `toggling a switch persists it and updates state`() = runTest {
        val repo = FakeSettingsRepository()
        val viewModel = buildViewModel(settingsRepository = repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onEightyPercentAlertsToggled(false)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.eightyPercentAlertsEnabled)
        assertTrue(viewModel.uiState.value.newSpendsAlertsEnabled) // untouched
        assertTrue(viewModel.uiState.value.overBudgetAlertsEnabled) // untouched
        collector.cancel()
    }

    @Test
    fun `no export recorded means lastExportAtMillis is null`() = runTest {
        val viewModel = buildViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.lastExportAtMillis)
        collector.cancel()
    }

    @Test
    fun `completing an export records the timestamp`() = runTest {
        val viewModel = buildViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onExportCompleted()
        dispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.lastExportAtMillis)
        collector.cancel()
    }

    @Test
    fun `selecting a valid backup file stages it for confirmation without touching data`() = runTest {
        val backupRepository = FakeBackupRepository()
        val viewModel = buildViewModel(backupRepository = backupRepository)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onImportFileSelected(BackupSerializer.serialize(emptyBundle))
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.pendingImport)
        assertEquals(1, state.pendingImport!!.categoryCount)
        assertNull(state.importError)
        assertNull(backupRepository.restoredBundle) // nothing written until confirmed
        collector.cancel()
    }

    @Test
    fun `selecting a malformed file surfaces an error and stages nothing`() = runTest {
        val viewModel = buildViewModel()
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onImportFileSelected("not json")
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.importError)
        assertNull(state.pendingImport)
        collector.cancel()
    }

    @Test
    fun `confirming a staged import restores it and clears the pending state`() = runTest {
        val backupRepository = FakeBackupRepository()
        val viewModel = buildViewModel(backupRepository = backupRepository)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onImportFileSelected(BackupSerializer.serialize(emptyBundle))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onConfirmImport()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(emptyBundle, backupRepository.restoredBundle)
        val state = viewModel.uiState.value
        assertNull(state.pendingImport)
        assertNotNull(state.importSuccessMessage)
        collector.cancel()
    }

    @Test
    fun `cancelling a staged import discards it without restoring`() = runTest {
        val backupRepository = FakeBackupRepository()
        val viewModel = buildViewModel(backupRepository = backupRepository)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onImportFileSelected(BackupSerializer.serialize(emptyBundle))
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onCancelImport()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(backupRepository.restoredBundle)
        assertNull(viewModel.uiState.value.pendingImport)
        collector.cancel()
    }
}
