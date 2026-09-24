package com.budgetmanager.app.core.designsystem.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.model.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChip(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    suggested: Boolean = false
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text("${category.emoji} ${category.name}" + if (suggested) " · Suggested" else "") },
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChipGrid(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
    suggestedCategoryId: Long? = null
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            CategoryChip(
                category = category,
                selected = category.id == selectedCategoryId,
                suggested = category.id == suggestedCategoryId,
                onClick = { onSelect(category.id) }
            )
        }
    }
}
