package com.budgetmanager.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.budgetmanager.app.data.database.entity.CashTopUpEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashTopUpDao {
    @Query("SELECT * FROM cash_top_up ORDER BY occurred_at DESC, id DESC")
    fun observeAll(): Flow<List<CashTopUpEntity>>

    @Query("SELECT * FROM cash_top_up")
    suspend fun getAllOnce(): List<CashTopUpEntity>

    @Insert
    suspend fun insert(topUp: CashTopUpEntity): Long

    @Insert
    suspend fun insertAll(topUps: List<CashTopUpEntity>)

    @Query("DELETE FROM cash_top_up WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM cash_top_up")
    suspend fun deleteAll()
}
