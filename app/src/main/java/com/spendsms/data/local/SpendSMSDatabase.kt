package com.spendsms.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.spendsms.data.local.converter.Converters
import com.spendsms.data.local.dao.BudgetDao
import com.spendsms.data.local.dao.CategoryRuleDao
import com.spendsms.data.local.dao.RecurringPaymentDao
import com.spendsms.data.local.dao.TransactionDao
import com.spendsms.data.local.dao.UnparsedSmsDao
import com.spendsms.data.local.entity.BudgetEntity
import com.spendsms.data.local.entity.CategoryRuleEntity
import com.spendsms.data.local.entity.RecurringPaymentEntity
import com.spendsms.data.local.entity.TransactionEntity
import com.spendsms.data.local.entity.UnparsedSmsEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryRuleEntity::class,
        UnparsedSmsEntity::class,
        BudgetEntity::class,
        RecurringPaymentEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SpendSMSDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun unparsedSmsDao(): UnparsedSmsDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringPaymentDao(): RecurringPaymentDao

    companion object {
        const val DATABASE_NAME = "spendsms.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `budgets` (
                        `category` TEXT NOT NULL,
                        `monthly_limit` REAL NOT NULL,
                        `alert_80_notified_month` TEXT,
                        `alert_100_notified_month` TEXT,
                        PRIMARY KEY(`category`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recurring_payments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `merchant` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `category` TEXT NOT NULL,
                        `frequency` TEXT NOT NULL,
                        `approximate_day` INTEGER NOT NULL,
                        `last_payment_date` INTEGER NOT NULL,
                        `next_expected_date` INTEGER NOT NULL,
                        `is_dismissed` INTEGER NOT NULL,
                        `is_confirmed` INTEGER NOT NULL,
                        `reminder_days_before` INTEGER NOT NULL,
                        `last_reminded_date` INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_payments_merchant` ON `recurring_payments` (`merchant`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_payments_next_expected_date` ON `recurring_payments` (`next_expected_date`)")
            }
        }
    }
}
