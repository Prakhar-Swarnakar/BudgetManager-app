package com.budgetmanager.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.budgetmanager.app.core.model.TaxonomyType

/** Maps a keyword (e.g. "gpay") to a payment-method taxonomy value - a separate table from
 *  keyword_rule, so the same word can map independently to a category and a taxonomy value.
 *  No foreign key: taxonomy is a fixed enum, not a user-created row to reference. */
@Entity(tableName = "taxonomy_keyword_rule")
data class TaxonomyKeywordRuleEntity(
    @PrimaryKey val keyword: String,
    val taxonomy: TaxonomyType
)
