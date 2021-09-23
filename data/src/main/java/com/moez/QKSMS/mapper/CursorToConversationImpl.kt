
package com.moez.QKSMS.mapper

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony.Threads
import com.moez.QKSMS.manager.PermissionManager
import com.moez.QKSMS.model.Conversation
import com.moez.QKSMS.model.Recipient
import javax.inject.Inject

class CursorToConversationImpl @Inject constructor(
    private val context: Context,
    private val permissionManager: PermissionManager
) : CursorToConversation {

    companion object {
        val URI: Uri = Uri.parse("content://mms-sms/conversations?simple=true")
        val PROJECTION = arrayOf(
                Threads._ID,
                Threads.RECIPIENT_IDS
        )

        const val ID = 0
        const val RECIPIENT_IDS = 1
    }

    override fun map(from: Cursor): Conversation {
        return Conversation().apply {
            id = from.getLong(ID)
            recipients.addAll(from.getString(RECIPIENT_IDS)
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .map { recipientId -> recipientId.toLong() }
                    .map { recipientId -> Recipient().apply { id = recipientId } })
        }
    }

    override fun getConversationsCursor(): Cursor? {
        return when (permissionManager.hasReadSms()) {
            true -> context.contentResolver.query(URI, PROJECTION, null, null, "date desc")
            false -> null
        }
    }

}