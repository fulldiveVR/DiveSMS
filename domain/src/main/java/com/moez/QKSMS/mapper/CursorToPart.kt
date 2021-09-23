
package com.moez.QKSMS.mapper

import android.database.Cursor
import com.moez.QKSMS.model.MmsPart

interface CursorToPart : Mapper<Cursor, MmsPart> {

    fun getPartsCursor(messageId: Long): Cursor?

}