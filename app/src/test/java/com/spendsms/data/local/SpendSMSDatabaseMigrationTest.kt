package com.spendsms.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SpendSMSDatabaseMigrationTest {

    @Test
    fun migration1To2_createsBudgetsTableAndRetainsExistingData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_db"
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Create v1 schema
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `transactions` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `sms_id` INTEGER NOT NULL,
                            `sender` TEXT NOT NULL,
                            `amount` REAL NOT NULL,
                            `type` TEXT NOT NULL,
                            `merchant` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `timestamp` INTEGER NOT NULL,
                            `balance` REAL,
                            `reference_id` TEXT,
                            `raw_body` TEXT NOT NULL,
                            `is_user_edited` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_timestamp` ON `transactions` (`timestamp`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_category` ON `transactions` (`category`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_type` ON `transactions` (`type`)")

                    // Insert pre-migration sample transaction
                    db.execSQL(
                        """
                        INSERT INTO transactions (sms_id, sender, amount, type, merchant, category, timestamp, balance, reference_id, raw_body, is_user_edited)
                        VALUES (101, 'bKash', 1500.0, 'DEBIT', 'Daraz', 'shopping', 1700000000000, 3500.0, 'TRX123', 'Payment of Tk 1500', 0)
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    // Not invoked on creation
                }
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val v1Db = helper.writableDatabase

        // Ensure transaction is in v1
        val v1Cursor = v1Db.query("SELECT merchant, amount FROM transactions WHERE sms_id = 101")
        assertThat(v1Cursor.moveToFirst()).isTrue()
        assertThat(v1Cursor.getString(0)).isEqualTo("Daraz")
        assertThat(v1Cursor.getDouble(1)).isEqualTo(1500.0)
        v1Cursor.close()

        // Execute MIGRATION_1_2
        SpendSMSDatabase.MIGRATION_1_2.migrate(v1Db)

        // Verify transaction data was retained after migration
        val postMigrationCursor = v1Db.query("SELECT merchant, amount, category FROM transactions WHERE sms_id = 101")
        assertThat(postMigrationCursor.moveToFirst()).isTrue()
        assertThat(postMigrationCursor.getString(0)).isEqualTo("Daraz")
        assertThat(postMigrationCursor.getDouble(1)).isEqualTo(1500.0)
        assertThat(postMigrationCursor.getString(2)).isEqualTo("shopping")
        postMigrationCursor.close()

        // Verify budgets table exists and supports operations
        v1Db.execSQL(
            """
            INSERT INTO budgets (category, monthly_limit, alert_80_notified_month, alert_100_notified_month)
            VALUES ('food', 5000.0, '2026-10', NULL)
            """.trimIndent()
        )
        val budgetCursor = v1Db.query("SELECT category, monthly_limit, alert_80_notified_month FROM budgets WHERE category = 'food'")
        assertThat(budgetCursor.moveToFirst()).isTrue()
        assertThat(budgetCursor.getString(0)).isEqualTo("food")
        assertThat(budgetCursor.getDouble(1)).isEqualTo(5000.0)
        assertThat(budgetCursor.getString(2)).isEqualTo("2026-10")
        budgetCursor.close()

        v1Db.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun migration2To3_createsRecurringPaymentsTableAndRetainsData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_v2_to_v3_db"
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Create v2 schema (transactions + budgets)
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `transactions` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `sms_id` INTEGER NOT NULL,
                            `sender` TEXT NOT NULL,
                            `amount` REAL NOT NULL,
                            `type` TEXT NOT NULL,
                            `merchant` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `timestamp` INTEGER NOT NULL,
                            `balance` REAL,
                            `reference_id` TEXT,
                            `raw_body` TEXT NOT NULL,
                            `is_user_edited` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL("CREATE TABLE IF NOT EXISTS `budgets` (`category` TEXT NOT NULL, `monthly_limit` REAL NOT NULL, `alert_80_notified_month` TEXT, `alert_100_notified_month` TEXT, PRIMARY KEY(`category`))")

                    // Pre-populate data in v2
                    db.execSQL("INSERT INTO budgets (category, monthly_limit) VALUES ('utilities', 3000.0)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val v2Db = helper.writableDatabase

        // Execute MIGRATION_2_3
        SpendSMSDatabase.MIGRATION_2_3.migrate(v2Db)

        // Verify existing budget data is intact
        val budgetCursor = v2Db.query("SELECT category, monthly_limit FROM budgets WHERE category = 'utilities'")
        assertThat(budgetCursor.moveToFirst()).isTrue()
        assertThat(budgetCursor.getString(0)).isEqualTo("utilities")
        assertThat(budgetCursor.getDouble(1)).isEqualTo(3000.0)
        budgetCursor.close()

        // Verify recurring_payments table was created and supports CRUD
        v2Db.execSQL(
            """
            INSERT INTO recurring_payments (
                merchant, amount, category, frequency, approximate_day,
                last_payment_date, next_expected_date, is_dismissed, is_confirmed,
                reminder_days_before, last_reminded_date
            ) VALUES (
                'Carnival Internet', 1150.0, 'utilities', 'MONTHLY', 5,
                1700000000000, 1702592000000, 0, 1, 2, NULL
            )
            """.trimIndent()
        )

        val recurringCursor = v2Db.query("SELECT merchant, amount, frequency, is_confirmed FROM recurring_payments WHERE merchant = 'Carnival Internet'")
        assertThat(recurringCursor.moveToFirst()).isTrue()
        assertThat(recurringCursor.getString(0)).isEqualTo("Carnival Internet")
        assertThat(recurringCursor.getDouble(1)).isEqualTo(1150.0)
        assertThat(recurringCursor.getString(2)).isEqualTo("MONTHLY")
        assertThat(recurringCursor.getInt(3)).isEqualTo(1)
        recurringCursor.close()

        v2Db.close()
        context.deleteDatabase(dbName)
    }
}

