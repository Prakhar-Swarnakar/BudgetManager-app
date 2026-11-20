package com.budgetmanager.app.feature.owedtoyou

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.designsystem.components.EmptyState
import com.budgetmanager.app.core.designsystem.components.FilterChipRow
import com.budgetmanager.app.core.designsystem.components.NoSwipeAction
import com.budgetmanager.app.core.designsystem.components.SwipeAction
import com.budgetmanager.app.core.designsystem.components.SwipeRow
import com.budgetmanager.app.feature.owedtoyou.components.OwedToYouFiltersSheet
import com.budgetmanager.app.feature.owedtoyou.components.OwedToYouRow
import com.budgetmanager.app.ui.theme.StatusColors

private val viewModeTabs = listOf(OwedToYouViewMode.BY_TRANSACTION to "By transaction", OwedToYouViewMode.BY_FRIEND to "By friend")

@Composable
fun OwedToYouContent(
    state: OwedToYouUiState,
    onViewModeChanged: (OwedToYouViewMode) -> Unit,
    onStatusFilterChanged: (ReminderFilter) -> Unit,
    onFriendFilterChanged: (Long?) -> Unit,
    onFriendSectionToggled: (Long) -> Unit,
    onReminderToggled: (transactionId: Long, reminded: Boolean) -> Unit,
    onRowClick: (transactionId: Long) -> Unit,
    onFiltersClicked: () -> Unit,
    onFiltersDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Friend is the only dimension behind the sheet, so it alone decides the badge/empty-state
    // wording - same role state.directionFilter/categoryFilter/taxonomyFilter play together on
    // Messages, just with one dimension here instead of three.
    val filtersActive = state.friendFilter != null || state.statusFilter != ReminderFilter.ALL
    if (!state.isLoading && state.rows.isEmpty() && !filtersActive) {
        EmptyState(
            icon = Icons.Default.Groups,
            message = "No split transactions yet. Split a transaction with a friend from Add Transaction.",
            modifier = modifier
        )
        return
    }

    Column(modifier = modifier.fillMaxSize()) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Pending", style = MaterialTheme.typography.labelLarge)
                Text(state.totalPendingText, style = MaterialTheme.typography.headlineSmall)
            }
        }

        val selectedTabIndex = viewModeTabs.indexOfFirst { it.first == state.viewMode }.coerceAtLeast(0)
        TabRow(selectedTabIndex = selectedTabIndex) {
            viewModeTabs.forEach { (mode, label) ->
                Tab(
                    selected = state.viewMode == mode,
                    onClick = { onViewModeChanged(mode) },
                    text = { Text(label) }
                )
            }
        }

        // Status stays the one filter row always visible, same split as Messages - Friend lives
        // behind the Filters icon instead of a second permanent chip row. The badge reflects only
        // the sheet's own dimension (Friend), not Status, which is already visible on its own.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            BadgedBox(badge = { if (state.friendFilter != null) Badge() }) {
                IconButton(onClick = onFiltersClicked) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filters")
                }
            }
            FilterChipRow(
                items = state.statusFilterOptions,
                selected = state.statusFilter,
                onSelect = onStatusFilterChanged,
                modifier = Modifier.weight(1f)
            )
        }

        when (state.viewMode) {
            OwedToYouViewMode.BY_TRANSACTION -> if (state.rows.isEmpty()) {
                NoFilterMatches()
            } else {
                ByTransactionList(
                    rows = state.rows,
                    onReminderToggled = onReminderToggled,
                    onRowClick = onRowClick
                )
            }
            OwedToYouViewMode.BY_FRIEND -> if (state.friendSections.isEmpty()) {
                NoFilterMatches()
            } else {
                ByFriendList(
                    sections = state.friendSections,
                    expandedFriendIds = state.expandedFriendIds,
                    onFriendSectionToggled = onFriendSectionToggled,
                    onReminderToggled = onReminderToggled,
                    onRowClick = onRowClick
                )
            }
        }
    }

    if (state.showFiltersSheet) {
        OwedToYouFiltersSheet(
            friendFilter = state.friendFilter,
            friendFilterOptions = state.friendFilterOptions,
            onFriendSelected = onFriendFilterChanged,
            onDismiss = onFiltersDismissed
        )
    }
}

/** Shown in place of a list when the chosen filters match nothing - without it the page just
 *  ended after the chips, with no sign of why. The whole-page [EmptyState] above deliberately
 *  only covers "no split transactions exist at all". */
@Composable
private fun NoFilterMatches() {
    EmptyState(
        icon = Icons.Default.Groups,
        message = "Nothing here for these filters."
    )
}

@Composable
private fun ByTransactionList(
    rows: List<OwedToYouRowUi>,
    onReminderToggled: (Long, Boolean) -> Unit,
    onRowClick: (Long) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(rows, key = { it.transactionId }) { row ->
            SwipeableOwedRow(row = row, onReminderToggled = onReminderToggled, onClick = { onRowClick(row.transactionId) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun ByFriendList(
    sections: List<OwedToYouFriendSectionUi>,
    expandedFriendIds: Set<Long>,
    onFriendSectionToggled: (Long) -> Unit,
    onReminderToggled: (Long, Boolean) -> Unit,
    onRowClick: (Long) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        sections.forEach { section ->
            val expanded = section.friendId in expandedFriendIds
            item(key = "header-${section.friendId}") {
                FriendSectionHeader(
                    section = section,
                    expanded = expanded,
                    onClick = { onFriendSectionToggled(section.friendId) }
                )
                HorizontalDivider()
            }
            if (expanded) {
                items(section.rows, key = { "${section.friendId}-${it.transactionId}" }) { row ->
                    SwipeableOwedRow(row = row, onReminderToggled = onReminderToggled, onClick = { onRowClick(row.transactionId) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun FriendSectionHeader(section: OwedToYouFriendSectionUi, expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(section.friendName, style = MaterialTheme.typography.titleMedium)
            Text(
                "${section.rows.size} transaction${if (section.rows.size == 1) "" else "s"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(section.pendingText, style = MaterialTheme.typography.titleMedium)
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun SwipeableOwedRow(
    row: OwedToYouRowUi,
    onReminderToggled: (Long, Boolean) -> Unit,
    onClick: () -> Unit
) {
    SwipeRow(
        onSwipeStart = { if (!row.reminded) onReminderToggled(row.transactionId, true) },
        onSwipeEnd = { if (row.reminded) onReminderToggled(row.transactionId, false) },
        startAction = if (!row.reminded) {
            SwipeAction(Icons.Default.NotificationsActive, StatusColors.inProgress)
        } else {
            NoSwipeAction
        },
        endAction = if (row.reminded) {
            SwipeAction(Icons.Default.Schedule, MaterialTheme.colorScheme.outline)
        } else {
            NoSwipeAction
        }
    ) {
        OwedToYouRow(row = row, onClick = onClick)
    }
}
