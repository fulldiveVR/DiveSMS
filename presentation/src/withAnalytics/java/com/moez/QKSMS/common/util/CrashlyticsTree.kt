
package com.moez.QKSMS.common.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class CrashlyticsTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String?, throwable: Throwable?) {
        val crashlytics = FirebaseCrashlytics.getInstance()
        val priorityString = when (priority) {
            Log.VERBOSE -> "V"
            Log.DEBUG -> "D"
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            else -> "WTF"
        }

        crashlytics.log("$priorityString/$tag: $message")
        throwable?.run(crashlytics::recordException)
    }

}
