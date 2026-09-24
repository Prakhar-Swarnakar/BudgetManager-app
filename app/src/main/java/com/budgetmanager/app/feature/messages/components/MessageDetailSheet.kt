package com.budgetmanager.app.feature.messages.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.model.SmsMessage

/** The full SMS text with Accept and Reject buttons, per 04-messages-and-notifications.md. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailSheet(
    message: SmsMessage,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(message.sender, fontWeight = FontWeight.Bold)
            Text(message.body, modifier = Modifier.padding(top = 8.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) { Text("Reject") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onAccept, modifier = Modifier.weight(1f)) { Text("Accept") }
            }
        }
    }
}
