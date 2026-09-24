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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
                row.receivedAt.atZone(ZoneId.systemDefault()).format(timeFormatter),
                style = MaterialTheme.typography.bodySmall
            )
            StatusBadge(row.status, modifier = Modifier.padding(top = 4.dp))
        }
        row.amountText?.let { amount ->
            Text(amount, fontWeight = FontWeight.Bold)
        }
    }
}
