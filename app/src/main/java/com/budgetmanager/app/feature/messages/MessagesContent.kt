package com.budgetmanager.app.feature.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.BuildConfig
import com.budgetmanager.app.core.designsystem.components.EmptyState
import com.budgetmanager.app.core.designsystem.components.FilterChipItem
import com.budgetmanager.app.core.designsystem.components.FilterChipRow
import com.budgetmanager.app.core.designsystem.components.SwipeRow
import com.budgetmanager.app.core.designsystem.components.UndoSnackbarEffect
import com.budgetmanager.app.feature.messages.components.MessageDetailSheet
import com.budgetmanager.app.feature.messages.components.MessageRow

@Composable
fun MessagesContent(
    state: MessagesUiState,
    onFilterSelected: (MessageFilter) -> Unit,
    onSwipeStart: (Long) -> Unit,
    onSwipeEnd: (Long) -> Unit,
    onRowClick: (Long) -> Unit,
    onDetailDismissed: () -> Unit,
    onAcceptFromDetail: (Long) -> Unit,
    onRejectFromDetail: (Long) -> Unit,
    onUndoReject: () -> Unit,
    onUndoDismissed: () -> Unit,
    onImportTodaySms: () -> Unit,
    onAddTestMessage: () -> Unit,
    onDebugReset: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    UndoSnackbarEffect(
        trigger = state.undoRejectedMessageId,
        snackbarHostState = snackbarHostState,
        message = "Message rejected",
        onUndo = onUndoReject,
        onDismissed = onUndoDismissed
    )

    Column(modifier = modifier.fillMaxSize()) {
        if (BuildConfig.DEBUG) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(onClick = onImportTodaySms) { Text("Import today's SMS") }
                TextButton(onClick = onAddTestMessage) { Text("Add test message") }
            }
        }

        FilterChipRow(
            items = MessageFilter.entries.map { f -> FilterChipItem(f, f.label(), state.counts[f] ?: 0) },
            selected = state.filter,
            onSelect = onFilterSelected,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            if (state.rows.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.MarkEmailRead,
                    message = if (state.filter == MessageFilter.ALL) {
                        "No messages yet. They'll show up here as bank SMS arrive."
                    } else {
                        "Nothing here for this filter."
                    }
                )
            } else {
                LazyColumn {
                    items(state.rows, key = { it.id }) { row ->
                        SwipeRow(
                            onSwipeStart = { onSwipeStart(row.id) },
                            onSwipeEnd = { onSwipeEnd(row.id) }
                        ) {
                            MessageRow(row = row, onClick = { onRowClick(row.id) })
                        }
                    }
                }
            }
        }

        SnackbarHost(snackbarHostState)
    }

    state.selectedMessage?.let { message ->
        MessageDetailSheet(
            message = message,
            onDismiss = onDetailDismissed,
            onAccept = { onAcceptFromDetail(message.id) },
            onReject = { onRejectFromDetail(message.id) },
            onDebugReset = { onDebugReset(message.id) }
        )
    }
}

private fun MessageFilter.label(): String = when (this) {
    MessageFilter.ALL -> "All"
    MessageFilter.NOT_ASSIGNED -> "Not assigned"
    MessageFilter.ACCEPTED -> "Accepted"
    MessageFilter.REJECTED -> "Rejected"
}
