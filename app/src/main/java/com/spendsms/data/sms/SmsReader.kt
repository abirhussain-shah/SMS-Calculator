package com.spendsms.data.sms

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import com.spendsms.data.notification.BudgetNotificationManager
import com.spendsms.data.datastore.UserPreferencesDataStore
import com.spendsms.data.local.entity.TransactionEntity
import com.spendsms.data.local.entity.UnparsedSmsEntity
import com.spendsms.domain.repository.SettingsRepository
import com.spendsms.domain.repository.TransactionRepository
import com.spendsms.parser.SmsParser
import com.spendsms.parser.model.ParseResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository,
    private val budgetNotificationManager: BudgetNotificationManager
) {

    suspend fun importSmsInbox(sinceTimestamp: Long = 0L): Int = withContext(Dispatchers.IO) {
        val contentResolver: ContentResolver = context.contentResolver
        val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        val selection = if (sinceTimestamp > 0L) {
            "${Telephony.Sms.DATE} > ?"
        } else {
            null
        }

        val selectionArgs = if (sinceTimestamp > 0L) {
            arrayOf(sinceTimestamp.toString())
        } else {
            null
        }

        val sortOrder = "${Telephony.Sms.DATE} DESC"

        val userPreferences = settingsRepository.userPreferences.first()
        val monitoredSenders = userPreferences.monitoredSenders.ifEmpty {
            UserPreferencesDataStore.DEFAULT_SENDERS
        }

        val transactionsToInsert = mutableListOf<TransactionEntity>()
        var importedCount = 0

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            if (cursor != null && cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

                do {
                    val smsId = cursor.getLong(idIndex)
                    val address = cursor.getString(addressIndex).orEmpty()
                    val body = cursor.getString(bodyIndex).orEmpty()
                    val date = cursor.getLong(dateIndex)

                    // Filter by monitored senders (case-insensitive or partial match)
                    val isMonitored = monitoredSenders.any { sender ->
                        address.contains(sender, ignoreCase = true) || sender.contains(address, ignoreCase = true)
                    }

                    if (!isMonitored) continue

                    // Deduplicate against local database
                    if (transactionRepository.isTransactionDuplicate(smsId, date)) {
                        continue
                    }

                    when (val result = SmsParser.parse(address, body, date)) {
                        is ParseResult.Parsed -> {
                            val parsed = result.transaction
                            transactionsToInsert.add(
                                TransactionEntity(
                                    smsId = smsId,
                                    sender = address,
                                    amount = parsed.amount,
                                    type = parsed.type,
                                    merchant = parsed.merchant,
                                    category = parsed.category,
                                    timestamp = date,
                                    balance = parsed.balance,
                                    referenceId = parsed.referenceId,
                                    rawBody = body
                                )
                            )
                            importedCount++
                        }
                        is ParseResult.Failed -> {
                            transactionRepository.insertUnparsedSms(
                                UnparsedSmsEntity(
                                    smsId = smsId,
                                    sender = address,
                                    body = body,
                                    timestamp = date,
                                    reason = result.reason
                                )
                            )
                        }
                        is ParseResult.Ignored -> {
                            // Ignored (OTP, marketing, etc.) - do not clutter transactions
                        }
                    }
                } while (cursor.moveToNext())
            }

            if (transactionsToInsert.isNotEmpty()) {
                transactionRepository.insertTransactions(transactionsToInsert)
                budgetNotificationManager.checkAndNotifyBudgets()
            }

            settingsRepository.setLastSyncTimestamp(System.currentTimeMillis())
        } catch (_: SecurityException) {
            // Permission revoked or not granted
        } catch (_: Exception) {
            // ContentResolver or parsing error
        } finally {
            cursor?.close()
        }

        importedCount
    }
}
