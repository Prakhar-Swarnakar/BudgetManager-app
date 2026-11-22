package com.budgetmanager.app.feature.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.core.model.TaxonomyType
import com.budgetmanager.app.data.repository.CategoryRepository
import com.budgetmanager.app.data.repository.FriendRepository
import com.budgetmanager.app.data.repository.KeywordRuleRepository
import com.budgetmanager.app.data.repository.MessageRepository
import com.budgetmanager.app.data.repository.TaxonomyKeywordRuleRepository
import com.budgetmanager.app.data.repository.TransactionRepository
import com.budgetmanager.app.data.repository.TransactionSplitInput
import com.budgetmanager.app.data.repository.TransactionSplitRepository
import com.budgetmanager.app.domain.ComputeSplit
import com.budgetmanager.app.domain.EvaluateBudgetAlerts
import com.budgetmanager.app.domain.SplitFriendInput
import com.budgetmanager.app.domain.SplitResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val messageRepository: MessageRepository,
    private val evaluateBudgetAlerts: EvaluateBudgetAlerts,
    private val keywordRuleRepository: KeywordRuleRepository,
    private val taxonomyKeywordRuleRepository: TaxonomyKeywordRuleRepository,
    private val friendRepository: FriendRepository,
    private val transactionSplitRepository: TransactionSplitRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    private val internalState = MutableStateFlow(AddTransactionUiState())
    private var loadedFor: String? = null

    val uiState: StateFlow<AddTransactionUiState> = combine(
        internalState,
        categoryRepository.observeActive(),
        friendRepository.observeAll()
    ) { state, categories, friends ->
        state.copy(categories = categories, friendSuggestions = friends.map { it.name })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddTransactionUiState())

    /** Called once per distinct (messageId, transactionId) pair - safe to call again on
     *  recomposition, it no-ops if already loaded for these arguments. The prefill params only
     *  ever apply when both ids are null (a statement line with no linked message - see
     *  [com.budgetmanager.app.navigation.Destination.AddTransaction]'s own doc comment). */
    fun load(
        messageId: Long?,
        transactionId: Long?,
        prefillAmountPaise: Long? = null,
        prefillTitle: String? = null,
        prefillDateEpochDay: Long? = null,
        prefillBannerText: String? = null
    ) {
        val key = "$messageId:$transactionId:$prefillAmountPaise:$prefillDateEpochDay"
        if (loadedFor == key) return
        loadedFor = key

        viewModelScope.launch {
            when {
                transactionId != null -> {
                    val transaction = transactionRepository.getById(transactionId) ?: return@launch
                    // The linked message (if any) is still what the SMS banner and the keyword
                    // fields' seed come from, same as before. The Title field itself now prefers
                    // whatever was last saved on the transaction - so reopening an already-
                    // renamed transaction shows your title, not the original SMS merchant, while
                    // a transaction saved before this field existed still falls back to it.
                    val linkedMessage = transaction.sourceMessageId?.let { messageRepository.getById(it) }
                    val smsMerchant = linkedMessage?.merchant
                    val merchant = transaction.title?.takeIf { it.isNotBlank() } ?: smsMerchant.orEmpty()
                    val existingSplits = transactionSplitRepository.observeByTransaction(transactionId).first()
                    val friends = friendRepository.observeAll().first()
                    val splitEntries = existingSplits.map { split ->
                        val name = friends.firstOrNull { it.id == split.friendId }?.name.orEmpty()
                        SplitEntryUi(
                            name = name,
                            customAmountInput = Money.formatForInput(split.amountOwed.paise),
                            splitId = split.id,
                            settled = split.settled,
                            settledAt = split.settledAt
                        )
                    }
                    internalState.update {
                        recomputeSplit(
                            it.copy(
                                isLoading = false,
                                isEditMode = true,
                                transactionIdForEdit = transactionId,
                                amountInput = Money.formatForInput(transaction.amount.paise),
                                merchant = merchant,
                                smsMerchant = smsMerchant,
                                categoryKeyword = smsMerchant.orEmpty(),
                                taxonomyKeyword = smsMerchant.orEmpty(),
                                smsBannerText = linkedMessage?.body,
                                note = transaction.note.orEmpty(),
                                avoidable = transaction.avoidable,
                                date = transaction.occurredAt.atZone(ZoneId.systemDefault()).toLocalDate(),
                                selectedCategoryId = transaction.categoryId,
                                selectedTaxonomy = transaction.taxonomy,
                                isSplitExpanded = splitEntries.isNotEmpty(),
                                splitEntries = splitEntries
                            )
                        )
                    }
                }
                messageId != null -> {
                    val message = messageRepository.getById(messageId) ?: return@launch
                    internalState.update {
                        it.copy(
                            isLoading = false,
                            messageIdForAccept = messageId,
                            amountInput = message.parsedAmount?.let { amount -> Money.formatForInput(amount.paise) } ?: "",
                            merchant = message.merchant.orEmpty(),
                            smsMerchant = message.merchant,
                            categoryKeyword = message.merchant.orEmpty(),
                            taxonomyKeyword = message.merchant.orEmpty(),
                            // Always starts unchecked - the user opts in deliberately in every
                            // mode, rather than this defaulting on for message-linked saves.
                            addToRule = false,
                            date = message.receivedAt.atZone(ZoneId.systemDefault()).toLocalDate(),
                            selectedCategoryId = message.suggestedCategoryId,
                            suggestedCategoryId = message.suggestedCategoryId,
                            selectedTaxonomy = message.suggestedTaxonomy,
                            suggestedTaxonomy = message.suggestedTaxonomy,
                            smsBannerText = message.body
                        )
                    }
                }
                else -> internalState.update {
                    it.copy(
                        isLoading = false,
                        amountInput = prefillAmountPaise?.let { paise -> Money.formatForInput(paise) } ?: "",
                        merchant = prefillTitle.orEmpty(),
                        date = prefillDateEpochDay?.let { epochDay -> LocalDate.ofEpochDay(epochDay) } ?: it.date,
                        smsBannerText = prefillBannerText
                    )
                }
            }
        }
    }

    fun onAmountChanged(value: String) {
        internalState.update { recomputeSplit(it.copy(amountInput = value, amountError = null)) }
    }

    /** Title is purely a display label now - editing it never touches the rule checkboxes or
     *  keywords, which are seeded from [AddTransactionUiState.smsMerchant] instead. */
    fun onMerchantChanged(value: String) {
        internalState.update { it.copy(merchant = value) }
    }

    fun onAddToRuleToggled(checked: Boolean) {
        internalState.update { it.copy(addToRule = checked, categoryKeywordError = null) }
    }

    fun onCategoryKeywordChanged(value: String) {
        internalState.update { it.copy(categoryKeyword = value, categoryKeywordError = null) }
    }

    fun onTaxonomySelected(taxonomy: TaxonomyType) {
        internalState.update { it.copy(selectedTaxonomy = taxonomy) }
    }

    fun onAddTaxonomyToRuleToggled(checked: Boolean) {
        internalState.update { it.copy(addTaxonomyToRule = checked, taxonomyKeywordError = null) }
    }

    fun onTaxonomyKeywordChanged(value: String) {
        internalState.update { it.copy(taxonomyKeyword = value, taxonomyKeywordError = null) }
    }

    fun onAvoidableChanged(value: Boolean) {
        internalState.update { it.copy(avoidable = value) }
    }

    fun onNoteChanged(value: String) {
        internalState.update { it.copy(note = value) }
    }

    fun onDateChanged(value: LocalDate) {
        internalState.update { it.copy(date = value) }
    }

    fun onCategorySelected(id: Long) {
        internalState.update { it.copy(selectedCategoryId = id, categoryError = null) }
    }

    fun onSplitToggled(expanded: Boolean) {
        internalState.update { it.copy(isSplitExpanded = expanded) }
    }

    fun onAddSplitFriend(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        internalState.update { recomputeSplit(it.copy(splitEntries = it.splitEntries + SplitEntryUi(trimmed))) }
    }

    fun onRemoveSplitFriend(index: Int) {
        internalState.update {
            recomputeSplit(it.copy(splitEntries = it.splitEntries.filterIndexed { i, _ -> i != index }))
        }
    }

    /** Only ever called on an entry with a real [SplitEntryUi.splitId] - a friend added this
     *  session has no persisted split yet to settle (see [SplitEntryUi]'s doc comment). A fresh
     *  [Instant.now] is stamped only on the actual tap that turns this on; turning it back off
     *  clears it. If the chip is never tapped during this edit, the value loaded from the
     *  database flows through to save untouched. */
    fun onSplitFriendSettledToggled(index: Int, settled: Boolean) {
        internalState.update { state ->
            state.copy(
                splitEntries = state.splitEntries.mapIndexed { i, entry ->
                    if (i == index) entry.copy(settled = settled, settledAt = if (settled) Instant.now() else null) else entry
                }
            )
        }
    }

    fun onSplitFriendAmountChanged(index: Int, value: String) {
        internalState.update { state ->
            recomputeSplit(
                state.copy(
                    splitEntries = state.splitEntries.mapIndexed { i, entry ->
                        if (i == index) entry.copy(customAmountInput = value) else entry
                    }
                )
            )
        }
    }

    /** Recomputes [AddTransactionUiState.yourShare] and [AddTransactionUiState.splitError] from
     *  the current amount and split entries - called after every edit to either, so the "Your
     *  share" figure is always live rather than only updated on save. */
    private fun recomputeSplit(state: AddTransactionUiState): AddTransactionUiState {
        if (state.splitEntries.isEmpty()) {
            return state.copy(yourShare = null, splitError = null)
        }
        val amount = Money.parseRupeeInput(state.amountInput)
            ?: return state.copy(yourShare = null, splitError = "Enter a valid amount first")
        val result = buildSplitResult(amount, state.splitEntries)
        val error = if (result.yourShare.paise < 0) {
            "Your share can't be negative - reduce the split amounts"
        } else null
        return state.copy(yourShare = result.yourShare, splitError = error)
    }

    private fun buildSplitResult(amount: Money, entries: List<SplitEntryUi>): SplitResult {
        // A blank box already parses to null, which ComputeSplit already treats as "split this
        // one equally" - no separate "is this custom" flag needed to drive the same decision.
        val friends = entries.map { entry ->
            SplitFriendInput(entry.name, Money.parseRupeeInput(entry.customAmountInput))
        }
        return ComputeSplit.compute(amount, friends)
    }

    fun onSave() {
        val current = internalState.value
        // Guards against a double-tap firing saveFromMessage twice before the screen has a
        // chance to navigate away - the second insert would violate the one-transaction-per-
        // message unique constraint and crash (a real bug hit during testing, not hypothetical).
        if (current.isSaving) return

        val amount = Money.parseRupeeInput(current.amountInput)
        val categoryId = current.selectedCategoryId

        if (amount == null) {
            internalState.update { it.copy(amountError = "Enter a valid amount", saveAttempt = it.saveAttempt + 1) }
            return
        }
        if (categoryId == null) {
            internalState.update { it.copy(categoryError = "Choose a category", saveAttempt = it.saveAttempt + 1) }
            return
        }
        // Checked but nothing (or too little) to learn from - block rather than silently skip,
        // since checking the box was a deliberate choice.
        if (current.addToRule && current.categoryKeyword.trim().length < 3) {
            internalState.update {
                it.copy(categoryKeywordError = "Enter a keyword (at least 3 characters)", saveAttempt = it.saveAttempt + 1)
            }
            return
        }
        if (current.addTaxonomyToRule && current.taxonomyKeyword.trim().length < 3) {
            internalState.update {
                it.copy(taxonomyKeywordError = "Enter a keyword (at least 3 characters)", saveAttempt = it.saveAttempt + 1)
            }
            return
        }

        val splitResult = if (current.splitEntries.isNotEmpty()) buildSplitResult(amount, current.splitEntries) else null
        if (splitResult != null && splitResult.yourShare.paise < 0) {
            internalState.update {
                it.copy(splitError = "Your share can't be negative - reduce the split amounts", saveAttempt = it.saveAttempt + 1)
            }
            return
        }
        val owedTotal = splitResult?.owedTotal ?: Money(0)

        internalState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val zone = ZoneId.systemDefault()
            val occurredAt = current.date.atStartOfDay(zone).toInstant()
            val monthKey = MonthKey.from(occurredAt, zone)
            val note = current.note.ifBlank { null }
            // Saved as-is, blank treated as absent - same convention as note. This is what makes
            // a renamed Title actually stick, on Category detail and (once Accepted) on the
            // Messages row, without ever touching the linked message's own merchant.
            val title = current.merchant.trim().ifBlank { null }

            val transactionId = when {
                current.transactionIdForEdit != null -> {
                    val existing = transactionRepository.getById(current.transactionIdForEdit) ?: return@launch
                    transactionRepository.update(
                        existing.copy(
                            amount = amount,
                            occurredAt = occurredAt,
                            monthKey = monthKey,
                            categoryId = categoryId,
                            note = note,
                            taxonomy = current.selectedTaxonomy,
                            owedTotal = owedTotal,
                            title = title,
                            avoidable = current.avoidable
                        )
                    )
                    existing.id
                }
                current.messageIdForAccept != null -> {
                    val message = messageRepository.getById(current.messageIdForAccept) ?: return@launch
                    val id = transactionRepository.saveFromMessage(
                        message = message,
                        amount = amount,
                        occurredAt = occurredAt,
                        monthKey = monthKey,
                        categoryId = categoryId,
                        note = note,
                        taxonomy = current.selectedTaxonomy,
                        owedTotal = owedTotal,
                        title = title,
                        avoidable = current.avoidable
                    )
                    evaluateBudgetAlerts(monthKey, categoryId)
                    id
                }
                else -> {
                    val id = transactionRepository.insert(
                        amount = amount,
                        occurredAt = occurredAt,
                        monthKey = monthKey,
                        categoryId = categoryId,
                        note = note,
                        taxonomy = current.selectedTaxonomy,
                        owedTotal = owedTotal,
                        title = title,
                        avoidable = current.avoidable
                    )
                    evaluateBudgetAlerts(monthKey, categoryId)
                    id
                }
            }
            // smsMerchant == null means there's no real SMS behind this transaction (manual add,
            // or a linked message the parser found no merchant for) - the single enforcement
            // point for "a manual transaction can never add a rule," regardless of how the
            // checkbox state got set, not just the UI's disabled checkbox.
            if (current.smsMerchant != null) {
                if (current.addToRule) {
                    learnFromChoice(current.categoryKeyword, categoryId)
                }
                if (current.addTaxonomyToRule) {
                    learnTaxonomyFromChoice(current.taxonomyKeyword, current.selectedTaxonomy)
                }
            }
            // Edits always replace wholesale (even with an empty list, if every friend was
            // removed) so a removed friend's split never lingers; new transactions only bother
            // calling this when there's actually something to split. ComputeSplit returns one
            // result per input friend in the same order, so a plain index zip (rather than
            // matching by name) is enough to carry each entry's own settled status back into
            // what gets saved - the same "indices, not names" reasoning ComputeSplit itself
            // already uses internally.
            if (current.isEditMode || splitResult != null) {
                val splitInputs = splitResult?.friends.orEmpty().mapIndexed { index, friendResult ->
                    val entry = current.splitEntries[index]
                    TransactionSplitInput(
                        friendId = friendRepository.getOrCreate(friendResult.name),
                        amount = friendResult.amountOwed,
                        settled = entry.settled,
                        settledAt = entry.settledAt
                    )
                }
                transactionSplitRepository.replaceSplits(transactionId, splitInputs)
            }
            internalState.update { it.copy(saved = true, isSaving = false) }
        }
    }

    fun onSavedHandled() {
        internalState.update { it.copy(saved = false) }
    }

    /** Remembers keyword -> category so next time it shows up it's suggested automatically
     *  (06-backlog.md, "Learning from choices") - called from any save (manual, message-linked,
     *  or edit) when the user has "Add to rule" checked. [keyword] is
     *  [AddTransactionUiState.categoryKeyword], not the editable Title - already validated as
     *  >= 3 characters by [onSave] before this is ever called; the length check here is a
     *  harmless defensive backstop, not the real enforcement point. Always takes the category
     *  the user actually chose here, even overriding an existing rule for the same keyword -
     *  that's the point of learning from a correction. */
    private suspend fun learnFromChoice(keyword: String, categoryId: Long) {
        val trimmed = keyword.trim().lowercase()
        if (trimmed.length < 3) return
        keywordRuleRepository.upsert(trimmed, categoryId)
    }

    /** Same idea as [learnFromChoice], for the separate taxonomy rule set. */
    private suspend fun learnTaxonomyFromChoice(keyword: String, taxonomy: TaxonomyType?) {
        val trimmed = keyword.trim().lowercase()
        if (trimmed.length < 3 || taxonomy == null) return
        taxonomyKeywordRuleRepository.upsert(trimmed, taxonomy)
    }
}
