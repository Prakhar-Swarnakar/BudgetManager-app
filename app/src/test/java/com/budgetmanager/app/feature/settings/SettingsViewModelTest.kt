package com.budgetmanager.app.feature.settings

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `switches default to on`() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsRepository())
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
        val viewModel = SettingsViewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onEightyPercentAlertsToggled(false)
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.eightyPercentAlertsEnabled)
        assertTrue(viewModel.uiState.value.newSpendsAlertsEnabled) // untouched
        assertTrue(viewModel.uiState.value.overBudgetAlertsEnabled) // untouched
        collector.cancel()
    }
}
