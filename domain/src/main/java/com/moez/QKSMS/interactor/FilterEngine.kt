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

package com.moez.QKSMS.interactor

import android.telephony.PhoneNumberUtils
import com.moez.QKSMS.model.ForwardingFilter
import com.moez.QKSMS.repository.ContactRepository
import com.moez.QKSMS.repository.ForwardingRepository
import timber.log.Timber
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Engine for matching incoming SMS messages against forwarding filters.
 */
class FilterEngine @Inject constructor(
    private val forwardingRepo: ForwardingRepository,
    private val contactRepo: ContactRepository
) {
    /**
     * Data class representing an incoming SMS message.
     */
    data class SmsData(
        val sender: String,
        val body: String,
        val timestamp: Long,
        val simSlot: Int
    )

    /**
     * Finds all filters that match the given SMS data.
     * Returns filters sorted by priority (lower priority number = higher priority).
     */
    fun findMatchingFilters(sms: SmsData): List<ForwardingFilter> {
        val activeFilters = forwardingRepo.getActiveFilters()

        if (activeFilters.isEmpty()) {
            Timber.d("FilterEngine: No active filters")
            return emptyList()
        }

        Timber.d("FilterEngine: Checking ${activeFilters.size} active filters for SMS from ${sms.sender}")

        val matchingFilters = activeFilters.filter { filter ->
            val senderMatch = matchesSender(sms, filter)
            val contentMatch = matchesContent(sms, filter)
            val timeMatch = matchesTimeWindow(sms, filter)
            val simMatch = matchesSimSlot(sms, filter)

            val matches = senderMatch && contentMatch && timeMatch && simMatch

            if (matches) {
                Timber.d("FilterEngine: Filter '${filter.name}' matches SMS from ${sms.sender}")
            }

            // Handle include/exclude logic
            if (filter.isIncludeFilter) {
                matches
            } else {
                !matches // Exclude filter: match if conditions are NOT met
            }
        }.sortedBy { it.priority }

        Timber.d("FilterEngine: ${matchingFilters.size} filters matched")
        return matchingFilters
    }

    /**
     * Check if SMS sender matches the filter's sender criteria.
     */
    private fun matchesSender(sms: SmsData, filter: ForwardingFilter): Boolean {
        // Empty sender filter means match all
        if (filter.senderFilter.isBlank()) {
            return true
        }

        val senderFilter = filter.senderFilter.trim()

        return when (filter.senderMatchType.uppercase()) {
            "EXACT" -> {
                // Exact phone number match (normalized)
                PhoneNumberUtils.compare(sms.sender, senderFilter)
            }
            "CONTAINS" -> {
                // Check if sender contains any of the comma-separated keywords
                val keywords = senderFilter.split(",").map { it.trim().lowercase() }
                val senderLower = sms.sender.lowercase()
                val contactName = getContactName(sms.sender)?.lowercase() ?: ""

                keywords.any { keyword ->
                    senderLower.contains(keyword) || contactName.contains(keyword)
                }
            }
            "REGEX" -> {
                try {
                    val regex = senderFilter.toRegex(RegexOption.IGNORE_CASE)
                    regex.containsMatchIn(sms.sender) ||
                            getContactName(sms.sender)?.let { regex.containsMatchIn(it) } == true
                } catch (e: Exception) {
                    Timber.w(e, "FilterEngine: Invalid regex pattern: $senderFilter")
                    false
                }
            }
            else -> {
                // Default to CONTAINS behavior
                sms.sender.contains(senderFilter, ignoreCase = true)
            }
        }
    }

    /**
     * Check if SMS content matches the filter's content criteria.
     */
    private fun matchesContent(sms: SmsData, filter: ForwardingFilter): Boolean {
        // Empty content filter means match all
        if (filter.contentFilter.isBlank()) {
            return true
        }

        val contentFilter = filter.contentFilter.trim()

        return when (filter.contentMatchType.uppercase()) {
            "EXACT" -> {
                sms.body.equals(contentFilter, ignoreCase = true)
            }
            "CONTAINS" -> {
                // Check if body contains any of the comma-separated keywords
                val keywords = contentFilter.split(",").map { it.trim().lowercase() }
                val bodyLower = sms.body.lowercase()

                keywords.any { keyword ->
                    bodyLower.contains(keyword)
                }
            }
            "REGEX" -> {
                try {
                    val regex = contentFilter.toRegex(RegexOption.IGNORE_CASE)
                    regex.containsMatchIn(sms.body)
                } catch (e: Exception) {
                    Timber.w(e, "FilterEngine: Invalid regex pattern: $contentFilter")
                    false
                }
            }
            else -> {
                // Default to CONTAINS behavior
                sms.body.contains(contentFilter, ignoreCase = true)
            }
        }
    }

    /**
     * Check if current time is within the filter's time window.
     */
    private fun matchesTimeWindow(sms: SmsData, filter: ForwardingFilter): Boolean {
        // If time window is not enabled, always match
        if (!filter.timeWindowEnabled) {
            return true
        }

        val startTime = filter.startTime
        val endTime = filter.endTime

        // If times are not set, match all
        if (startTime.isNullOrBlank() || endTime.isNullOrBlank()) {
            return true
        }

        try {
            val now = LocalDateTime.now()
            val currentTime = now.toLocalTime()
            val currentDay = now.dayOfWeek

            // Check if current day is in active days
            if (filter.activeDays.isNotBlank()) {
                val activeDays = filter.activeDays.split(",")
                    .map { it.trim().uppercase() }
                    .mapNotNull { dayName ->
                        try {
                            DayOfWeek.valueOf(dayName)
                        } catch (e: Exception) {
                            null
                        }
                    }

                if (activeDays.isNotEmpty() && currentDay !in activeDays) {
                    return false
                }
            }

            // Parse time window
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val start = LocalTime.parse(startTime, formatter)
            val end = LocalTime.parse(endTime, formatter)

            // Check if current time is within window
            return if (start.isBefore(end)) {
                // Normal case: start < end (e.g., 09:00 - 17:00)
                !currentTime.isBefore(start) && !currentTime.isAfter(end)
            } else {
                // Overnight case: start > end (e.g., 22:00 - 06:00)
                !currentTime.isBefore(start) || !currentTime.isAfter(end)
            }
        } catch (e: Exception) {
            Timber.w(e, "FilterEngine: Error parsing time window")
            return true // On error, allow the match
        }
    }

    /**
     * Check if SMS SIM slot matches the filter's SIM slot criteria.
     */
    private fun matchesSimSlot(sms: SmsData, filter: ForwardingFilter): Boolean {
        return when (filter.simSlotFilter.uppercase()) {
            "ALL" -> true
            "SIM1" -> sms.simSlot == 0 || sms.simSlot == 1
            "SIM2" -> sms.simSlot == 1 || sms.simSlot == 2
            else -> true
        }
    }

    /**
     * Get contact name for a phone number.
     */
    private fun getContactName(address: String): String? {
        return try {
            contactRepo.getUnmanagedContacts()
                .blockingFirst()
                .find { contact ->
                    contact.numbers.any { PhoneNumberUtils.compare(it.address, address) }
                }
                ?.name
        } catch (e: Exception) {
            Timber.w(e, "FilterEngine: Error getting contact name for $address")
            null
        }
    }
}
