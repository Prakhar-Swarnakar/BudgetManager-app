package com.budgetmanager.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.budgetmanager.app.data.database.entity.KeywordRuleEntity

@Dao
interface KeywordRuleDao {
    @Query("SELECT * FROM keyword_rule")
    suspend fun getAll(): List<KeywordRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<KeywordRuleEntity>)
}
