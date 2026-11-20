package com.budgetmanager.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.budgetmanager.app.data.database.entity.FriendEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {
    @Query("SELECT * FROM friend ORDER BY name ASC")
    fun observeAll(): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friend WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getByName(name: String): FriendEntity?

    @Query("SELECT * FROM friend WHERE id = :id")
    suspend fun getById(id: Long): FriendEntity?

    @Insert
    suspend fun insert(friend: FriendEntity): Long
}
