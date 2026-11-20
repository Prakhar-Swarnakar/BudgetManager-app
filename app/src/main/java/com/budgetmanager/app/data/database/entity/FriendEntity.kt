package com.budgetmanager.app.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "friend", indices = [Index(value = ["name"], unique = true)])
data class FriendEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
