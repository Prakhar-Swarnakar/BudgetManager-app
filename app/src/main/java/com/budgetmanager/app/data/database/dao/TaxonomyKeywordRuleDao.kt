package com.budgetmanager.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.budgetmanager.app.data.database.entity.TaxonomyKeywordRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaxonomyKeywordRuleDao {
    /** Every rule, for TaxonomySuggester's matching pass. */
    @Query("SELECT * FROM taxonomy_keyword_rule")
    suspend fun getAll(): List<TaxonomyKeywordRuleEntity>

    @Query("SELECT * FROM taxonomy_keyword_rule ORDER BY keyword ASC")
    fun observeAll(): Flow<List<TaxonomyKeywordRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<TaxonomyKeywordRuleEntity>)

    /** Replaces any existing rule for this keyword - the primary key makes this an upsert via
     *  REPLACE, same as KeywordRuleDao.upsert. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: TaxonomyKeywordRuleEntity)

    @Query("DELETE FROM taxonomy_keyword_rule WHERE keyword = :keyword")
    suspend fun delete(keyword: String)

    @Query("DELETE FROM taxonomy_keyword_rule")
    suspend fun deleteAll()
}
