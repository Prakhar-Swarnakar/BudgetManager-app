package com.budgetmanager.app.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Shared shape for CategoryRules/TaxonomyRules' group lists and edit sheet - the two screens
 *  group keyword rules by a different kind of key (a repository-backed category id vs. a fixed
 *  [com.budgetmanager.app.core.model.TaxonomyType] enum), but otherwise render the identical
 *  list/chip/sheet mechanics. The category-or-taxonomy-specific picker is injected as a trailing
 *  `@Composable` slot into [RuleEditSheet] rather than built in here, same reasoning as
 *  [RuleLearningSection]'s own picker slot: the two screens have a genuinely different data shape,
 *  only the surrounding behavior is shared. */
data class RuleGroupUi<K : Any>(val key: K, val label: String, val keywords: List<String>)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun <K : Any> RuleGroupsList(
    groups: List<RuleGroupUi<K>>,
    onChipClick: (keyword: String, key: K) -> Unit,
    onChipDelete: (keyword: String) -> Unit,
    onAddClick: (key: K) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(groups, key = { it.key }) { group ->
            RuleGroupSection(
                group = group,
                onChipClick = { keyword -> onChipClick(keyword, group.key) },
                onChipDelete = onChipDelete,
                onAddClick = { onAddClick(group.key) }
            )
            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun <K : Any> RuleGroupSection(
    group: RuleGroupUi<K>,
    onChipClick: (String) -> Unit,
    onChipDelete: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(group.label, style = MaterialTheme.typography.titleSmall)
            Text(
                wordCountLabel(group.keywords.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            group.keywords.forEach { keyword ->
                InputChip(
                    selected = false,
                    onClick = { onChipClick(keyword) },
                    label = { Text(keyword) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Delete \"$keyword\"",
                            modifier = Modifier
                                .size(InputChipDefaults.IconSize)
                                // Its own tap target, separate from the chip's onClick above -
                                // otherwise tapping the x would open the edit sheet instead of
                                // deleting.
                                .clickable(onClick = { onChipDelete(keyword) })
                        )
                    }
                )
            }
            AssistChip(onClick = onAddClick, label = { Text("+ add") })
        }
    }
}

private fun wordCountLabel(count: Int): String = when (count) {
    0 -> "No words"
    1 -> "1 word"
    else -> "$count words"
}

/** The add/edit bottom sheet shell shared by CategoryRules/TaxonomyRules - [picker] is the
 *  category-or-taxonomy-specific chip grid, injected as a trailing slot. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleEditSheet(
    isNew: Boolean,
    keyword: String,
    onKeywordChanged: (String) -> Unit,
    pickerLabel: String,
    saveEnabled: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    picker: @Composable () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(if (isNew) "New rule" else "Edit rule", style = MaterialTheme.typography.titleMedium)

            if (isNew) {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = onKeywordChanged,
                    label = { Text("Word to match") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            } else {
                Text(
                    "\"$keyword\"",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Text(
                pickerLabel,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            picker()

            Button(
                onClick = onSave,
                enabled = saveEnabled,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("Save")
            }
        }
    }
}
