
package com.moez.QKSMS.compat

import android.annotation.TargetApi
import android.os.Build
import android.telephony.SubscriptionInfo

@TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
data class SubscriptionInfoCompat(private val subscriptionInfo: SubscriptionInfo) {

    val subscriptionId get() = subscriptionInfo.subscriptionId

    val simSlotIndex get() = subscriptionInfo.simSlotIndex

    val displayName get() = subscriptionInfo.displayName

}