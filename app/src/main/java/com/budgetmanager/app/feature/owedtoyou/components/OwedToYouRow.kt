package com.budgetmanager.app.feature.owedtoyou.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.common.toDayTimeText
import com.budgetmanager.app.core.designsystem.components.ActivityRow
import com.budgetmanager.app.feature.owedtoyou.OwedToYouRowUi
import com.budgetmanager.app.ui.theme.StatusColors

/** [ActivityRow] built for Owed to You - see its own doc comment for what's shared with
 *  [com.budgetmanager.app.feature.messages.components.MessageRow]. Tap opens the underlying
 *  transaction for editing; swiping (wrapped externally, same pattern as MessageRow) steps
 *  between Pending and Reminded. */
@Composable
fun OwedToYouRow(row: OwedToYouRowUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (statusIcon, statusLabel, statusColor) = if (row.reminded) {
        Triple(Icons.Default.NotificationsActive, "Reminded", StatusColors.inProgress)
    } else {
        Triple(Icons.Default.Schedule, "Pending", MaterialTheme.colorScheme.onSurfaceVariant)
    }

    ActivityRow(
        title = { Text(row.title) },
        subtitle = row.occurredAt.toDayTimeText(),
        tags = row.friendNames,
        statusIcon = statusIcon,
        statusLabel = statusLabel,
        statusColor = statusColor,
        amount = { AmountColumn(row) },
        categoryEmoji = row.categoryEmoji,
        categoryName = row.categoryName,
        taxonomyLabel = row.taxonomyLabel,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun AmountColumn(row: OwedToYouRowUi) {
    Row {
        Text(row.amountText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        // The combined total across every friend on the split, struck through, right after this
        // friend's own share - same idiom as MessageRow/TransactionRow's "your share" + struck
        // full amount. Only set (by the ViewModel) in By friend view on an actual multi-friend
        // split.
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
}
