
package com.moez.QKSMS.feature.conversationinfo

import com.moez.QKSMS.model.MmsPart
import com.moez.QKSMS.model.Recipient
import io.realm.RealmList
import io.realm.RealmResults

data class ConversationInfoState(
    val name: String = "",
    val recipients: RealmList<Recipient>? = null,
    val threadId: Long = 0,
    val archived: Boolean = false,
    val blocked: Boolean = false,
    val media: RealmResults<MmsPart>? = null,
    val hasError: Boolean = false
)