package com.budgetmanager.app.feature.transaction

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AddTransactionScreen(
    messageId: Long?,
    transactionId: Long?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    // A distinct key per (messageId, transactionId): without it, every navigation to this
    // screen would share one Hilt-cached ViewModel instance (there's no per-NavEntry
    // ViewModelStore scoping wired up for Navigation 3 here), so accepting message B after
    // message A would still show A's already-loaded state.
    viewModel: AddTransactionViewModel = hiltViewModel(key = "AddTransaction:$messageId:$transactionId")
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(messageId, transactionId) {
        viewModel.load(messageId, transactionId)
    }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            onDone()
            viewModel.onSavedHandled()
        }
    }

    AddTransactionContent(
        state = state,
        onAmountChanged = viewModel::onAmountChanged,
        onNoteChanged = viewModel::onNoteChanged,
        onDateChanged = viewModel::onDateChanged,
        onCategorySelected = viewModel::onCategorySelected,
        onSave = viewModel::onSave,
        onCancel = onDone,
        modifier = modifier
    )
}
