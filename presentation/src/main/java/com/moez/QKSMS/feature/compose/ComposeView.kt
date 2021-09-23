
package com.moez.QKSMS.feature.compose

import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.view.inputmethod.InputContentInfoCompat
import com.moez.QKSMS.common.base.QkView
import com.moez.QKSMS.model.Attachment
import com.moez.QKSMS.model.Contact
import io.reactivex.Observable
import io.reactivex.subjects.Subject

interface ComposeView : QkView<ComposeState> {

    val activityVisibleIntent: Observable<Boolean>
    val queryChangedIntent: Observable<CharSequence>
    val queryBackspaceIntent: Observable<*>
    val queryEditorActionIntent: Observable<Int>
    val chipSelectedIntent: Subject<Contact>
    val chipDeletedIntent: Subject<Contact>
    val menuReadyIntent: Observable<Unit>
    val optionsItemIntent: Observable<Int>
    val sendAsGroupIntent: Observable<*>
    val messageClickIntent: Subject<Long>
    val messagePartClickIntent: Subject<Long>
    val messagesSelectedIntent: Observable<List<Long>>
    val cancelSendingIntent: Subject<Long>
    val attachmentDeletedIntent: Subject<Attachment>
    val textChangedIntent: Observable<CharSequence>
    val attachIntent: Observable<Unit>
    val cameraIntent: Observable<*>
    val galleryIntent: Observable<*>
    val scheduleIntent: Observable<*>
    val attachContactIntent: Observable<*>
    val attachmentSelectedIntent: Observable<Uri>
    val contactSelectedIntent: Observable<Uri>
    val inputContentIntent: Observable<InputContentInfoCompat>
    val scheduleSelectedIntent: Observable<Long>
    val scheduleCancelIntent: Observable<*>
    val changeSimIntent: Observable<*>
    val sendIntent: Observable<Unit>
    val viewQksmsPlusIntent: Subject<Unit>
    val backPressedIntent: Observable<Unit>

    fun clearSelection()
    fun showDetails(details: String)
    fun requestDefaultSms()
    fun requestStoragePermission()
    fun requestSmsPermission()
    fun requestCamera()
    fun requestGallery()
    fun requestDatePicker()
    fun requestContact()
    fun setDraft(draft: String)
    fun scrollToMessage(id: Long)
    fun showQksmsPlusSnackbar(@StringRes message: Int)

}