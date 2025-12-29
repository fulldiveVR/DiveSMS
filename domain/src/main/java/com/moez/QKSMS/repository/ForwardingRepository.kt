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

package com.moez.QKSMS.repository

import com.moez.QKSMS.model.EmailAccount
import com.moez.QKSMS.model.ForwardingFilter
import com.moez.QKSMS.model.ForwardingLog
import io.realm.RealmResults

interface ForwardingRepository {

    // Filters

    /**
     * Returns all forwarding filters
     */
    fun getFilters(): RealmResults<ForwardingFilter>

    /**
     * Returns all enabled forwarding filters as an unmanaged list
     */
    fun getActiveFilters(): List<ForwardingFilter>

    /**
     * Returns the filter with the given [id]
     */
    fun getFilter(id: Long): ForwardingFilter?

    /**
     * Saves a forwarding filter and returns its ID
     */
    fun saveFilter(
        name: String,
        destinationEmail: String,
        emailAccountId: Long,
        senderFilter: String = "",
        senderMatchType: String = "CONTAINS",
        contentFilter: String = "",
        contentMatchType: String = "CONTAINS",
        isIncludeFilter: Boolean = true,
        timeWindowEnabled: Boolean = false,
        startTime: String? = null,
        endTime: String? = null,
        activeDays: String = "",
        simSlotFilter: String = "ALL",
        emailSubjectTemplate: String = "",
        emailBodyTemplate: String = "",
        useHtmlTemplate: Boolean = false,
        priority: Int = 0,
        enabled: Boolean = true
    ): Long

    /**
     * Updates an existing filter
     */
    fun updateFilter(filter: ForwardingFilter)

    /**
     * Deletes the filter with the given [id]
     */
    fun deleteFilter(id: Long)

    /**
     * Enables or disables a filter
     */
    fun updateFilterEnabled(id: Long, enabled: Boolean)

    /**
     * Increments the forwarded count for a filter
     */
    fun incrementForwardedCount(id: Long)

    // Logs

    /**
     * Returns forwarding logs, most recent first
     */
    fun getLogs(limit: Int = 100): RealmResults<ForwardingLog>

    /**
     * Returns logs for a specific filter
     */
    fun getLogsByFilter(filterId: Long): RealmResults<ForwardingLog>

    /**
     * Returns a specific log entry by ID (unmanaged copy)
     */
    fun getLog(id: Long): ForwardingLog?

    /**
     * Saves a forwarding log and returns its ID
     */
    fun saveLog(
        filterId: Long,
        filterName: String,
        sender: String,
        messageBody: String,
        receivedAt: Long,
        simSlot: Int,
        destinationEmail: String,
        deliveryStatus: String = "PENDING",
        deliveryMethod: String = ""
    ): Long

    /**
     * Updates the delivery status of a log entry
     */
    fun updateLogStatus(id: Long, status: String, errorMessage: String? = null, sentAt: Long? = null)

    /**
     * Deletes logs older than the specified number of days
     */
    fun deleteOldLogs(olderThanDays: Int = 30)

    // Email Accounts

    /**
     * Returns all email accounts
     */
    fun getEmailAccounts(): RealmResults<EmailAccount>

    /**
     * Returns the default email account
     */
    fun getDefaultEmailAccount(): EmailAccount?

    /**
     * Returns the email account with the given [id]
     */
    fun getEmailAccount(id: Long): EmailAccount?

    /**
     * Saves an email account and returns its ID
     */
    fun saveEmailAccount(
        accountType: String,
        email: String,
        gmailAccountName: String? = null,
        smtpHost: String? = null,
        smtpPort: Int = 587,
        smtpUsername: String? = null,
        smtpUseTls: Boolean = true,
        isDefault: Boolean = false
    ): Long

    /**
     * Deletes the email account with the given [id]
     */
    fun deleteEmailAccount(id: Long)

    /**
     * Sets the specified account as the default
     */
    fun setDefaultEmailAccount(id: Long)
}
