package com.budgetmanager.app.feature.budget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MonthlyBudgetScreen(
    modifier: Modifier = Modifier,
    viewModel: MonthlyBudgetViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MonthlyBudgetContent(
        state = state,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onRowClick = viewModel::onRowClicked,
        onReorder = viewModel::onReorder,
        onSheetNameChanged = viewModel::onSheetNameChanged,
        onSheetEmojiChanged = viewModel::onSheetEmojiChanged,
        onSheetAmountChanged = viewModel::onSheetAmountChanged,
        onSheetSaved = viewModel::onSheetSaved,
        onSheetDismissed = viewModel::onSheetDismissed,
        modifier = modifier
    )
}
