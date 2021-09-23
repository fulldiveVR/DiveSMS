
package com.moez.QKSMS.mapper

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract.CommonDataKinds.Phone
import com.moez.QKSMS.manager.PermissionManager
import com.moez.QKSMS.model.Contact
import com.moez.QKSMS.model.PhoneNumber
import javax.inject.Inject

class CursorToContactImpl @Inject constructor(
    private val context: Context,
    private val permissionManager: PermissionManager
) : CursorToContact {

    companion object {
        val URI = Phone.CONTENT_URI
        val PROJECTION = arrayOf(
                Phone.LOOKUP_KEY,
                Phone.NUMBER,
                Phone.TYPE,
                Phone.LABEL,
                Phone.DISPLAY_NAME,
                Phone.CONTACT_LAST_UPDATED_TIMESTAMP
        )

        const val COLUMN_LOOKUP_KEY = 0
        const val COLUMN_NUMBER = 1
        const val COLUMN_TYPE = 2
        const val COLUMN_LABEL = 3
        const val COLUMN_DISPLAY_NAME = 4
        const val CONTACT_LAST_UPDATED = 5
    }

    override fun map(from: Cursor) = Contact().apply {
        lookupKey = from.getString(COLUMN_LOOKUP_KEY)
        name = from.getString(COLUMN_DISPLAY_NAME) ?: ""
        numbers.add(PhoneNumber(
                address = from.getString(COLUMN_NUMBER) ?: "",
                type = Phone.getTypeLabel(context.resources, from.getInt(COLUMN_TYPE),
                        from.getString(COLUMN_LABEL)).toString()
        ))
        lastUpdate = from.getLong(CONTACT_LAST_UPDATED)
    }

    override fun getContactsCursor(): Cursor? {
        return when (permissionManager.hasContacts()) {
            true -> context.contentResolver.query(URI, PROJECTION, null, null, null)
            false -> null
        }
    }

}