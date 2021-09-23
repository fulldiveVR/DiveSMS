
package com.moez.QKSMS.interactor

import com.moez.QKSMS.repository.MessageRepository
import io.reactivex.Flowable
import javax.inject.Inject

class CancelDelayedMessage @Inject constructor(private val messageRepo: MessageRepository) : Interactor<Long>() {

    override fun buildObservable(params: Long): Flowable<*> {
        return Flowable.just(params)
                .doOnNext { id -> messageRepo.cancelDelayedSms(id) }
                .doOnNext { id -> messageRepo.deleteMessages(id) }
    }

}