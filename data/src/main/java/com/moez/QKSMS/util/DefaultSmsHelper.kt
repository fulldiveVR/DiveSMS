/*
 * Copyright (C) 2017 Moez Bhatti <moez.bhatti@gmail.com>
 *
 * This file is part of QKSMS.
 *
 * QKSMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * QKSMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with QKSMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.moez.QKSMS.util

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for managing default SMS app status.
 *
 * On Android 4.4+ (KitKat), only the default SMS app can:
 * - Receive SMS_DELIVER broadcasts
 * - Write to the SMS Provider
 * - Receive WAP_PUSH_DELIVER broadcasts
 *
 * This is critical for message reception - if the app is not set as default,
 * users will NOT receive incoming messages.
 */
@Singleton
class DefaultSmsHelper @Inject constructor(
    private val context: Context
) {

    companion object {
        const val REQUEST_CODE_DEFAULT_SMS = 1001
        const val REQUEST_CODE_DEFAULT_SMS_ROLE = 1002

        // Preference key for tracking if user has been prompted
        const val PREF_DEFAULT_SMS_PROMPTED = "default_sms_prompted"
        const val PREF_DEFAULT_SMS_DECLINED_COUNT = "default_sms_declined_count"
    }

    /**
     * Check if this app is the default SMS application
     */
    fun isDefaultSmsApp(): Boolean {
        return try {
            val defaultPackage = Telephony.Sms.getDefaultSmsPackage(context)
            val isDefault = defaultPackage == context.packageName
            Timber.d("Default SMS check: default=$defaultPackage, this=${context.packageName}, isDefault=$isDefault")
            isDefault
        } catch (e: Exception) {
            Timber.w(e, "Failed to check default SMS app status")
            false
        }
    }

    /**
     * Get the intent to set this app as default SMS application
     * Returns null if not supported (pre-KitKat)
     */
    fun getSetDefaultIntent(): Intent? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ uses RoleManager
                val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                    roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                } else {
                    // Fallback to legacy method
                    Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                        putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
                    }
                }
            } else {
                // Android 4.4 to 9
                Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                    putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to create default SMS intent")
            null
        }
    }

    /**
     * Request to set this app as default SMS app
     * @param activity The activity to receive the result
     */
    fun requestSetAsDefault(activity: Activity) {
        try {
            val intent = getSetDefaultIntent()
            if (intent != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    activity.startActivityForResult(intent, REQUEST_CODE_DEFAULT_SMS_ROLE)
                } else {
                    activity.startActivityForResult(intent, REQUEST_CODE_DEFAULT_SMS)
                }
                Timber.d("Launched default SMS app request")
            } else {
                Timber.w("Unable to create default SMS intent")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to request default SMS app")
        }
    }

    /**
     * Get a user-friendly message explaining why setting as default is important
     */
    fun getDefaultSmsExplanation(): String {
        return "To receive text messages, Wize SMS must be set as your default messaging app. " +
                "Without this, you will not receive incoming SMS or MMS messages."
    }

    /**
     * Get a short warning message for the app banner
     */
    fun getDefaultSmsWarning(): String {
        return "Wize SMS is not your default messaging app. You may miss incoming messages."
    }

    /**
     * Log the current default SMS app status for debugging
     */
    fun logDefaultSmsStatus() {
        try {
            val defaultPackage = Telephony.Sms.getDefaultSmsPackage(context)
            val isDefault = isDefaultSmsApp()
            Timber.i(
                "Default SMS Status: " +
                        "\n  Current default: $defaultPackage" +
                        "\n  This app: ${context.packageName}" +
                        "\n  Is default: $isDefault"
            )
        } catch (e: Exception) {
            Timber.w(e, "Failed to log default SMS status")
        }
    }

    /**
     * Check if we should show the default SMS prompt
     * This considers if the user has previously declined
     */
    fun shouldShowDefaultPrompt(declinedCount: Int, hasBeenPrompted: Boolean): Boolean {
        // Always show if not default
        if (isDefaultSmsApp()) return false

        // First time users should always see prompt
        if (!hasBeenPrompted) return true

        // For users who declined, show less frequently
        // Show on: 1st decline, 3rd launch after, 7th launch after, etc.
        return when {
            declinedCount == 0 -> true
            declinedCount == 1 -> true // Show once more after first decline
            declinedCount < 5 -> false // Don't nag too much
            else -> declinedCount % 10 == 0 // Occasional reminder
        }
    }
}
