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
import com.moez.QKSMS.util.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * Sends emails via SMTP.
 */
@Singleton
class SmtpEmailSender @Inject constructor(
    private val context: Context,
    private val prefs: Preferences
) : EmailSender {

    companion object {
        const val ACCOUNT_TYPE = "SMTP"

        // Common SMTP configurations
        val GMAIL_CONFIG = SmtpConfig("smtp.gmail.com", 587, true)
        val OUTLOOK_CONFIG = SmtpConfig("smtp-mail.outlook.com", 587, true)
        val YAHOO_CONFIG = SmtpConfig("smtp.mail.yahoo.com", 587, true)
    }

    data class SmtpConfig(
        val host: String,
        val port: Int,
        val useTls: Boolean
    )

    override fun canHandle(accountType: String): Boolean {
        return accountType.uppercase() == ACCOUNT_TYPE
    }

    override suspend fun send(email: EmailSender.Email, account: EmailAccount): EmailSender.SendResult {
        return withContext(Dispatchers.IO) {
            try {
                Timber.d("SmtpEmailSender: Sending email to ${email.to}")

                val host = account.smtpHost ?: return@withContext EmailSender.SendResult.Failure(
                    "SMTP host not configured",
                    retryable = false
                )

                val password = getStoredPassword(account.id)
                if (password.isNullOrBlank()) {
                    return@withContext EmailSender.SendResult.AuthRequired(
                        "SMTP password not configured"
                    )
                }

                val properties = buildProperties(host, account.smtpPort, account.smtpUseTls)

                val session = Session.getInstance(properties, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(
                            account.smtpUsername ?: account.email,
                            password
                        )
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(account.email))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(email.to))
                    subject = email.subject

                    if (email.isHtml) {
                        setContent(email.body, "text/html; charset=utf-8")
                    } else {
                        setText(email.body, "utf-8")
                    }
                }

                Transport.send(message)

                Timber.d("SmtpEmailSender: Email sent successfully to ${email.to}")
                EmailSender.SendResult.Success

            } catch (e: javax.mail.AuthenticationFailedException) {
                Timber.e(e, "SmtpEmailSender: Authentication failed")
                EmailSender.SendResult.AuthRequired("SMTP authentication failed: ${e.message}")

            } catch (e: javax.mail.MessagingException) {
                Timber.e(e, "SmtpEmailSender: Messaging error")
                val retryable = isRetryableError(e)
                EmailSender.SendResult.Failure("SMTP error: ${e.message}", retryable)

            } catch (e: Exception) {
                Timber.e(e, "SmtpEmailSender: Unexpected error")
                EmailSender.SendResult.Failure("Unexpected error: ${e.message}", retryable = true)
            }
        }
    }

    private fun buildProperties(host: String, port: Int, useTls: Boolean): Properties {
        return Properties().apply {
            put("mail.smtp.host", host)
            put("mail.smtp.port", port.toString())
            put("mail.smtp.auth", "true")

            if (useTls) {
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.starttls.required", "true")
            }

            // Timeouts
            put("mail.smtp.connectiontimeout", "30000")
            put("mail.smtp.timeout", "30000")
            put("mail.smtp.writetimeout", "30000")
        }
    }

    private fun isRetryableError(e: javax.mail.MessagingException): Boolean {
        val message = e.message?.lowercase() ?: return true
        return when {
            message.contains("connection") -> true
            message.contains("timeout") -> true
            message.contains("network") -> true
            message.contains("temporary") -> true
            message.contains("try again") -> true
            message.contains("authentication") -> false
            message.contains("invalid") -> false
            message.contains("rejected") -> false
            else -> true
        }
    }

    /**
     * Gets the stored SMTP password for an account.
     * Passwords are stored securely in encrypted shared preferences.
     */
    private fun getStoredPassword(accountId: Long): String? {
        return prefs.getSmtpPassword(accountId)
    }

    /**
     * Stores an SMTP password securely.
     */
    fun storePassword(accountId: Long, password: String) {
        prefs.setSmtpPassword(accountId, password)
    }

    /**
     * Clears the stored SMTP password.
     */
    fun clearPassword(accountId: Long) {
        prefs.clearSmtpPassword(accountId)
    }
}
