package com.budgetmanager.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.budgetmanager.app.data.database.entity.KeywordRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KeywordRuleDao {
    @Query("SELECT * FROM keyword_rule")
    suspend fun getAll(): List<KeywordRuleEntity>

    @Query("SELECT * FROM keyword_rule ORDER BY keyword ASC")
    fun observeAll(): Flow<List<KeywordRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<KeywordRuleEntity>)

    /** Replaces any existing rule for this keyword - the primary key makes this an upsert,
     *  both for learning from a user's choice and for the editable-rules screen. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: KeywordRuleEntity)

    @Query("DELETE FROM keyword_rule WHERE keyword = :keyword")
    suspend fun delete(keyword: String)

    @Query("DELETE FROM keyword_rule")
    suspend fun deleteAll()
}
