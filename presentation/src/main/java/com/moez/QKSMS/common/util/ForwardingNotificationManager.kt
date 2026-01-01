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

package com.moez.QKSMS.common.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.fulldive.extension.divesms.R
import com.moez.QKSMS.common.util.extensions.PENDING_INTENT_FLAG
import com.moez.QKSMS.feature.forwarding.ForwardingActivity
import com.moez.QKSMS.manager.ForwardingStatusManager
import com.moez.QKSMS.model.ForwardingType
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages notifications for forwarding status issues.
 * Shows a single persistent notification when forwarding failures exceed threshold.
 */
@Singleton
class ForwardingNotificationManager @Inject constructor(
    private val context: Context,
    private val statusManager: ForwardingStatusManager,
    private val colors: Colors
) {
    companion object {
        const val FORWARDING_CHANNEL_ID = "forwarding_status"
        const val EMAIL_NOTIFICATION_ID = 200001
        const val TELEGRAM_NOTIFICATION_ID = 200002
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FORWARDING_CHANNEL_ID,
                context.getString(R.string.forwarding_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.forwarding_notification_channel_description)
                enableVibration(false)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Update the notification for a forwarding type based on current status.
     * Shows notification if failures exceed threshold, dismisses if resolved.
     */
    fun updateNotification(type: ForwardingType) {
        val failCount = statusManager.getFailCount(type)
        val isAuthRequired = statusManager.isAuthRequired(type)
        val shouldShow = statusManager.shouldShowNotification(type)

        Timber.d("ForwardingNotificationManager: updateNotification type=$type, failCount=$failCount, authRequired=$isAuthRequired, shouldShow=$shouldShow")

        if (shouldShow) {
            showNotification(type, failCount, isAuthRequired)
        } else {
            dismissNotification(type)
        }
    }

    /**
     * Show a notification for forwarding issues.
     */
    private fun showNotification(type: ForwardingType, failCount: Int, isAuthRequired: Boolean) {
        val notificationId = when (type) {
            ForwardingType.EMAIL -> EMAIL_NOTIFICATION_ID
            ForwardingType.TELEGRAM -> TELEGRAM_NOTIFICATION_ID
        }

        val title = when {
            isAuthRequired && type == ForwardingType.EMAIL ->
                context.getString(R.string.forwarding_notification_email_auth_title)
            isAuthRequired && type == ForwardingType.TELEGRAM ->
                context.getString(R.string.forwarding_notification_telegram_auth_title)
            type == ForwardingType.EMAIL ->
                context.getString(R.string.forwarding_notification_email_failed_title)
            else ->
                context.getString(R.string.forwarding_notification_telegram_failed_title)
        }

        val content = when {
            isAuthRequired ->
                context.getString(R.string.forwarding_notification_auth_content)
            else ->
                context.resources.getQuantityString(
                    R.plurals.forwarding_notification_failed_content,
                    failCount,
                    failCount
                )
        }

        val actionText = when {
            isAuthRequired -> context.getString(R.string.forwarding_notification_action_reconnect)
            else -> context.getString(R.string.forwarding_notification_action_open)
        }

        // Intent to open ForwardingActivity
        val intent = Intent(context, ForwardingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("forwarding_type", type.name)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PENDING_INTENT_FLAG
        )

        val smallIcon = when (type) {
            ForwardingType.EMAIL -> R.drawable.ic_message_black_24dp
            ForwardingType.TELEGRAM -> R.drawable.ic_send_black_24dp
        }

        val notification = NotificationCompat.Builder(context, FORWARDING_CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setColor(colors.theme().theme)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_settings_black_24dp,
                actionText,
                pendingIntent
            )
            .build()

        notificationManager.notify(notificationId, notification)
        Timber.d("ForwardingNotificationManager: Showed notification for $type")
    }

    /**
     * Dismiss notification for a forwarding type.
     */
    fun dismissNotification(type: ForwardingType) {
        val notificationId = when (type) {
            ForwardingType.EMAIL -> EMAIL_NOTIFICATION_ID
            ForwardingType.TELEGRAM -> TELEGRAM_NOTIFICATION_ID
        }
        notificationManager.cancel(notificationId)
        Timber.d("ForwardingNotificationManager: Dismissed notification for $type")
    }

    /**
     * Dismiss all forwarding notifications.
     */
    fun dismissAllNotifications() {
        notificationManager.cancel(EMAIL_NOTIFICATION_ID)
        notificationManager.cancel(TELEGRAM_NOTIFICATION_ID)
    }
}
