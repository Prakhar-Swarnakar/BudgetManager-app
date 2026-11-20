package com.budgetmanager.app.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A labelled selection (Category, Payment method, ...) plus the "Add to rule" checkbox and its
 * keyword field - the shape every rule-learning section on Add Transaction shares. The actual
 * selector (CategoryDropdown, TaxonomyDropdown, ...) is passed in as [dropdown] rather than built
 * in here, since Category and Taxonomy each have a genuinely different data shape (a
 * repository-backed list with real ids vs. a fixed enum) - only the surrounding rule-learning
 * behavior is shared.
 *
 * [keyword] is only shown while [addToRule] is checked, and is never cleared by toggling
 * [addToRule] off - unchecking just hides it, so re-checking shows whatever was already there.
 * The checkbox sits beside the dropdown rather than below it; [checkboxLabel] (what it actually
 * does) shows underneath as a caption, so the control stays compact without losing the
 * explanation.
 */
@Composable
fun RuleLearningSection(
    label: String,
    checkboxLabel: String,
    addToRule: Boolean,
    onAddToRuleToggled: (Boolean) -> Unit,
    checkboxEnabled: Boolean,
    keyword: String,
    onKeywordChanged: (String) -> Unit,
    keywordError: String?,
    modifier: Modifier = Modifier,
    dropdown: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) { dropdown() }
            Checkbox(checked = addToRule, onCheckedChange = onAddToRuleToggled, enabled = checkboxEnabled)
        }
        Text(
            checkboxLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
        if (addToRule) {
            OutlinedTextField(
                value = keyword,
                onValueChange = onKeywordChanged,
                label = { Text("Keyword") },
                isError = keywordError != null,
                supportingText = keywordError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        }
    }
}
