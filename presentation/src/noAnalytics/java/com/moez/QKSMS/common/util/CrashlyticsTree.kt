
package com.moez.QKSMS.common.util

import timber.log.Timber
import javax.inject.Inject

class CrashlyticsTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String?, throwable: Throwable?) {
        // Do nothing for noAnalytics build flavor
    }

}
