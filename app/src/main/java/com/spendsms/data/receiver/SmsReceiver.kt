package com.spendsms.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.spendsms.data.datastore.UserPreferencesDataStore
import com.spendsms.data.local.entity.TransactionEntity
import com.spendsms.data.local.entity.UnparsedSmsEntity
import com.spendsms.domain.repository.SettingsRepository
import com.spendsms.domain.repository.TransactionRepository
import com.spendsms.parser.SmsParser
import com.spendsms.parser.model.ParseResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userPreferences = settingsRepository.userPreferences.first()
                val monitoredSenders = userPreferences.monitoredSenders.ifEmpty {
                    UserPreferencesDataStore.DEFAULT_SENDERS
                }

                // Group multi-part messages by originating address
                val groupedBySender = messages.groupBy { it.originatingAddress.orEmpty() }

                for ((sender, parts) in groupedBySender) {
                    val isMonitored = monitoredSenders.any { monitored ->
                        sender.contains(monitored, ignoreCase = true) || monitored.contains(sender, ignoreCase = true)
                    }

                    if (!isMonitored) continue

                    val fullBody = parts.joinToString(separator = "") { it.messageBody.orEmpty() }
                    val timestamp = parts.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()
                    val syntheticSmsId = timestamp // In SMS_RECEIVED, intent does not carry SMS provider _id yet

                    // Deduplicate against local database
                    if (transactionRepository.isTransactionDuplicate(syntheticSmsId, timestamp)) {
                        continue
                    }

                    when (val result = SmsParser.parse(sender, fullBody, timestamp)) {
                        is ParseResult.Parsed -> {
                            val parsed = result.transaction
                            transactionRepository.insertTransaction(
                                TransactionEntity(
                                    smsId = syntheticSmsId,
                                    sender = sender,
                                    amount = parsed.amount,
                                    type = parsed.type,
                                    merchant = parsed.merchant,
                                    category = parsed.category,
                                    timestamp = timestamp,
                                    balance = parsed.balance,
                                    referenceId = parsed.referenceId,
                                    rawBody = fullBody
                                )
                            )
                        }
                        is ParseResult.Failed -> {
                            transactionRepository.insertUnparsedSms(
                                UnparsedSmsEntity(
                                    smsId = syntheticSmsId,
                                    sender = sender,
                                    body = fullBody,
                                    timestamp = timestamp,
                                    reason = result.reason
                                )
                            )
                        }
                        is ParseResult.Ignored -> {
                            // Ignored (OTP, marketing, etc.)
                        }
                    }
                }
            } catch (_: Exception) {
                // Catch any runtime errors to ensure receiver finishes cleanly
            } finally {
                pendingResult.finish()
            }
        }
    }
}
