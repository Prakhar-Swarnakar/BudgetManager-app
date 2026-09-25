package com.budgetmanager.app.sms

import com.budgetmanager.app.data.repository.KeywordRuleRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Suggests a category by matching keywords against a message's merchant (or its raw text, when
 * nothing was parsed) - the starter categories come seeded with a keyword list (StarterData).
 * Only a pre-fill: the user always sees it and can change it before saving (02-features.md, F6).
 */
@Singleton
class CategorySuggester @Inject constructor(
    private val keywordRuleRepository: KeywordRuleRepository
) {
    /** The longest matching keyword wins, so a more specific word ("bigbasket") beats a shorter
     *  one that happens to also be a substring of the text. Case-insensitive. Null if nothing
     *  matches - the field is then left for the user to fill in themselves. */
    suspend fun suggest(text: String): Long? {
        val lower = text.lowercase()
        return keywordRuleRepository.getAll()
            .filter { rule -> lower.contains(rule.keyword.lowercase()) }
            .maxByOrNull { rule -> rule.keyword.length }
            ?.categoryId
    }
}
