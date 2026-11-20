package com.budgetmanager.app.feature.messages.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.common.toDayTimeText
import com.budgetmanager.app.core.designsystem.components.ActivityRow
import com.budgetmanager.app.core.model.MessageDirection
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.feature.messages.MessageRowUi
import com.budgetmanager.app.ui.theme.StatusColors

@Composable
fun MessageRow(row: MessageRowUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val backgroundColor = when (row.status) {
        MessageStatus.NOT_ASSIGNED -> MaterialTheme.colorScheme.surface
        MessageStatus.ACCEPTED -> StatusColors.accepted.copy(alpha = 0.12f)
        MessageStatus.REJECTED -> StatusColors.overBudget.copy(alpha = 0.12f)
    }
    val (statusIcon, statusLabel, statusColor) = when (row.status) {
        MessageStatus.NOT_ASSIGNED -> Triple(Icons.Default.Schedule, "Not assigned", MaterialTheme.colorScheme.onSurfaceVariant)
        MessageStatus.ACCEPTED -> Triple(Icons.Default.Check, "Accepted", StatusColors.accepted)
        MessageStatus.REJECTED -> Triple(Icons.Default.Close, "Rejected", StatusColors.overBudget)
    }

    ActivityRow(
        title = {
            Text(row.merchantOrBody, fontWeight = if (row.isNew) FontWeight.Bold else FontWeight.Normal)
        },
        subtitle = subtitleFor(row),
        statusIcon = statusIcon,
        statusLabel = statusLabel,
        statusColor = statusColor,
        amount = { AmountColumn(row) },
        categoryEmoji = row.categoryEmoji,
        categoryName = row.categoryName,
        taxonomyLabel = row.taxonomyLabel,
        onClick = onClick,
        modifier = modifier,
        backgroundColor = backgroundColor,
        leadingIndicator = if (row.isNew) {
            { Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape)) }
        } else {
            null
        },
        belowStatus = if (row.isPossibleDuplicate) {
            {
                Row {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = StatusColors.warning,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "Possible duplicate",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusColors.warning,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        } else {
            null
        }
    )
}

@Composable
private fun AmountColumn(row: MessageRowUi) {
    Row {
        if (row.amountText != null) {
            Text(row.amountText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        } else {
            // The inclusion gate now keeps some messages that never state an amount inline
            // (06-backlog.md / real gap found 2026-10-07) - shown explicitly rather than
            // rendering nothing, so it reads as a known gap, not a bug.
            Text(
                "Amount unknown",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // The original full amount, struck through, right after "your share" - only set when
        // the linked transaction is actually split with friends.
        row.splitTotalText?.let { total ->
            Text(
                total,
                style = MaterialTheme.typography.bodySmall,
                textDecoration = TextDecoration.LineThrough,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        // Same idiom as TrendsContent.kt's ComparisonRow - a plain Text arrow glyph tinted by
        // StatusColors, not a separate Icon.
        val (arrow, arrowColor) = if (row.direction == MessageDirection.CREDIT) {
            "↑" to StatusColors.accepted
        } else {
            "↓" to StatusColors.overBudget
        }
        Text(
            arrow,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = arrowColor,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

private fun subtitleFor(row: MessageRowUi): String {
    val time = row.receivedAt.toDayTimeText()
    return if (row.paymentMethod != null) "${row.paymentMethod} · $time" else time
}
