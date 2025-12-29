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
 * Interface for sending emails.
 */
interface EmailSender {

    /**
     * Result of an email send operation.
     */
    sealed class SendResult {
        object Success : SendResult()
        data class Failure(val error: String, val retryable: Boolean = true) : SendResult()
        data class AuthRequired(val message: String) : SendResult()
    }

    /**
     * Email data to be sent.
     */
    data class Email(
        val to: String,
        val subject: String,
        val body: String,
        val isHtml: Boolean = false
    )

    /**
     * Sends an email using the provided account.
     *
     * @param email The email content to send
     * @param account The email account configuration
     * @return SendResult indicating success or failure
     */
    suspend fun send(email: Email, account: EmailAccount): SendResult

    /**
     * Checks if this sender can handle the given account type.
     */
    fun canHandle(accountType: String): Boolean
}
