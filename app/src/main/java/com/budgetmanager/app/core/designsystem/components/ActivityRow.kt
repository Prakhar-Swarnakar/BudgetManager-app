package com.budgetmanager.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * The row behind [com.budgetmanager.app.feature.owedtoyou.components.OwedToYouRow] (title, a
 * one-line subtitle, tags, a status badge, a trailing amount/category/taxonomy column). It was
 * shared with Messages until Messages got its own avatar-and-badge row layout, so the slots only
 * Messages used (new-row dot, duplicate warning, tinted background) are gone. Swiping is *not*
 * built in here - the caller wraps this in [SwipeRow].
 */
@Composable
fun ActivityRow(
    title: @Composable () -> Unit,
    subtitle: String,
    statusIcon: ImageVector,
    statusLabel: String,
    statusColor: Color,
    amount: @Composable () -> Unit,
    categoryEmoji: String?,
    categoryName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tags: List<String> = emptyList(),
    taxonomyLabel: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            title()
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (tags.isNotEmpty()) {
                TagRow(tags, modifier = Modifier.padding(top = 4.dp))
            }
            StatusBadge(statusIcon, statusLabel, statusColor, modifier = Modifier.padding(top = 4.dp))
        }
        Column(horizontalAlignment = Alignment.End) {
            amount()
            if (categoryEmoji != null && categoryName != null) {
                Text(
                    "$categoryEmoji $categoryName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (taxonomyLabel != null) {
                Text(
                    taxonomyLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

private const val MAX_VISIBLE_TAGS = 2

/** Up to [MAX_VISIBLE_TAGS] name pills, then a "+N" overflow pill rather than wrapping onto a
 *  second line - a row stays one height regardless of how many friends were on a transaction. */
@Composable
private fun TagRow(tags: List<String>, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        tags.take(MAX_VISIBLE_TAGS).forEachIndexed { index, tag ->
            TagPill(tag, modifier = if (index > 0) Modifier.padding(start = 4.dp) else Modifier)
        }
        val overflow = tags.size - MAX_VISIBLE_TAGS
        if (overflow > 0) {
            TagPill("+$overflow", modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun TagPill(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
