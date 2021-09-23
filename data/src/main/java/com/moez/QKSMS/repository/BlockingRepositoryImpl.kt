
package com.moez.QKSMS.repository

import com.moez.QKSMS.extensions.anyOf
import com.moez.QKSMS.model.BlockedNumber
import com.moez.QKSMS.util.PhoneNumberUtils
import io.realm.Realm
import io.realm.RealmResults
import javax.inject.Inject

class BlockingRepositoryImpl @Inject constructor(
    private val phoneNumberUtils: PhoneNumberUtils
) : BlockingRepository {

    override fun blockNumber(vararg addresses: String) {
        Realm.getDefaultInstance().use { realm ->
            realm.refresh()

            val blockedNumbers = realm.where(BlockedNumber::class.java).findAll()
            val newAddresses = addresses.filter { address ->
                blockedNumbers.none { number -> phoneNumberUtils.compare(number.address, address) }
            }

            val maxId = realm.where(BlockedNumber::class.java)
                    .max("id")?.toLong() ?: -1

            realm.executeTransaction {
                realm.insert(newAddresses.mapIndexed { index, address ->
                    BlockedNumber(maxId + 1 + index, address)
                })
            }
        }
    }

    override fun getBlockedNumbers(): RealmResults<BlockedNumber> {
        return Realm.getDefaultInstance()
                .where(BlockedNumber::class.java)
                .findAllAsync()
    }

    override fun getBlockedNumber(id: Long): BlockedNumber? {
        return Realm.getDefaultInstance()
                .where(BlockedNumber::class.java)
                .equalTo("id", id)
                .findFirst()
    }

    override fun isBlocked(address: String): Boolean {
        return Realm.getDefaultInstance().use { realm ->
            realm.where(BlockedNumber::class.java)
                    .findAll()
                    .any { number -> phoneNumberUtils.compare(number.address, address) }
        }
    }

    override fun unblockNumber(id: Long) {
        Realm.getDefaultInstance().use { realm ->
            realm.executeTransaction {
                realm.where(BlockedNumber::class.java)
                        .equalTo("id", id)
                        .findAll()
                        .deleteAllFromRealm()
            }
        }
    }

    override fun unblockNumbers(vararg addresses: String) {
        Realm.getDefaultInstance().use { realm ->
            val ids = realm.where(BlockedNumber::class.java)
                    .findAll()
                    .filter { number ->
                        addresses.any { address -> phoneNumberUtils.compare(number.address, address) }
                    }
                    .map { number -> number.id }
                    .toLongArray()

            realm.executeTransaction {
                realm.where(BlockedNumber::class.java)
                        .anyOf("id", ids)
                        .findAll()
                        .deleteAllFromRealm()
            }
        }
    }

}
