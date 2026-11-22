package com.budgetmanager.app.feature.messages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.InputChip
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.designsystem.components.ConfirmDialog
import com.budgetmanager.app.core.designsystem.components.EmptyState
import com.budgetmanager.app.core.designsystem.components.FilterChipItem
import com.budgetmanager.app.core.designsystem.components.FilterChipRow
import com.budgetmanager.app.core.designsystem.components.MonthSelector
import com.budgetmanager.app.core.designsystem.components.UndoSnackbarEffect
import com.budgetmanager.app.core.model.MessageDirection
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.feature.messages.components.MessageDetailSheet
import com.budgetmanager.app.feature.messages.components.MessageFiltersSheet
import com.budgetmanager.app.feature.messages.components.MessageRow
import com.budgetmanager.app.feature.messages.components.RevealAction
import com.budgetmanager.app.feature.messages.components.RevealSide
import com.budgetmanager.app.feature.messages.components.RevealSwipeRow
import com.budgetmanager.app.ui.theme.StatusColors
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalFoundationApi::class)
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
    onUndoFromDetail: (Long) -> Unit,
    onUndoReject: () -> Unit,
    onUndoDismissed: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onFetchMonth: () -> Unit,
    onRunRule: () -> Unit,
    onClearMonthClicked: () -> Unit,
    onClearMonthConfirmed: () -> Unit,
    onClearMonthDismissed: () -> Unit,
    onUndoConfirmConfirmed: () -> Unit,
    onUndoConfirmDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // At most one row is slid open: its id and which way. An open row closes by itself after a few
    // seconds, when the list scrolls, when the tab or month changes, or when another row opens.
    var openRow by remember { mutableStateOf<Pair<Long, RevealSide>?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    LaunchedEffect(openRow) {
        if (openRow != null) {
            delay(OPEN_ROW_TIMEOUT_MS)
            openRow = null
        }
    }
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) openRow = null }
    LaunchedEffect(state.filter, state.monthKey) { openRow = null }

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
            // Month tools live in a menu: they are occasional, the filters are constant.
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, contentDescription = "More") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Fetch SMS") }, onClick = { menuOpen = false; onFetchMonth() })
                    DropdownMenuItem(text = { Text("Run rule") }, onClick = { menuOpen = false; onRunRule() })
                    DropdownMenuItem(text = { Text("Clear month") }, onClick = { menuOpen = false; onClearMonthClicked() })
                }
            }
        }

        if (filtersActive) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.directionFilter?.let { d ->
                    RemovableChip(if (d == MessageDirection.DEBIT) "Outgoing" else "Incoming") { onDirectionFilterSelected(null) }
                }
                when (val c = state.categoryFilter) {
                    CategoryFilter.Any -> Unit
                    CategoryFilter.None -> RemovableChip("No category") { onCategoryFilterSelected(CategoryFilter.Any) }
                    is CategoryFilter.Specific -> RemovableChip(
                        state.categories.firstOrNull { it.id == c.categoryId }?.let { "${it.emoji} ${it.name}" } ?: "Category"
                    ) { onCategoryFilterSelected(CategoryFilter.Any) }
                }
                when (val t = state.taxonomyFilter) {
                    TaxonomyFilter.Any -> Unit
                    TaxonomyFilter.None -> RemovableChip("No payment method") { onTaxonomyFilterSelected(TaxonomyFilter.Any) }
                    is TaxonomyFilter.Specific -> RemovableChip(t.taxonomy.label) { onTaxonomyFilterSelected(TaxonomyFilter.Any) }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (state.rows.isEmpty()) {
                EmptyState(
                    icon = if (!filtersActive && state.filter == MessageFilter.NOT_ASSIGNED) Icons.Default.CheckCircle else Icons.Default.MarkEmailRead,
                    actionLabel = if (filtersActive) "Clear filters" else null,
                    onAction = {
                        onDirectionFilterSelected(null)
                        onCategoryFilterSelected(CategoryFilter.Any)
                        onTaxonomyFilterSelected(TaxonomyFilter.Any)
                    },
                    message = when {
                        filtersActive -> "Nothing here for these filters."
                        state.filter == MessageFilter.NOT_ASSIGNED ->
                            "All caught up for ${state.monthKey.label()}. New bank SMS will show up here."
                        state.filter == MessageFilter.ACCEPTED -> "No confirmed messages in ${state.monthKey.label()}."
                        state.filter == MessageFilter.REJECTED -> "No rejected messages in ${state.monthKey.label()}."
                        else -> "No messages yet. They'll show up here as bank SMS arrive."
                    }
                )
            } else {
                val groups = remember(state.rows) { groupByDay(state.rows, LocalDate.now(), ZoneId.systemDefault()) }
                LazyColumn(state = listState) {
                    groups.forEach { group ->
                        stickyHeader(key = "day-${group.title}") {
                            DayHeader(group, toReview = state.filter == MessageFilter.NOT_ASSIGNED)
                        }
                        items(group.rows, key = { it.id }) { row ->
                            // A swipe only slides the row open to show one button; the tap on that
                            // button acts. Each status shows only the buttons that do something:
                            // To review has Confirm (right) and Reject (left), Confirmed has Undo
                            // on the left swipe, Rejected has Undo on the right swipe.
                            val buttons = swipeButtonsFor(row.status)
                            val rowOpenSide = openRow?.takeIf { it.first == row.id }?.second
                            Column {
                                RevealSwipeRow(
                                    swipeRightAction = buttons.onSwipeRight?.toAction { onSwipeStart(row.id) },
                                    swipeLeftAction = buttons.onSwipeLeft?.toAction { onSwipeEnd(row.id) },
                                    open = rowOpenSide,
                                    onOpenChange = { side -> openRow = side?.let { row.id to it } }
                                ) {
                                    MessageRow(
                                        row = row,
                                        // Tapping an open row just closes it.
                                        onClick = {
                                            openRow = null
                                            if (rowOpenSide == null) onRowClick(row.id)
                                        }
                                    )
                                }
                                HorizontalDivider()
                            }
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
            row = state.rows.firstOrNull { it.id == message.id },
            duplicateOf = state.selectedMessageDuplicateOf,
            onDismiss = onDetailDismissed,
            onUndo = { onUndoFromDetail(message.id) },
            onAccept = { onAcceptFromDetail(message.id) },
            onReject = { onRejectFromDetail(message.id) }
        )
    }

    if (state.showClearMonthConfirm) {
        ConfirmDialog(
            title = "Clear ${state.monthKey.label()}?",
            message = "Removes every message in this month, whatever its status. Any transaction " +
                "they're linked to is kept, just unlinked - its amount and category stay as they are.",
            onConfirm = onClearMonthConfirmed,
            onDismiss = onClearMonthDismissed,
            confirmLabel = "Clear"
        )
    }

    state.undoConfirm?.let { prompt ->
        ConfirmDialog(
            title = "Undo confirm?",
            message = prompt.message(),
            onConfirm = onUndoConfirmConfirmed,
            onDismiss = onUndoConfirmDismissed,
            confirmLabel = "Undo confirm"
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

private const val OPEN_ROW_TIMEOUT_MS = 5_000L

/** Green Confirm, red Reject, grey Undo - the same colours on the swipe buttons as on the badges. */
@Composable
private fun SwipeButton.toAction(onClick: () -> Unit): RevealAction = when (this) {
    SwipeButton.CONFIRM -> RevealAction(label, Icons.Default.Check, StatusColors.accepted, onClick)
    SwipeButton.REJECT -> RevealAction(label, Icons.Default.Close, StatusColors.overBudget, onClick)
    SwipeButton.UNDO -> RevealAction(label, Icons.AutoMirrored.Filled.Undo, MaterialTheme.colorScheme.outline, onClick)
}

/** An active filter, with a cross that clears just that one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemovableChip(label: String, onRemove: () -> Unit) {
    InputChip(
        selected = true,
        onClick = onRemove,
        label = { Text(label) },
        trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove $label filter", modifier = Modifier.size(16.dp)) }
    )
}

/** Day title on the left; on the right, how many are left (To review) or the day's money out. */
@Composable
private fun DayHeader(group: DayGroup, toReview: Boolean) {
    val right = when {
        toReview -> "${group.rows.size} to review"
        group.outPaise > 0 -> "${Money(group.outPaise).formatted()} out"
        else -> null
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            group.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        right?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun MessageFilter.label(): String = when (this) {
    MessageFilter.ALL -> "All"
    MessageFilter.NOT_ASSIGNED -> "To review"
    MessageFilter.ACCEPTED -> "Confirmed"
    MessageFilter.REJECTED -> "Rejected"
}
