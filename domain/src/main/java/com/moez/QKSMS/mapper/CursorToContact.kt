
package com.moez.QKSMS.mapper

import android.database.Cursor
import com.moez.QKSMS.model.Contact

interface CursorToContact : Mapper<Cursor, Contact> {

    fun getContactsCursor(): Cursor?

}