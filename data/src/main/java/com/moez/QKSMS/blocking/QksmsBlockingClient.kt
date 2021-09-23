
package com.moez.QKSMS.blocking

import com.moez.QKSMS.repository.BlockingRepository
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject

class QksmsBlockingClient @Inject constructor(
    private val blockingRepo: BlockingRepository
) : BlockingClient {

    override fun isAvailable(): Boolean = true

    override fun getClientCapability() = BlockingClient.Capability.BLOCK_WITHOUT_PERMISSION

    override fun getAction(address: String): Single<BlockingClient.Action> = Single.fromCallable {
        when (blockingRepo.isBlocked(address)) {
            true -> BlockingClient.Action.Block()
            false -> BlockingClient.Action.Unblock
        }
    }

    override fun block(addresses: List<String>): Completable = Completable.fromCallable {
        blockingRepo.blockNumber(*addresses.toTypedArray())
    }

    override fun unblock(addresses: List<String>): Completable = Completable.fromCallable {
        blockingRepo.unblockNumbers(*addresses.toTypedArray())
    }

    override fun openSettings() = Unit // TODO: Do this here once we implement AndroidX navigation

}
