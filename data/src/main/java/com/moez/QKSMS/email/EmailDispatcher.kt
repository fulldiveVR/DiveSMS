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

package com.moez.QKSMS.email

import com.moez.QKSMS.model.EmailAccount
import com.moez.QKSMS.repository.ForwardingRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dispatches emails to the appropriate sender based on account type.
 * Handles retry logic and fallback between senders.
 */
@Singleton
class EmailDispatcher @Inject constructor(
    private val gmailSender: GmailEmailSender,
    private val smtpSender: SmtpEmailSender,
    private val forwardingRepo: ForwardingRepository
) {
    /**
     * Result of a dispatch operation.
     */
    sealed class DispatchResult {
        object Success : DispatchResult()
        data class Failure(val error: String, val retryable: Boolean) : DispatchResult()
        data class AuthRequired(val message: String, val accountType: String) : DispatchResult()
    }

    /**
     * Sends an email using the appropriate sender for the account type.
     *
     * @param email The email to send
     * @param account The email account to use
     * @param logId Optional log ID to update with status
     * @return DispatchResult indicating success or failure
     */
    suspend fun dispatch(
        email: EmailSender.Email,
        account: EmailAccount,
        logId: Long? = null
    ): DispatchResult {
        Timber.d("EmailDispatcher: Dispatching email to ${email.to} via ${account.accountType}")

        val sender = getSenderForAccount(account)
        if (sender == null) {
            val error = "No sender available for account type: ${account.accountType}"
            Timber.e("EmailDispatcher: $error")
            logId?.let { updateLogStatus(it, "FAILED", error) }
            return DispatchResult.Failure(error, retryable = false)
        }

        val result = sender.send(email, account)

        return when (result) {
            is EmailSender.SendResult.Success -> {
                Timber.d("EmailDispatcher: Email sent successfully")
                logId?.let { updateLogStatus(it, "SENT", null) }
                DispatchResult.Success
            }

            is EmailSender.SendResult.Failure -> {
                Timber.e("EmailDispatcher: Send failed - ${result.error}")
                logId?.let { updateLogStatus(it, if (result.retryable) "RETRY" else "FAILED", result.error) }
                DispatchResult.Failure(result.error, result.retryable)
            }

            is EmailSender.SendResult.AuthRequired -> {
                Timber.w("EmailDispatcher: Authentication required - ${result.message}")
                logId?.let { updateLogStatus(it, "FAILED", result.message) }
                DispatchResult.AuthRequired(result.message, account.accountType)
            }
        }
    }

    /**
     * Gets the appropriate sender for an email account.
     */
    private fun getSenderForAccount(account: EmailAccount): EmailSender? {
        return when {
            gmailSender.canHandle(account.accountType) -> gmailSender
            smtpSender.canHandle(account.accountType) -> smtpSender
            else -> null
        }
    }

    /**
     * Updates the log entry status.
     */
    private fun updateLogStatus(logId: Long, status: String, errorMessage: String?) {
        try {
            forwardingRepo.updateLogStatus(logId, status, errorMessage)
        } catch (e: Exception) {
            Timber.e(e, "EmailDispatcher: Failed to update log status")
        }
    }

    /**
     * Checks if an email account is properly configured and authorized.
     */
    fun isAccountReady(account: EmailAccount): Boolean {
        return when (account.accountType.uppercase()) {
            "GMAIL" -> gmailSender.isSignedIn(account)
            "SMTP" -> account.smtpHost != null && account.smtpUsername != null
            else -> false
        }
    }

    /**
     * Gets the delivery method string for logging purposes.
     */
    fun getDeliveryMethod(account: EmailAccount): String {
        return account.accountType.uppercase()
    }
}
