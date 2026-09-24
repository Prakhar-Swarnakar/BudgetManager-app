package com.budgetmanager.app.feature.messages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MessagesScreen(
    onNavigateToAddTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MessagesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose { viewModel.onLeftScreen() }
    }

    LaunchedEffect(state.navigateToAddTransactionForMessageId) {
        val id = state.navigateToAddTransactionForMessageId ?: return@LaunchedEffect
        onNavigateToAddTransaction(id)
        viewModel.onNavigationHandled()
    }

    MessagesContent(
        state = state,
        onFilterSelected = viewModel::onFilterSelected,
        onSwipeStart = viewModel::onSwipeStart,
        onSwipeEnd = viewModel::onSwipeEnd,
        onRowClick = viewModel::onRowClick,
        onDetailDismissed = viewModel::onDetailDismissed,
        onAcceptFromDetail = viewModel::onAcceptFromDetail,
        onRejectFromDetail = viewModel::onRejectFromDetail,
        onUndoReject = viewModel::onUndoReject,
        onUndoDismissed = viewModel::onUndoDismissed,
        onImportTodaySms = viewModel::onImportTodaySms,
        onAddTestMessage = viewModel::onAddTestMessage,
        modifier = modifier
    )
}
