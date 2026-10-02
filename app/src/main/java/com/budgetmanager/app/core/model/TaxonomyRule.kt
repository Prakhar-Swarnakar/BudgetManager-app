package com.budgetmanager.app.core.model

/** Maps a keyword (e.g. "gpay") to a payment-method taxonomy value, driving the suggestion when
 *  a message is received - a separate rule set from [KeywordRule], so the same word can map
 *  independently to a category and a taxonomy value. See TaxonomySuggester. */
data class TaxonomyRule(val keyword: String, val taxonomy: TaxonomyType)
