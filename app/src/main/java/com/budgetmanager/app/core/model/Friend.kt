package com.budgetmanager.app.core.model

/** A person you split expenses with. Just a name - no contact integration, no account; created
 *  inline the first time you type a new name into a split. */
data class Friend(val id: Long, val name: String)
