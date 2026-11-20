package com.budgetmanager.app.feature.wallet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetmanager.app.core.model.MonthKey

@Composable
fun WalletScreen(
    onNavigateToCategoryDetail: (categoryId: Long, categoryName: String, monthKey: MonthKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WalletViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    WalletContent(
        state = state,
        onAddClick = viewModel::onAddClicked,
        onSpendClick = { spend ->
            onNavigateToCategoryDetail(spend.categoryId, spend.categoryName, spend.monthKey)
        },
        onDeleteTopUp = viewModel::onDeleteTopUpRequested,
        onDeleteConfirmed = viewModel::onDeleteTopUpConfirmed,
        onDeleteCancelled = viewModel::onDeleteTopUpCancelled,
        onSheetAmountChanged = viewModel::onSheetAmountChanged,
        onSheetDateChanged = viewModel::onSheetDateChanged,
        onSheetNoteChanged = viewModel::onSheetNoteChanged,
        onSheetSaved = viewModel::onSheetSaved,
        onSheetDismissed = viewModel::onSheetDismissed,
        modifier = modifier
    )
}
