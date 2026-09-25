package com.budgetmanager.app.core.designsystem.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.budgetmanager.app.core.model.MonthKey
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Previous/next arrows around a month label, e.g. "September 2026". Used on Monthly budget and
 * later on Home. [canGoNext] lets a caller disable moving into the future - Home does that,
 * Monthly budget doesn't, since setting next month's budget ahead of time is normal.
 */
@Composable
fun MonthSelector(
    monthKey: MonthKey,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    canGoNext: Boolean = true
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
        }
        Text(
            monthKey.label(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
        }
    }
}

private fun MonthKey.label(): String {
    val yearMonth = YearMonth.of(year, month)
    val monthName = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    return "$monthName ${yearMonth.year}"
}
