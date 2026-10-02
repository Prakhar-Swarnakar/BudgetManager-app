package com.budgetmanager.app.feature.taxonomyrules

import com.budgetmanager.app.core.model.TaxonomyRule
import com.budgetmanager.app.core.model.TaxonomyType
import com.budgetmanager.app.data.repository.FakeTaxonomyKeywordRuleRepository
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
class TaxonomyRulesViewModelTest {

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
    fun `every taxonomy value gets a group, even one with no rules`() = runTest {
        val viewModel = TaxonomyRulesViewModel(FakeTaxonomyKeywordRuleRepository())
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        val groups = viewModel.uiState.value.groups
        assertEquals(TaxonomyType.entries.size, groups.size)
        assertTrue(groups.all { it.keywords.isEmpty() })
        collector.cancel()
    }

    @Test
    fun `rules are sorted into their taxonomy's group, alphabetically`() = runTest {
        val rules = FakeTaxonomyKeywordRuleRepository().apply {
            seed(
                listOf(
                    TaxonomyRule("phonepe", TaxonomyType.UPI),
                    TaxonomyRule("gpay", TaxonomyType.UPI),
                    TaxonomyRule("neft", TaxonomyType.BANK_TRANSFER)
                )
            )
        }
        val viewModel = TaxonomyRulesViewModel(rules)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        val groups = viewModel.uiState.value.groups.associateBy { it.taxonomy }
        assertEquals(listOf("gpay", "phonepe"), groups[TaxonomyType.UPI]!!.keywords)
        assertEquals(listOf("neft"), groups[TaxonomyType.BANK_TRANSFER]!!.keywords)
        collector.cancel()
    }

    @Test
    fun `adding from a group's chip pre-fills that group's taxonomy`() = runTest {
        val viewModel = TaxonomyRulesViewModel(FakeTaxonomyKeywordRuleRepository())
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onAddClicked(TaxonomyType.WALLET)
        dispatcher.scheduler.advanceUntilIdle()

        val editing = viewModel.uiState.value.editing!!
        assertTrue(editing.isNew)
        assertEquals(TaxonomyType.WALLET, editing.selectedTaxonomy)
        collector.cancel()
    }

    @Test
    fun `saving a new rule upserts it and closes the sheet`() = runTest {
        val rules = FakeTaxonomyKeywordRuleRepository()
        val viewModel = TaxonomyRulesViewModel(rules)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onAddClicked(TaxonomyType.UPI)
        viewModel.onKeywordChanged("GPay")
        viewModel.onSaveEdit()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(TaxonomyRule("gpay", TaxonomyType.UPI), rules.getAll().single())
        assertNull(viewModel.uiState.value.editing)
        collector.cancel()
    }

    @Test
    fun `clicking an existing rule opens edit with its current taxonomy and keyword fixed`() = runTest {
        val rules = FakeTaxonomyKeywordRuleRepository().apply {
            seed(listOf(TaxonomyRule("gpay", TaxonomyType.UPI)))
        }
        val viewModel = TaxonomyRulesViewModel(rules)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onRuleClicked("gpay", TaxonomyType.UPI)
        dispatcher.scheduler.advanceUntilIdle()

        val editing = viewModel.uiState.value.editing!!
        assertTrue(!editing.isNew)
        assertEquals("gpay", editing.keyword)
        assertEquals(TaxonomyType.UPI, editing.selectedTaxonomy)
        collector.cancel()
    }

    @Test
    fun `reassigning an existing rule's taxonomy upserts under the same keyword`() = runTest {
        val rules = FakeTaxonomyKeywordRuleRepository().apply {
            seed(listOf(TaxonomyRule("gpay", TaxonomyType.UPI)))
        }
        val viewModel = TaxonomyRulesViewModel(rules)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onRuleClicked("gpay", TaxonomyType.UPI)
        viewModel.onTaxonomySelected(TaxonomyType.WALLET)
        viewModel.onSaveEdit()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(TaxonomyRule("gpay", TaxonomyType.WALLET), rules.getAll().single())
        collector.cancel()
    }

    @Test
    fun `deleting a rule requires confirmation before it's removed`() = runTest {
        val rules = FakeTaxonomyKeywordRuleRepository().apply {
            seed(listOf(TaxonomyRule("gpay", TaxonomyType.UPI)))
        }
        val viewModel = TaxonomyRulesViewModel(rules)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onDeleteRequested("gpay")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, rules.getAll().size) // not deleted yet

        viewModel.onConfirmDelete()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(rules.getAll().isEmpty())
        collector.cancel()
    }

    @Test
    fun `cancelling a delete request leaves the rule alone`() = runTest {
        val rules = FakeTaxonomyKeywordRuleRepository().apply {
            seed(listOf(TaxonomyRule("gpay", TaxonomyType.UPI)))
        }
        val viewModel = TaxonomyRulesViewModel(rules)
        val collector = viewModel.uiState.onEach { }.launchIn(this)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.onDeleteRequested("gpay")
        viewModel.onCancelDelete()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, rules.getAll().size)
        assertNull(viewModel.uiState.value.pendingDeleteKeyword)
        collector.cancel()
    }
}
