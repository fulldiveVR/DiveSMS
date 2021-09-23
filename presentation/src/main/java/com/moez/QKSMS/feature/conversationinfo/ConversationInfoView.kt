
package com.moez.QKSMS.feature.conversationinfo

import com.moez.QKSMS.common.base.QkViewContract
import io.reactivex.Observable

interface ConversationInfoView : QkViewContract<ConversationInfoState> {

    fun recipientClicks(): Observable<Long>
    fun nameClicks(): Observable<*>
    fun nameChanges(): Observable<String>
    fun notificationClicks(): Observable<*>
    fun themeClicks(): Observable<*>
    fun archiveClicks(): Observable<*>
    fun blockClicks(): Observable<*>
    fun deleteClicks(): Observable<*>
    fun confirmDelete(): Observable<*>

    fun showNameDialog(name: String)
    fun showThemePicker(threadId: Long)
    fun showBlockingDialog(conversations: List<Long>, block: Boolean)
    fun requestDefaultSms()
    fun showDeleteDialog()

}
