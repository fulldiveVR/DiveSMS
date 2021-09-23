
package com.moez.QKSMS.feature.compose.part

import android.view.View
import com.moez.QKSMS.common.util.Colors
import com.moez.QKSMS.model.Message
import com.moez.QKSMS.model.MmsPart
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

abstract class PartBinder {

    val clicks: Subject<Long> = PublishSubject.create()

    abstract val partLayout: Int

    abstract var theme: Colors.Theme

    abstract fun canBindPart(part: MmsPart): Boolean

    abstract fun bindPart(
        view: View,
        part: MmsPart,
        message: Message,
        canGroupWithPrevious: Boolean,
        canGroupWithNext: Boolean
    )

}
