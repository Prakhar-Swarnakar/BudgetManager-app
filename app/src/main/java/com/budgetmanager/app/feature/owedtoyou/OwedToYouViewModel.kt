package com.budgetmanager.app.feature.owedtoyou

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.core.designsystem.components.FilterChipItem
import com.budgetmanager.app.core.model.Money
import com.budgetmanager.app.core.model.OwedFriendShare
import com.budgetmanager.app.core.model.OwedToYouEntry
import com.budgetmanager.app.data.repository.TransactionRepository
import com.budgetmanager.app.data.repository.TransactionSplitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OwedToYouViewModel @Inject constructor(
    private val transactionSplitRepository: TransactionSplitRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val viewMode = MutableStateFlow(OwedToYouViewMode.BY_TRANSACTION)
    private val statusFilter = MutableStateFlow(ReminderFilter.ALL)
    private val friendFilter = MutableStateFlow<Long?>(null)
    private val expandedFriendIds = MutableStateFlow<Set<Long>>(emptySet())
    private val showFiltersSheet = MutableStateFlow(false)

    val uiState: StateFlow<OwedToYouUiState> = combine(
        transactionSplitRepository.observeOwedToYou(),
        viewMode,
        statusFilter,
        friendFilter,
        expandedFriendIds
    ) { entries, mode, status, friend, expanded ->
        buildUiState(entries, mode, status, friend, expanded)
    }.combine(showFiltersSheet) { state, showFilters ->
        state.copy(showFiltersSheet = showFilters)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OwedToYouUiState())

    fun onViewModeChanged(mode: OwedToYouViewMode) {
        viewMode.value = mode
    }

    fun onStatusFilterChanged(filter: ReminderFilter) {
        statusFilter.value = filter
    }

    fun onFriendFilterChanged(friendId: Long?) {
        friendFilter.value = friendId
    }

    fun onFriendSectionToggled(friendId: Long) {
        expandedFriendIds.update { current -> if (friendId in current) current - friendId else current + friendId }
    }

    fun onFiltersClicked() {
        showFiltersSheet.value = true
    }

    fun onFiltersDismissed() {
        showFiltersSheet.value = false
    }

    /** Transaction-level, not per friend - see [OwedToYouUiState]'s doc comment. Never touches
     *  settled state or the budget-counted amount. */
    fun onReminderToggled(transactionId: Long, reminded: Boolean) {
        viewModelScope.launch {
            transactionRepository.setReminded(transactionId, reminded)
        }
    }

    private fun buildUiState(
        entries: List<OwedToYouEntry>,
        mode: OwedToYouViewMode,
        status: ReminderFilter,
        friendId: Long?,
        expanded: Set<Long>
    ): OwedToYouUiState {
        val distinctFriends = entries.flatMap { it.friends }
            .distinctBy { it.friendId }
            .sortedBy { it.friendName }

        val statusOptions = listOf(
            FilterChipItem(ReminderFilter.ALL, "All", entries.size),
            FilterChipItem(ReminderFilter.PENDING, "Pending", entries.count { !it.reminded }),
            FilterChipItem(ReminderFilter.REMINDED, "Reminded", entries.count { it.reminded })
        )
        val friendOptions: List<FilterChipItem<Long?>> = listOf(FilterChipItem<Long?>(null, "All", entries.size)) +
            distinctFriends.map { friend ->
                FilterChipItem<Long?>(
                    friend.friendId,
                    friend.friendName,
                    entries.count { entry -> entry.friends.any { it.friendId == friend.friendId } }
                )
            }

        val filtered = entries.filter { entry ->
            val matchesStatus = when (status) {
                ReminderFilter.ALL -> true
                ReminderFilter.PENDING -> !entry.reminded
                ReminderFilter.REMINDED -> entry.reminded
            }
            val matchesFriend = friendId == null || entry.friends.any { it.friendId == friendId }
            matchesStatus && matchesFriend
        }

        return OwedToYouUiState(
            isLoading = false,
            viewMode = mode,
            statusFilter = status,
            statusFilterOptions = statusOptions,
            friendFilter = friendId,
            friendFilterOptions = friendOptions,
            rows = filtered.map { it.toRowUi() },
            friendSections = buildFriendSections(filtered, distinctFriends, expanded),
            expandedFriendIds = expanded,
            totalPendingText = Money(entries.sumOf { it.totalOwed.paise }).formatted()
        )
    }

    private fun buildFriendSections(
        entries: List<OwedToYouEntry>,
        distinctFriends: List<OwedFriendShare>,
        expanded: Set<Long>
    ): List<OwedToYouFriendSectionUi> = distinctFriends.mapNotNull { friend ->
        val friendEntries = entries.filter { entry -> entry.friends.any { it.friendId == friend.friendId } }
        if (friendEntries.isEmpty()) return@mapNotNull null
        val pendingPaise = friendEntries.sumOf { entry ->
            entry.friends.filter { it.friendId == friend.friendId && !it.settled }.sumOf { it.amountOwed.paise }
        }
        OwedToYouFriendSectionUi(
            friendId = friend.friendId,
            friendName = friend.friendName,
            pendingText = Money(pendingPaise).formatted(),
            rows = friendEntries.map { it.toRowUi(highlightFriendId = friend.friendId) }
        )
    }
}

private fun OwedToYouEntry.toRowUi(highlightFriendId: Long? = null): OwedToYouRowUi {
    val highlightedShare = highlightFriendId?.let { id -> friends.firstOrNull { it.friendId == id } }
    // By friend view shows that friend's own share as the headline amount, not the whole
    // transaction's combined total - a multi-friend split used to show the SAME combined number
    // under every friend's section, which overstated what each one individually owes. The
    // combined total still shows too, struck through, exactly like a split's "your share" +
    // struck full amount elsewhere - but only when it would actually differ (more than one
    // friend on the split).
    val amountText = (highlightedShare?.amountOwed ?: totalOwed).formatted()
    val splitTotalText = if (highlightedShare != null && friends.size > 1) totalOwed.formatted() else null
    return OwedToYouRowUi(
        transactionId = transactionId,
        title = title,
        occurredAt = occurredAt,
        friendNames = if (highlightFriendId != null) {
            val rest = friends.filterNot { it.friendId == highlightFriendId }.map { it.friendName }
            listOfNotNull(highlightedShare?.friendName) + rest
        } else {
            friends.map { it.friendName }
        },
        reminded = reminded,
        amountText = amountText,
        splitTotalText = splitTotalText,
        categoryEmoji = categoryEmoji,
        categoryName = categoryName,
        taxonomyLabel = taxonomy?.label
    )
}
