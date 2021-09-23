
package com.moez.QKSMS.feature.qkreply

import com.moez.QKSMS.compat.SubscriptionInfoCompat
import com.moez.QKSMS.model.Conversation
import com.moez.QKSMS.model.Message
import io.realm.RealmResults

data class QkReplyState(
    val hasError: Boolean = false,
    val selectedConversation: Long = 0,
    val title: String = "",
    val expanded: Boolean = false,
    val data: Pair<Conversation, RealmResults<Message>>? = null,
    val remaining: String = "",
    val subscription: SubscriptionInfoCompat? = null,
    val canSend: Boolean = false
)