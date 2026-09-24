package com.budgetmanager.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.data.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Drives the small red badge on the Messages tab. Separate from MessagesViewModel because the
 *  bottom bar is part of the navigation shell, not the Messages screen itself. */
@HiltViewModel
class AppNavigationViewModel @Inject constructor(
    messageRepository: MessageRepository
) : ViewModel() {
    val newMessageCount: StateFlow<Int> = messageRepository.observeNewCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
