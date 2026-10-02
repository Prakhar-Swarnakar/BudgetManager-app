package com.budgetmanager.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.budgetmanager.app.data.database.dao.AlertLogDao
import com.budgetmanager.app.data.database.dao.CategoryDao
import com.budgetmanager.app.data.database.dao.KeywordRuleDao
import com.budgetmanager.app.data.database.dao.MonthlyBudgetDao
import com.budgetmanager.app.data.database.dao.SmsMessageDao
import com.budgetmanager.app.data.database.dao.TaxonomyKeywordRuleDao
import com.budgetmanager.app.data.database.dao.TransactionDao
import com.budgetmanager.app.data.database.entity.AlertLogEntity
import com.budgetmanager.app.data.database.entity.CategoryEntity
import com.budgetmanager.app.data.database.entity.KeywordRuleEntity
import com.budgetmanager.app.data.database.entity.MonthlyBudgetEntity
import com.budgetmanager.app.data.database.entity.SmsMessageEntity
import com.budgetmanager.app.data.database.entity.TaxonomyKeywordRuleEntity
import com.budgetmanager.app.data.database.entity.TransactionEntity

/** Version 3. Every future schema change bumps this and ships a tested migration - never
 *  fallbackToDestructiveMigration, that deletes the user's history. See 13-development-best-practices.md. */
@Database(
    entities = [
        CategoryEntity::class,
        MonthlyBudgetEntity::class,
        TransactionEntity::class,
        SmsMessageEntity::class,
        AlertLogEntity::class,
        KeywordRuleEntity::class,
        TaxonomyKeywordRuleEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun monthlyBudgetDao(): MonthlyBudgetDao
    abstract fun transactionDao(): TransactionDao
    abstract fun smsMessageDao(): SmsMessageDao
    abstract fun alertLogDao(): AlertLogDao
    abstract fun keywordRuleDao(): KeywordRuleDao
    abstract fun taxonomyKeywordRuleDao(): TaxonomyKeywordRuleDao

    companion object {
        const val NAME = "budget_manager.db"

        /** Adds sms_message.payment_method (M2b's parser can now read e.g. "UPI" off the SMS
         *  text). Nullable, no default needed beyond NULL for existing rows. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sms_message ADD COLUMN payment_method TEXT")
            }
        }

        /** Adds the payment-method taxonomy: a new taxonomy_keyword_rule table (separate from
         *  keyword_rule - a word can map to a category and a taxonomy value independently),
         *  plus a nullable taxonomy column on transactions and a nullable suggested_taxonomy
         *  column on sms_message, both mirroring their category counterparts. Purely additive -
         *  nothing existing is dropped, renamed, or backfilled. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS taxonomy_keyword_rule (" +
                        "keyword TEXT NOT NULL PRIMARY KEY, " +
                        "taxonomy TEXT NOT NULL)"
                )
                db.execSQL("ALTER TABLE transactions ADD COLUMN taxonomy TEXT")
                db.execSQL("ALTER TABLE sms_message ADD COLUMN suggested_taxonomy TEXT")
            }
        }
    }
}
