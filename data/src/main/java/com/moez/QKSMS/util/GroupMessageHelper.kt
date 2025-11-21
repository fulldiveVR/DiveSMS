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
import com.moez.QKSMS.model.Conversation
import com.moez.QKSMS.model.Message
import com.moez.QKSMS.model.Recipient
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for group messaging functionality.
 *
 * Group messaging (MMS group chat) has the following characteristics:
 * - Multiple recipients in a single conversation
 * - Messages are sent as MMS, not SMS
 * - Each message shows the sender (for messages from others)
 * - All recipients receive all messages
 */
@Singleton
class GroupMessageHelper @Inject constructor(
    private val context: Context,
    private val phoneNumberUtils: PhoneNumberUtils
) {

    companion object {
        // Minimum recipients for a group conversation
        const val MIN_GROUP_SIZE = 2
    }

    /**
     * Check if a conversation is a group conversation
     */
    fun isGroupConversation(conversation: Conversation?): Boolean {
        return (conversation?.recipients?.size ?: 0) >= MIN_GROUP_SIZE
    }

    /**
     * Check if a conversation is a group conversation based on recipient count
     */
    fun isGroupConversation(recipientCount: Int): Boolean {
        return recipientCount >= MIN_GROUP_SIZE
    }

    /**
     * Check if addresses represent a group conversation
     */
    fun isGroupConversation(addresses: List<String>): Boolean {
        return addresses.size >= MIN_GROUP_SIZE
    }

    /**
     * Get the display name for a sender in a group conversation
     * Returns the contact name if available, otherwise the phone number
     */
    fun getSenderDisplayName(conversation: Conversation, senderAddress: String): String {
        // Find the recipient matching this address
        val recipient = conversation.recipients.find { recipient ->
            phoneNumberUtils.compare(recipient.address, senderAddress)
        }

        return recipient?.getDisplayName() ?: senderAddress
    }

    /**
     * Get the sender for a message in a group conversation
     * Returns null if the message was sent by the user
     */
    fun getMessageSender(conversation: Conversation, message: Message): Recipient? {
        if (message.isMe()) {
            return null
        }

        return conversation.recipients.find { recipient ->
            phoneNumberUtils.compare(recipient.address, message.address)
        }
    }

    /**
     * Get a formatted title for a group conversation
     * - If the conversation has a custom name, use it
     * - Otherwise, format as "Name1, Name2, +N others"
     */
    fun getGroupTitle(conversation: Conversation, maxDisplayNames: Int = 3): String {
        // Use custom name if set
        if (conversation.name.isNotBlank()) {
            return conversation.name
        }

        val recipients = conversation.recipients
        if (recipients.isEmpty()) {
            return ""
        }

        if (recipients.size <= maxDisplayNames) {
            return recipients.joinToString(", ") { it.getDisplayName() }
        }

        // Show first N names + "and X others"
        val displayedNames = recipients.take(maxDisplayNames).joinToString(", ") { it.getDisplayName() }
        val othersCount = recipients.size - maxDisplayNames
        return "$displayedNames +$othersCount"
    }

    /**
     * Get the member count string for a group conversation
     */
    fun getMemberCountString(conversation: Conversation): String {
        val count = conversation.recipients.size
        return when {
            count == 0 -> ""
            count == 1 -> "1 member"
            else -> "$count members"
        }
    }

    /**
     * Get all recipient addresses from a conversation
     */
    fun getRecipientAddresses(conversation: Conversation): List<String> {
        return conversation.recipients.mapNotNull { it.address }
    }

    /**
     * Check if a message should show the sender name
     * (In group conversations, incoming messages should show sender)
     */
    fun shouldShowSenderName(conversation: Conversation, message: Message): Boolean {
        return isGroupConversation(conversation) && !message.isMe()
    }

    /**
     * Generate a default group name from recipients
     */
    fun generateDefaultGroupName(recipients: List<Recipient>): String {
        return when {
            recipients.isEmpty() -> "Group"
            recipients.size == 1 -> recipients[0].getDisplayName()
            recipients.size == 2 -> "${recipients[0].getDisplayName()} & ${recipients[1].getDisplayName()}"
            else -> {
                val firstName = recipients[0].getDisplayName()
                val othersCount = recipients.size - 1
                "$firstName & $othersCount others"
            }
        }
    }

    /**
     * Log group message information for debugging
     */
    fun logGroupMessageInfo(conversation: Conversation, message: Message) {
        if (!isGroupConversation(conversation)) {
            return
        }

        Timber.d(
            "Group message: " +
                    "\n  Thread ID: ${conversation.id}" +
                    "\n  Recipients: ${conversation.recipients.size}" +
                    "\n  Sender: ${message.address}" +
                    "\n  Is from me: ${message.isMe()}" +
                    "\n  Type: ${message.type}"
        )
    }

    /**
     * Validate that a group MMS can be sent
     * Returns error message if invalid, null if valid
     */
    fun validateGroupMms(addresses: List<String>, hasAttachments: Boolean): String? {
        if (addresses.isEmpty()) {
            return "No recipients specified"
        }

        // Group MMS requires at least 2 recipients OR attachments
        // Single recipient with attachment is also MMS
        if (addresses.size == 1 && !hasAttachments) {
            // This would be SMS, not MMS - that's fine
            return null
        }

        // Check for valid addresses
        val invalidAddresses = addresses.filter { it.isBlank() }
        if (invalidAddresses.isNotEmpty()) {
            return "One or more recipients have invalid addresses"
        }

        return null
    }
}
