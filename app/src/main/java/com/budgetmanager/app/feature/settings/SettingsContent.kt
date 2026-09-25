package com.budgetmanager.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.designsystem.components.ConfirmDialog
import java.text.SimpleDateFormat
import java.util.Locale

data class PermissionStatusUi(
    val smsGranted: Boolean,
    val notificationsGranted: Boolean,
    val batteryUnrestricted: Boolean
)

@Composable
fun SettingsContent(
    state: SettingsUiState,
    permissions: PermissionStatusUi,
    onFixSms: () -> Unit,
    onFixNotifications: () -> Unit,
    onFixBattery: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onNewSpendsAlertsToggled: (Boolean) -> Unit,
    onEightyPercentAlertsToggled: (Boolean) -> Unit,
    onOverBudgetAlertsToggled: (Boolean) -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onConfirmImport: () -> Unit,
    onCancelImport: () -> Unit,
    onDismissImportError: () -> Unit,
    onDismissImportSuccess: () -> Unit,
    onDismissExportError: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Permissions", style = MaterialTheme.typography.titleMedium)
        PermissionRow("SMS access", permissions.smsGranted, onFixSms)
        PermissionRow("Notifications", permissions.notificationsGranted, onFixNotifications)
        PermissionRow(
            "Battery: unrestricted",
            permissions.batteryUnrestricted,
            onFixBattery,
            hint = "Stops Android delaying spend notifications when the screen's been off a while"
        )
        TextButton(onClick = onOpenAppInfo, modifier = Modifier.padding(top = 4.dp)) {
            Text("Open App info")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("Alerts", style = MaterialTheme.typography.titleMedium)
        AlertSwitchRow(
            label = "New spends detected",
            checked = state.newSpendsAlertsEnabled,
            onCheckedChange = onNewSpendsAlertsToggled
        )
        AlertSwitchRow(
            label = "80% of a budget used",
            checked = state.eightyPercentAlertsEnabled,
            onCheckedChange = onEightyPercentAlertsToggled
        )
        AlertSwitchRow(
            label = "Over budget",
            checked = state.overBudgetAlertsEnabled,
            onCheckedChange = onOverBudgetAlertsToggled
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("Backup", style = MaterialTheme.typography.titleMedium)
        Text(
            state.lastExportAtMillis?.let { "Last export: ${formatExportTimestamp(it)}" }
                ?: "Never exported",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Row(modifier = Modifier.padding(top = 8.dp)) {
            OutlinedButton(onClick = onExportClick) { Text("Export backup") }
            OutlinedButton(onClick = onImportClick, modifier = Modifier.padding(start = 8.dp)) {
                Text("Import backup")
            }
        }
    }

    state.pendingImport?.let { pending ->
        ConfirmDialog(
            title = "Import backup?",
            message = "This replaces everything currently in the app - ${pending.categoryCount} " +
                "categories, ${pending.transactionCount} transactions, ${pending.smsMessageCount} " +
                "messages - with the backup's data. This cannot be undone.",
            confirmLabel = "Import",
            onConfirm = onConfirmImport,
            onDismiss = onCancelImport
        )
    }

    state.importError?.let { message ->
        InfoDialog(title = "Import failed", message = message, onDismiss = onDismissImportError)
    }

    state.importSuccessMessage?.let { message ->
        InfoDialog(title = "Import successful", message = message, onDismiss = onDismissImportSuccess)
    }

    state.exportError?.let { message ->
        InfoDialog(title = "Export failed", message = message, onDismiss = onDismissExportError)
    }
}

@Composable
private fun InfoDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}

private fun formatExportTimestamp(millis: Long): String =
    SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(millis)

@Composable
private fun PermissionRow(label: String, granted: Boolean, onFix: () -> Unit, hint: String? = null) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (granted) "Granted" else "Not granted",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (granted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
            if (!granted) {
                OutlinedButton(onClick = onFix) { Text("Fix") }
            }
        }
        hint?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AlertSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
