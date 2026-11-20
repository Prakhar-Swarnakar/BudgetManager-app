package com.budgetmanager.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Cash coming into your hand (an ATM withdrawal, a cash gift) - the "in" side of the Wallet
 *  page. A cash spend is not stored here: it is an ordinary transaction whose taxonomy is Cash,
 *  so the balance is always derived (top-ups minus those spends), never a stored number. */
@Entity(tableName = "cash_top_up")
data class CashTopUpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "amount_paise") val amountPaise: Long,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    val note: String?
)
