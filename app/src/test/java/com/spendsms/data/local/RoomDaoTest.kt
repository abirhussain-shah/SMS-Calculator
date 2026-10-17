package com.spendsms.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.spendsms.data.local.dao.CategoryRuleDao
import com.spendsms.data.local.dao.TransactionDao
import com.spendsms.data.local.dao.UnparsedSmsDao
import com.spendsms.data.local.entity.CategoryRuleEntity
import com.spendsms.data.local.entity.TransactionEntity
import com.spendsms.data.local.entity.UnparsedSmsEntity
import com.spendsms.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomDaoTest {

    private lateinit var database: SpendSMSDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var categoryRuleDao: CategoryRuleDao
    private lateinit var unparsedSmsDao: UnparsedSmsDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SpendSMSDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        transactionDao = database.transactionDao()
        categoryRuleDao = database.categoryRuleDao()
        unparsedSmsDao = database.unparsedSmsDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveTransaction() = runTest {
        val transaction = TransactionEntity(
            smsId = 101L,
            sender = "bKash",
            amount = 1250.0,
            type = TransactionType.DEBIT,
            merchant = "Star Kabab",
            category = "food",
            timestamp = 1729150000000L,
            balance = 5400.0,
            referenceId = "BLA776X",
            rawBody = "Payment Tk 1,250.00 to Star Kabab successful. Ref BLA776X."
        )

        val rowId = transactionDao.insertTransaction(transaction)
        assertThat(rowId).isGreaterThan(0L)

        val retrieved = transactionDao.getTransactionById(rowId).first()
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.amount).isEqualTo(1250.0)
        assertThat(retrieved?.merchant).isEqualTo("Star Kabab")
        assertThat(retrieved?.category).isEqualTo("food")
    }

    @Test
    fun deduplicationIgnoresDuplicateSmsIdAndTimestamp() = runTest {
        val t1 = TransactionEntity(
            smsId = 202L,
            sender = "NAGAD",
            amount = 500.0,
            type = TransactionType.DEBIT,
            merchant = "Mobile Recharge",
            category = "recharge",
            timestamp = 1729160000000L,
            rawBody = "Mobile recharge of Tk 500 successful."
        )
        val firstInsert = transactionDao.insertTransaction(t1)
        assertThat(firstInsert).isGreaterThan(0L)

        // Attempt exact duplicate with same smsId and timestamp
        val duplicateInsert = transactionDao.insertTransaction(t1)
        // With OnConflictStrategy.IGNORE, duplicate insert returns -1L
        assertThat(duplicateInsert).isEqualTo(-1L)

        val count = transactionDao.getTransactionCount()
        assertThat(count).isEqualTo(1)
    }

    @Test
    fun dateRangeAndTotalAggregationCalculations() = runTest {
        val baseTime = 1729000000000L

        val t1 = TransactionEntity(
            smsId = 1L, sender = "bKash", amount = 1000.0,
            type = TransactionType.DEBIT, merchant = "Merchant A",
            category = "food", timestamp = baseTime + 1000L, rawBody = "body"
        )
        val t2 = TransactionEntity(
            smsId = 2L, sender = "bKash", amount = 500.0,
            type = TransactionType.DEBIT, merchant = "Merchant B",
            category = "transport", timestamp = baseTime + 2000L, rawBody = "body"
        )
        val t3 = TransactionEntity(
            smsId = 3L, sender = "CityBank", amount = 30000.0,
            type = TransactionType.CREDIT, merchant = "Employer",
            category = "salary", timestamp = baseTime + 3000L, rawBody = "body"
        )

        transactionDao.insertTransactions(listOf(t1, t2, t3))

        val totalSpent = transactionDao.getTotalSpentInDateRange(baseTime, baseTime + 5000L).first()
        assertThat(totalSpent).isEqualTo(1500.0)

        val totalReceived = transactionDao.getTotalReceivedInDateRange(baseTime, baseTime + 5000L).first()
        assertThat(totalReceived).isEqualTo(30000.0)

        val categorySpending = transactionDao.getCategorySpendingInDateRange(baseTime, baseTime + 5000L).first()
        assertThat(categorySpending).hasSize(2)
        assertThat(categorySpending[0].category).isEqualTo("food")
        assertThat(categorySpending[0].totalAmount).isEqualTo(1000.0)
        assertThat(categorySpending[1].category).isEqualTo("transport")
        assertThat(categorySpending[1].totalAmount).isEqualTo(500.0)
    }

    @Test
    fun categoryRuleInsertAndRetroactiveUpdate() = runTest {
        val t1 = TransactionEntity(
            smsId = 10L, sender = "bKash", amount = 350.0,
            type = TransactionType.DEBIT, merchant = "Pathao Food",
            category = "other", timestamp = 1729000000000L, rawBody = "body",
            isUserEdited = false
        )
        val t2 = TransactionEntity(
            smsId = 11L, sender = "bKash", amount = 420.0,
            type = TransactionType.DEBIT, merchant = "Pathao Food",
            category = "other", timestamp = 1729010000000L, rawBody = "body",
            isUserEdited = false
        )
        transactionDao.insertTransactions(listOf(t1, t2))

        // Save rule for Pathao Food
        categoryRuleDao.insertOrUpdateRule(
            CategoryRuleEntity(merchant = "pathao food", category = "food")
        )
        val rule = categoryRuleDao.getRuleForMerchant("pathao food")
        assertThat(rule).isNotNull()
        assertThat(rule?.category).isEqualTo("food")

        // Retroactively update existing unedited transactions
        val updatedCount = categoryRuleDao.updateTransactionsCategoryForMerchant("Pathao Food", "food")
        assertThat(updatedCount).isEqualTo(2)

        val transactions = transactionDao.getAllTransactions().first()
        assertThat(transactions.all { it.category == "food" }).isTrue()
    }

    @Test
    fun unparsedSmsManagementInNeedsReview() = runTest {
        val sms = UnparsedSmsEntity(
            smsId = 999L,
            sender = "UNKNOWN_BANK",
            body = "Your one time password is 123456",
            timestamp = 1729000000000L,
            reason = "No financial keywords matched"
        )
        val id = unparsedSmsDao.insertUnparsedSms(sms)
        assertThat(id).isGreaterThan(0L)

        val pending = unparsedSmsDao.getPendingReviewSms().first()
        assertThat(pending).hasSize(1)
        assertThat(pending[0].body).contains("123456")

        // User ignores this message
        unparsedSmsDao.ignoreSms(id)

        val afterIgnore = unparsedSmsDao.getPendingReviewSms().first()
        assertThat(afterIgnore).isEmpty()
    }

    @Test
    fun budgetDao_crudOperationsAndAlertTracking() = runTest {
        val budgetDao = database.budgetDao()
        val budget = com.spendsms.data.local.entity.BudgetEntity(
            category = "food",
            monthlyLimit = 6000.0
        )

        budgetDao.upsertBudget(budget)

        val retrieved = budgetDao.getBudgetForCategoryDirect("food")
        assertThat(retrieved).isNotNull()
        assertThat(retrieved!!.monthlyLimit).isEqualTo(6000.0)

        // Update alert months
        budgetDao.updateAlert80Month("food", "2026-10")
        budgetDao.updateAlert100Month("food", "2026-10")

        val updated = budgetDao.getBudgetForCategoryDirect("food")
        assertThat(updated?.alert80NotifiedMonth).isEqualTo("2026-10")
        assertThat(updated?.alert100NotifiedMonth).isEqualTo("2026-10")

        // Delete budget
        budgetDao.deleteBudget("food")
        val deleted = budgetDao.getBudgetForCategoryDirect("food")
        assertThat(deleted).isNull()
    }

    @Test
    fun recurringPaymentDao_crudOperationsAndFiltering() = runTest {
        val recurringDao = database.recurringPaymentDao()
        val entity = com.spendsms.data.local.entity.RecurringPaymentEntity(
            merchant = "Carnival Internet",
            amount = 1150.0,
            category = "utilities",
            frequency = "MONTHLY",
            approximateDay = 5,
            lastPaymentDate = 1700000000000L,
            nextExpectedDate = 1702592000000L,
            isDismissed = false,
            isConfirmed = false,
            reminderDaysBefore = 2
        )

        val id = recurringDao.upsertRecurringPayment(entity)
        assertThat(id).isGreaterThan(0L)

        // It should appear in pending suggestions
        val suggestions = recurringDao.getPendingSuggestions().first()
        assertThat(suggestions).hasSize(1)
        assertThat(suggestions[0].merchant).isEqualTo("Carnival Internet")

        // Active confirmed should be empty initially
        val activeInitial = recurringDao.getActiveRecurringPayments().first()
        assertThat(activeInitial).isEmpty()

        // Confirm the suggestion
        recurringDao.confirmRecurring(id)
        val activeAfterConfirm = recurringDao.getActiveRecurringPayments().first()
        assertThat(activeAfterConfirm).hasSize(1)
        assertThat(activeAfterConfirm[0].isConfirmed).isTrue()

        // Test updateLastReminded
        val now = 1702500000000L
        recurringDao.updateLastReminded(id, now)
        val fetched = recurringDao.getRecurringPaymentById(id)
        assertThat(fetched?.lastRemindedDate).isEqualTo(now)

        // Dismiss the recurring payment
        recurringDao.dismissRecurring(id)
        val activeAfterDismiss = recurringDao.getActiveRecurringPayments().first()
        assertThat(activeAfterDismiss).isEmpty()

        // Delete payment
        recurringDao.deleteRecurringPayment(id)
        val afterDelete = recurringDao.getRecurringPaymentById(id)
        assertThat(afterDelete).isNull()
    }
}

