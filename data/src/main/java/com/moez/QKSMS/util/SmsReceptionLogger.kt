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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import androidx.core.content.ContextCompat
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Event types for SMS/MMS reception logging
 */
enum class SmsEventType {
    SMS_RECEIVED,
    SMS_STORED,
    SMS_DISPLAYED,
    MMS_RECEIVED,
    MMS_STORED,
    MMS_DISPLAYED,
    GROUP_MMS_RECEIVED,
    PERMISSION_CHECK,
    DEFAULT_APP_CHECK,
    BROADCAST_RECEIVED,
    DATABASE_INSERT,
    DATABASE_ERROR,
    UI_UPDATE,
    NOTIFICATION_SENT,
    BLOCKING_CHECK,
    ERROR
}

/**
 * Data class representing a logged SMS/MMS event
 * Note: Does NOT include message content for privacy
 */
data class SmsLogEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: SmsEventType,
    val status: String,
    val senderHash: String? = null, // Hashed for privacy
    val messageLength: Int? = null,
    val attachmentCount: Int? = null,
    val isGroupMessage: Boolean = false,
    val recipientCount: Int? = null,
    val errorDetails: String? = null,
    val additionalInfo: Map<String, String>? = null
) {
    fun toLogString(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        val formattedTime = dateFormat.format(Date(timestamp))
        val builder = StringBuilder()
        builder.append("[$formattedTime] $eventType | Status: $status")
        senderHash?.let { builder.append(" | From: $it") }
        messageLength?.let { builder.append(" | Length: $it") }
        attachmentCount?.let { builder.append(" | Attachments: $it") }
        if (isGroupMessage) {
            builder.append(" | Group: YES")
            recipientCount?.let { builder.append(" ($it recipients)") }
        }
        errorDetails?.let { builder.append(" | Error: $it") }
        additionalInfo?.forEach { (key, value) ->
            builder.append(" | $key: $value")
        }
        return builder.toString()
    }
}

/**
 * Logs SMS/MMS reception events for debugging purposes.
 * Stores logs in memory with a maximum limit.
 * Does NOT log message content for privacy.
 */
@Singleton
class SmsReceptionLogger @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val MAX_LOG_ENTRIES = 500
        private const val HASH_PREFIX_LENGTH = 8 // Show only first 8 chars of hash
    }

    private val logQueue = ConcurrentLinkedQueue<SmsLogEvent>()

    /**
     * Log an SMS received event
     */
    fun logSmsReceived(sender: String, bodyLength: Int, subId: Int) {
        log(SmsLogEvent(
            eventType = SmsEventType.SMS_RECEIVED,
            status = "RECEIVED",
            senderHash = hashSender(sender),
            messageLength = bodyLength,
            additionalInfo = mapOf("subId" to subId.toString())
        ))
    }

    /**
     * Log an SMS stored to database event
     */
    fun logSmsStored(messageId: Long, threadId: Long) {
        log(SmsLogEvent(
            eventType = SmsEventType.SMS_STORED,
            status = "SUCCESS",
            additionalInfo = mapOf(
                "messageId" to messageId.toString(),
                "threadId" to threadId.toString()
            )
        ))
    }

    /**
     * Log an MMS received event
     */
    fun logMmsReceived(sender: String, attachmentCount: Int, isGroup: Boolean, recipientCount: Int) {
        log(SmsLogEvent(
            eventType = if (isGroup) SmsEventType.GROUP_MMS_RECEIVED else SmsEventType.MMS_RECEIVED,
            status = "RECEIVED",
            senderHash = hashSender(sender),
            attachmentCount = attachmentCount,
            isGroupMessage = isGroup,
            recipientCount = if (isGroup) recipientCount else null
        ))
    }

    /**
     * Log an MMS stored to database event
     */
    fun logMmsStored(messageId: Long, threadId: Long, partCount: Int) {
        log(SmsLogEvent(
            eventType = SmsEventType.MMS_STORED,
            status = "SUCCESS",
            attachmentCount = partCount,
            additionalInfo = mapOf(
                "messageId" to messageId.toString(),
                "threadId" to threadId.toString()
            )
        ))
    }

    /**
     * Log permission check result
     */
    fun logPermissionCheck(permission: String, granted: Boolean) {
        log(SmsLogEvent(
            eventType = SmsEventType.PERMISSION_CHECK,
            status = if (granted) "GRANTED" else "DENIED",
            additionalInfo = mapOf("permission" to permission)
        ))
    }

    /**
     * Log default app check result
     */
    fun logDefaultAppCheck(isDefault: Boolean, currentDefault: String?) {
        log(SmsLogEvent(
            eventType = SmsEventType.DEFAULT_APP_CHECK,
            status = if (isDefault) "IS_DEFAULT" else "NOT_DEFAULT",
            additionalInfo = mapOf("currentDefault" to (currentDefault ?: "unknown"))
        ))
    }

    /**
     * Log a broadcast receiver event
     */
    fun logBroadcastReceived(action: String) {
        log(SmsLogEvent(
            eventType = SmsEventType.BROADCAST_RECEIVED,
            status = "RECEIVED",
            additionalInfo = mapOf("action" to action)
        ))
    }

    /**
     * Log database error
     */
    fun logDatabaseError(operation: String, error: String) {
        log(SmsLogEvent(
            eventType = SmsEventType.DATABASE_ERROR,
            status = "ERROR",
            errorDetails = error,
            additionalInfo = mapOf("operation" to operation)
        ))
    }

    /**
     * Log a generic error
     */
    fun logError(eventType: SmsEventType, error: String, additionalInfo: Map<String, String>? = null) {
        log(SmsLogEvent(
            eventType = eventType,
            status = "ERROR",
            errorDetails = error,
            additionalInfo = additionalInfo
        ))
    }

    /**
     * Log blocking check result
     */
    fun logBlockingCheck(sender: String, shouldBlock: Boolean, reason: String?) {
        log(SmsLogEvent(
            eventType = SmsEventType.BLOCKING_CHECK,
            status = if (shouldBlock) "BLOCKED" else "ALLOWED",
            senderHash = hashSender(sender),
            additionalInfo = reason?.let { mapOf("reason" to it) }
        ))
    }

    /**
     * Log notification sent
     */
    fun logNotificationSent(threadId: Long, messageCount: Int) {
        log(SmsLogEvent(
            eventType = SmsEventType.NOTIFICATION_SENT,
            status = "SENT",
            additionalInfo = mapOf(
                "threadId" to threadId.toString(),
                "messageCount" to messageCount.toString()
            )
        ))
    }

    /**
     * Add event to the log queue
     */
    private fun log(event: SmsLogEvent) {
        // Ensure we don't exceed max entries
        while (logQueue.size >= MAX_LOG_ENTRIES) {
            logQueue.poll()
        }
        logQueue.add(event)
        Timber.d(event.toLogString())
    }

    /**
     * Get all log entries (most recent first)
     */
    fun getLogs(): List<SmsLogEvent> {
        return logQueue.toList().reversed()
    }

    /**
     * Get logs filtered by event type
     */
    fun getLogsByType(eventType: SmsEventType): List<SmsLogEvent> {
        return getLogs().filter { it.eventType == eventType }
    }

    /**
     * Get recent error logs
     */
    fun getErrorLogs(): List<SmsLogEvent> {
        return getLogs().filter { it.status == "ERROR" || it.eventType == SmsEventType.ERROR }
    }

    /**
     * Export logs as formatted text
     */
    fun exportLogsAsText(): String {
        val builder = StringBuilder()
        builder.appendLine("=== Wize SMS Debug Log ===")
        builder.appendLine("Export time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        builder.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        builder.appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        builder.appendLine()

        // Add permission status
        builder.appendLine("=== Permission Status ===")
        listOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_MMS
        ).forEach { permission ->
            val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            builder.appendLine("$permission: ${if (granted) "GRANTED" else "DENIED"}")
        }
        builder.appendLine()

        // Add default app status
        builder.appendLine("=== Default App Status ===")
        try {
            val defaultSms = Telephony.Sms.getDefaultSmsPackage(context)
            val isDefault = defaultSms == context.packageName
            builder.appendLine("Default SMS app: $defaultSms")
            builder.appendLine("Is Wize SMS default: $isDefault")
        } catch (e: Exception) {
            builder.appendLine("Unable to check default SMS app: ${e.message}")
        }
        builder.appendLine()

        // Add statistics
        builder.appendLine("=== Statistics ===")
        val logs = getLogs()
        builder.appendLine("Total events logged: ${logs.size}")
        builder.appendLine("SMS received: ${logs.count { it.eventType == SmsEventType.SMS_RECEIVED }}")
        builder.appendLine("MMS received: ${logs.count { it.eventType == SmsEventType.MMS_RECEIVED }}")
        builder.appendLine("Group MMS received: ${logs.count { it.eventType == SmsEventType.GROUP_MMS_RECEIVED }}")
        builder.appendLine("Errors: ${logs.count { it.status == "ERROR" }}")
        builder.appendLine()

        // Add recent logs
        builder.appendLine("=== Recent Events (last 100) ===")
        logs.take(100).forEach { event ->
            builder.appendLine(event.toLogString())
        }

        return builder.toString()
    }

    /**
     * Clear all logs
     */
    fun clearLogs() {
        logQueue.clear()
        Timber.d("SMS reception logs cleared")
    }

    /**
     * Hash sender address for privacy
     */
    private fun hashSender(sender: String): String {
        return try {
            val hash = sender.hashCode().toString(16)
            if (hash.length > HASH_PREFIX_LENGTH) {
                hash.substring(0, HASH_PREFIX_LENGTH) + "..."
            } else {
                hash
            }
        } catch (e: Exception) {
            "unknown"
        }
    }
}
