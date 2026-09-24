package com.budgetmanager.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.budgetmanager.app.core.model.MessageStatus

/** An SMS that looked like a spend. Only debit-looking messages become a row; OTPs are never stored. */
@Entity(
    tableName = "sms_message",
    indices = [
        Index(value = ["dedupe_key"], unique = true),
        Index(value = ["status"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["suggested_category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class SmsMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val body: String,
    @ColumnInfo(name = "received_at") val receivedAt: Long,
    @ColumnInfo(name = "sms_provider_id") val smsProviderId: String?,
    @ColumnInfo(name = "dedupe_key") val dedupeKey: String,
    @ColumnInfo(name = "parsed_amount_paise") val parsedAmountPaise: Long?,
    val merchant: String?,
    @ColumnInfo(name = "suggested_category_id") val suggestedCategoryId: Long?,
    val status: MessageStatus = MessageStatus.NOT_ASSIGNED,
    @ColumnInfo(name = "is_new") val isNew: Boolean = true
)
