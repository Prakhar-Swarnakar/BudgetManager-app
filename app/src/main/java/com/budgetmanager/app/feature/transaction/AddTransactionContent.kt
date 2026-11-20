package com.budgetmanager.app.feature.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.budgetmanager.app.core.designsystem.components.AmountField
import com.budgetmanager.app.core.designsystem.components.CategoryDropdown
import com.budgetmanager.app.core.designsystem.components.RuleLearningSection
import com.budgetmanager.app.core.designsystem.components.TaxonomyDropdown
import com.budgetmanager.app.core.model.TaxonomyType
import com.budgetmanager.app.feature.transaction.components.DateEditLabel
import com.budgetmanager.app.feature.transaction.components.DateField
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.OutlinedTextField

@Composable
fun AddTransactionContent(
    state: AddTransactionUiState,
    onAmountChanged: (String) -> Unit,
    onMerchantChanged: (String) -> Unit,
    onAddToRuleToggled: (Boolean) -> Unit,
    onCategoryKeywordChanged: (String) -> Unit,
    onTaxonomySelected: (TaxonomyType) -> Unit,
    onAddTaxonomyToRuleToggled: (Boolean) -> Unit,
    onTaxonomyKeywordChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onDateChanged: (java.time.LocalDate) -> Unit,
    onCategorySelected: (Long) -> Unit,
    onSplitToggled: (Boolean) -> Unit,
    onAddSplitFriend: (String) -> Unit,
    onRemoveSplitFriend: (Int) -> Unit,
    onSplitFriendSettledToggled: (Int, Boolean) -> Unit,
    onSplitFriendAmountChanged: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Tapping the top-bar Save button (AppNavigation.kt) with an invalid field scrolls down to
    // put that field in view, since Save now lives far from the fields it validates - each
    // requester covers the one section a given error belongs to. Only one of these errors is
    // ever freshly set by a single onSave() attempt (it returns at the first failure), so there's
    // no ambiguity about which section to scroll to. Keyed on state.saveAttempt rather than the
    // error values themselves: splitError is already live-recomputed as the user types
    // (recomputeSplit), so a second failed Save tap often sets the exact same error string, which
    // wouldn't change a LaunchedEffect keyed on that value.
    val amountBringIntoView = remember { BringIntoViewRequester() }
    val categoryBringIntoView = remember { BringIntoViewRequester() }
    val taxonomyBringIntoView = remember { BringIntoViewRequester() }
    val splitBringIntoView = remember { BringIntoViewRequester() }

    LaunchedEffect(state.saveAttempt) {
        if (state.saveAttempt == 0) return@LaunchedEffect
        when {
            state.amountError != null -> amountBringIntoView.bringIntoView()
            state.categoryError != null || state.categoryKeywordError != null -> categoryBringIntoView.bringIntoView()
            state.taxonomyKeywordError != null -> taxonomyBringIntoView.bringIntoView()
            state.splitError != null -> splitBringIntoView.bringIntoView()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        state.smsBannerText?.let { text ->
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Text(
                    text,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            DateEditLabel(date = state.date, onDateChanged = onDateChanged)
            Spacer(Modifier.height(16.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = state.merchant,
                onValueChange = onMerchantChanged,
                label = { Text("Title") },
                modifier = Modifier.weight(1.4f)
            )
            AmountField(
                value = state.amountInput,
                onValueChange = onAmountChanged,
                isError = state.amountError != null,
                supportingText = state.amountError,
                modifier = Modifier.weight(1f).bringIntoViewRequester(amountBringIntoView)
            )
        }

        Spacer(Modifier.height(20.dp))
        RuleLearningSection(
            label = "Category",
            checkboxLabel = "Add to rule: always use this category for this keyword",
            addToRule = state.addToRule,
            onAddToRuleToggled = onAddToRuleToggled,
            checkboxEnabled = state.smsMerchant != null,
            keyword = state.categoryKeyword,
            onKeywordChanged = onCategoryKeywordChanged,
            keywordError = state.categoryKeywordError,
            modifier = Modifier.fillMaxWidth().bringIntoViewRequester(categoryBringIntoView)
        ) {
            CategoryDropdown(
                categories = state.categories,
                selectedCategoryId = state.selectedCategoryId,
                suggestedCategoryId = state.suggestedCategoryId,
                isError = state.categoryError != null,
                onSelect = onCategorySelected,
                modifier = Modifier.fillMaxWidth()
            )
            state.categoryError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
            }
        }

        Spacer(Modifier.height(20.dp))
        RuleLearningSection(
            label = "Payment method",
            checkboxLabel = "Add to rule: always use this payment method for this keyword",
            addToRule = state.addTaxonomyToRule,
            onAddToRuleToggled = onAddTaxonomyToRuleToggled,
            checkboxEnabled = state.smsMerchant != null,
            keyword = state.taxonomyKeyword,
            onKeywordChanged = onTaxonomyKeywordChanged,
            keywordError = state.taxonomyKeywordError,
            modifier = Modifier.fillMaxWidth().bringIntoViewRequester(taxonomyBringIntoView)
        ) {
            TaxonomyDropdown(
                selectedTaxonomy = state.selectedTaxonomy,
                suggestedTaxonomy = state.suggestedTaxonomy,
                onSelect = onTaxonomySelected,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(20.dp))
        SplitWithFriendsSection(
            state = state,
            onSplitToggled = onSplitToggled,
            onAddSplitFriend = onAddSplitFriend,
            onRemoveSplitFriend = onRemoveSplitFriend,
            onSplitFriendSettledToggled = onSplitFriendSettledToggled,
            onSplitFriendAmountChanged = onSplitFriendAmountChanged,
            modifier = Modifier.bringIntoViewRequester(splitBringIntoView)
        )

        // No SMS banner to put DateEditLabel under (a manually-added transaction) - keep the
        // boxed date field here, unchanged, same as before.
        if (state.smsBannerText == null) {
            Spacer(Modifier.height(20.dp))
            DateField(date = state.date, onDateChanged = onDateChanged)
        }

        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = state.note,
            onValueChange = onNoteChanged,
            label = { Text("Note (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun SplitWithFriendsSection(
    state: AddTransactionUiState,
    onSplitToggled: (Boolean) -> Unit,
    onAddSplitFriend: (String) -> Unit,
    onRemoveSplitFriend: (Int) -> Unit,
    onSplitFriendSettledToggled: (Int, Boolean) -> Unit,
    onSplitFriendAmountChanged: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var newFriendInput by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Split with friends", style = MaterialTheme.typography.labelLarge)
            Switch(checked = state.isSplitExpanded, onCheckedChange = onSplitToggled)
        }

        if (!state.isSplitExpanded) return@Column

        Spacer(Modifier.height(8.dp))

        state.splitEntries.forEachIndexed { index, entry ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                IconButton(onClick = { onRemoveSplitFriend(index) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove ${entry.name}")
                }
                Text(entry.name, modifier = Modifier.weight(1f))
                OutlinedTextField(
                    value = entry.customAmountInput,
                    onValueChange = { onSplitFriendAmountChanged(index, it) },
                    label = { Text("Amount") },
                    placeholder = { Text("Equal") },
                    modifier = Modifier.widthIn(min = 100.dp)
                )
                // Only a friend whose split already exists in the database can be marked settled -
                // one added this session has nothing persisted yet to settle (see SplitEntryUi).
                if (entry.splitId != null) {
                    FilterChip(
                        selected = entry.settled,
                        onClick = { onSplitFriendSettledToggled(index, !entry.settled) },
                        label = { Text(if (entry.settled) "Settled" else "Pending") },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = newFriendInput,
                onValueChange = { newFriendInput = it },
                label = { Text("Friend's name") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    onAddSplitFriend(newFriendInput)
                    newFriendInput = ""
                },
                enabled = newFriendInput.isNotBlank(),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Add")
            }
        }

        val existingNames = state.splitEntries.map { it.name }
        val suggestions = state.friendSuggestions.filter { suggestion ->
            suggestion.isNotBlank() &&
                suggestion !in existingNames &&
                (newFriendInput.isBlank() || suggestion.contains(newFriendInput, ignoreCase = true))
        }
        if (suggestions.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                suggestions.take(5).forEach { name ->
                    AssistChip(onClick = { onAddSplitFriend(name) }, label = { Text(name) })
                }
            }
        }

        state.yourShare?.let { share ->
            Spacer(Modifier.height(8.dp))
            Text("Your share: ${share.formatted()}", style = MaterialTheme.typography.bodyMedium)
        }
        state.splitError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
