
package com.moez.QKSMS.repository

import android.net.Uri
import com.moez.QKSMS.model.Message
import io.reactivex.Observable

interface SyncRepository {

    sealed class SyncProgress {
        class Idle : SyncProgress()
        data class Running(val max: Int, val progress: Int, val indeterminate: Boolean) : SyncProgress()
    }

    val syncProgress: Observable<SyncProgress>

    fun syncMessages()

    fun syncMessage(uri: Uri): Message?

    fun syncContacts()

    /**
     * Syncs a single contact to the Realm
     *
     * Return false if the contact couldn't be found
     */
    fun syncContact(address: String): Boolean

}