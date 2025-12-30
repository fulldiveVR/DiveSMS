/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.moez.QKSMS.telegram

import android.content.Context
import android.content.pm.ApplicationInfo
import io.reactivex.Single
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelegramServiceImpl @Inject constructor(
    private val context: Context
) : TelegramService {
    companion object {
        private const val CLOUD_FUNCTION_URL = "https://us-central1-ai-asia-382012.cloudfunctions.net/forwardSmsToTelegram"
        private const val TEST_FUNCTION_URL = "https://us-central1-ai-asia-382012.cloudfunctions.net/testTelegram"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Send an SMS to Telegram via Cloud Function.
     */
    override fun sendSms(
        chatId: String,
        senderName: String,
        senderNumber: String,
        messageBody: String,
        timestamp: Long
    ): Single<Boolean> {
        return Single.fromCallable {
            try {
                val isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                val json = JSONObject().apply {
                    put("chatId", chatId)
                    put("senderName", senderName)
                    put("senderNumber", senderNumber)
                    put("messageBody", messageBody)
                    put("timestamp", timestamp)
                    put("isDebug", isDebug)
                }

                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(CLOUD_FUNCTION_URL)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                Timber.d("Telegram API response: ${response.code} - $responseBody")

                if (response.isSuccessful) {
                    val result = JSONObject(responseBody ?: "{}")
                    result.optBoolean("success", false)
                } else {
                    Timber.e("Telegram API error: ${response.code} - $responseBody")
                    false
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to send SMS to Telegram")
                false
            }
        }
    }

    /**
     * Send a test message to verify the setup.
     */
    override fun sendTestMessage(chatId: String): Single<Boolean> {
        return Single.fromCallable {
            try {
                val url = "$TEST_FUNCTION_URL?chatId=$chatId"
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                Timber.d("Telegram test response: ${response.code} - $responseBody")

                if (response.isSuccessful) {
                    val result = JSONObject(responseBody ?: "{}")
                    result.optBoolean("success", false)
                } else {
                    Timber.e("Telegram test error: ${response.code} - $responseBody")
                    false
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to send test Telegram message")
                false
            }
        }
    }
}
