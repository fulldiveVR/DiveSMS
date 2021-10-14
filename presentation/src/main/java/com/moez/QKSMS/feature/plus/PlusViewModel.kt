
package com.moez.QKSMS.feature.plus

import com.moez.QKSMS.common.Navigator
import com.moez.QKSMS.common.base.QkViewModel
import com.moez.QKSMS.common.util.BillingManager
import com.moez.QKSMS.manager.AnalyticsManager
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import io.reactivex.Observable
import io.reactivex.rxkotlin.plusAssign
import javax.inject.Inject

class PlusViewModel @Inject constructor(
    private val analyticsManager: AnalyticsManager,
    private val billingManager: BillingManager,
    private val navigator: Navigator
) : QkViewModel<PlusView, PlusState>(PlusState()) {


}