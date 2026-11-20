package com.budgetmanager.app.core.designsystem.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.ui.theme.StatusColors

/** Status shown as an icon plus a label, so colour is never the only signal. */
@Composable
fun StatusBadge(status: MessageStatus, modifier: Modifier = Modifier) {
    val (icon, label, color) = when (status) {
        MessageStatus.NOT_ASSIGNED ->
            Triple(Icons.Default.Schedule, "Not assigned", MaterialTheme.colorScheme.onSurfaceVariant)
        MessageStatus.ACCEPTED -> Triple(Icons.Default.Check, "Accepted", StatusColors.accepted)
        MessageStatus.REJECTED -> Triple(Icons.Default.Close, "Rejected", StatusColors.overBudget)
    }
    StatusBadge(icon, label, color, modifier)
}

/** The general form every status badge in the app is built from - an icon, a label, and the
 *  colour both share, so a new status (e.g. Owed to You's Pending/Reminded) gets the same
 *  icon+colour treatment as [MessageStatus] without needing its own enum here. */
@Composable
fun StatusBadge(icon: ImageVector, label: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Icon(icon, contentDescription = null, tint = color)
        CompositionLocalProvider(LocalContentColor provides color) {
            Text(label, modifier = Modifier.padding(start = 4.dp))
        }
    }
}
