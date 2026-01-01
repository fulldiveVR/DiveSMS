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

package com.moez.QKSMS.model

/**
 * Represents the current status of a forwarding service (Email or Telegram).
 */
sealed class ForwardingStatus {
    /**
     * Service is working correctly.
     */
    object Connected : ForwardingStatus()

    /**
     * Service has pending messages that failed to send.
     * @param failedCount Number of messages that failed to forward
     * @param lastError Last error message
     */
    data class Pending(
        val failedCount: Int,
        val lastError: String? = null
    ) : ForwardingStatus()

    /**
     * Service needs reconnection (auth expired but may recover with silentSignIn).
     * @param failedCount Number of messages that failed to forward
     */
    data class NeedsReconnect(
        val failedCount: Int
    ) : ForwardingStatus()

    /**
     * Service is disconnected (auth revoked, needs manual re-authentication).
     * @param failedCount Number of messages that failed to forward
     */
    data class Disconnected(
        val failedCount: Int
    ) : ForwardingStatus()

    /**
     * Service is disabled by user.
     */
    object Disabled : ForwardingStatus()
}

/**
 * Type of forwarding service.
 */
enum class ForwardingType {
    EMAIL,
    TELEGRAM
}
