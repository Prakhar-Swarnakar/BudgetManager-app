package com.budgetmanager.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.budgetmanager.app.data.database.dao.AlertLogDao
import com.budgetmanager.app.data.database.dao.CashTopUpDao
import com.budgetmanager.app.data.database.dao.CategoryDao
import com.budgetmanager.app.data.database.dao.FriendDao
import com.budgetmanager.app.data.database.dao.KeywordRuleDao
import com.budgetmanager.app.data.database.dao.MonthlyBudgetDao
import com.budgetmanager.app.data.database.dao.SmsMessageDao
import com.budgetmanager.app.data.database.dao.TaxonomyKeywordRuleDao
import com.budgetmanager.app.data.database.dao.TransactionDao
import com.budgetmanager.app.data.database.dao.TransactionSplitDao
import com.budgetmanager.app.data.database.entity.AlertLogEntity
import com.budgetmanager.app.data.database.entity.CashTopUpEntity
import com.budgetmanager.app.data.database.entity.CategoryEntity
import com.budgetmanager.app.data.database.entity.FriendEntity
import com.budgetmanager.app.data.database.entity.KeywordRuleEntity
import com.budgetmanager.app.data.database.entity.MonthlyBudgetEntity
import com.budgetmanager.app.data.database.entity.SmsMessageEntity
import com.budgetmanager.app.data.database.entity.TaxonomyKeywordRuleEntity
import com.budgetmanager.app.data.database.entity.TransactionEntity
import com.budgetmanager.app.data.database.entity.TransactionSplitEntity

/** Version 8. Every future schema change bumps this and ships a tested migration - never
 *  fallbackToDestructiveMigration, that deletes the user's history. See 13-development-best-practices.md. */
@Database(
    entities = [
        CategoryEntity::class,
        MonthlyBudgetEntity::class,
        TransactionEntity::class,
        SmsMessageEntity::class,
        AlertLogEntity::class,
        KeywordRuleEntity::class,
        TaxonomyKeywordRuleEntity::class,
        FriendEntity::class,
        TransactionSplitEntity::class,
        CashTopUpEntity::class
    ],
    version = 8,
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
    abstract fun friendDao(): FriendDao
    abstract fun transactionSplitDao(): TransactionSplitDao
    abstract fun cashTopUpDao(): CashTopUpDao

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

        /** Adds "Split with friends" (06-backlog.md): a new friend table, a new transaction_split
         *  table (one row per friend per transaction), and a nullable-in-practice
         *  owed_total_paise column on transactions (NOT NULL DEFAULT 0, so every existing
         *  transaction's budget-counted amount is unchanged - amount_paise - 0 = amount_paise). */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS friend (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_friend_name ON friend (name)"
                )
                db.execSQL("ALTER TABLE transactions ADD COLUMN owed_total_paise INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS transaction_split (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "transaction_id INTEGER NOT NULL, " +
                        "friend_id INTEGER NOT NULL, " +
                        "amount_owed_paise INTEGER NOT NULL, " +
                        "settled INTEGER NOT NULL DEFAULT 0, " +
                        "settled_at INTEGER, " +
                        "FOREIGN KEY(transaction_id) REFERENCES transactions(id) ON DELETE CASCADE, " +
                        "FOREIGN KEY(friend_id) REFERENCES friend(id) ON DELETE CASCADE)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transaction_split_transaction_id " +
                        "ON transaction_split (transaction_id)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_transaction_split_friend_id " +
                        "ON transaction_split (friend_id)"
                )
            }
        }

        /** Adds generic credit (money-in) detection (06-backlog.md, "Credit handling" - v1 slice:
         *  detection only, no Refund/Settlement/Income sub-typing yet). DEFAULT 'DEBIT' means
         *  every existing row keeps meaning exactly what it already meant. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sms_message ADD COLUMN direction TEXT NOT NULL DEFAULT 'DEBIT'")
            }
        }

        /** Adds a permanent title to a transaction - Add Transaction's "Title" field (renamed
         *  from "Merchant") was previously UI-only and never saved anywhere. Nullable, no default
         *  needed: every existing transaction gets NULL, and callers already fall back to the
         *  linked message's merchant, then the note, then a placeholder, for a transaction with
         *  no title saved yet - see [com.budgetmanager.app.core.model.Transaction]'s doc comment. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN title TEXT")
            }
        }

        /** Adds Owed to You's "I've nudged about this" marker - transaction-level (not per
         *  friend, see TransactionEntity's doc comment). NOT NULL DEFAULT 0 means every existing
         *  transaction reads as not-yet-reminded, which is exactly right - nothing has been
         *  reminded about yet. */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN reminded INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Adds the Wallet page's cash top-ups (cash coming in). Purely additive - a new table
         *  and nothing else touched. A cash spend is an existing transaction with taxonomy Cash,
         *  so it needs no schema change at all. */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS cash_top_up (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "amount_paise INTEGER NOT NULL, " +
                        "occurred_at INTEGER NOT NULL, " +
                        "note TEXT)"
                )
            }
        }
    }
}
