package com.budgetmanager.app.data.repository

import com.budgetmanager.app.core.model.Friend
import com.budgetmanager.app.data.database.dao.FriendDao
import com.budgetmanager.app.data.database.entity.FriendEntity
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomFriendRepository @Inject constructor(
    private val friendDao: FriendDao
) : FriendRepository {

    override fun observeAll() =
        friendDao.observeAll().map { entities -> entities.map { Friend(it.id, it.name) } }

    override suspend fun getOrCreate(name: String): Long {
        val trimmed = name.trim()
        friendDao.getByName(trimmed)?.let { return it.id }
        return friendDao.insert(FriendEntity(name = trimmed))
    }
}
