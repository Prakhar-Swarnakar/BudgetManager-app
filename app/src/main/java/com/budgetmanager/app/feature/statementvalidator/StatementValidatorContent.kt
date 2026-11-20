package com.budgetmanager.app.feature.statementvalidator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.ui.theme.StatusColors

@Composable
fun StatementValidatorContent(
    state: StatementValidatorUiState,
    onPickFile: () -> Unit,
    onAddRequested: (StatementLineUi) -> Unit,
    onReset: () -> Unit,
    onDismissError: () -> Unit,
    onPasswordSubmitted: (String) -> Unit,
    onPasswordPromptCancelled: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (val step = state.step) {
        StatementValidatorStep.PickFile -> PickFileSection(
            isProcessing = state.isProcessing,
            onPickFile = onPickFile,
            modifier = modifier
        )
        is StatementValidatorStep.Report -> ReportSection(
            report = step,
            onAddRequested = onAddRequested,
            onReset = onReset,
            modifier = modifier
        )
    }

    state.passwordPrompt?.let { prompt ->
        PasswordPromptDialog(
            prompt = prompt,
            isProcessing = state.isProcessing,
            onSubmit = onPasswordSubmitted,
            onCancel = onPasswordPromptCancelled
        )
    }

    state.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text("Couldn't read that file") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = onDismissError) { Text("OK") } }
        )
    }
}

@Composable
private fun PickFileSection(isProcessing: Boolean, onPickFile: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "A second opinion on what the SMS pipeline caught - not a second way to add " +
                "transactions. Nothing here is saved automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "CSV, Excel (.xls/.xlsx), or PDF. Export a statement from your bank or card issuer's " +
                "website or app, then pick it below - it's read entirely on your phone, never " +
                "uploaded anywhere. A password-protected PDF will ask for its password next.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Button(
            onClick = onPickFile,
            enabled = !isProcessing,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Icon(Icons.Default.UploadFile, contentDescription = null)
            Text(if (isProcessing) "Reading..." else "Choose file", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun PasswordPromptDialog(
    prompt: PasswordPrompt,
    isProcessing: Boolean,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit
) {
    var password by remember(prompt.fileName) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(Icons.Default.Lock, contentDescription = null) },
        title = { Text("Password needed") },
        text = {
            Column {
                Text(
                    "\"${prompt.fileName}\" is password-protected. Enter its password to continue - " +
                        "it's only used to open the file on your phone, never stored or sent anywhere.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    isError = prompt.error != null,
                    supportingText = prompt.error?.let { { Text(it) } },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    enabled = !isProcessing,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(password) }, enabled = !isProcessing && password.isNotEmpty()) {
                Text(if (isProcessing) "Checking..." else "Unlock")
            }
        },
        dismissButton = { TextButton(onClick = onCancel, enabled = !isProcessing) { Text("Cancel") } }
    )
}

@Composable
private fun ReportSection(
    report: StatementValidatorStep.Report,
    onAddRequested: (StatementLineUi) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Resolved once here rather than read inline as a statementGroup() argument below - a
    // @Composable property read (MaterialTheme.colorScheme.*) can't be passed directly as an
    // argument to a plain (non-@Composable) function call.
    val neutralColor = MaterialTheme.colorScheme.onSurfaceVariant

    LazyColumn(modifier = modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text(report.fileName, style = MaterialTheme.typography.titleMedium)
            Text(
                report.dateRangeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )
            SummaryCard(report)
            if (report.skippedRowCount > 0) {
                Text(
                    "${report.skippedRowCount} row(s) in the file couldn't be read and were skipped.",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusColors.warning,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        statementGroup(report.needsReview, "Needs review", StatusColors.inProgress) { row ->
            InfoRow(row.description, row.dateLabel, row.amountText)
        }
        statementGroup(report.missing, "Missing from our data", StatusColors.overBudget) { line ->
            MissingRow(line = line, onAddClick = { onAddRequested(line) })
        }
        statementGroup(report.rejectedButOnStatement, "Rejected, but on statement", StatusColors.warning) { row ->
            InfoRow(row.description, row.dateLabel, row.amountText)
        }
        statementGroup(report.amountMismatches, "Amount differs", StatusColors.warning) { mismatch ->
            MismatchRow(mismatch)
        }
        statementGroup(
            report.possibleFalsePositives,
            "In our data, not on statement",
            neutralColor
        ) { row ->
            InfoRow(row.description, row.dateLabel, row.amountText)
        }

        item {
            OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Text("Validate another file")
            }
        }
    }
}

/** A bucket is only shown when it actually has rows - an empty "Needs review" section, say, is
 *  just noise. [color] can't be read until composition, so this stays a plain function taking an
 *  already-resolved color rather than a @Composable default. */
private fun <T> androidx.compose.foundation.lazy.LazyListScope.statementGroup(
    rows: List<T>,
    title: String,
    color: androidx.compose.ui.graphics.Color,
    row: @Composable (T) -> Unit
) {
    if (rows.isEmpty()) return
    item { SectionHeader(title, color, rows.size) }
    items(rows) { row(it) }
}

@Composable
private fun SummaryCard(report: StatementValidatorStep.Report) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${report.coveragePercent}%",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (report.coveragePercent >= 90) StatusColors.accepted else StatusColors.warning
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text("caught", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${report.matchedCount} of ${report.lineCount} statement lines matched",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            SummaryRow("Statement total", report.statementTotalText)
            SummaryRow("In-app total", report.inAppTotalText)
            SummaryRow("Difference", report.differenceText)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SectionHeader(title: String, color: androidx.compose.ui.graphics.Color, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = color)
        Text("$count", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoRow(description: String, dateLabel: String, amountText: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(description, style = MaterialTheme.typography.bodyMedium)
                Text(dateLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(amountText, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun MissingRow(line: StatementLineUi, onAddClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(line.description, style = MaterialTheme.typography.bodyMedium)
                Text(line.dateLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(line.amountText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 12.dp))
            TextButton(onClick = onAddClick) { Text("Add") }
        }
    }
}

@Composable
private fun MismatchRow(mismatch: StatementMismatchUi) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(mismatch.description, style = MaterialTheme.typography.bodyMedium)
            Text(mismatch.dateLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Statement: ${mismatch.statementAmountText}", style = MaterialTheme.typography.bodySmall)
                Text("Ours: ${mismatch.ourAmountText}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
