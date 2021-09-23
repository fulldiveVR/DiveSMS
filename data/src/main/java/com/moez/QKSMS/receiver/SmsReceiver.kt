
package com.moez.QKSMS.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony.Sms
import com.moez.QKSMS.interactor.ReceiveSms
import dagger.android.AndroidInjection
import timber.log.Timber
import javax.inject.Inject

class SmsReceiver : BroadcastReceiver() {

    @Inject lateinit var receiveMessage: ReceiveSms

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)
        Timber.v("onReceive")

        Sms.Intents.getMessagesFromIntent(intent)?.let { messages ->
            val subId = intent.extras?.getInt("subscription", -1) ?: -1

            val pendingResult = goAsync()
            receiveMessage.execute(ReceiveSms.Params(subId, messages)) { pendingResult.finish() }
        }
    }

}