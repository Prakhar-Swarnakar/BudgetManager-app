package com.budgetmanager.app.feature.messages.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetmanager.app.core.common.toTimeText
import com.budgetmanager.app.core.model.MessageDirection
import com.budgetmanager.app.core.model.MessageStatus
import com.budgetmanager.app.feature.messages.MessageRowUi
import com.budgetmanager.app.ui.theme.StatusColors

/**
 * One message: an avatar (category emoji, with a check or cross badge once decided), the title and
 * "method · time", a meta line, and the signed amount. The row sits under a day header, so it shows
 * only the time. Its background stays opaque because the swipe strip is drawn behind it.
 */
@Composable
fun MessageRow(row: MessageRowUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Avatar(row)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (row.isNew) {
                    Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    row.merchantOrBody,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (row.isNew) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                subtitleFor(row),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            CategoryLine(row)
            TagLine(row)
            if (row.isPossibleDuplicate) {
                // Amber for every row that has a possible duplicate, whatever has been decided
                // about it - the detail sheet says whether it is still a risk. Icon plus words, so
                // it is not carried by colour alone, and it wraps rather than cutting the text.
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = StatusColors.warning,
                        modifier = Modifier.padding(top = 1.dp).size(14.dp)
                    )
                    Text(
                        "Possible duplicate of ${row.duplicateOfLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = StatusColors.warning,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
        AmountColumn(row)
    }
}

/** Emoji in a circle; a Confirmed or Rejected message gets a badge at the bottom right. The badge
 *  has its own icon and a spoken label, so the status is never carried by colour alone. */
@Composable
internal fun Avatar(row: MessageRowUi) {
    Box(modifier = Modifier.size(40.dp)) {
        Box(
            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                row.categoryEmoji ?: row.merchantOrBody.firstOrNull()?.uppercase() ?: "?",
                fontSize = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val badge = when (row.status) {
            MessageStatus.ACCEPTED -> Triple(Icons.Default.Check, StatusColors.accepted, "Confirmed")
            MessageStatus.REJECTED -> Triple(Icons.Default.Close, StatusColors.overBudget, "Rejected")
            MessageStatus.NOT_ASSIGNED -> null
        }
        badge?.let { (icon, color, label) ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(18.dp)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .background(color, CircleShape)
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
    }
}

/** Category (a suggestion while still To review) and the payment method label. Wraps to a second
 *  line instead of cutting the text off. */
@Composable
private fun CategoryLine(row: MessageRowUi) {
    val text = listOfNotNull(
        row.categoryName?.let { if (row.status == MessageStatus.NOT_ASSIGNED) "Suggested: $it" else it },
        row.taxonomyLabel
    ).joinToString(" · ")
    if (text.isEmpty()) return
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

/** "Split with friends" and "Avoidable" on their own line, so a long category can never push them
 *  out of sight. Wraps if the font is large. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagLine(row: MessageRowUi) {
    val split = row.splitTotalText != null
    if (!split && !row.isAvoidable) return
    FlowRow(
        modifier = Modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (split) {
            Text(
                "Split with friends",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        if (row.isAvoidable) {
            Text(
                "Avoidable",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = StatusColors.warning,
                modifier = Modifier
                    .border(1.dp, StatusColors.warning, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
    }
}

/** "−₹486" for money out, "+₹12,000" in green for money in. A split spend shows your share with
 *  the full amount struck through underneath. */
@Composable
internal fun AmountColumn(row: MessageRowUi) {
    Column(horizontalAlignment = Alignment.End) {
        if (row.amountText != null) {
            val isCredit = row.direction == MessageDirection.CREDIT
            Text(
                (if (isCredit) "+" else "−") + row.amountText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = when {
                    row.status == MessageStatus.REJECTED -> MaterialTheme.colorScheme.onSurfaceVariant
                    isCredit -> StatusColors.accepted
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        } else {
            // Some bank SMS never state an amount inline (06-backlog.md, 2026-10-07) - said
            // plainly rather than rendering nothing, so it reads as a known gap, not a bug.
            Text(
                "No amount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        row.splitTotalText?.let { total ->
            Text(
                total,
                style = MaterialTheme.typography.bodySmall,
                textDecoration = TextDecoration.LineThrough,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun subtitleFor(row: MessageRowUi): String {
    val time = row.receivedAt.toTimeText()
    return if (row.paymentMethod != null) "${row.paymentMethod} · $time" else time
}
