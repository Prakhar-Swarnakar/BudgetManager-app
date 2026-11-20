package com.budgetmanager.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.budgetmanager.app.core.model.TaxonomyType

/** The table is named "transactions" because "transaction" is a SQL keyword.
 *  [sourceMessageId] links back to the SMS that produced this row, when there is one. */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["month_key"]),
        Index(value = ["category_id"]),
        Index(value = ["source_message_id"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SmsMessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_message_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "amount_paise") val amountPaise: Long,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "month_key") val monthKey: String,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val note: String?,
    @ColumnInfo(name = "source_message_id") val sourceMessageId: Long? = null,
    val taxonomy: TaxonomyType? = null,
    /** Sum of every friend's share on this transaction (0 when it's never been split). Fixed at
     *  creation/edit time - never changes when a friend settles. Denormalized here so the
     *  spent-for-budget queries stay a single cheap SUM rather than a join against
     *  transaction_split. */
    @ColumnInfo(name = "owed_total_paise", defaultValue = "0") val owedTotalPaise: Long = 0,
    val title: String? = null,
    /** Owed to You's "I've nudged about this" marker - transaction-level, not per friend, since
     *  a single GPay/Splitwise request commonly covers the whole group expense at once. Never
     *  touches settled state or the budget-counted amount. */
    @ColumnInfo(defaultValue = "0") val reminded: Boolean = false
)
