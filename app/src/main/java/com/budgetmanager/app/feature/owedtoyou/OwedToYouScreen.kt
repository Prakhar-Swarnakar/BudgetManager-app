package com.budgetmanager.app.feature.owedtoyou

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OwedToYouScreen(
    onNavigateToEditTransaction: (transactionId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OwedToYouViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OwedToYouContent(
        state = state,
        onViewModeChanged = viewModel::onViewModeChanged,
        onStatusFilterChanged = viewModel::onStatusFilterChanged,
        onFriendFilterChanged = viewModel::onFriendFilterChanged,
        onFriendSectionToggled = viewModel::onFriendSectionToggled,
        onReminderToggled = viewModel::onReminderToggled,
        onRowClick = onNavigateToEditTransaction,
        onFiltersClicked = viewModel::onFiltersClicked,
        onFiltersDismissed = viewModel::onFiltersDismissed,
        modifier = modifier
    )
}
