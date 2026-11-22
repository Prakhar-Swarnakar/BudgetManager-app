package com.budgetmanager.app.feature.messages.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.common.toTimeText
import com.budgetmanager.app.core.designsystem.components.StatusBadge
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.core.model.SmsMessage
import com.budgetmanager.app.feature.messages.MessageRowUi
import com.budgetmanager.app.feature.messages.displayName
import com.budgetmanager.app.feature.messages.duplicateNote
import com.budgetmanager.app.ui.theme.StatusColors

/** One half of the duplicate comparison: who sent it, when, the amount, and its status. */
@Composable
private fun DuplicatePreview(title: String, message: SmsMessage, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            listOfNotNull(message.receivedAt.toTimeText(), message.parsedAmount?.formatted()).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        StatusBadge(message.status, modifier = Modifier.padding(top = 4.dp))
    }
}

/** The full SMS text, with Confirm and Reject buttons on a message that's still To review,
 *  per 04-messages-and-notifications.md. [duplicateOf] is set when DetectPossibleDuplicates
 *  thinks this message might be the same real payment as another one still Not assigned - shown
 *  so the user can check before accepting both. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailSheet(
    message: SmsMessage,
    row: MessageRowUi? = null,
    duplicateOf: SmsMessage? = null,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onUndo: () -> Unit = {}
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (row != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(row)
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(row.merchantOrBody, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${message.sender} · ${message.status.displayName()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        row.splitTotalText?.let {
                            Text("Split with friends", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    AmountColumn(row)
                }
            } else {
                Text(message.sender, fontWeight = FontWeight.Bold)
            }
            Text(message.body, modifier = Modifier.padding(top = 8.dp))

            duplicateOf?.let { other ->
                val note = duplicateNote(message, other)
                val tint = if (note.warn) StatusColors.warning else MaterialTheme.colorScheme.onSurfaceVariant
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (note.warn) {
                            StatusColors.warning.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row {
                            Icon(
                                if (note.warn) Icons.Default.Warning else Icons.Default.Check,
                                contentDescription = null,
                                tint = tint
                            )
                            Text(
                                note.heading,
                                color = tint,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        note.body?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DuplicatePreview("This message", message, Modifier.weight(1f))
                            DuplicatePreview(other.sender, other, Modifier.weight(1f))
                        }
                    }
                }
            }

            // Only Not assigned can still move either way - the ViewModel treats Accept and
            // Reject on an already-decided message as no-ops, so offering live-looking buttons
            // here just promised an action that would silently do nothing. Reverting an
            // Accepted or Rejected message is the swipe on its row, as the reveal strip shows.
            if (message.status == MessageStatus.NOT_ASSIGNED) {
                // When the match is already Confirmed, say what each button really does.
                val matchConfirmed = duplicateOf?.status == MessageStatus.ACCEPTED
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) {
                        Text(if (matchConfirmed) "Reject as duplicate" else "Reject")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onAccept, modifier = Modifier.weight(1f)) {
                        Text(if (matchConfirmed) "Confirm anyway" else "Confirm")
                    }
                }
            } else {
                // Both undo paths go through the ViewModel's swipe handlers, so a Confirmed spend
                // still asks "Undo confirm?" before its transaction is deleted.
                OutlinedButton(onClick = onUndo, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Text(if (message.status == MessageStatus.ACCEPTED) "Undo confirm" else "Put back to To review")
                }
            }
        }
    }
}
