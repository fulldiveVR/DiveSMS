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
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Engine for rendering email templates with SMS data variables.
 */
class TemplateEngine @Inject constructor(
    private val contactRepo: ContactRepository
) {
    /**
     * Rendered email content.
     */
    data class EmailContent(
        val subject: String,
        val body: String,
        val isHtml: Boolean
    )

    companion object {
        const val DEFAULT_SUBJECT = "SMS from {{contact_name}} ({{sender}})"

        val DEFAULT_BODY = """
New message received:
---
From: {{contact_name}} ({{sender}})
Time: {{date}} {{time}}
SIM: {{sim_slot}}

Message:
{{message}}
---

Sent via Wize SMS
        """.trimIndent()

        // Template variables
        const val VAR_SENDER = "{{sender}}"
        const val VAR_MESSAGE = "{{message}}"
        const val VAR_TIME = "{{time}}"
        const val VAR_DATE = "{{date}}"
        const val VAR_CONTACT_NAME = "{{contact_name}}"
        const val VAR_SIM_SLOT = "{{sim_slot}}"
    }

    /**
     * Renders the email template with the given SMS data.
     */
    fun render(filter: ForwardingFilter, sms: FilterEngine.SmsData): EmailContent {
        val variables = buildVariableMap(sms)

        val subject = filter.emailSubjectTemplate.ifBlank { DEFAULT_SUBJECT }
        val body = filter.emailBodyTemplate.ifBlank { DEFAULT_BODY }

        return EmailContent(
            subject = replaceVariables(subject, variables),
            body = replaceVariables(body, variables),
            isHtml = filter.useHtmlTemplate
        )
    }

    /**
     * Renders a preview of the template with sample data.
     */
    fun renderPreview(
        subjectTemplate: String,
        bodyTemplate: String,
        isHtml: Boolean
    ): EmailContent {
        val sampleData = FilterEngine.SmsData(
            sender = "+1-555-123-4567",
            body = "Your verification code is 123456. It expires in 10 minutes.",
            timestamp = System.currentTimeMillis(),
            simSlot = 0
        )

        val variables = buildVariableMap(sampleData)
        variables["contact_name"] = "John Doe" // Use sample contact name for preview

        return EmailContent(
            subject = replaceVariables(subjectTemplate.ifBlank { DEFAULT_SUBJECT }, variables),
            body = replaceVariables(bodyTemplate.ifBlank { DEFAULT_BODY }, variables),
            isHtml = isHtml
        )
    }

    /**
     * Builds a map of template variables from SMS data.
     */
    private fun buildVariableMap(sms: FilterEngine.SmsData): MutableMap<String, String> {
        val contactName = getContactName(sms.sender) ?: sms.sender

        return mutableMapOf(
            "sender" to sms.sender,
            "message" to sms.body,
            "time" to formatTime(sms.timestamp),
            "date" to formatDate(sms.timestamp),
            "contact_name" to contactName,
            "sim_slot" to formatSimSlot(sms.simSlot)
        )
    }

    /**
     * Replaces all template variables in the text with their values.
     */
    private fun replaceVariables(template: String, variables: Map<String, String>): String {
        var result = template

        variables.forEach { (key, value) ->
            result = result.replace("{{$key}}", value)
        }

        return result
    }

    /**
     * Formats timestamp as time string.
     */
    private fun formatTime(timestamp: Long): String {
        return try {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
        } catch (e: Exception) {
            Timber.w(e, "TemplateEngine: Error formatting time")
            ""
        }
    }

    /**
     * Formats timestamp as date string.
     */
    private fun formatDate(timestamp: Long): String {
        return try {
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
        } catch (e: Exception) {
            Timber.w(e, "TemplateEngine: Error formatting date")
            ""
        }
    }

    /**
     * Formats SIM slot as a user-friendly string.
     */
    private fun formatSimSlot(simSlot: Int): String {
        return when {
            simSlot < 0 -> "Unknown"
            simSlot == 0 -> "SIM 1"
            else -> "SIM ${simSlot + 1}"
        }
    }

    /**
     * Gets contact name for a phone number.
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
            Timber.w(e, "TemplateEngine: Error getting contact name for $address")
            null
        }
    }

    /**
     * Returns list of available template variables with descriptions.
     */
    fun getAvailableVariables(): List<Pair<String, String>> {
        return listOf(
            "{{sender}}" to "Phone number of the sender",
            "{{message}}" to "Full message body",
            "{{time}}" to "Time message was received (e.g., 2:34 PM)",
            "{{date}}" to "Date message was received (e.g., Dec 27, 2025)",
            "{{contact_name}}" to "Contact name or phone number if not in contacts",
            "{{sim_slot}}" to "SIM card that received the message (SIM 1 or SIM 2)"
        )
    }
}
