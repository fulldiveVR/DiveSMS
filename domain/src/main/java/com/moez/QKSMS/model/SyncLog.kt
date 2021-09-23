
package com.moez.QKSMS.model

import io.realm.RealmObject

open class SyncLog : RealmObject() {

    var date: Long = System.currentTimeMillis()

}