
package com.moez.QKSMS.interactor

import com.moez.QKSMS.repository.ConversationRepository
import io.reactivex.Flowable
import javax.inject.Inject

class MarkUnarchived @Inject constructor(private val conversationRepo: ConversationRepository) : Interactor<List<Long>>() {

    override fun buildObservable(params: List<Long>): Flowable<*> {
        return Flowable.just(params.toLongArray())
                .doOnNext { threadIds -> conversationRepo.markUnarchived(*threadIds) }
    }

}