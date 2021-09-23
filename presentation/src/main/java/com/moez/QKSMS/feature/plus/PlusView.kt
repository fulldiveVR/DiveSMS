
package com.moez.QKSMS.feature.plus

import com.moez.QKSMS.common.base.QkView
import com.moez.QKSMS.common.util.BillingManager
import io.reactivex.Observable

interface PlusView : QkView<PlusState> {

    val upgradeIntent: Observable<Unit>
    val upgradeDonateIntent: Observable<Unit>
    val donateIntent: Observable<*>
    val themeClicks: Observable<*>
    val scheduleClicks: Observable<*>
    val backupClicks: Observable<*>
    val delayedClicks: Observable<*>
    val nightClicks: Observable<*>

    fun initiatePurchaseFlow(billingManager: BillingManager, sku: String)

}