package com.budgetmanager.app.feature.categories

import com.budgetmanager.app.core.model.Category
import com.budgetmanager.app.data.repository.FakeCategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `add category opens the sheet in new mode`() = runTest {
        val repo = FakeCategoryRepository()
        val viewModel = CategoriesViewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        viewModel.onAddClicked()
        dispatcher.scheduler.advanceUntilIdle()

        val sheet = viewModel.uiState.value.sheet
        assertNotNull(sheet)
        assertNull(sheet!!.editingCategoryId)
        collector.cancel()
    }

    @Test
    fun `saving a new category with a valid name and emoji creates it and closes the sheet`() = runTest {
        val repo = FakeCategoryRepository()
        val viewModel = CategoriesViewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        viewModel.onAddClicked()
        viewModel.onSheetNameChanged("Pets")
        viewModel.onSheetEmojiChanged("🐶")
        viewModel.onSheetSaved()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.sheet)
        assertEquals(1, viewModel.uiState.value.activeCategories.size)
        val created = viewModel.uiState.value.activeCategories.first()
        assertEquals("Pets", created.name)
        assertEquals("🐶", created.emoji)
        collector.cancel()
    }

    @Test
    fun `saving with a blank name keeps the sheet open with an error`() = runTest {
        val repo = FakeCategoryRepository()
        val viewModel = CategoriesViewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        viewModel.onAddClicked()
        viewModel.onSheetEmojiChanged("🐶")
        viewModel.onSheetSaved()
        dispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.sheet)
        assertNotNull(viewModel.uiState.value.sheet!!.nameError)
        assertTrue(viewModel.uiState.value.activeCategories.isEmpty())
        collector.cancel()
    }

    @Test
    fun `saving with more than one emoji keeps the sheet open with an error`() = runTest {
        val repo = FakeCategoryRepository()
        val viewModel = CategoriesViewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        viewModel.onAddClicked()
        viewModel.onSheetNameChanged("Pets")
        viewModel.onSheetEmojiChanged("🐶🐱")
        viewModel.onSheetSaved()
        dispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.sheet)
        assertNotNull(viewModel.uiState.value.sheet!!.emojiError)
        assertTrue(viewModel.uiState.value.activeCategories.isEmpty())
        collector.cancel()
    }

    @Test
    fun `clicking a category opens the sheet pre-filled in edit mode`() = runTest {
        val repo = FakeCategoryRepository()
        repo.seed(listOf(Category(1, "Food", "🍔", 0, archived = false)))
        val viewModel = CategoriesViewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        viewModel.onCategoryClicked(1)
        dispatcher.scheduler.advanceUntilIdle()

        val sheet = viewModel.uiState.value.sheet
        assertEquals(1L, sheet?.editingCategoryId)
        assertEquals("Food", sheet?.name)
        assertEquals("🍔", sheet?.emoji)
        collector.cancel()
    }

    @Test
    fun `saving edits from the sheet renames the category`() = runTest {
        val repo = FakeCategoryRepository()
        repo.seed(listOf(Category(1, "Food", "🍔", 0, archived = false)))
        val viewModel = CategoriesViewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        viewModel.onCategoryClicked(1)
        dispatcher.scheduler.advanceUntilIdle() // let the sheet load before editing it
        viewModel.onSheetNameChanged("Food & Dining")
        viewModel.onSheetSaved()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Food & Dining", viewModel.uiState.value.activeCategories.first().name)
        collector.cancel()
    }

    @Test
    fun `archiving moves a category out of active and into archived`() = runTest {
        val repo = FakeCategoryRepository()
        repo.seed(listOf(Category(1, "Food", "🍔", 0, archived = false)))
        val viewModel = CategoriesViewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        viewModel.onArchiveSwiped(1)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.activeCategories.isEmpty())
        assertEquals(1, viewModel.uiState.value.archivedCategories.size)
        collector.cancel()
    }

    @Test
    fun `unarchiving moves a category back into active`() = runTest {
        val repo = FakeCategoryRepository()
        repo.seed(listOf(Category(1, "Food", "🍔", 0, archived = true)))
        val viewModel = CategoriesViewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        viewModel.onUnarchiveClicked(1)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.activeCategories.size)
        assertTrue(viewModel.uiState.value.archivedCategories.isEmpty())
        collector.cancel()
    }

    @Test
    fun `toggling the archived section flips archivedExpanded`() = runTest {
        val repo = FakeCategoryRepository()
        val viewModel = CategoriesViewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        assertEquals(false, viewModel.uiState.value.archivedExpanded)
        viewModel.onArchivedSectionToggled()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(true, viewModel.uiState.value.archivedExpanded)
        collector.cancel()
    }

    @Test
    fun `reorder persists the new active order and leaves archived categories alone`() = runTest {
        val repo = FakeCategoryRepository()
        repo.seed(
            listOf(
                Category(1, "Rent", "🏠", 0, archived = false),
                Category(2, "Food", "🍔", 1, archived = false),
                Category(3, "Old", "📦", 2, archived = true)
            )
        )
        val viewModel = CategoriesViewModel(repo)
        val collector = viewModel.uiState.onEach { }.launchIn(this)

        viewModel.onReorder(listOf(2L, 1L))
        dispatcher.scheduler.advanceUntilIdle()

        val activeIds = viewModel.uiState.value.activeCategories.map { it.id }
        assertEquals(listOf(2L, 1L), activeIds)
        assertEquals(1, viewModel.uiState.value.archivedCategories.size)
        collector.cancel()
    }
}
