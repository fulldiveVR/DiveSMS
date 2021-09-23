
package com.moez.QKSMS.mapper

import android.database.Cursor
import com.moez.QKSMS.model.Conversation

interface CursorToConversation : Mapper<Cursor, Conversation> {

    fun getConversationsCursor(): Cursor?

}