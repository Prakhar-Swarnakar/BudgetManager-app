package com.budgetmanager.app.sms

import com.budgetmanager.app.core.model.TaxonomyType
import com.budgetmanager.app.data.repository.TaxonomyKeywordRuleRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Suggests a payment-method taxonomy value by matching keywords against a message's merchant
 * (or its raw text, when nothing was parsed) - same shape as CategorySuggester, but against the
 * separate taxonomy keyword rule set, so a word can map independently to a category and a
 * taxonomy value. Only a pre-fill: the user always sees it and can change it before saving.
 */
@Singleton
class TaxonomySuggester @Inject constructor(
    private val taxonomyKeywordRuleRepository: TaxonomyKeywordRuleRepository
) {
    /** The longest matching keyword wins. Case-insensitive. Null if nothing matches. */
    suspend fun suggest(text: String): TaxonomyType? {
        val lower = text.lowercase()
        return taxonomyKeywordRuleRepository.getAll()
            .filter { rule -> lower.contains(rule.keyword.lowercase()) }
            .maxByOrNull { rule -> rule.keyword.length }
            ?.taxonomy
    }
}
