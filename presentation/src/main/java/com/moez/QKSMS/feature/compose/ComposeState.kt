
package com.moez.QKSMS.feature.compose

import com.moez.QKSMS.compat.SubscriptionInfoCompat
import com.moez.QKSMS.model.Attachment
import com.moez.QKSMS.model.Contact
import com.moez.QKSMS.model.Conversation
import com.moez.QKSMS.model.Message
import io.realm.RealmResults

data class ComposeState(
    val hasError: Boolean = false,
    val editingMode: Boolean = false,
    val contacts: List<Contact> = ArrayList(),
    val contactsVisible: Boolean = false,
    val selectedConversation: Long = 0,
    val selectedContacts: List<Contact> = ArrayList(),
    val sendAsGroup: Boolean = true,
    val conversationtitle: String = "",
    val loading: Boolean = false,
    val query: String = "",
    val searchSelectionId: Long = -1,
    val searchSelectionPosition: Int = 0,
    val searchResults: Int = 0,
    val messages: Pair<Conversation, RealmResults<Message>>? = null,
    val selectedMessages: Int = 0,
    val scheduled: Long = 0,
    val attachments: List<Attachment> = ArrayList(),
    val attaching: Boolean = false,
    val remaining: String = "",
    val subscription: SubscriptionInfoCompat? = null,
    val canSend: Boolean = false
)