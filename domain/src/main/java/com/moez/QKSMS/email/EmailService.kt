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

/**
 * Service interface for sending emails.
 * Implemented in the data module by EmailServiceImpl.
 */
interface EmailService {

    /**
     * Result of an email send operation.
     */
    sealed class SendResult {
        object Success : SendResult()
        data class Failure(val error: String, val retryable: Boolean) : SendResult()
        data class AuthRequired(val message: String, val accountType: String) : SendResult()
    }

    /**
     * Sends an email.
     *
     * @param to Destination email address
     * @param subject Email subject
     * @param body Email body content
     * @param isHtml Whether the body is HTML
     * @param account The email account to send from
     * @param logId Optional log ID to update with delivery status
     * @return SendResult indicating success or failure
     */
    suspend fun sendEmail(
        to: String,
        subject: String,
        body: String,
        isHtml: Boolean,
        account: EmailAccount,
        logId: Long? = null
    ): SendResult

    /**
     * Checks if an email account is properly configured and ready to send.
     */
    fun isAccountReady(account: EmailAccount): Boolean

    /**
     * Schedules a retry for a failed email send.
     *
     * @param logId The log ID to retry
     */
    fun scheduleRetry(logId: Long)
}
