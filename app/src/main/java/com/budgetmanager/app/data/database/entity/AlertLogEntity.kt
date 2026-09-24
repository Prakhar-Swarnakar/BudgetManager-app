package com.budgetmanager.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.budgetmanager.app.core.model.AlertType

/** One row per alert actually sent, so it is never sent twice for the same category/month/type. */
@Entity(
    tableName = "alert_log",
    indices = [Index(value = ["month_key", "category_id", "type"], unique = true)]
)
data class AlertLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "month_key") val monthKey: String,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val type: AlertType
)
