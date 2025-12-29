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

import android.content.Context
import com.moez.QKSMS.model.EmailAccount
import com.moez.QKSMS.service.ForwardingWorker
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of EmailService that uses EmailDispatcher for sending
 * and ForwardingWorker for retry scheduling.
 */
@Singleton
class EmailServiceImpl @Inject constructor(
    private val context: Context,
    private val dispatcher: EmailDispatcher
) : EmailService {

    override suspend fun sendEmail(
        to: String,
        subject: String,
        body: String,
        isHtml: Boolean,
        account: EmailAccount,
        logId: Long?
    ): EmailService.SendResult {
        Timber.d("EmailServiceImpl: Sending email to $to via ${account.accountType}")

        val email = EmailSender.Email(
            to = to,
            subject = subject,
            body = body,
            isHtml = isHtml
        )

        val result = dispatcher.dispatch(email, account, logId)

        return when (result) {
            is EmailDispatcher.DispatchResult.Success -> {
                Timber.d("EmailServiceImpl: Email sent successfully")
                EmailService.SendResult.Success
            }

            is EmailDispatcher.DispatchResult.Failure -> {
                Timber.e("EmailServiceImpl: Send failed - ${result.error}")
                if (result.retryable && logId != null) {
                    scheduleRetry(logId)
                }
                EmailService.SendResult.Failure(result.error, result.retryable)
            }

            is EmailDispatcher.DispatchResult.AuthRequired -> {
                Timber.w("EmailServiceImpl: Auth required - ${result.message}")
                EmailService.SendResult.AuthRequired(result.message, result.accountType)
            }
        }
    }

    override fun isAccountReady(account: EmailAccount): Boolean {
        return dispatcher.isAccountReady(account)
    }

    override fun scheduleRetry(logId: Long) {
        Timber.d("EmailServiceImpl: Scheduling retry for log $logId")
        ForwardingWorker.retryLog(context, logId)
    }
}
