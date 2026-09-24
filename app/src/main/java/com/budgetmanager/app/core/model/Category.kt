package com.budgetmanager.app.core.model

data class Category(
    val id: Long,
    val name: String,
    val emoji: String,
    val sortOrder: Int,
    val archived: Boolean
)
