package com.budgetmanager.app.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class FilterChipItem<T>(val value: T, val label: String, val count: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FilterChipRow(
    items: List<FilterChipItem<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            FilterChip(
                selected = item.value == selected,
                onClick = { onSelect(item.value) },
                label = { Text("${item.label} (${item.count})") }
            )
        }
    }
}
