
package com.moez.QKSMS.feature.blocking.numbers

import com.moez.QKSMS.common.base.QkViewContract
import io.reactivex.Observable

interface BlockedNumbersView : QkViewContract<BlockedNumbersState> {

    fun unblockAddress(): Observable<Long>
    fun addAddress(): Observable<*>
    fun saveAddress(): Observable<String>

    fun showAddDialog()

}
