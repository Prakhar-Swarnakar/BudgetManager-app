package com.budgetmanager.app.feature.messages.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.common.toDayTimeText
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.SmsMessage
import com.budgetmanager.app.ui.theme.StatusColors

/** The full SMS text, with Accept and Reject buttons on a message that's still Not assigned,
 *  per 04-messages-and-notifications.md. [duplicateOf] is set when DetectPossibleDuplicates
 *  thinks this message might be the same real payment as another one still Not assigned - shown
 *  so the user can check before accepting both. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailSheet(
    message: SmsMessage,
    duplicateOf: SmsMessage? = null,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(message.sender, fontWeight = FontWeight.Bold)
            Text(message.body, modifier = Modifier.padding(top = 8.dp))

            duplicateOf?.let { other ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusColors.warning.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusColors.warning)
                            Text(
                                "Might be the same payment as another message",
                                color = StatusColors.warning,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        Text(
                            // The amount segment is dropped rather than printed as "null" when
                            // the other message never stated one inline.
                            listOfNotNull(
                                other.sender,
                                other.parsedAmount?.formatted(),
                                other.receivedAt.toDayTimeText()
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Only Not assigned can still move either way - the ViewModel treats Accept and
            // Reject on an already-decided message as no-ops, so offering live-looking buttons
            // here just promised an action that would silently do nothing. Reverting an
            // Accepted or Rejected message is the swipe on its row, as the reveal strip shows.
            if (message.status == MessageStatus.NOT_ASSIGNED) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) { Text("Reject") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onAccept, modifier = Modifier.weight(1f)) { Text("Accept") }
                }
            } else {
                Text(
                    when (message.status) {
                        MessageStatus.ACCEPTED -> "Already accepted. Swipe the row to undo."
                        else -> "Rejected. Swipe the row to put it back."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}
