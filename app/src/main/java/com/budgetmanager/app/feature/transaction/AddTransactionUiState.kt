package com.budgetmanager.app.feature.transaction

import com.budgetmanager.app.core.model.Category
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.TaxonomyType
import java.time.Instant
import java.time.LocalDate

/** One friend in the split step. A blank [customAmountInput] means "split this one equally" -
 *  shown as an "Equal" placeholder rather than sitting empty, computed live from
 *  [ComputeSplit][com.budgetmanager.app.domain.ComputeSplit], not typed in; typing a real amount
 *  makes it custom. [splitId] is this friend's existing database row once editing an
 *  already-saved split - null for a friend added this session, before anything's been saved.
 *  [settled]/[settledAt] mirror that row's current settled status (only ever shown/editable when
 *  [splitId] is non-null, since there's nothing persisted yet to settle otherwise) and are carried
 *  forward into [com.budgetmanager.app.data.repository.TransactionSplitInput] on save, which is
 *  what makes settled state survive an edit despite the underlying wholesale replace. */
data class SplitEntryUi(
    val name: String,
    val customAmountInput: String = "",
    val splitId: Long? = null,
    val settled: Boolean = false,
    val settledAt: Instant? = null
)

data class AddTransactionUiState(
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false,
    val amountInput: String = "",
    val amountError: String? = null,
    val merchant: String = "",
    /** The merchant as the SMS itself parsed it, captured once at load time - null whenever
     *  there's no real SMS behind this transaction (manual add, or editing a transaction with no
     *  linked message, or a linked message whose parser found no merchant). Unlike [merchant]
     *  (now labelled "Title" in the UI and freely renameable for the user's own readability),
     *  this value never changes after load - it's what [categoryKeyword]/[taxonomyKeyword] are
     *  seeded from, so a rule still learns from what the SMS actually said even if Title is
     *  edited into something unrelated. Also what gates whether the "Add to rule" checkboxes are
     *  enabled at all - a manual transaction has nothing to learn from, so they're disabled. */
    val smsMerchant: String? = null,
    /** Whether saving should also upsert ([categoryKeyword], category) as a keyword rule.
     *  Disabled in the UI whenever [smsMerchant] is null. */
    val addToRule: Boolean = false,
    /** The keyword that will actually be learned for Category if [addToRule] is checked -
     *  seeded from [smsMerchant] once at load, then independently editable; preserved across
     *  unchecking/rechecking [addToRule], only ever shown while it's checked. */
    val categoryKeyword: String = "",
    val categoryKeywordError: String? = null,
    val note: String = "",
    val date: LocalDate = LocalDate.now(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val categoryError: String? = null,
    val suggestedCategoryId: Long? = null,
    val selectedTaxonomy: TaxonomyType? = null,
    val suggestedTaxonomy: TaxonomyType? = null,
    /** Mirrors [addToRule] for the separate taxonomy rule set - independent, since you might
     *  want to remember a merchant's category but not its taxonomy, or vice versa. */
    val addTaxonomyToRule: Boolean = false,
    /** Mirrors [categoryKeyword] for the separate taxonomy rule set. */
    val taxonomyKeyword: String = "",
    val taxonomyKeywordError: String? = null,
    /** The original SMS text, shown as a banner - only set when opened from a message. */
    val smsBannerText: String? = null,
    val messageIdForAccept: Long? = null,
    val transactionIdForEdit: Long? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    /** Whether the "Split with friends" section is expanded - collapsed by default, since most
     *  transactions aren't split. */
    val isSplitExpanded: Boolean = false,
    val splitEntries: List<SplitEntryUi> = emptyList(),
    /** Existing friend names, for autocomplete while typing a new entry. */
    val friendSuggestions: List<String> = emptyList(),
    /** Null when there's nothing to split (no entries yet) - the UI only shows "Your share"
     *  once there's at least one friend. */
    val yourShare: Money? = null,
    val splitError: String? = null,
    /** Bumped on every failed [onSave][AddTransactionViewModel.onSave] attempt - the trigger for
     *  scrolling to the first invalid field (AddTransactionContent.kt). A plain "is some error
     *  non-null" check isn't enough: splitError, unlike the others, is already live-recomputed as
     *  the user types (see recomputeSplit), so tapping Save again often re-sets the exact same
     *  error string, which wouldn't change a LaunchedEffect keyed on the error value itself. */
    val saveAttempt: Int = 0
)
