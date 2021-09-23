
package com.moez.QKSMS.interactor

import com.moez.QKSMS.repository.ConversationRepository
import io.reactivex.Flowable
import javax.inject.Inject

class MarkBlocked @Inject constructor(
    private val conversationRepo: ConversationRepository,
    private val markRead: MarkRead
) : Interactor<MarkBlocked.Params>() {

    data class Params(val threadIds: List<Long>, val blockingClient: Int, val blockReason: String?)

    override fun buildObservable(params: Params): Flowable<*> {
        return Flowable.just(params)
                .doOnNext { (threadIds, blockingClient, blockReason) ->
                    conversationRepo.markBlocked(threadIds, blockingClient, blockReason)
                }
                .flatMap { (threadIds) -> markRead.buildObservable(threadIds) }
    }

}
