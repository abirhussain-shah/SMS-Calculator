package com.spendsms.data.sms

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.spendsms.data.datastore.UserPreferencesDataStore
import com.spendsms.data.local.SpendSMSDatabase
import com.spendsms.data.repository.SettingsRepositoryImpl
import com.spendsms.data.repository.TransactionRepositoryImpl
import com.spendsms.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.room.Room
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SmsImportTest {

    private lateinit var context: Context
    private lateinit var database: SpendSMSDatabase
    private lateinit var transactionRepository: TransactionRepositoryImpl
    private lateinit var settingsRepository: SettingsRepositoryImpl
    private lateinit var smsReader: SmsReader

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SpendSMSDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        transactionRepository = TransactionRepositoryImpl(
            database.transactionDao(),
            database.categoryRuleDao(),
            database.unparsedSmsDao(),
            database.budgetDao()
        )
        val dataStore = UserPreferencesDataStore(context)
        settingsRepository = SettingsRepositoryImpl(dataStore)
        val budgetNotificationManager = com.spendsms.data.notification.BudgetNotificationManager(context, transactionRepository)
        smsReader = SmsReader(context, transactionRepository, settingsRepository, budgetNotificationManager)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testSmsImportAndDeduplication() = runTest {
        val shadowContentResolver: ShadowContentResolver =
            org.robolectric.Shadows.shadowOf(context.contentResolver)

        val roboCursor = org.robolectric.fakes.RoboCursor()
        roboCursor.setColumnNames(listOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        ))
        roboCursor.setResults(arrayOf(
            // Synthetic Message 1: bKash payment
            arrayOf(
                1001L,
                "bKash",
                "Payment Tk 1,250.00 to Star Kabab successful. Fee Tk 0.00. Balance Tk 5,400.00. TrxID BLA776X",
                1729150000000L
            ),
            // Synthetic Message 2: Non-monitored random contact
            arrayOf(
                1002L,
                "RandomFriend",
                "Payment Tk 50 to Friend",
                1729151000000L
            ),
            // Synthetic Message 3: Unparsed financial message from monitored bank
            arrayOf(
                1003L,
                "CITYBANK",
                "Important notice regarding account maintenance schedule.",
                1729152000000L
            )
        ))

        shadowContentResolver.setCursor(Telephony.Sms.Inbox.CONTENT_URI, roboCursor)

        val importedCount = smsReader.importSmsInbox()
        assertThat(importedCount).isEqualTo(1)

        val transactions = transactionRepository.getAllTransactions().first()
        assertThat(transactions).hasSize(1)
        assertThat(transactions[0].merchant).contains("Star Kabab")
        assertThat(transactions[0].amount).isEqualTo(1250.0)
        assertThat(transactions[0].type).isEqualTo(TransactionType.DEBIT)

        val pendingReview = transactionRepository.getPendingReviewSms().first()
        assertThat(pendingReview).hasSize(1)
        assertThat(pendingReview[0].sender).isEqualTo("CITYBANK")

        // Second run with the same messages must deduplicate and import 0 new transactions
        val secondRunCount = smsReader.importSmsInbox()
        assertThat(secondRunCount).isEqualTo(0)

        val afterSecondRun = transactionRepository.getAllTransactions().first()
        assertThat(afterSecondRun).hasSize(1)
    }
}
