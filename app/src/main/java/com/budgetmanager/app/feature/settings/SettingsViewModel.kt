package com.budgetmanager.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.observeNewSpendsAlertsEnabled(),
        settingsRepository.observeEightyPercentAlertsEnabled(),
        settingsRepository.observeOverBudgetAlertsEnabled()
    ) { newSpends, eightyPercent, overBudget ->
        SettingsUiState(
            isLoading = false,
            newSpendsAlertsEnabled = newSpends,
            eightyPercentAlertsEnabled = eightyPercent,
            overBudgetAlertsEnabled = overBudget
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun onNewSpendsAlertsToggled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNewSpendsAlertsEnabled(enabled) }
    }

    fun onEightyPercentAlertsToggled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setEightyPercentAlertsEnabled(enabled) }
    }

    fun onOverBudgetAlertsToggled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setOverBudgetAlertsEnabled(enabled) }
    }
}
