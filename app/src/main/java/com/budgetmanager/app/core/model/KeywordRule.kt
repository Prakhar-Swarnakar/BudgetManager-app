package com.budgetmanager.app.core.model

/** Maps a keyword (e.g. "swiggy") to a category, driving the suggestion when a message is
 *  received. See CategorySuggester. */
data class KeywordRule(val keyword: String, val categoryId: Long)
