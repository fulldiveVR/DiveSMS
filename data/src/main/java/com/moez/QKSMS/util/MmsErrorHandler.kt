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

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.os.StatFs
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Error codes for MMS operations
 */
enum class MmsErrorCode(val code: Int) {
    SUCCESS(0),
    IMAGE_TOO_LARGE(1001),
    COMPRESSION_FAILED(1002),
    NO_DATA_CONNECTION(1003),
    WIFI_ONLY_NOT_SUPPORTED(1004),
    PERMISSION_DENIED(1005),
    STORAGE_FULL(1006),
    CARRIER_TIMEOUT(1007),
    MMS_NOT_SUPPORTED(1008),
    INVALID_ATTACHMENT(1009),
    NETWORK_ERROR(1010),
    UNKNOWN_ERROR(9999)
}

/**
 * Data class representing an MMS error with user-friendly messaging
 */
data class MmsError(
    val code: MmsErrorCode,
    val userMessage: String,
    val technicalDetails: String,
    val isRetryable: Boolean,
    val suggestedAction: String?
)

/**
 * Handles MMS-related errors with user-friendly messaging and logging
 */
@Singleton
class MmsErrorHandler @Inject constructor(
    private val context: Context
) {

    companion object {
        // Minimum storage required for MMS operations (50MB)
        private const val MIN_STORAGE_BYTES = 50L * 1024 * 1024

        // Default MMS size limit if carrier config unavailable
        const val DEFAULT_MMS_SIZE_LIMIT = 1024 * 1024 // 1MB
    }

    /**
     * Check network connectivity and return appropriate error if not suitable for MMS
     */
    fun checkNetworkForMms(): MmsError? {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return createError(MmsErrorCode.NETWORK_ERROR, "Unable to check network status")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)

            if (capabilities == null) {
                return createError(MmsErrorCode.NO_DATA_CONNECTION, "No active network connection")
            }

            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val hasCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            val hasWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)

            if (!hasInternet) {
                return createError(MmsErrorCode.NO_DATA_CONNECTION, "No internet connection available")
            }

            // MMS typically requires cellular data, but some carriers support WiFi calling/MMS
            if (hasWifi && !hasCellular) {
                Timber.d("Device on WiFi only - MMS may not work on all carriers")
                // Don't return error, just log - some carriers support WiFi MMS
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            if (networkInfo == null || !networkInfo.isConnected) {
                return createError(MmsErrorCode.NO_DATA_CONNECTION, "No network connection")
            }
        }

        return null
    }

    /**
     * Check if device has sufficient storage for MMS operations
     */
    fun checkStorageForMms(): MmsError? {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val availableBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                stat.availableBlocksLong * stat.blockSizeLong
            } else {
                @Suppress("DEPRECATION")
                stat.availableBlocks.toLong() * stat.blockSize.toLong()
            }

            if (availableBytes < MIN_STORAGE_BYTES) {
                createError(
                    MmsErrorCode.STORAGE_FULL,
                    "Available: ${availableBytes / (1024 * 1024)}MB, Required: ${MIN_STORAGE_BYTES / (1024 * 1024)}MB"
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to check storage")
            null // Don't block MMS if we can't check storage
        }
    }

    /**
     * Create an MmsError with user-friendly messaging based on error code
     */
    fun createError(code: MmsErrorCode, technicalDetails: String = ""): MmsError {
        return when (code) {
            MmsErrorCode.SUCCESS -> MmsError(
                code = code,
                userMessage = "Message sent successfully",
                technicalDetails = technicalDetails,
                isRetryable = false,
                suggestedAction = null
            )

            MmsErrorCode.IMAGE_TOO_LARGE -> MmsError(
                code = code,
                userMessage = "Image is too large to send. It will be automatically compressed.",
                technicalDetails = technicalDetails,
                isRetryable = true,
                suggestedAction = "The app will compress the image to fit carrier limits."
            )

            MmsErrorCode.COMPRESSION_FAILED -> MmsError(
                code = code,
                userMessage = "Unable to compress image to fit carrier size limits. Please try a smaller image.",
                technicalDetails = technicalDetails,
                isRetryable = false,
                suggestedAction = "Try selecting a smaller image or reducing the number of attachments."
            )

            MmsErrorCode.NO_DATA_CONNECTION -> MmsError(
                code = code,
                userMessage = "No data connection. MMS requires mobile data or WiFi.",
                technicalDetails = technicalDetails,
                isRetryable = true,
                suggestedAction = "Please check your internet connection and try again."
            )

            MmsErrorCode.WIFI_ONLY_NOT_SUPPORTED -> MmsError(
                code = code,
                userMessage = "MMS may not work over WiFi with your carrier. Try using mobile data.",
                technicalDetails = technicalDetails,
                isRetryable = true,
                suggestedAction = "Switch to mobile data and try again."
            )

            MmsErrorCode.PERMISSION_DENIED -> MmsError(
                code = code,
                userMessage = "Permission required to send attachments. Please grant storage access in Settings.",
                technicalDetails = technicalDetails,
                isRetryable = false,
                suggestedAction = "Go to Settings > Apps > Wize SMS > Permissions and enable Storage access."
            )

            MmsErrorCode.STORAGE_FULL -> MmsError(
                code = code,
                userMessage = "Device storage is full. Please free up space to send images.",
                technicalDetails = technicalDetails,
                isRetryable = false,
                suggestedAction = "Delete unused apps or files to free up storage space."
            )

            MmsErrorCode.CARRIER_TIMEOUT -> MmsError(
                code = code,
                userMessage = "Message sending timed out. Your carrier may be experiencing issues.",
                technicalDetails = technicalDetails,
                isRetryable = true,
                suggestedAction = "Check your signal strength and try again in a few minutes."
            )

            MmsErrorCode.MMS_NOT_SUPPORTED -> MmsError(
                code = code,
                userMessage = "MMS is not supported by your carrier or SIM card.",
                technicalDetails = technicalDetails,
                isRetryable = false,
                suggestedAction = "Contact your carrier to verify MMS is enabled on your plan."
            )

            MmsErrorCode.INVALID_ATTACHMENT -> MmsError(
                code = code,
                userMessage = "This attachment type is not supported for MMS.",
                technicalDetails = technicalDetails,
                isRetryable = false,
                suggestedAction = "Try sending as a different file type (JPEG, PNG, or GIF)."
            )

            MmsErrorCode.NETWORK_ERROR -> MmsError(
                code = code,
                userMessage = "Network error occurred while sending. Please try again.",
                technicalDetails = technicalDetails,
                isRetryable = true,
                suggestedAction = "Check your connection and try again."
            )

            MmsErrorCode.UNKNOWN_ERROR -> MmsError(
                code = code,
                userMessage = "An unexpected error occurred. Please try again.",
                technicalDetails = technicalDetails,
                isRetryable = true,
                suggestedAction = "If the problem persists, try restarting the app."
            )
        }
    }

    /**
     * Log an MMS error for debugging purposes
     */
    fun logError(error: MmsError, additionalContext: Map<String, Any>? = null) {
        val logBuilder = StringBuilder()
        logBuilder.append("MMS Error [${error.code}]: ${error.userMessage}")
        logBuilder.append("\n  Technical: ${error.technicalDetails}")
        logBuilder.append("\n  Retryable: ${error.isRetryable}")

        additionalContext?.forEach { (key, value) ->
            logBuilder.append("\n  $key: $value")
        }

        when {
            error.code == MmsErrorCode.SUCCESS -> Timber.d(logBuilder.toString())
            error.isRetryable -> Timber.w(logBuilder.toString())
            else -> Timber.e(logBuilder.toString())
        }
    }

    /**
     * Get carrier-specific MMS size limit with fallback
     */
    fun getMmsSizeLimit(carrierLimit: Int?): Int {
        return when {
            carrierLimit != null && carrierLimit > 0 -> carrierLimit
            else -> DEFAULT_MMS_SIZE_LIMIT
        }
    }

    /**
     * Format file size for user display
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes bytes"
        }
    }
}
