
package com.moez.QKSMS.listener

import io.reactivex.Observable

interface ContactAddedListener {

    fun listen(address: String): Observable<*>

}