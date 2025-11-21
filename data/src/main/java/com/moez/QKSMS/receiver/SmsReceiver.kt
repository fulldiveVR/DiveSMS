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
package com.moez.QKSMS.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony.Sms
import com.moez.QKSMS.interactor.ReceiveSms
import com.moez.QKSMS.util.SmsEventType
import com.moez.QKSMS.util.SmsReceptionLogger
import dagger.android.AndroidInjection
import timber.log.Timber
import javax.inject.Inject

class SmsReceiver : BroadcastReceiver() {

    @Inject lateinit var receiveMessage: ReceiveSms
    @Inject lateinit var smsReceptionLogger: SmsReceptionLogger

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)
        Timber.v("onReceive")

        // Log broadcast received
        smsReceptionLogger.logBroadcastReceived(intent.action ?: "unknown")

        Sms.Intents.getMessagesFromIntent(intent)?.let { messages ->
            val subId = intent.extras?.getInt("subscription", -1) ?: -1

            // Log SMS received event
            if (messages.isNotEmpty()) {
                val address = messages[0].displayOriginatingAddress ?: "unknown"
                val totalLength = messages.sumOf { it.displayMessageBody?.length ?: 0 }
                smsReceptionLogger.logSmsReceived(address, totalLength, subId)
                Timber.d("SMS received from: $address, parts: ${messages.size}, total length: $totalLength, subId: $subId")
            }

            val pendingResult = goAsync()
            receiveMessage.execute(ReceiveSms.Params(subId, messages)) { pendingResult.finish() }
        } ?: run {
            // Log error if no messages could be extracted
            smsReceptionLogger.logError(
                SmsEventType.SMS_RECEIVED,
                "Failed to extract messages from intent",
                mapOf("action" to (intent.action ?: "null"))
            )
            Timber.w("Failed to extract SMS messages from intent")
        }
    }

}