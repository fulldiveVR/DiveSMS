
package com.moez.QKSMS.manager

import com.moez.QKSMS.model.Message
import io.realm.Realm
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyManagerImpl @Inject constructor() : KeyManager {

    private var initialized = false
    private var maxValue: Long = 0

    /**
     * Should be called when a new sync is being started
     */
    override fun reset() {
        initialized = true
        maxValue = 0L
    }

    /**
     * Returns a valid ID that can be used to store a new message
     */
    override fun newId(): Long {
        if (!initialized) {
            maxValue = Realm.getDefaultInstance().use { realm ->
                realm.where(Message::class.java).max("id")?.toLong() ?: 0L
            }
            initialized = true
        }

        maxValue++
        return maxValue
    }

}