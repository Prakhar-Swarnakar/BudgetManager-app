package com.budgetmanager.app.feature.categoryrules

import com.budgetmanager.app.core.model.Category
import com.budgetmanager.app.core.model.KeywordRule
import com.budgetmanager.app.data.repository.FakeCategoryRepository
import com.budgetmanager.app.data.repository.FakeKeywordRuleRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryRulesViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun categories() = FakeCategoryRepository().apply {
        seed(
            listOf(
                Category(1, "Food & Dining", "🍔", 0, false),
                Category(2, "Transport", "🚗", 1, false)
            )
        )
    }

    @Test
    fun `every active category gets a group, even one with no rules`() = runTest {
        val viewModel = CategoryRulesViewModel(FakeKeywordRuleRepository(), categories())
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        val groups = viewModel.uiState.value.groups
        assertEquals(2, groups.size)
        assertTrue(groups.all { it.keywords.isEmpty() })
        collector.cancel()
    }

    @Test
    fun `rules are sorted into their category's group, alphabetically`() = runTest {
        val rules = FakeKeywordRuleRepository().apply {
            seed(listOf(KeywordRule("zomato", 1), KeywordRule("swiggy", 1), KeywordRule("uber", 2)))
        }
        val viewModel = CategoryRulesViewModel(rules, categories())
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        val groups = viewModel.uiState.value.groups.associateBy { it.categoryId }
        assertEquals(listOf("swiggy", "zomato"), groups[1]!!.keywords)
        assertEquals(listOf("uber"), groups[2]!!.keywords)
        collector.cancel()
    }

    @Test
    fun `adding from a group's chip pre-fills that group's category`() = runTest {
        val viewModel = CategoryRulesViewModel(FakeKeywordRuleRepository(), categories())
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onAddClicked(2)
        dispatcher.scheduler.advanceUntilIdle()

        val editing = viewModel.uiState.value.editing!!
        assertTrue(editing.isNew)
        assertEquals(2L, editing.selectedCategoryId)
        collector.cancel()
    }

    @Test
    fun `saving a new rule upserts it and closes the sheet`() = runTest {
        val rules = FakeKeywordRuleRepository()
        val viewModel = CategoryRulesViewModel(rules, categories())
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onAddClicked(1)
        viewModel.onKeywordChanged("Swiggy")
        viewModel.onSaveEdit()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(KeywordRule("swiggy", 1), rules.getAll().single())
        assertNull(viewModel.uiState.value.editing)
        collector.cancel()
    }

    @Test
    fun `clicking an existing rule opens edit with its current category and keyword fixed`() = runTest {
        val rules = FakeKeywordRuleRepository().apply { seed(listOf(KeywordRule("swiggy", 1))) }
        val viewModel = CategoryRulesViewModel(rules, categories())
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onRuleClicked("swiggy", 1)
        dispatcher.scheduler.advanceUntilIdle()

        val editing = viewModel.uiState.value.editing!!
        assertTrue(!editing.isNew)
        assertEquals("swiggy", editing.keyword)
        assertEquals(1L, editing.selectedCategoryId)
        collector.cancel()
    }

    @Test
    fun `reassigning an existing rule's category upserts under the same keyword`() = runTest {
        val rules = FakeKeywordRuleRepository().apply { seed(listOf(KeywordRule("swiggy", 1))) }
        val viewModel = CategoryRulesViewModel(rules, categories())
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onRuleClicked("swiggy", 1)
        viewModel.onCategorySelected(2)
        viewModel.onSaveEdit()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(KeywordRule("swiggy", 2), rules.getAll().single())
        collector.cancel()
    }

    @Test
    fun `deleting a rule requires confirmation before it's removed`() = runTest {
        val rules = FakeKeywordRuleRepository().apply { seed(listOf(KeywordRule("swiggy", 1))) }
        val viewModel = CategoryRulesViewModel(rules, categories())
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onDeleteRequested("swiggy")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, rules.getAll().size) // not deleted yet

        viewModel.onConfirmDelete()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(rules.getAll().isEmpty())
        collector.cancel()
    }

    @Test
    fun `cancelling a delete request leaves the rule alone`() = runTest {
        val rules = FakeKeywordRuleRepository().apply { seed(listOf(KeywordRule("swiggy", 1))) }
        val viewModel = CategoryRulesViewModel(rules, categories())
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onDeleteRequested("swiggy")
        viewModel.onCancelDelete()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, rules.getAll().size)
        assertNull(viewModel.uiState.value.pendingDeleteKeyword)
        collector.cancel()
    }
}
