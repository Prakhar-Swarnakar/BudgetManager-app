package com.budgetmanager.app.feature.categorydetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.common.toDayTimeText
import com.budgetmanager.app.core.designsystem.components.NoSwipeAction
import com.budgetmanager.app.core.designsystem.components.SwipeAction
import com.budgetmanager.app.core.designsystem.components.SwipeRow
import com.budgetmanager.app.feature.categorydetail.TransactionRowUi
import com.budgetmanager.app.ui.theme.StatusColors

/** Swipe left to delete, with an Undo bar (03-user-flows.md, flow 12). Tap to edit. */
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
                    row.occurredAt.toDayTimeText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(row.amountText, style = MaterialTheme.typography.bodyLarge)
                    // The original full amount, struck through, right after "your share" - only
                    // set once this transaction is actually split with friends.
                    row.splitTotalText?.let { total ->
                        Text(
                            total,
                            style = MaterialTheme.typography.bodySmall,
                            textDecoration = TextDecoration.LineThrough,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                // Where this transaction came from, as plain label text - it used to be a
                // permanently disabled AssistChip, which reads to the user (and to a screen
                // reader) as a greyed-out control they can't use rather than a caption.
                Text(
                    if (row.fromSms) "SMS" else "Manual",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
