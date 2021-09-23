
package com.moez.QKSMS.interactor

import com.moez.QKSMS.repository.MessageRepository
import io.reactivex.Flowable
import javax.inject.Inject

class MarkAllSeen @Inject constructor(private val messageRepo: MessageRepository) : Interactor<Unit>() {

    override fun buildObservable(params: Unit): Flowable<Unit> {
        return Flowable.just(Unit)
                .doOnNext { messageRepo.markAllSeen() }
    }

}