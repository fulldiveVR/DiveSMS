
package com.moez.QKSMS.interactor

import com.moez.QKSMS.repository.MessageRepository
import io.reactivex.Flowable
import javax.inject.Inject

class SaveImage @Inject constructor(
    private val messageRepo: MessageRepository
) : Interactor<Long>() {

    override fun buildObservable(params: Long): Flowable<*> {
        return Flowable.just(params)
                .doOnNext { partId -> messageRepo.savePart(partId) }
    }

}