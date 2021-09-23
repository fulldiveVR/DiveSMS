
package com.moez.QKSMS.model

import io.realm.RealmObject

open class PhoneNumber(
    var address: String = "",
    var type: String = ""
) : RealmObject()