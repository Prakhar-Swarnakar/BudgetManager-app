package com.budgetmanager.app.feature.messages.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.designsystem.components.StatusBadge
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.feature.messages.MessageRowUi
import com.budgetmanager.app.ui.theme.StatusColors
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("dd MMM, HH:mm")

@Composable
fun MessageRow(row: MessageRowUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val backgroundColor = when (row.status) {
        MessageStatus.NOT_ASSIGNED -> MaterialTheme.colorScheme.surface
        MessageStatus.ACCEPTED -> StatusColors.accepted.copy(alpha = 0.12f)
        MessageStatus.REJECTED -> StatusColors.overBudget.copy(alpha = 0.12f)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        if (row.isNew) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp, end = 8.dp)
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(row.merchantOrBody, fontWeight = if (row.isNew) FontWeight.Bold else FontWeight.Normal)
            Text(
                subtitleFor(row),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            StatusBadge(row.status, modifier = Modifier.padding(top = 4.dp))
            if (row.isPossibleDuplicate) {
                Row(modifier = Modifier.padding(top = 4.dp)) {
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
        }
        Column(horizontalAlignment = Alignment.End) {
            row.amountText?.let { amount ->
                Text(amount, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            // The ViewModel already decides what this means per status - the transaction's real
            // category once Accepted, a keyword suggestion while still Not assigned, or nothing
            // for Rejected - so the row just shows whatever it's given, the same way either time.
            if (row.categoryEmoji != null) {
                Text(
                    "${row.categoryEmoji} ${row.categoryName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

private fun subtitleFor(row: MessageRowUi): String {
    val time = row.receivedAt.atZone(ZoneId.systemDefault()).format(timeFormatter)
    return if (row.paymentMethod != null) "${row.paymentMethod} · $time" else time
}
