package com.budgetmanager.app.data.backup

import com.budgetmanager.app.core.model.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSerializerTest {

    private val bundle = BackupBundle(
        formatVersion = BackupBundle.CURRENT_FORMAT_VERSION,
        exportedAtMillis = 1_700_000_000_000,
        categories = listOf(CategoryBackup(1, "Food", "🍔", 0, false)),
        monthlyBudgets = listOf(MonthlyBudgetBackup(1, "2026-09", 1, 500000)),
        transactions = listOf(TransactionBackup(1, 12000, 1_700_000_000_000, "2026-09", 1, "lunch", null)),
        smsMessages = listOf(
            SmsMessageBackup(
                1, "ICICIB", "Rs 120 debited", 1_700_000_000_000, null, "dedupe1",
                12000, "Merchant", "UPI", 1, MessageStatus.ACCEPTED, false
            )
        ),
        alertLogs = emptyList(),
        keywordRules = listOf(KeywordRuleBackup("swiggy", 1))
    )

    @Test
    fun `serialize then validate round-trips every field`() {
        val json = BackupSerializer.serialize(bundle)

        val result = BackupSerializer.validate(json)

        assertTrue(result is BackupValidationResult.Valid)
        assertEquals(bundle, (result as BackupValidationResult.Valid).bundle)
    }

    @Test
    fun `malformed json is rejected`() {
        val result = BackupSerializer.validate("not json at all")

        assertTrue(result is BackupValidationResult.Invalid)
    }

    @Test
    fun `json missing a required field is rejected`() {
        val result = BackupSerializer.validate("""{"formatVersion": 1}""")

        assertTrue(result is BackupValidationResult.Invalid)
    }

    @Test
    fun `a newer format version is rejected with a specific message`() {
        val newerJson = """{"formatVersion": 99, "exportedAtMillis": 0, "categories": [], """ +
            """"monthlyBudgets": [], "transactions": [], "smsMessages": [], "alertLogs": [], """ +
            """"keywordRules": []}"""

        val result = BackupSerializer.validate(newerJson)

        assertTrue(result is BackupValidationResult.Invalid)
        assertTrue((result as BackupValidationResult.Invalid).reason.contains("newer version"))
    }
}
