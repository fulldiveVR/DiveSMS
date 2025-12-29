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

package com.moez.QKSMS.service

import com.moez.QKSMS.email.EmailDispatcher
import com.moez.QKSMS.email.EmailSender
import com.moez.QKSMS.repository.ForwardingRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ForwardingWorkerHelperImpl.
 * This class provides the retry logic for failed email sends.
 */
@Singleton
class ForwardingWorkerHelperImplProvider @Inject constructor(
    private val forwardingRepo: ForwardingRepository,
    private val emailDispatcher: EmailDispatcher
) : ForwardingWorkerHelperImpl {

    override suspend fun retryEmailSend(logId: Long): ForwardingWorkerHelperImpl.RetryResult {
        Timber.d("ForwardingWorkerHelperImpl: Retrying email send for log $logId")

        // Get the log entry
        val log = forwardingRepo.getLog(logId)
        if (log == null) {
            Timber.e("ForwardingWorkerHelperImpl: Log $logId not found")
            return ForwardingWorkerHelperImpl.RetryResult(success = false, shouldRetry = false)
        }

        // Check if already sent
        if (log.deliveryStatus == "SENT") {
            Timber.d("ForwardingWorkerHelperImpl: Log $logId already sent")
            return ForwardingWorkerHelperImpl.RetryResult(success = true, shouldRetry = false)
        }

        // Get the filter to find the email account
        val filter = forwardingRepo.getFilter(log.filterId)
        if (filter == null) {
            Timber.e("ForwardingWorkerHelperImpl: Filter ${log.filterId} not found")
            forwardingRepo.updateLogStatus(logId, "FAILED", "Filter no longer exists")
            return ForwardingWorkerHelperImpl.RetryResult(success = false, shouldRetry = false)
        }

        // Get the email account
        val emailAccount = forwardingRepo.getEmailAccount(filter.emailAccountId)
        if (emailAccount == null) {
            Timber.e("ForwardingWorkerHelperImpl: Email account ${filter.emailAccountId} not found")
            forwardingRepo.updateLogStatus(logId, "FAILED", "Email account no longer exists")
            return ForwardingWorkerHelperImpl.RetryResult(success = false, shouldRetry = false)
        }

        // Check if account is ready
        if (!emailDispatcher.isAccountReady(emailAccount)) {
            Timber.w("ForwardingWorkerHelperImpl: Email account not ready, auth required")
            forwardingRepo.updateLogStatus(logId, "AUTH_REQUIRED", "Please re-authenticate your email account")
            return ForwardingWorkerHelperImpl.RetryResult(success = false, shouldRetry = false)
        }

        // Create the email (we need to reconstruct from log data)
        // Note: Subject template isn't stored in log, so we use a generic one
        val email = EmailSender.Email(
            to = log.destinationEmail,
            subject = "SMS from ${log.sender}",
            body = buildEmailBody(log),
            isHtml = false
        )

        // Try to send
        val result = emailDispatcher.dispatch(email, emailAccount, logId)

        return when (result) {
            is EmailDispatcher.DispatchResult.Success -> {
                Timber.d("ForwardingWorkerHelperImpl: Retry successful for log $logId")
                ForwardingWorkerHelperImpl.RetryResult(success = true, shouldRetry = false)
            }
            is EmailDispatcher.DispatchResult.Failure -> {
                Timber.e("ForwardingWorkerHelperImpl: Retry failed for log $logId: ${result.error}")
                ForwardingWorkerHelperImpl.RetryResult(success = false, shouldRetry = result.retryable)
            }
            is EmailDispatcher.DispatchResult.AuthRequired -> {
                Timber.w("ForwardingWorkerHelperImpl: Auth required for log $logId")
                ForwardingWorkerHelperImpl.RetryResult(success = false, shouldRetry = false)
            }
        }
    }

    private fun buildEmailBody(log: com.moez.QKSMS.model.ForwardingLog): String {
        val timestamp = java.text.SimpleDateFormat("MMM d, yyyy h:mm a", java.util.Locale.getDefault())
            .format(java.util.Date(log.receivedAt))

        return """
New message received:
---
From: ${log.sender}
Time: $timestamp
SIM: ${if (log.simSlot >= 0) "SIM ${log.simSlot + 1}" else "Unknown"}

Message:
${log.messageBody}
---

Sent via Wize SMS (Retry)
        """.trimIndent()
    }
}
