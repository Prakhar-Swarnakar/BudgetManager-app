package com.budgetmanager.app.feature.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.core.common.EmojiValidation
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.data.repository.CategoryRepository
import com.budgetmanager.app.data.repository.MonthlyBudgetRepository
import com.budgetmanager.app.domain.CopyBudgetFromPreviousMonth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MonthlyBudgetViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val monthlyBudgetRepository: MonthlyBudgetRepository,
    private val copyBudgetFromPreviousMonth: CopyBudgetFromPreviousMonth
) : ViewModel() {

    private val monthKey = MutableStateFlow(MonthKey.current())
    private val copiedFromPreviousMonth = MutableStateFlow(false)
    private val sheet = MutableStateFlow<BudgetSheetUiState?>(null)
    private var copyCheckedFor: MonthKey? = null

    val uiState: StateFlow<MonthlyBudgetUiState> = monthKey
        .flatMapLatest { month ->
            checkCopyForward(month)
            combine(
                categoryRepository.observeActive(),
                monthlyBudgetRepository.observeForMonth(month),
                copiedFromPreviousMonth,
                sheet
            ) { categories, amounts, copied, sheetState ->
                val rows = categories.map { category ->
                    val amount = amounts[category.id]
                    BudgetRowUi(
                        categoryId = category.id,
                        emoji = category.emoji,
                        name = category.name,
                        amountText = (amount ?: Money.Zero).formatted(),
                        hasAmount = amount != null
                    )
                }
                val total = amounts.values.fold(Money.Zero) { acc, m -> acc + m }
                MonthlyBudgetUiState(
                    isLoading = false,
                    monthKey = month,
                    rows = rows,
                    totalText = total.formatted(),
                    copiedFromPreviousMonth = copied,
                    sheet = sheetState
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthlyBudgetUiState())

    /** Runs the copy-forward check once per distinct month, not on every recomposition/collector
     *  restart - flatMapLatest re-invokes its lambda on each new [month], so this guard stops a
     *  cheap-but-pointless repeat check from running every time a collector resubscribes. */
    private fun checkCopyForward(month: MonthKey) {
        if (copyCheckedFor == month) return
        copyCheckedFor = month
        viewModelScope.launch {
            copiedFromPreviousMonth.value = copyBudgetFromPreviousMonth(month)
        }
    }

    fun onPreviousMonth() {
        monthKey.value = monthKey.value.previous()
    }

    fun onNextMonth() {
        monthKey.value = monthKey.value.next()
    }

    /** Called from the top bar's + button (see AppNavigation, which shares this ViewModel
     *  instance with the screen since Monthly budget has no per-instance arguments). */
    fun onAddClicked() {
        sheet.value = BudgetSheetUiState(mode = BudgetSheetMode.New)
    }

    /** Opens the same sheet pre-filled, for renaming, changing the icon, or changing the
     *  amount - category management lives here, there is no separate Categories page. */
    fun onRowClicked(categoryId: Long) {
        viewModelScope.launch {
            val category = categoryRepository.getById(categoryId) ?: return@launch
            val amount = monthlyBudgetRepository.observeForMonth(monthKey.value).first()[categoryId]
            sheet.value = BudgetSheetUiState(
                mode = BudgetSheetMode.Edit(categoryId),
                name = category.name,
                emoji = category.emoji,
                amountInput = amount?.let { formatForInput(it.paise) } ?: ""
            )
        }
    }

    /** Called once, when a drag ends - not on every step, so a drag never round-trips through
     *  the repository's Flow mid-gesture. See BudgetCategoryList. */
    fun onReorder(orderedActiveIds: List<Long>) {
        viewModelScope.launch { categoryRepository.reorder(orderedActiveIds) }
    }

    fun onSheetDismissed() {
        sheet.value = null
    }

    fun onSheetNameChanged(value: String) {
        sheet.update { it?.copy(name = value, nameError = null) }
    }

    fun onSheetEmojiChanged(value: String) {
        sheet.update { it?.copy(emoji = value, emojiError = null) }
    }

    fun onSheetAmountChanged(value: String) {
        sheet.update { it?.copy(amountInput = value, amountError = null) }
    }

    fun onSheetSaved() {
        val current = sheet.value ?: return
        val name = current.name.trim()
        val emoji = current.emoji.trim()
        val amount = Money.parseRupeeInput(current.amountInput)

        if (name.isEmpty()) {
            sheet.update { it?.copy(nameError = "Enter a name") }
            return
        }
        if (!EmojiValidation.isSingleEmoji(emoji)) {
            sheet.update { it?.copy(emojiError = "Pick one emoji") }
            return
        }
        if (amount == null) {
            sheet.update { it?.copy(amountError = "Enter a valid amount") }
            return
        }

        when (val mode = current.mode) {
            is BudgetSheetMode.Edit -> viewModelScope.launch {
                categoryRepository.update(mode.categoryId, name, emoji)
                monthlyBudgetRepository.setAmount(monthKey.value, mode.categoryId, amount)
                sheet.value = null
            }
            BudgetSheetMode.New -> viewModelScope.launch {
                // Never edits an existing category's amount - + only ever creates a brand new
                // one, per 08-pages-and-navigation.md.
                val categoryId = categoryRepository.create(name, emoji)
                monthlyBudgetRepository.setAmount(monthKey.value, categoryId, amount)
                sheet.value = null
            }
        }
    }

    private fun formatForInput(paise: Long): String {
        val rupees = paise / 100
        val remainder = paise % 100
        return if (remainder == 0L) rupees.toString() else "%d.%02d".format(rupees, remainder)
    }
}
