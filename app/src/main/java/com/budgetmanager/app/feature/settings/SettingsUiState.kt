package com.budgetmanager.app.feature.settings

data class SettingsUiState(
    val isLoading: Boolean = true,
    val newSpendsAlertsEnabled: Boolean = true,
    val eightyPercentAlertsEnabled: Boolean = true,
    val overBudgetAlertsEnabled: Boolean = true
)
