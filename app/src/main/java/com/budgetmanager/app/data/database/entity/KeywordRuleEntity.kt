package com.budgetmanager.app.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/** Maps a keyword (e.g. "swiggy") to a category, driving the suggestion when accepting a message. */
@Entity(
    tableName = "keyword_rule",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class KeywordRuleEntity(
    @PrimaryKey val keyword: String,
    @ColumnInfo(name = "category_id") val categoryId: Long
)
