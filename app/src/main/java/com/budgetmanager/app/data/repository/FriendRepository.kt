package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.Friend
import kotlinx.coroutines.flow.Flow

interface FriendRepository {
    fun observeAll(): Flow<List<Friend>>

    /** Case-insensitive: returns the existing friend's id if [name] already matches one,
     *  otherwise creates a new friend and returns its id - the "type a new name into a split"
     *  flow never needs its own separate create step. */
    suspend fun getOrCreate(name: String): Long
}
