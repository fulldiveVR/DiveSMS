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
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import timber.log.Timber
import javax.inject.Inject

class ForwardingRepositoryImpl @Inject constructor() : ForwardingRepository {

    // ==================== Filters ====================

    override fun getFilters(): RealmResults<ForwardingFilter> {
        return Realm.getDefaultInstance()
            .where(ForwardingFilter::class.java)
            .sort("priority")
            .findAll()
    }

    override fun getActiveFilters(): List<ForwardingFilter> {
        return Realm.getDefaultInstance().use { realm ->
            realm.refresh()
            realm.where(ForwardingFilter::class.java)
                .equalTo("enabled", true)
                .sort("priority")
                .findAll()
                .let { realm.copyFromRealm(it) }
        }
    }

    override fun getFilter(id: Long): ForwardingFilter? {
        return Realm.getDefaultInstance()
            .apply { refresh() }
            .where(ForwardingFilter::class.java)
            .equalTo("id", id)
            .findFirst()
    }

    override fun saveFilter(
        name: String,
        destinationEmail: String,
        emailAccountId: Long,
        senderFilter: String,
        senderMatchType: String,
        contentFilter: String,
        contentMatchType: String,
        isIncludeFilter: Boolean,
        timeWindowEnabled: Boolean,
        startTime: String?,
        endTime: String?,
        activeDays: String,
        simSlotFilter: String,
        emailSubjectTemplate: String,
        emailBodyTemplate: String,
        useHtmlTemplate: Boolean,
        priority: Int,
        enabled: Boolean
    ): Long {
        return Realm.getDefaultInstance().use { realm ->
            val id = (realm.where(ForwardingFilter::class.java).max("id")?.toLong() ?: -1) + 1
            val now = System.currentTimeMillis()

            val filter = ForwardingFilter(
                id = id,
                name = name,
                enabled = enabled,
                priority = priority,
                createdAt = now,
                updatedAt = now,
                senderFilter = senderFilter,
                senderMatchType = senderMatchType,
                contentFilter = contentFilter,
                contentMatchType = contentMatchType,
                isIncludeFilter = isIncludeFilter,
                timeWindowEnabled = timeWindowEnabled,
                startTime = startTime,
                endTime = endTime,
                activeDays = activeDays,
                simSlotFilter = simSlotFilter,
                destinationEmail = destinationEmail,
                emailAccountId = emailAccountId,
                emailSubjectTemplate = emailSubjectTemplate,
                emailBodyTemplate = emailBodyTemplate,
                useHtmlTemplate = useHtmlTemplate
            )

            realm.executeTransaction { realm.insertOrUpdate(filter) }
            id
        }
    }

    override fun updateFilter(filter: ForwardingFilter) {
        Realm.getDefaultInstance().use { realm ->
            realm.executeTransaction {
                filter.updatedAt = System.currentTimeMillis()
                realm.insertOrUpdate(filter)
            }
        }
    }

    override fun deleteFilter(id: Long) {
        Realm.getDefaultInstance()
            .apply { refresh() }
            .use { realm ->
                val filter = realm.where(ForwardingFilter::class.java)
                    .equalTo("id", id)
                    .findFirst()

                realm.executeTransaction { filter?.deleteFromRealm() }
            }
    }

    override fun updateFilterEnabled(id: Long, enabled: Boolean) {
        Realm.getDefaultInstance().use { realm ->
            realm.refresh()
            val filter = realm.where(ForwardingFilter::class.java)
                .equalTo("id", id)
                .findFirst()

            realm.executeTransaction {
                filter?.enabled = enabled
                filter?.updatedAt = System.currentTimeMillis()
            }
        }
    }

    override fun incrementForwardedCount(id: Long) {
        Realm.getDefaultInstance().use { realm ->
            realm.refresh()
            val filter = realm.where(ForwardingFilter::class.java)
                .equalTo("id", id)
                .findFirst()

            realm.executeTransaction {
                filter?.forwardedCount = (filter?.forwardedCount ?: 0) + 1
                filter?.lastTriggered = System.currentTimeMillis()
            }
        }
    }

    // ==================== Logs ====================

    override fun getLogs(limit: Int): RealmResults<ForwardingLog> {
        return Realm.getDefaultInstance()
            .where(ForwardingLog::class.java)
            .sort("receivedAt", Sort.DESCENDING)
            .limit(limit.toLong())
            .findAll()
    }

    override fun getLogsByFilter(filterId: Long): RealmResults<ForwardingLog> {
        return Realm.getDefaultInstance()
            .where(ForwardingLog::class.java)
            .equalTo("filterId", filterId)
            .sort("receivedAt", Sort.DESCENDING)
            .findAll()
    }

    override fun getLog(id: Long): ForwardingLog? {
        return Realm.getDefaultInstance().use { realm ->
            realm.refresh()
            realm.where(ForwardingLog::class.java)
                .equalTo("id", id)
                .findFirst()
                ?.let { realm.copyFromRealm(it) }
        }
    }

    override fun saveLog(
        filterId: Long,
        filterName: String,
        sender: String,
        messageBody: String,
        receivedAt: Long,
        simSlot: Int,
        destinationEmail: String,
        deliveryStatus: String,
        deliveryMethod: String
    ): Long {
        return Realm.getDefaultInstance().use { realm ->
            val id = (realm.where(ForwardingLog::class.java).max("id")?.toLong() ?: -1) + 1

            val log = ForwardingLog(
                id = id,
                filterId = filterId,
                filterName = filterName,
                sender = sender,
                messageBody = messageBody,
                receivedAt = receivedAt,
                simSlot = simSlot,
                destinationEmail = destinationEmail,
                deliveryStatus = deliveryStatus,
                deliveryMethod = deliveryMethod
            )

            realm.executeTransaction { realm.insertOrUpdate(log) }
            id
        }
    }

    override fun updateLogStatus(id: Long, status: String, errorMessage: String?, sentAt: Long?) {
        Realm.getDefaultInstance().use { realm ->
            realm.refresh()
            val log = realm.where(ForwardingLog::class.java)
                .equalTo("id", id)
                .findFirst()

            realm.executeTransaction {
                log?.deliveryStatus = status
                log?.errorMessage = errorMessage
                log?.sentAt = sentAt
                if (status == "RETRY") {
                    log?.retryCount = (log?.retryCount ?: 0) + 1
                }
            }
        }
    }

    override fun deleteOldLogs(olderThanDays: Int) {
        val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)

        Realm.getDefaultInstance().use { realm ->
            realm.refresh()
            val oldLogs = realm.where(ForwardingLog::class.java)
                .lessThan("receivedAt", cutoffTime)
                .findAll()

            realm.executeTransaction {
                oldLogs.deleteAllFromRealm()
            }
        }
    }

    // ==================== Email Accounts ====================

    override fun getEmailAccounts(): RealmResults<EmailAccount> {
        return Realm.getDefaultInstance()
            .where(EmailAccount::class.java)
            .sort("createdAt")
            .findAll()
    }

    override fun getDefaultEmailAccount(): EmailAccount? {
        val realm = Realm.getDefaultInstance()
        realm.refresh()
        val count = realm.where(EmailAccount::class.java).count()
        Timber.d("ForwardingRepo: getDefaultEmailAccount - total accounts in DB: $count")

        val defaultAccount = realm.where(EmailAccount::class.java)
            .equalTo("isDefault", true)
            .findFirst()

        if (defaultAccount != null) {
            Timber.d("ForwardingRepo: Found default account: ${defaultAccount.email}")
            return defaultAccount
        }

        val anyAccount = realm.where(EmailAccount::class.java).findFirst()
        Timber.d("ForwardingRepo: No default, falling back to first account: ${anyAccount?.email}")
        return anyAccount
    }

    override fun getEmailAccount(id: Long): EmailAccount? {
        return Realm.getDefaultInstance()
            .apply { refresh() }
            .where(EmailAccount::class.java)
            .equalTo("id", id)
            .findFirst()
    }

    override fun saveEmailAccount(
        accountType: String,
        email: String,
        gmailAccountName: String?,
        smtpHost: String?,
        smtpPort: Int,
        smtpUsername: String?,
        smtpUseTls: Boolean,
        isDefault: Boolean
    ): Long {
        Timber.i("ForwardingRepo: saveEmailAccount called - email=$email, type=$accountType")
        return try {
            Realm.getDefaultInstance().use { realm ->
                val id = (realm.where(EmailAccount::class.java).max("id")?.toLong() ?: -1) + 1
                Timber.d("ForwardingRepo: Generated new account id=$id")

                // If this is the first account or marked as default, ensure it's the only default
                val existingCount = realm.where(EmailAccount::class.java).count()
                val shouldBeDefault = isDefault || existingCount == 0L
                Timber.d("ForwardingRepo: existingCount=$existingCount, shouldBeDefault=$shouldBeDefault")

                if (shouldBeDefault) {
                    // Clear existing defaults
                    realm.executeTransaction {
                        realm.where(EmailAccount::class.java)
                            .equalTo("isDefault", true)
                            .findAll()
                            .forEach { it.isDefault = false }
                    }
                }

                val account = EmailAccount(
                    id = id,
                    accountType = accountType,
                    email = email,
                    gmailAccountName = gmailAccountName,
                    smtpHost = smtpHost,
                    smtpPort = smtpPort,
                    smtpUsername = smtpUsername,
                    smtpUseTls = smtpUseTls,
                    isDefault = shouldBeDefault,
                    createdAt = System.currentTimeMillis()
                )

                realm.executeTransaction { realm.insertOrUpdate(account) }
                Timber.i("ForwardingRepo: Email account saved successfully with id=$id")
                id
            }
        } catch (e: Exception) {
            Timber.e(e, "ForwardingRepo: Failed to save email account")
            -1L
        }
    }

    override fun deleteEmailAccount(id: Long) {
        Realm.getDefaultInstance()
            .apply { refresh() }
            .use { realm ->
                val account = realm.where(EmailAccount::class.java)
                    .equalTo("id", id)
                    .findFirst()

                val wasDefault = account?.isDefault == true

                realm.executeTransaction { account?.deleteFromRealm() }

                // If we deleted the default, make the first remaining account the default
                if (wasDefault) {
                    val firstAccount = realm.where(EmailAccount::class.java).findFirst()
                    if (firstAccount != null) {
                        realm.executeTransaction { firstAccount.isDefault = true }
                    }
                }
            }
    }

    override fun setDefaultEmailAccount(id: Long) {
        Realm.getDefaultInstance().use { realm ->
            realm.refresh()

            realm.executeTransaction {
                // Clear existing defaults
                realm.where(EmailAccount::class.java)
                    .equalTo("isDefault", true)
                    .findAll()
                    .forEach { it.isDefault = false }

                // Set new default
                realm.where(EmailAccount::class.java)
                    .equalTo("id", id)
                    .findFirst()
                    ?.isDefault = true
            }
        }
    }
}
