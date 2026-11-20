package com.budgetmanager.app.feature.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.MonthKey
import com.budgetmanager.app.data.repository.CashRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** Cash on hand = every top-up minus every cash spend (a transaction paid with Cash, at its FULL
 *  amount - a split bill paid in cash leaves your hand in full). See 06-backlog.md, "Cash
 *  balance tracking". */
@HiltViewModel
class WalletViewModel @Inject constructor(
    private val cashRepository: CashRepository
) : ViewModel() {

    private val sheet = MutableStateFlow<AddCashSheetUiState?>(null)
    private val pendingDelete = MutableStateFlow<Long?>(null)
    private val zone = ZoneId.systemDefault()
    private val dateFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm")

    val uiState: StateFlow<WalletUiState> = combine(
        cashRepository.observeTopUps(),
        cashRepository.observeSpends(),
        sheet,
        pendingDelete
    ) { topUps, spends, sheetState, pendingDeleteId ->
        val balance = Money(topUps.sumOf { it.amount.paise } - spends.sumOf { it.amount.paise })
        val entries = (
            topUps.map {
                it.occurredAt to WalletEntryUi.TopUp(
                    id = it.id,
                    title = it.note?.takeIf { n -> n.isNotBlank() } ?: "Cash added",
                    dateText = it.occurredAt.atZone(zone).format(dateFormatter),
                    amountText = "+" + it.amount.formatted()
                )
            } + spends.map {
                it.occurredAt to WalletEntryUi.Spend(
                    transactionId = it.transactionId,
                    title = it.title,
                    dateText = it.occurredAt.atZone(zone).format(dateFormatter),
                    amountText = "-" + it.amount.formatted(),
                    categoryId = it.categoryId,
                    categoryEmoji = it.categoryEmoji,
                    categoryName = it.categoryName,
                    monthKey = MonthKey.from(it.occurredAt, zone)
                )
            }
            ).sortedByDescending { it.first }.map { it.second }
        WalletUiState(
            isLoading = false,
            balanceText = balance.formatted(),
            isNegative = balance.paise < 0,
            entries = entries,
            sheet = sheetState,
            pendingDeleteTopUpId = pendingDeleteId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WalletUiState())

    fun onAddClicked() {
        sheet.value = AddCashSheetUiState(date = LocalDate.now())
    }

    fun onSheetAmountChanged(value: String) {
        sheet.update { it?.copy(amountInput = value, amountError = null) }
    }

    fun onSheetDateChanged(date: LocalDate) {
        sheet.update { it?.copy(date = date) }
    }

    fun onSheetNoteChanged(value: String) {
        sheet.update { it?.copy(note = value) }
    }

    fun onSheetDismissed() {
        sheet.value = null
    }

    fun onSheetSaved() {
        val current = sheet.value ?: return
        val amount = Money.parseRupeeInput(current.amountInput)
        if (amount == null) {
            sheet.value = current.copy(amountError = "Enter an amount greater than 0")
            return
        }
        viewModelScope.launch {
            // Date has no time component in the picker - keep the time of day for ordering within
            // a day: today's top-up gets "now", a past date gets noon.
            val occurredAt: Instant =
                if (current.date == LocalDate.now()) Instant.now()
                else current.date.atTime(12, 0).atZone(zone).toInstant()
            cashRepository.addTopUp(amount, occurredAt, current.note.trim().ifBlank { null })
            sheet.value = null
        }
    }

    fun onDeleteTopUpRequested(id: Long) {
        pendingDelete.value = id
    }

    fun onDeleteTopUpCancelled() {
        pendingDelete.value = null
    }

    fun onDeleteTopUpConfirmed() {
        val id = pendingDelete.value ?: return
        pendingDelete.value = null
        viewModelScope.launch { cashRepository.deleteTopUp(id) }
    }
}
