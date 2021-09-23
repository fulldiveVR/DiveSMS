
package com.moez.QKSMS.interactor

import com.moez.QKSMS.manager.NotificationManager
import com.moez.QKSMS.repository.ConversationRepository
import com.moez.QKSMS.repository.MessageRepository
import io.reactivex.Flowable
import javax.inject.Inject

class DeleteMessages @Inject constructor(
    private val conversationRepo: ConversationRepository,
    private val messageRepo: MessageRepository,
    private val notificationManager: NotificationManager,
    private val updateBadge: UpdateBadge
) : Interactor<DeleteMessages.Params>() {

    data class Params(val messageIds: List<Long>, val threadId: Long? = null)

    override fun buildObservable(params: Params): Flowable<*> {
        return Flowable.just(params.messageIds.toLongArray())
                .doOnNext { messageIds -> messageRepo.deleteMessages(*messageIds) } // Delete the messages
                .doOnNext {
                    params.threadId?.let { conversationRepo.updateConversations(it) } // Update the conversation
                }
                .doOnNext { params.threadId?.let { notificationManager.update(it) } }
                .flatMap { updateBadge.buildObservable(Unit) } // Update the badge
    }

}