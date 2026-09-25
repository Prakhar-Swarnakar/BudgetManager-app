package com.budgetmanager.app.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmanager.app.core.common.EmojiValidation
import com.budgetmanager.app.data.repository.CategoryRepository
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
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val archivedExpanded = MutableStateFlow(false)
    private val sheet = MutableStateFlow<CategorySheetUiState?>(null)

    val uiState: StateFlow<CategoriesUiState> = combine(
        categoryRepository.observeAll(),
        archivedExpanded,
        sheet
    ) { all, expanded, sheetState ->
        CategoriesUiState(
            isLoading = false,
            activeCategories = all.filter { !it.archived },
            archivedCategories = all.filter { it.archived },
            archivedExpanded = expanded,
            sheet = sheetState
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    /** Called from the top bar's + button (see AppNavigation, which shares this ViewModel
     *  instance with the screen since Categories has no per-instance arguments). */
    fun onAddClicked() {
        sheet.value = CategorySheetUiState()
    }

    fun onCategoryClicked(id: Long) {
        viewModelScope.launch {
            val category = categoryRepository.getById(id) ?: return@launch
            sheet.value = CategorySheetUiState(
                editingCategoryId = id,
                name = category.name,
                emoji = category.emoji
            )
        }
    }

    fun onSheetDismissed() {
        sheet.value = null
    }

    fun onSheetNameChanged(value: String) {
        sheet.update { it?.copy(name = value, nameError = null) }
    }

    fun onSheetEmojiChanged(value: String) {
        sheet.update { it?.copy(emoji = value, emojiError = null) }
    }

    fun onSheetSaved() {
        val current = sheet.value ?: return
        val name = current.name.trim()
        val emoji = current.emoji.trim()

        if (name.isEmpty()) {
            sheet.update { it?.copy(nameError = "Enter a name") }
            return
        }
        if (!EmojiValidation.isSingleEmoji(emoji)) {
            sheet.update { it?.copy(emojiError = "Pick one emoji") }
            return
        }

        viewModelScope.launch {
            if (current.editingCategoryId != null) {
                categoryRepository.update(current.editingCategoryId, name, emoji)
            } else {
                categoryRepository.create(name, emoji)
            }
            sheet.value = null
        }
    }

    /** Swipe-to-archive on an active row. Only one direction is ever live here - see R30 in
     *  09-risks-and-phases.md. */
    fun onArchiveSwiped(id: Long) {
        viewModelScope.launch { categoryRepository.setArchived(id, archived = true) }
    }

    fun onUnarchiveClicked(id: Long) {
        viewModelScope.launch { categoryRepository.setArchived(id, archived = false) }
    }

    fun onArchivedSectionToggled() {
        archivedExpanded.update { !it }
    }

    /** Called once, when a drag ends - not on every step, so a drag never round-trips through
     *  the repository's Flow mid-gesture. See CategoryList. */
    fun onReorder(orderedActiveIds: List<Long>) {
        viewModelScope.launch { categoryRepository.reorder(orderedActiveIds) }
    }
}
