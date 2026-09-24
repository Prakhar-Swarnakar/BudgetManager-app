package com.budgetmanager.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    @ColumnInfo(name = "source_message_id") val sourceMessageId: Long? = null
)
