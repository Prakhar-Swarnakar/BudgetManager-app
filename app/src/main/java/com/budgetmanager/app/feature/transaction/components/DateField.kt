package com.budgetmanager.app.feature.transaction.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val displayFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

/** A read-only field that opens a date picker on tap. Wrapped in a Box because a read-only
 *  OutlinedTextField still consumes touches for text-cursor placement rather than firing a
 *  click, so a transparent clickable layer sits on top instead. Used only when there's no SMS
 *  banner for [DateEditLabel] to sit under (a manually-added transaction) - see
 *  AddTransactionContent.kt. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(date: LocalDate, onDateChanged: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    var showPicker by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = date.format(displayFormatter),
            onValueChange = {},
            readOnly = true,
            label = { Text("Date") },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showPicker = true }
        )
    }

    if (showPicker) {
        DatePickerDialogFor(date = date, onDateChanged = onDateChanged, onDismiss = { showPicker = false })
    }
}

/** A small tappable text line - "15 Nov 2026 · tap to change" - opening the same date picker as
 *  [DateField], for when a boxed input field would be overkill. Shown right under the SMS banner
 *  on Add Transaction, replacing [DateField] there. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateEditLabel(date: LocalDate, onDateChanged: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    var showPicker by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { showPicker = true }
    ) {
        Text("📅", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.width(4.dp))
        Text(
            "${date.format(displayFormatter)} · tap to change",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }

    if (showPicker) {
        DatePickerDialogFor(date = date, onDateChanged = onDateChanged, onDismiss = { showPicker = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogFor(date: LocalDate, onDateChanged: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { millis ->
                    onDateChanged(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = pickerState)
    }
}
