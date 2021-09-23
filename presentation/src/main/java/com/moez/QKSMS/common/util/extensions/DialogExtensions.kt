
package com.moez.QKSMS.common.util.extensions

import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import io.reactivex.subjects.Subject

fun AlertDialog.Builder.setPositiveButton(@StringRes textId: Int, subject: Subject<Unit>): AlertDialog.Builder {
    return setPositiveButton(textId) { _, _ -> subject.onNext(Unit) }
}

fun AlertDialog.Builder.setNegativeButton(@StringRes textId: Int, subject: Subject<Unit>): AlertDialog.Builder {
    return setNegativeButton(textId) { _, _ -> subject.onNext(Unit) }
}

fun AlertDialog.Builder.setNeutralButton(@StringRes textId: Int, subject: Subject<Unit>): AlertDialog.Builder {
    return setNeutralButton(textId) { _, _ -> subject.onNext(Unit) }
}