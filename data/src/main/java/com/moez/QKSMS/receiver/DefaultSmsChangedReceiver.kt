
package com.moez.QKSMS.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.annotation.RequiresApi
import com.moez.QKSMS.interactor.SyncMessages
import com.moez.QKSMS.util.Preferences
import dagger.android.AndroidInjection
import javax.inject.Inject

class DefaultSmsChangedReceiver : BroadcastReceiver() {

    @Inject lateinit var prefs: Preferences
    @Inject lateinit var syncMessages: SyncMessages

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)

        if (intent.getBooleanExtra(Telephony.Sms.Intents.EXTRA_IS_DEFAULT_SMS_APP, false)) {
            val pendingResult = goAsync()
            syncMessages.execute(Unit) { pendingResult.finish() }
        }
    }

}