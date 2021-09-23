
package com.moez.QKSMS.mapper

import android.database.Cursor
import com.moez.QKSMS.model.Recipient

interface CursorToRecipient : Mapper<Cursor, Recipient> {

    fun getRecipientCursor(): Cursor?

    fun getRecipientCursor(id: Long): Cursor?

}