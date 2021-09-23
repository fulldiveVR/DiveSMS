
package com.moez.QKSMS.repository

import android.net.Uri
import com.moez.QKSMS.model.Contact
import io.reactivex.Flowable
import io.reactivex.Single
import io.realm.RealmResults

interface ContactRepository {

    fun findContactUri(address: String): Single<Uri>

    fun getContacts(): RealmResults<Contact>

    fun getUnmanagedContacts(): Flowable<List<Contact>>

}