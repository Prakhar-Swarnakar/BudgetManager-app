package com.budgetmanager.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One friend's share of one transaction. Deleted and recreated wholesale on edit (see
 *  TransactionSplitRepository.replaceSplits) rather than diffed in place - simplest correct
 *  behavior for v1. [settled]/[settledAt] still survive an edit despite the wholesale replace:
 *  Add Transaction's edit screen carries a still-present friend's current settled status forward
 *  explicitly as part of what it writes back, rather than this layer trying to preserve it on
 *  its own. */
@Entity(
    tableName = "transaction_split",
    indices = [
        Index(value = ["transaction_id"]),
        Index(value = ["friend_id"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FriendEntity::class,
            parentColumns = ["id"],
            childColumns = ["friend_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TransactionSplitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "transaction_id") val transactionId: Long,
    @ColumnInfo(name = "friend_id") val friendId: Long,
    @ColumnInfo(name = "amount_owed_paise") val amountOwedPaise: Long,
    val settled: Boolean = false,
    @ColumnInfo(name = "settled_at") val settledAt: Long? = null
)
