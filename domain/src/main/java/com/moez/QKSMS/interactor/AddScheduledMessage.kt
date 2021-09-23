
package com.moez.QKSMS.interactor

import com.moez.QKSMS.repository.ScheduledMessageRepository
import io.reactivex.Flowable
import javax.inject.Inject

class AddScheduledMessage @Inject constructor(
    private val scheduledMessageRepo: ScheduledMessageRepository,
    private val updateScheduledMessageAlarms: UpdateScheduledMessageAlarms
) : Interactor<AddScheduledMessage.Params>() {

    data class Params(
        val date: Long,
        val subId: Int,
        val recipients: List<String>,
        val sendAsGroup: Boolean,
        val body: String,
        val attachments: List<String>
    )

    override fun buildObservable(params: Params): Flowable<*> {
        return Flowable.just(params)
                .map {
                    scheduledMessageRepo.saveScheduledMessage(it.date, it.subId, it.recipients, it.sendAsGroup, it.body,
                            it.attachments)
                }
                .flatMap { updateScheduledMessageAlarms.buildObservable(Unit) }
    }

}