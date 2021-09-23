
package com.moez.QKSMS.manager

interface AnalyticsManager {

    fun track(event: String, vararg properties: Pair<String, Any>)

    fun setUserProperty(key: String, value: Any)

}