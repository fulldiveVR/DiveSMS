
package com.moez.QKSMS.feature.blocking.numbers

import com.moez.QKSMS.common.base.QkPresenter
import com.moez.QKSMS.interactor.MarkUnblocked
import com.moez.QKSMS.repository.BlockingRepository
import com.moez.QKSMS.repository.ConversationRepository
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class BlockedNumbersPresenter @Inject constructor(
    private val blockingRepo: BlockingRepository,
    private val conversationRepo: ConversationRepository,
    private val markUnblocked: MarkUnblocked
) : QkPresenter<BlockedNumbersView, BlockedNumbersState>(
        BlockedNumbersState(numbers = blockingRepo.getBlockedNumbers())
) {

    override fun bindIntents(view: BlockedNumbersView) {
        super.bindIntents(view)

        view.unblockAddress()
                .doOnNext { id ->
                    blockingRepo.getBlockedNumber(id)?.address
                            ?.let(conversationRepo::getThreadId)
                            ?.let { threadId -> markUnblocked.execute(listOf(threadId)) }
                }
                .doOnNext(blockingRepo::unblockNumber)
                .subscribeOn(Schedulers.io())
                .autoDisposable(view.scope())
                .subscribe()

        view.addAddress()
                .autoDisposable(view.scope())
                .subscribe { view.showAddDialog() }

        view.saveAddress()
                .subscribeOn(Schedulers.io())
                .autoDisposable(view.scope())
                .subscribe { address -> blockingRepo.blockNumber(address) }
    }

}
