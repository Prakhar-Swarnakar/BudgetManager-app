package com.budgetmanager.app.feature.categorydetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.designsystem.components.NoSwipeAction
import com.budgetmanager.app.core.designsystem.components.SwipeAction
import com.budgetmanager.app.core.designsystem.components.SwipeRow
import com.budgetmanager.app.feature.categorydetail.TransactionRowUi
import com.budgetmanager.app.ui.theme.StatusColors
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("dd MMM, HH:mm")

/** Swipe left to delete, with an Undo bar (03-user-flows.md, flow 12). Tap to edit. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionRow(row: TransactionRowUi, onClick: () -> Unit, onDeleteSwiped: () -> Unit, modifier: Modifier = Modifier) {
    val deleteAction = SwipeAction(Icons.Default.Close, StatusColors.overBudget)

    SwipeRow(
        onSwipeStart = {},
        onSwipeEnd = onDeleteSwiped,
        startAction = NoSwipeAction,
        endAction = deleteAction,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(row.noteOrPlaceholder, style = MaterialTheme.typography.bodyLarge)
                Text(
                    row.occurredAt.atZone(ZoneId.systemDefault()).format(timeFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(row.amountText, style = MaterialTheme.typography.bodyLarge)
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(if (row.fromSms) "SMS" else "Manual", style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    }
}
