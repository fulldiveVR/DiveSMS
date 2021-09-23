
package com.moez.QKSMS.model

import android.telephony.PhoneNumberUtils
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

open class Recipient(
    @PrimaryKey var id: Long = 0,
    var address: String = "",
    var contact: Contact? = null,
    var lastUpdate: Long = 0
) : RealmObject() {

    /**
     * Return a string that can be displayed to represent the name of this contact
     */
    fun getDisplayName(): String = contact?.name?.takeIf { it.isNotBlank() }
            ?: PhoneNumberUtils.formatNumber(address, Locale.getDefault().country) // TODO: Use our own PhoneNumberUtils
            ?: address

}
