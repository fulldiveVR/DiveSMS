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
import android.util.Base64
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.Scope
import com.moez.QKSMS.model.EmailAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.Properties
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.mail.Message
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * Sends emails via Gmail API using OAuth 2.0.
 */
@Singleton
class GmailEmailSender @Inject constructor(
    private val context: Context,
    private val oAuthManager: GmailOAuthManager
) : EmailSender {

    companion object {
        const val ACCOUNT_TYPE = "GMAIL"
        private const val GMAIL_SEND_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send"
        val GMAIL_SEND_SCOPE = Scope("https://www.googleapis.com/auth/gmail.send")
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun canHandle(accountType: String): Boolean {
        return accountType.uppercase() == ACCOUNT_TYPE
    }

    override suspend fun send(email: EmailSender.Email, account: EmailAccount): EmailSender.SendResult {
        // Extract Realm object values BEFORE switching threads
        val accountEmail = account.email
        val gmailAccountName = account.gmailAccountName

        return withContext(Dispatchers.IO) {
            try {
                Timber.d("GmailEmailSender: Sending email to ${email.to}")

                // Get the OAuth token (pass extracted string, not Realm object)
                val token = oAuthManager.getValidToken(gmailAccountName)
                if (token == null) {
                    Timber.w("GmailEmailSender: No valid OAuth token")
                    return@withContext EmailSender.SendResult.AuthRequired(
                        "Gmail authentication required. Please sign in again."
                    )
                }

                // Create MIME message
                val rawMessage = createMimeMessage(email, accountEmail)

                // Send via Gmail API
                val result = sendViaGmailApi(rawMessage, token)

                if (result) {
                    Timber.d("GmailEmailSender: Email sent successfully to ${email.to}")
                    EmailSender.SendResult.Success
                } else {
                    EmailSender.SendResult.Failure("Failed to send email via Gmail API", retryable = true)
                }

            } catch (e: Exception) {
                Timber.e(e, "GmailEmailSender: Error sending email")
                handleException(e)
            }
        }
    }

    private fun createMimeMessage(email: EmailSender.Email, fromEmail: String): String {
        val props = Properties()
        val session = Session.getDefaultInstance(props, null)

        val mimeMessage = MimeMessage(session).apply {
            setFrom(InternetAddress(fromEmail))
            addRecipient(Message.RecipientType.TO, InternetAddress(email.to))
            subject = email.subject

            if (email.isHtml) {
                setContent(email.body, "text/html; charset=utf-8")
            } else {
                setText(email.body, "utf-8")
            }
        }

        // Convert to base64url encoded string
        val buffer = ByteArrayOutputStream()
        mimeMessage.writeTo(buffer)
        return Base64.encodeToString(
            buffer.toByteArray(),
            Base64.URL_SAFE or Base64.NO_WRAP
        )
    }

    private fun sendViaGmailApi(rawMessage: String, accessToken: String): Boolean {
        val json = JSONObject().apply {
            put("raw", rawMessage)
        }

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(GMAIL_SEND_URL)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()

        return response.use {
            if (it.isSuccessful) {
                true
            } else {
                val errorBody = it.body?.string()
                Timber.e("GmailEmailSender: API error ${it.code}: $errorBody")

                // Check for specific error codes
                when (it.code) {
                    401 -> throw AuthenticationException("Token expired or invalid")
                    403 -> throw AuthenticationException("Insufficient permissions")
                    429 -> throw RateLimitException("Rate limit exceeded")
                    else -> false
                }
            }
        }
    }

    private fun handleException(e: Exception): EmailSender.SendResult {
        return when (e) {
            is AuthenticationException -> {
                EmailSender.SendResult.AuthRequired(e.message ?: "Authentication required")
            }
            is RateLimitException -> {
                EmailSender.SendResult.Failure("Rate limit exceeded. Try again later.", retryable = true)
            }
            else -> {
                val message = e.message ?: "Unknown error"
                val retryable = !message.contains("auth", ignoreCase = true)
                EmailSender.SendResult.Failure(message, retryable)
            }
        }
    }

    /**
     * Checks if Gmail is signed in for the given account.
     */
    fun isSignedIn(account: EmailAccount): Boolean {
        val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
        return googleAccount != null &&
                googleAccount.email == account.gmailAccountName &&
                googleAccount.grantedScopes.contains(GMAIL_SEND_SCOPE)
    }

    private class AuthenticationException(message: String) : Exception(message)
    private class RateLimitException(message: String) : Exception(message)
}
