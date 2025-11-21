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
package com.moez.QKSMS.extensions

import com.moez.QKSMS.model.Conversation

/**
 * Extension functions for Conversation model
 */

/**
 * Check if this conversation is a group conversation (2 or more recipients)
 */
fun Conversation.isGroup(): Boolean = recipients.size >= 2

/**
 * Get the number of recipients in this conversation
 */
fun Conversation.recipientCount(): Int = recipients.size

/**
 * Get a short description of the conversation type
 */
fun Conversation.getTypeDescription(): String {
    return when {
        recipients.isEmpty() -> "Empty"
        recipients.size == 1 -> "Direct"
        else -> "Group (${recipients.size} members)"
    }
}

/**
 * Get a formatted member count string
 */
fun Conversation.getMemberCountString(): String {
    return when (val count = recipients.size) {
        0 -> ""
        1 -> "1 recipient"
        else -> "$count recipients"
    }
}

/**
 * Get all recipient addresses as a list
 */
fun Conversation.getRecipientAddresses(): List<String> {
    return recipients.mapNotNull { it.address }
}

/**
 * Get a truncated title with a maximum number of displayed names
 */
fun Conversation.getTruncatedTitle(maxNames: Int = 3): String {
    // Use custom name if set
    if (name.isNotBlank()) {
        return name
    }

    val recipientList = recipients.toList()
    if (recipientList.isEmpty()) {
        return ""
    }

    if (recipientList.size <= maxNames) {
        return recipientList.joinToString(", ") { it.getDisplayName() }
    }

    val displayedNames = recipientList.take(maxNames).joinToString(", ") { it.getDisplayName() }
    val othersCount = recipientList.size - maxNames
    return "$displayedNames +$othersCount"
}
