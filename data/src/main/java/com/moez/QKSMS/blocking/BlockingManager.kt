/*
 *  Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 *  This file is part of QKSMS.
 *
 *  QKSMS is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  QKSMS is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.moez.QKSMS.blocking

import com.moez.QKSMS.util.Preferences
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Delegates requests to the active blocking client
 */
@Singleton
class BlockingManager @Inject constructor(
    private val prefs: Preferences,
    private val callControlBlockingClient: CallControlBlockingClient,
    private val qksmsBlockingClient: QksmsBlockingClient,
    private val shouldIAnswerBlockingClient: ShouldIAnswerBlockingClient
) : BlockingClient {

    private val client: BlockingClient
        get() = when (prefs.blockingManager.get()) {
            Preferences.BLOCKING_MANAGER_SIA -> shouldIAnswerBlockingClient
            Preferences.BLOCKING_MANAGER_CC -> callControlBlockingClient
            else -> qksmsBlockingClient
        }

    override fun isAvailable(): Boolean = client.isAvailable()

    override fun getClientCapability(): BlockingClient.Capability = client.getClientCapability()

    override fun getAction(address: String): Single<BlockingClient.Action> = client.getAction(address)

    override fun block(addresses: List<String>): Completable = client.block(addresses)

    override fun unblock(addresses: List<String>): Completable = client.unblock(addresses)

    override fun openSettings() = client.openSettings()

}
