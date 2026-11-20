package com.budgetmanager.app.feature.messages.components

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
import com.budgetmanager.app.core.model.Category
import com.budgetmanager.app.core.model.MessageDirection
import com.budgetmanager.app.core.model.TaxonomyType
import com.budgetmanager.app.feature.messages.CategoryFilter
import com.budgetmanager.app.feature.messages.TaxonomyFilter

/** Every filter beyond status (06-backlog.md, "Richer Messages filters") lives here, behind one
 *  icon on the Messages page - status is the only filter row left outside, after the first cut's
 *  separate Direction row made the page feel too tall. Category and Taxonomy lists lead with
 *  "Any" (no restriction) and "None" (nothing assigned or suggested at all - useful for finding
 *  messages that still need a new keyword rule) before the real values. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageFiltersSheet(
    directionFilter: MessageDirection?,
    directionCounts: Map<MessageDirection?, Int>,
    categories: List<Category>,
    categoryFilter: CategoryFilter,
    categoryCounts: Map<CategoryFilter, Int>,
    taxonomyFilter: TaxonomyFilter,
    taxonomyCounts: Map<TaxonomyFilter, Int>,
    onDirectionSelected: (MessageDirection?) -> Unit,
    onCategorySelected: (CategoryFilter) -> Unit,
    onTaxonomySelected: (TaxonomyFilter) -> Unit,
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
                "Direction",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            FilterChipRow(
                items = directionFilterItems(directionCounts),
                selected = directionFilter,
                onSelect = onDirectionSelected
            )
            Text(
                "Category",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            FilterChipRow(
                items = categoryFilterItems(categories, categoryCounts),
                selected = categoryFilter,
                onSelect = onCategorySelected
            )
            Text(
                "Payment method",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            FilterChipRow(
                items = taxonomyFilterItems(taxonomyCounts),
                selected = taxonomyFilter,
                onSelect = onTaxonomySelected
            )
        }
    }
}

private fun directionFilterItems(counts: Map<MessageDirection?, Int>): List<FilterChipItem<MessageDirection?>> =
    listOf(
        FilterChipItem(null, "All", counts[null] ?: 0),
        FilterChipItem(MessageDirection.DEBIT, "Outgoing", counts[MessageDirection.DEBIT] ?: 0),
        FilterChipItem(MessageDirection.CREDIT, "Incoming", counts[MessageDirection.CREDIT] ?: 0)
    )

private fun categoryFilterItems(
    categories: List<Category>,
    counts: Map<CategoryFilter, Int>
): List<FilterChipItem<CategoryFilter>> {
    val items = mutableListOf(
        FilterChipItem<CategoryFilter>(CategoryFilter.Any, "Any", counts[CategoryFilter.Any] ?: 0),
        FilterChipItem<CategoryFilter>(CategoryFilter.None, "None", counts[CategoryFilter.None] ?: 0)
    )
    categories.forEach { category ->
        val value = CategoryFilter.Specific(category.id)
        items += FilterChipItem(value, "${category.emoji} ${category.name}", counts[value] ?: 0)
    }
    return items
}

private fun taxonomyFilterItems(counts: Map<TaxonomyFilter, Int>): List<FilterChipItem<TaxonomyFilter>> {
    val items = mutableListOf(
        FilterChipItem<TaxonomyFilter>(TaxonomyFilter.Any, "Any", counts[TaxonomyFilter.Any] ?: 0),
        FilterChipItem<TaxonomyFilter>(TaxonomyFilter.None, "None", counts[TaxonomyFilter.None] ?: 0)
    )
    TaxonomyType.entries.forEach { taxonomy ->
        val value = TaxonomyFilter.Specific(taxonomy)
        items += FilterChipItem(value, taxonomy.label, counts[value] ?: 0)
    }
    return items
}
