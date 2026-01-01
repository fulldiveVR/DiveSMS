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

package com.moez.QKSMS.manager

import android.content.Context
import android.content.SharedPreferences
import com.moez.QKSMS.model.ForwardingStatus
import com.moez.QKSMS.model.ForwardingType
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages forwarding status tracking, failure counting, and status updates.
 * Provides reactive streams for UI updates.
 */
@Singleton
class ForwardingStatusManager @Inject constructor(
    context: Context
) : ForwardingStatusReporter {
    companion object {
        private const val PREFS_NAME = "forwarding_status"
        private const val KEY_EMAIL_FAIL_COUNT = "email_fail_count"
        private const val KEY_EMAIL_LAST_ERROR = "email_last_error"
        private const val KEY_EMAIL_AUTH_REQUIRED = "email_auth_required"
        private const val KEY_TELEGRAM_FAIL_COUNT = "telegram_fail_count"
        private const val KEY_TELEGRAM_LAST_ERROR = "telegram_last_error"
        private const val KEY_TELEGRAM_AUTH_REQUIRED = "telegram_auth_required"

        // Number of failures before showing notification
        const val FAILURE_THRESHOLD = 3
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Reactive subjects for status updates
    private val emailStatusSubject = BehaviorSubject.create<ForwardingStatus>()
    private val telegramStatusSubject = BehaviorSubject.create<ForwardingStatus>()

    init {
        // Initialize with current status
        emailStatusSubject.onNext(calculateEmailStatus())
        telegramStatusSubject.onNext(calculateTelegramStatus())
    }

    /**
     * Observable stream of email forwarding status changes.
     */
    val emailStatus: Observable<ForwardingStatus> = emailStatusSubject.distinctUntilChanged()

    /**
     * Observable stream of Telegram forwarding status changes.
     */
    val telegramStatus: Observable<ForwardingStatus> = telegramStatusSubject.distinctUntilChanged()

    /**
     * Get current email forwarding status.
     */
    fun getEmailStatus(): ForwardingStatus = calculateEmailStatus()

    /**
     * Get current Telegram forwarding status.
     */
    fun getTelegramStatus(): ForwardingStatus = calculateTelegramStatus()

    /**
     * Report a successful forward.
     */
    override fun reportSuccess(type: ForwardingType) {
        Timber.d("ForwardingStatusManager: Reporting success for $type")
        when (type) {
            ForwardingType.EMAIL -> {
                prefs.edit()
                    .putInt(KEY_EMAIL_FAIL_COUNT, 0)
                    .remove(KEY_EMAIL_LAST_ERROR)
                    .putBoolean(KEY_EMAIL_AUTH_REQUIRED, false)
                    .apply()
                emailStatusSubject.onNext(calculateEmailStatus())
            }
            ForwardingType.TELEGRAM -> {
                prefs.edit()
                    .putInt(KEY_TELEGRAM_FAIL_COUNT, 0)
                    .remove(KEY_TELEGRAM_LAST_ERROR)
                    .putBoolean(KEY_TELEGRAM_AUTH_REQUIRED, false)
                    .apply()
                telegramStatusSubject.onNext(calculateTelegramStatus())
            }
        }
    }

    /**
     * Report a failed forward attempt.
     * @param type The forwarding type
     * @param error Error message
     * @param isAuthError Whether this is an authentication error
     * @return The new failure count
     */
    override fun reportFailure(type: ForwardingType, error: String, isAuthError: Boolean): Int {
        Timber.d("ForwardingStatusManager: Reporting failure for $type: $error (auth=$isAuthError)")
        return when (type) {
            ForwardingType.EMAIL -> {
                val newCount = getEmailFailCount() + 1
                prefs.edit()
                    .putInt(KEY_EMAIL_FAIL_COUNT, newCount)
                    .putString(KEY_EMAIL_LAST_ERROR, error)
                    .putBoolean(KEY_EMAIL_AUTH_REQUIRED, isAuthError)
                    .apply()
                emailStatusSubject.onNext(calculateEmailStatus())
                newCount
            }
            ForwardingType.TELEGRAM -> {
                val newCount = getTelegramFailCount() + 1
                prefs.edit()
                    .putInt(KEY_TELEGRAM_FAIL_COUNT, newCount)
                    .putString(KEY_TELEGRAM_LAST_ERROR, error)
                    .putBoolean(KEY_TELEGRAM_AUTH_REQUIRED, isAuthError)
                    .apply()
                telegramStatusSubject.onNext(calculateTelegramStatus())
                newCount
            }
        }
    }

    /**
     * Mark a service as needing reconnection (e.g., auth expired).
     */
    fun markNeedsReconnect(type: ForwardingType) {
        Timber.d("ForwardingStatusManager: Marking $type as needs reconnect")
        when (type) {
            ForwardingType.EMAIL -> {
                prefs.edit()
                    .putBoolean(KEY_EMAIL_AUTH_REQUIRED, true)
                    .apply()
                emailStatusSubject.onNext(calculateEmailStatus())
            }
            ForwardingType.TELEGRAM -> {
                prefs.edit()
                    .putBoolean(KEY_TELEGRAM_AUTH_REQUIRED, true)
                    .apply()
                telegramStatusSubject.onNext(calculateTelegramStatus())
            }
        }
    }

    /**
     * Clear all failure state for a service (e.g., after user reconnects).
     */
    fun clearFailures(type: ForwardingType) {
        Timber.d("ForwardingStatusManager: Clearing failures for $type")
        when (type) {
            ForwardingType.EMAIL -> {
                prefs.edit()
                    .putInt(KEY_EMAIL_FAIL_COUNT, 0)
                    .remove(KEY_EMAIL_LAST_ERROR)
                    .putBoolean(KEY_EMAIL_AUTH_REQUIRED, false)
                    .apply()
                emailStatusSubject.onNext(calculateEmailStatus())
            }
            ForwardingType.TELEGRAM -> {
                prefs.edit()
                    .putInt(KEY_TELEGRAM_FAIL_COUNT, 0)
                    .remove(KEY_TELEGRAM_LAST_ERROR)
                    .putBoolean(KEY_TELEGRAM_AUTH_REQUIRED, false)
                    .apply()
                telegramStatusSubject.onNext(calculateTelegramStatus())
            }
        }
    }

    /**
     * Check if notification should be shown based on failure count.
     */
    override fun shouldShowNotification(type: ForwardingType): Boolean {
        val failCount = when (type) {
            ForwardingType.EMAIL -> getEmailFailCount()
            ForwardingType.TELEGRAM -> getTelegramFailCount()
        }
        // Show notification after threshold OR on auth error
        return failCount >= FAILURE_THRESHOLD || isAuthRequired(type)
    }

    /**
     * Get the failure count for a type.
     */
    fun getFailCount(type: ForwardingType): Int {
        return when (type) {
            ForwardingType.EMAIL -> getEmailFailCount()
            ForwardingType.TELEGRAM -> getTelegramFailCount()
        }
    }

    /**
     * Check if auth is required for a type.
     */
    fun isAuthRequired(type: ForwardingType): Boolean {
        return when (type) {
            ForwardingType.EMAIL -> prefs.getBoolean(KEY_EMAIL_AUTH_REQUIRED, false)
            ForwardingType.TELEGRAM -> prefs.getBoolean(KEY_TELEGRAM_AUTH_REQUIRED, false)
        }
    }

    /**
     * Get the last error message for a type.
     */
    fun getLastError(type: ForwardingType): String? {
        return when (type) {
            ForwardingType.EMAIL -> prefs.getString(KEY_EMAIL_LAST_ERROR, null)
            ForwardingType.TELEGRAM -> prefs.getString(KEY_TELEGRAM_LAST_ERROR, null)
        }
    }

    private fun getEmailFailCount(): Int = prefs.getInt(KEY_EMAIL_FAIL_COUNT, 0)
    private fun getTelegramFailCount(): Int = prefs.getInt(KEY_TELEGRAM_FAIL_COUNT, 0)

    private fun calculateEmailStatus(): ForwardingStatus {
        val failCount = getEmailFailCount()
        val authRequired = prefs.getBoolean(KEY_EMAIL_AUTH_REQUIRED, false)
        val lastError = prefs.getString(KEY_EMAIL_LAST_ERROR, null)

        return when {
            authRequired && failCount > 0 -> ForwardingStatus.NeedsReconnect(failCount)
            authRequired -> ForwardingStatus.Disconnected(failCount)
            failCount > 0 -> ForwardingStatus.Pending(failCount, lastError)
            else -> ForwardingStatus.Connected
        }
    }

    private fun calculateTelegramStatus(): ForwardingStatus {
        val failCount = getTelegramFailCount()
        val authRequired = prefs.getBoolean(KEY_TELEGRAM_AUTH_REQUIRED, false)
        val lastError = prefs.getString(KEY_TELEGRAM_LAST_ERROR, null)

        return when {
            authRequired && failCount > 0 -> ForwardingStatus.NeedsReconnect(failCount)
            authRequired -> ForwardingStatus.Disconnected(failCount)
            failCount > 0 -> ForwardingStatus.Pending(failCount, lastError)
            else -> ForwardingStatus.Connected
        }
    }
}
