package com.budgetmanager.app.feature.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.designsystem.components.EmptyState
import com.budgetmanager.app.feature.categories.components.CategoryList
import com.budgetmanager.app.feature.categories.components.CategorySheet

@Composable
fun CategoriesContent(
    state: CategoriesUiState,
    onRowClick: (Long) -> Unit,
    onArchive: (Long) -> Unit,
    onUnarchive: (Long) -> Unit,
    onReorder: (List<Long>) -> Unit,
    onArchivedSectionToggled: () -> Unit,
    onSheetNameChanged: (String) -> Unit,
    onSheetEmojiChanged: (String) -> Unit,
    onSheetSaved: () -> Unit,
    onSheetDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (state.activeCategories.isEmpty() && state.archivedCategories.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Category,
                message = "No categories yet. Tap + to add one."
            )
        } else {
            CategoryList(
                categories = state.activeCategories,
                onRowClick = onRowClick,
                onArchive = onArchive,
                onReorder = onReorder
            )

            if (state.archivedCategories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onArchivedSectionToggled)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Archived (${state.archivedCategories.size})",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (state.archivedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }

                if (state.archivedExpanded) {
                    state.archivedCategories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUnarchive(category.id) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(category.emoji, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                category.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 12.dp).weight(1f)
                            )
                            Icon(Icons.Default.Unarchive, contentDescription = "Restore")
                        }
                    }
                }
            }
        }
    }

    state.sheet?.let { sheet ->
        CategorySheet(
            state = sheet,
            onNameChanged = onSheetNameChanged,
            onEmojiChanged = onSheetEmojiChanged,
            onSave = onSheetSaved,
            onDismiss = onSheetDismissed
        )
    }
}
