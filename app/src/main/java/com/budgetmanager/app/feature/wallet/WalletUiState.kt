package com.budgetmanager.app.feature.wallet

import com.budgetmanager.app.core.model.MonthKey
import java.time.LocalDate

/** One line of the Wallet ledger - a top-up (cash in) or a cash spend, newest first. */
sealed interface WalletEntryUi {
    val key: String

    data class TopUp(
        val id: Long,
        val title: String,
        val dateText: String,
        val amountText: String
    ) : WalletEntryUi {
        override val key get() = "topup-$id"
    }

    data class Spend(
        val transactionId: Long,
        val title: String,
        val dateText: String,
        val amountText: String,
        val categoryId: Long,
        val categoryEmoji: String,
        val categoryName: String,
        val monthKey: MonthKey
    ) : WalletEntryUi {
        override val key get() = "spend-$transactionId"
    }
}

data class AddCashSheetUiState(
    val amountInput: String = "",
    val amountError: String? = null,
    val date: LocalDate = LocalDate.now(),
    val note: String = ""
)

data class WalletUiState(
    val isLoading: Boolean = true,
    /** Top-ups minus cash spends - derived on every emission, never stored anywhere. Carries its
     *  own "-" when negative (Money.formatted()). */
    val balanceText: String = "₹0",
    /** A negative balance means a top-up wasn't logged, or something got tagged Cash by mistake -
     *  physical cash can't actually go below zero - so the UI treats it as a warning. */
    val isNegative: Boolean = false,
    val entries: List<WalletEntryUi> = emptyList(),
    val sheet: AddCashSheetUiState? = null,
    /** Set while the "delete this top-up?" confirmation is showing. */
    val pendingDeleteTopUpId: Long? = null
)
