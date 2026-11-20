package com.budgetmanager.app.feature.owedtoyou.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.designsystem.components.FilterChipItem
import com.budgetmanager.app.core.designsystem.components.FilterChipRow

/** Friend lives here, behind the Filters icon, rather than as a second permanent chip row next to
 *  Status - same split as Messages' own Filters sheet (status stays outside, everything else
 *  moves behind one entry point). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwedToYouFiltersSheet(
    friendFilter: Long?,
    friendFilterOptions: List<FilterChipItem<Long?>>,
    onFriendSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                "Filters",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                "Friend",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            FilterChipRow(
                items = friendFilterOptions,
                selected = friendFilter,
                onSelect = onFriendSelected
            )
        }
    }
}
