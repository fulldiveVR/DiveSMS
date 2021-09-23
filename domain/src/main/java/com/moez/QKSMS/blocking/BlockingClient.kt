
package com.moez.QKSMS.blocking

import io.reactivex.Completable
import io.reactivex.Single

interface BlockingClient {

    enum class Capability {
        BLOCK_WITHOUT_PERMISSION,
        BLOCK_WITH_PERMISSION ,
        CANT_BLOCK
    }

    sealed class Action {
        class Block(val reason: String? = null) : Action()
        object Unblock : Action()

        // We only need these for Should I Answer, because they don't allow us to block numbers in their app directly.
        // This means there's a good chance that if a number is blocked in QK, it won't be blocked there, so we
        // shouldn't unblock the conversation in that case
        object DoNothing : Action()
    }

    /**
     * Returns true if the target blocking client is available for use, ie. it is installed
     */
    fun isAvailable(): Boolean

    /**
     * Returns the level of access that the given blocking client provides to QKSMS
     */
    fun getClientCapability(): Capability

    /**
     * Returns the recommendation action to perform given a message from the [address]
     */
    fun getAction(address: String): Single<Action>

    /**
     * Blocks the numbers or opens the manager
     */
    fun block(addresses: List<String>): Completable

    /**
     * Unblocks the numbers or opens the manager
     */
    fun unblock(addresses: List<String>): Completable

    /**
     * Opens the settings page for the blocking manager
     */
    fun openSettings()

}
