
package com.moez.QKSMS.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.moez.QKSMS.interactor.MarkSeen
import dagger.android.AndroidInjection
import javax.inject.Inject

class MarkSeenReceiver : BroadcastReceiver() {

    @Inject lateinit var markSeen: MarkSeen

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)

        val pendingResult = goAsync()
        val threadId = intent.getLongExtra("threadId", 0)
        markSeen.execute(threadId) { pendingResult.finish() }
    }

}