
package com.moez.QKSMS.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.moez.QKSMS.interactor.DeleteMessages
import dagger.android.AndroidInjection
import javax.inject.Inject

class DeleteMessagesReceiver : BroadcastReceiver() {

    @Inject lateinit var deleteMessages: DeleteMessages

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)

        val pendingResult = goAsync()
        val threadId = intent.getLongExtra("threadId", 0)
        val messageIds = intent.getLongArrayExtra("messageIds")
        deleteMessages.execute(DeleteMessages.Params(messageIds.toList(), threadId)) { pendingResult.finish() }
    }

}