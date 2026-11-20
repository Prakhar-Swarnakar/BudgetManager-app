package com.budgetmanager.app.feature.messages

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.designsystem.components.ConfirmDialog
import com.budgetmanager.app.core.designsystem.components.EmptyState
import com.budgetmanager.app.core.designsystem.components.FilterChipItem
import com.budgetmanager.app.core.designsystem.components.FilterChipRow
import com.budgetmanager.app.core.designsystem.components.MonthSelector
import com.budgetmanager.app.core.designsystem.components.NoSwipeAction
import com.budgetmanager.app.core.designsystem.components.SwipeAction
import com.budgetmanager.app.core.designsystem.components.SwipeRow
import com.budgetmanager.app.core.designsystem.components.UndoSnackbarEffect
import com.budgetmanager.app.core.designsystem.components.revertSwipeAction
import com.budgetmanager.app.core.model.MessageDirection
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.feature.messages.components.MessageDetailSheet
import com.budgetmanager.app.feature.messages.components.MessageFiltersSheet
import com.budgetmanager.app.feature.messages.components.MessageRow
import com.budgetmanager.app.ui.theme.StatusColors

@Composable
fun MessagesContent(
    state: MessagesUiState,
    onFilterSelected: (MessageFilter) -> Unit,
    onDirectionFilterSelected: (MessageDirection?) -> Unit,
    onCategoryFilterSelected: (CategoryFilter) -> Unit,
    onTaxonomyFilterSelected: (TaxonomyFilter) -> Unit,
    onFiltersClicked: () -> Unit,
    onFiltersDismissed: () -> Unit,
    onSwipeStart: (Long) -> Unit,
    onSwipeEnd: (Long) -> Unit,
    onRowClick: (Long) -> Unit,
    onDetailDismissed: () -> Unit,
    onAcceptFromDetail: (Long) -> Unit,
    onRejectFromDetail: (Long) -> Unit,
    onUndoReject: () -> Unit,
    onUndoDismissed: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onFetchMonth: () -> Unit,
    onRunRule: () -> Unit,
    onClearMonthClicked: () -> Unit,
    onClearMonthConfirmed: () -> Unit,
    onClearMonthDismissed: () -> Unit,
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

    // Any dimension narrowed past its default means an empty list is the filters' doing, not an
    // empty month - the two cases need different wording, and status alone isn't enough to tell
    // them apart now that Direction/Category/Taxonomy can each narrow it from the sheet.
    val filtersActive = state.directionFilter != null ||
        state.categoryFilter != CategoryFilter.Any ||
        state.taxonomyFilter != TaxonomyFilter.Any

    Column(modifier = modifier.fillMaxSize()) {
        MonthSelector(
            monthKey = state.monthKey,
            onPrevious = onPreviousMonth,
            onNext = onNextMonth,
            canGoNext = state.canGoNext,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onFetchMonth) { Text("Fetch SMS") }
            OutlinedButton(onClick = onRunRule) { Text("Run rule") }
            OutlinedButton(onClick = onClearMonthClicked) { Text("Clear month") }
        }

        // Status stays the one filter row always visible outside - Direction, Category, and
        // Taxonomy all live behind the Filters icon pinned at the start of this same row, rather
        // than owning a second row of their own (06-backlog.md, "Richer Messages filters", revised
        // 2026-10-07 after the first cut had too many stacked rows). The badge shows whenever any
        // of those three is narrowed past its default.
        Row(verticalAlignment = Alignment.CenterVertically) {
            BadgedBox(badge = { if (filtersActive) Badge() }) {
                IconButton(onClick = onFiltersClicked) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filters")
                }
            }
            FilterChipRow(
                items = MessageFilter.entries.map { f -> FilterChipItem(f, f.label(), state.counts[f] ?: 0) },
                selected = state.filter,
                onSelect = onFilterSelected,
                modifier = Modifier.weight(1f)
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            if (state.rows.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.MarkEmailRead,
                    message = if (state.filter == MessageFilter.ALL && !filtersActive) {
                        "No messages yet. They'll show up here as bank SMS arrive."
                    } else {
                        "Nothing here for these filters."
                    }
                )
            } else {
                val defaultAccept = SwipeAction(Icons.Default.Check, StatusColors.accepted)
                val defaultReject = SwipeAction(Icons.Default.Close, StatusColors.overBudget)
                val revert = revertSwipeAction()

                LazyColumn {
                    items(state.rows, key = { it.id }) { row ->
                        // The reveal strip always matches what the swipe will really do - only
                        // Not assigned ever shows the green/red accept-reject look; Accepted and
                        // Rejected show a neutral "undo" on their one live direction, and nothing
                        // on the other (a no-op there per the swipe rules).
                        val (startAction, endAction) = when (row.status) {
                            MessageStatus.NOT_ASSIGNED -> defaultAccept to defaultReject
                            MessageStatus.ACCEPTED -> NoSwipeAction to revert
                            MessageStatus.REJECTED -> revert to NoSwipeAction
                        }
                        SwipeRow(
                            onSwipeStart = { onSwipeStart(row.id) },
                            onSwipeEnd = { onSwipeEnd(row.id) },
                            startAction = startAction,
                            endAction = endAction
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
            duplicateOf = state.selectedMessageDuplicateOf,
            onDismiss = onDetailDismissed,
            onAccept = { onAcceptFromDetail(message.id) },
            onReject = { onRejectFromDetail(message.id) }
        )
    }

    if (state.showClearMonthConfirm) {
        ConfirmDialog(
            title = "Clear ${state.monthKey.label()}?",
            message = "Removes every message in this month, accepted or not. Any transaction " +
                "they're linked to is kept, just unlinked - its amount and category stay as they are.",
            onConfirm = onClearMonthConfirmed,
            onDismiss = onClearMonthDismissed,
            confirmLabel = "Clear"
        )
    }

    if (state.showFiltersSheet) {
        MessageFiltersSheet(
            directionFilter = state.directionFilter,
            directionCounts = state.directionCounts,
            categories = state.categories,
            categoryFilter = state.categoryFilter,
            categoryCounts = state.categoryCounts,
            taxonomyFilter = state.taxonomyFilter,
            taxonomyCounts = state.taxonomyCounts,
            onDirectionSelected = onDirectionFilterSelected,
            onCategorySelected = onCategoryFilterSelected,
            onTaxonomySelected = onTaxonomyFilterSelected,
            onDismiss = onFiltersDismissed
        )
    }
}

private fun MessageFilter.label(): String = when (this) {
    MessageFilter.ALL -> "All"
    MessageFilter.NOT_ASSIGNED -> "Not assigned"
    MessageFilter.ACCEPTED -> "Accepted"
    MessageFilter.REJECTED -> "Rejected"
}
