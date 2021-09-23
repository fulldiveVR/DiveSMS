
package com.moez.QKSMS.feature.blocking.numbers

import com.moez.QKSMS.model.BlockedNumber
import io.realm.RealmList
import io.realm.RealmResults

data class BlockedNumbersState(
    val numbers: RealmResults<BlockedNumber>? = null
)
