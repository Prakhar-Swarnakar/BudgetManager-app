package com.budgetmanager.app.core.designsystem.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.budgetmanager.app.core.model.TaxonomyType

/** Same shape as CategoryDropdown, for the separate taxonomy (payment method) field - a fixed
 *  list rather than a repository-backed one, so there's no suggested-category-style id lookup,
 *  just the enum itself. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxonomyDropdown(
    selectedTaxonomy: TaxonomyType?,
    onSelect: (TaxonomyType) -> Unit,
    modifier: Modifier = Modifier,
    suggestedTaxonomy: TaxonomyType? = null
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedTaxonomy?.label ?: "",
            onValueChange = {},
            readOnly = true,
            placeholder = { Text("Select a payment method") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TaxonomyType.entries.forEach { taxonomy ->
                DropdownMenuItem(
                    text = {
                        Text(taxonomy.label + if (taxonomy == suggestedTaxonomy) " · Suggested" else "")
                    },
                    onClick = {
                        onSelect(taxonomy)
                        expanded = false
                    }
                )
            }
        }
    }
}
