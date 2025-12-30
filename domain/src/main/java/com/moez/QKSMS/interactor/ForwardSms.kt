/*
 *  Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 *  This file is part of QKSMS.
 *
 *  QKSMS is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  QKSMS is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.moez.QKSMS.interactor

import com.moez.QKSMS.email.EmailService
import com.moez.QKSMS.repository.ContactRepository
import com.moez.QKSMS.repository.ForwardingRepository
import com.moez.QKSMS.telegram.TelegramService
import com.moez.QKSMS.util.Preferences
import io.reactivex.Flowable
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Interactor for forwarding ALL SMS messages to email and/or Telegram.
 * Forwards every incoming SMS to configured destinations.
 */
class ForwardSms @Inject constructor(
    private val prefs: Preferences,
    private val contactRepo: ContactRepository,
    private val forwardingRepo: ForwardingRepository,
    private val emailService: EmailService,
    private val telegramService: TelegramService
) : Interactor<ForwardSms.Params>() {

    /**
     * Parameters for forwarding an SMS.
     */
    data class Params(
        val sender: String,
        val body: String,
        val timestamp: Long,
        val simSlot: Int
    )

    override fun buildObservable(params: Params): Flowable<*> {
        Timber.i("ForwardSms: buildObservable called for SMS from ${params.sender}")

        return Flowable.just(params)
            .doOnSubscribe {
                Timber.i("ForwardSms: Observable subscribed")
            }
            .doOnNext { p ->
                // Forward to Telegram if enabled
                forwardToTelegram(p)
            }
            .filter {
                // Check if email forwarding is enabled
                val enabled = prefs.emailForwardingEnabled.get()
                Timber.i("ForwardSms: Email forwarding enabled = $enabled")
                if (!enabled) {
                    Timber.d("ForwardSms: Email forwarding is disabled, skipping email")
                }
                enabled
            }
            .filter {
                // Check if destination email is configured
                val destination = prefs.emailForwardingAddress.get()
                Timber.i("ForwardSms: Destination email = '$destination'")
                if (destination.isBlank()) {
                    Timber.d("ForwardSms: No destination email configured, skipping email")
                }
                destination.isNotBlank()
            }
            .doOnNext { p ->
                Timber.i("ForwardSms: Processing SMS from ${p.sender}, body: ${p.body.take(50)}...")

                val destinationEmail = prefs.emailForwardingAddress.get()
                Timber.i("ForwardSms: Will send to $destinationEmail")

                // Get the default email account
                val emailAccount = forwardingRepo.getDefaultEmailAccount()
                Timber.i("ForwardSms: Email account = ${emailAccount?.email ?: "NULL"}, type = ${emailAccount?.accountType ?: "N/A"}")

                if (emailAccount == null) {
                    Timber.e("ForwardSms: No email account configured - user needs to sign in with Gmail")
                    return@doOnNext
                }

                // Check if account is ready
                val isReady = emailService.isAccountReady(emailAccount)
                Timber.i("ForwardSms: Account ready = $isReady")

                if (!isReady) {
                    Timber.w("ForwardSms: Email account not ready, needs authentication")
                    // Save a log entry
                    forwardingRepo.saveLog(
                        filterId = 0,
                        filterName = "All Messages",
                        sender = p.sender,
                        messageBody = p.body,
                        receivedAt = p.timestamp,
                        simSlot = p.simSlot,
                        destinationEmail = destinationEmail,
                        deliveryStatus = "AUTH_REQUIRED",
                        deliveryMethod = emailAccount.accountType
                    )
                    return@doOnNext
                }

                // Build email content
                val contactName = getContactName(p.sender) ?: p.sender
                val subject = "SMS from $contactName"
                val body = buildEmailBody(p, contactName)

                // Create a pending log entry
                val logId = forwardingRepo.saveLog(
                    filterId = 0,
                    filterName = "All Messages",
                    sender = p.sender,
                    messageBody = p.body,
                    receivedAt = p.timestamp,
                    simSlot = p.simSlot,
                    destinationEmail = destinationEmail,
                    deliveryStatus = "PENDING",
                    deliveryMethod = emailAccount.accountType
                )

                Timber.d("ForwardSms: Created log entry $logId")

                // Send the email
                runBlocking {
                    val result = emailService.sendEmail(
                        to = destinationEmail,
                        subject = subject,
                        body = body,
                        isHtml = false,
                        account = emailAccount,
                        logId = logId
                    )

                    when (result) {
                        is EmailService.SendResult.Success -> {
                            Timber.i("ForwardSms: Email sent successfully to $destinationEmail")
                        }
                        is EmailService.SendResult.Failure -> {
                            Timber.e("ForwardSms: Failed to send email: ${result.error}")
                        }
                        is EmailService.SendResult.AuthRequired -> {
                            Timber.w("ForwardSms: Auth required for ${result.accountType}")
                        }
                    }
                }
            }
    }

    private fun buildEmailBody(params: Params, contactName: String): String {
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val date = Date(params.timestamp)

        val simInfo = when {
            params.simSlot < 0 -> "Unknown"
            params.simSlot == 0 -> "SIM 1"
            else -> "SIM ${params.simSlot + 1}"
        }

        return """
New SMS received:
---
From: $contactName (${params.sender})
Time: ${dateFormat.format(date)} ${timeFormat.format(date)}
SIM: $simInfo

Message:
${params.body}
---

Sent via Wize SMS
        """.trimIndent()
    }

    private fun getContactName(address: String): String? {
        return try {
            contactRepo.getUnmanagedContacts()
                .blockingFirst()
                .find { contact ->
                    contact.numbers.any {
                        android.telephony.PhoneNumberUtils.compare(it.address, address)
                    }
                }
                ?.name
        } catch (e: Exception) {
            Timber.w(e, "ForwardSms: Error getting contact name")
            null
        }
    }

    /**
     * Forward SMS to Telegram if enabled.
     */
    private fun forwardToTelegram(params: Params) {
        val telegramEnabled = prefs.telegramForwardingEnabled.get()
        val chatId = prefs.telegramChatId.get()

        Timber.i("ForwardSms: Telegram enabled = $telegramEnabled, chatId = '$chatId'")

        if (!telegramEnabled) {
            Timber.d("ForwardSms: Telegram forwarding disabled, skipping")
            return
        }

        if (chatId.isBlank()) {
            Timber.w("ForwardSms: No Telegram chat ID configured, skipping")
            return
        }

        val contactName = getContactName(params.sender) ?: params.sender

        try {
            val result = telegramService.sendSms(
                chatId = chatId,
                senderName = contactName,
                senderNumber = params.sender,
                messageBody = params.body,
                timestamp = params.timestamp
            ).blockingGet()

            if (result) {
                Timber.i("ForwardSms: SMS forwarded to Telegram successfully")
            } else {
                Timber.e("ForwardSms: Failed to forward SMS to Telegram")
            }
        } catch (e: Exception) {
            Timber.e(e, "ForwardSms: Error forwarding SMS to Telegram")
        }
    }
}
